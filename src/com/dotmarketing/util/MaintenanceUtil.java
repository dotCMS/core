package com.dotmarketing.util;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.hibernate.HibernateException;

import org.apache.commons.io.FileUtils;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.util.FileUtil;

public class MaintenanceUtil {

	private static final FileAPI fileAPI = APILocator.getFileAPI();

	/**
	 * Use to delete the velocity static store
	 */
	public static void deleteStaticFileStore(){
		deleteStaticFileStore(false);
		deleteStaticFileStore(true);
	}

	/**
	 * Use to delete the velocity static store either working or live
	 * @param live
	 */
	public static void deleteStaticFileStore(boolean live){
		String realPath = "";

		String velocityRootPath = ConfigUtils.getDynamicVelocityPath();

		if(live){
			realPath = velocityRootPath + File.separator + "live";
		}
		else{
			realPath = velocityRootPath + File.separator + "working";
		}
		File file = new File(realPath);
		if (file.isDirectory())	{
			FileUtil.deltree(file, false);
			Logger.debug(MaintenanceUtil.class,"The directory " + realPath + " has been deleted");
		}else{
			Logger.error(MaintenanceUtil.class,file.getPath() + " is not a directory");
		}
	}

	public static void deleteMenuCache(){
		String velocityRootPath =ConfigUtils.getDynamicVelocityPath();
		File file = new File(velocityRootPath + File.separator + "menus");
		if (file.isDirectory())	{
			FileUtil.deltree(file, false);
			Logger.debug(MaintenanceUtil.class,"The directory " + file.getPath() + " has been deleted");
		}else{
			Logger.error(MaintenanceUtil.class,file.getPath() + " is not a directory");
		}
	}

	public static int cleanTreeTable(){
		final String countSQL = "select count(*) as count from tree t";
		final String deleteFromSQL ="delete from tree where child not in (select i.inode from inode i)";
		final String deleteParentFromSQL ="delete from tree where parent not in (select i.inode from inode i)";
		DotConnect dc = new DotConnect();
		dc.setSQL(countSQL);
		List<HashMap<String, String>> result =null;
		int before = 0;
		try {
			result = dc.getResults();
			before = Integer.parseInt(result.get(0).get("count"));
			dc.setSQL(deleteFromSQL);
			dc.getResult();
			dc.setSQL(deleteParentFromSQL);
			dc.getResult();
			dc.setSQL(countSQL);
			result = dc.getResults();
		} catch (Exception e) {
			Logger.error(MaintenanceUtil.class, e.getMessage(), e);
		}
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;
	}

	public static int cleanMultiTreeTable(){
		final String countSQL = "select count(*) as count from multi_tree t";
		final String deleteChildFromSQL ="delete from multi_tree where child not in (select i.inode from inode i)";
		final String deleteParent1FromSQL ="delete from multi_tree where parent1 not in (select i.inode from inode i)";
		final String deleteParent2FromSQL ="delete from multi_tree where parent2 not in (select i.inode from inode i)";
		DotConnect dc = new DotConnect();
		dc.setSQL(countSQL);
		List<HashMap<String, String>> result=null;
		int before = 0;
		try {
			result = dc.getResults();
			before = Integer.parseInt(result.get(0).get("count"));
			dc.setSQL(deleteChildFromSQL);
			dc.getResult();
			dc.setSQL(deleteParent1FromSQL);
			dc.getResult();
			dc.setSQL(deleteParent2FromSQL);
			dc.getResult();
			dc.setSQL(countSQL);
			result = dc.getResults();
		} catch (Exception e) {
			Logger.error(MaintenanceUtil.class, e.getMessage(), e);
		}
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;
	}

	public static int deleteAllAssetsWithNoIdentifier(){
		int count = 0;
		count += deleteContentletsWithNoIdentifier();
		count += deleteContainersWithNoIdentifier();
		count += deleteLinksWithNoIdentifier();
		count += deleteHTMLPagesWithNoIdentifier();
		count += deleteTemplatesWithNoIdentifier();
		count += deleteFileAssetsWithNoIdentifier();
		return count;
	}

	public static void cleanInodeTableData()throws DotDataException{
		Map map = new HashMap();
		try {
			map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();
		} catch (HibernateException e) {
			throw new DotDataException(e.getMessage(),e);
		}
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			Class x = (Class) pairs.getKey();
			if (!x.equals(Inode.class)){
				Object o;
				try {
					o = x.newInstance();
				} catch (Exception e) {
					Logger.info(MaintenanceUtil.class, "Unable to instaniate object");
					Logger.debug(MaintenanceUtil.class,"Unable to instaniate object", e);
					continue;
				}
				if(o instanceof Inode){
					Inode i = (Inode)o;
					String type = i.getType();
					String tableName = ((net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
					cleanInodeTableData(tableName, type);
				}
			}
		}
		it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			Class x = (Class) pairs.getKey();
			if (!x.equals(Inode.class)){
				Object o;
				try {
					o = x.newInstance();
				} catch (Exception e) {
					Logger.info(MaintenanceUtil.class,"Unable to instaniate object");
					Logger.debug(MaintenanceUtil.class,"Unable to instaniate object", e);
					continue;
				}
				if(o instanceof Inode){
					Inode i = (Inode)o;
					String type = i.getType();
					String tableName = ((net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
					removeOphanedInodes(tableName, type);
				}
			}
		}
	}

	/**
	 * Will update inode type column in db
	 * @return
	 */
	public static void cleanInodeTableData(String tableName, String inodeType){
		String sql = "update inode set type = ? where inode in " +
					 "(select inode from " + tableName + ") and type <> ?";
		String inodesToDeleteSql = "select inode from inode where type = ? and inode not in " +
									"(select inode from " + tableName + ")";
		String deleteSQL = "delete from inode where type = ? and inode not in " +
							"(select inode from " + tableName + ")";
		DotConnect dc = new DotConnect();
		dc.setSQL(sql);
		dc.addParam(inodeType);
		dc.addParam(inodeType);
		dc.getResult();
	}

	protected static int cleanPermissionReferences(List<String> inodes, int offset) {
		final String BASE_DELETE_ASSETID="delete from permission where inode_id in (";
		DotConnect dc = new DotConnect();
		StringBuffer deletePermissionAssetConSQL = new StringBuffer(BASE_DELETE_ASSETID);
		int i = 0;
		List<String> inodesAux =  new ArrayList<String>();
		boolean first = true;
		for (String inode : inodes) {
			inodesAux.add(inode);
			if(!first){
				deletePermissionAssetConSQL.append("," +"'"+ inode+"'");
			}else{
				deletePermissionAssetConSQL.append("'"+inode+"'");
			}
			first = false;
			 if((i % offset) == 0 && i != 0)
			 {
				 deletePermissionAssetConSQL.append(")");
				 dc.setSQL(deletePermissionAssetConSQL.toString());
				 dc.getResult();
				 deletePermissionAssetConSQL  = new StringBuffer(BASE_DELETE_ASSETID);
				 inodesAux.clear();
				 first = true;
			 }
			 i++;
		}


		if(!(inodes.size() % offset == 0) && inodesAux.size()>0)
		{
		  deletePermissionAssetConSQL.append(")");
		  dc.setSQL(deletePermissionAssetConSQL.toString());
		  dc.getResult();
		}
		return 0;
	}
	
	/**
	 * Will update inode type column in db
	 * @return
	 */
	public static void removeOphanedInodes(String tableName, String inodeType){
		String inodesToDeleteSql = "select inode from inode where type = ? and inode not in " +
									"(select inode from " + tableName + ")";
		String deleteSQL = "delete from inode where type = ? and inode not in " +
							"(select inode from " + tableName + ")";
		DotConnect dc = new DotConnect();

		dc.setSQL(inodesToDeleteSql);
		dc.addParam(inodeType);
		List<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(MaintenanceUtil.class,e.getMessage(), e);
		}
		List<String> inodesToDelete = new ArrayList<String>();
		for (HashMap<String, String> r : results) {
			inodesToDelete.add(r.get("inode"));
		}

		cleanInodesFromTree(inodesToDelete, 500);
		cleanPermissionReferences(inodesToDelete, 500);

		dc.setSQL(deleteSQL);
		dc.addParam(inodeType);
		if(tableName.contains("event")){
			Logger.info(MaintenanceUtil.class,inodeType);
		}
		dc.getResult();
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteContentletsWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("contentlet");
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteContainersWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("containers");
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteFileAssetsWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("file_asset");
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteHTMLPagesWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("htmlpage");
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteLinksWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("links");
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	public static int deleteTemplatesWithNoIdentifier(){
		return deleteAssetsWithNoIdentifier("template");
	}

	/**
	 * Will delete all content where the structure =0 or the structure doesn't exist
	 */
	public static int deleteContentletsWithNoStructure(){
		final String countSQL = "select count(*) as count from contentlet t";
		String selectSQL = "select inode from contentlet where structure_inode not in (select inode from structure)";
		DotConnect dc = new DotConnect();
		dc.setSQL(countSQL);
		List<HashMap<String, String>> result =null;
		int before = 0;
		try {
			result = dc.getResults();
			before = Integer.parseInt(result.get(0).get("count"));
			dc.setSQL(selectSQL);
			List<HashMap<String, String>> results =  dc.getResults();
			List<String> inodesToClean = new ArrayList<String>();
			boolean runDelete = false;
			for (HashMap<String, String> r : results) {
				inodesToClean.add(r.get("inode"));
				runDelete = true;
			}
			if(runDelete){
				deleteAssets(inodesToClean, "contentlet", 500);
			}
			dc.setSQL(countSQL);
			result = dc.getResults();
		} catch (Exception e) {
			Logger.error(MaintenanceUtil.class,e.getMessage(), e);
		}
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;
	}

	public static void flushCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.flushAll();
	}

	public static void deleteAssets(List<String> inodes, String tableName, int offset){
		if(inodes == null || inodes.size() < 1){
			return;
		}
		DotConnect dc = new DotConnect();
		StringBuffer deleteInodeSQL = new StringBuffer("delete from inode where inode in (");
		StringBuffer deleteConSQL = new StringBuffer("delete from " + tableName + " where inode in (");
		List<String> inodesAux =  new ArrayList<String>();
		int i = 0;
		boolean first = true;
		for (String inode : inodes) {
			inodesAux.add(inode);
			if(!first){
				deleteInodeSQL.append("," +"'"+ inode+"'");
				deleteConSQL.append("," + "'"+inode+"'");
			}else{
				deleteInodeSQL.append("'"+inode+ "'");
				deleteConSQL.append("'"+inode+ "'");
			}
			first = false;

		 if((i % offset) == 0 && i != 0)
		 {
			 deleteInodeSQL.append(")");
			 deleteConSQL.append(")");
			 dc.setSQL(deleteConSQL.toString());
			 dc.getResult();
			 cleanInodesFromTree(inodes, offset);
			 dc.setSQL(deleteInodeSQL.toString());
			 dc.getResult();
			 deleteInodeSQL = new StringBuffer("delete from inode where inode in (");
			 deleteConSQL = new StringBuffer("delete from " + tableName + " where inode in (");
			 inodesAux.clear();
			 first = true;
		 }
		 i++;

		}
		if(!(inodes.size() % offset == 0) && inodesAux.size()>0)
		{
			deleteInodeSQL.append(")");
			deleteConSQL.append(")");
			dc.setSQL(deleteConSQL.toString());
			dc.getResult();
			cleanInodesFromTree(inodes, offset);
			dc.setSQL(deleteInodeSQL.toString());
			dc.getResult();
		}
	}

	/**
	 * Use to delete rows from tree containing a collection of inodes.
	 * Usually need to use this to delete tree records because of the foreign key to inode
	 * @param inodes
	 */
	public static void cleanInodesFromTree(List<String> inodes, int offset){
		DotConnect dc = new DotConnect();
		StringBuffer deleteTreeChildConSQL = new StringBuffer("delete from tree where child in (");
		StringBuffer deleteTreeParentConSQL = new StringBuffer("delete from tree where parent in (");
		int i = 0;
		List<String> inodesAux =  new ArrayList<String>();
		boolean first = true;
		for (String inode : inodes) {
			inodesAux.add(inode);
			if(!first){
				deleteTreeChildConSQL.append("," +"'"+ inode+"'");
				deleteTreeParentConSQL.append("," +"'"+inode+"'");
			}else{
				deleteTreeChildConSQL.append("'"+inode+"'");
				deleteTreeParentConSQL.append("'"+inode+"'");
			}
			first = false;
			 if((i % offset) == 0 && i != 0)
			 {
				 deleteTreeChildConSQL.append(")");
				 deleteTreeParentConSQL.append(")");
				 dc.setSQL(deleteTreeChildConSQL.toString());
				 dc.getResult();
				 dc.setSQL(deleteTreeParentConSQL.toString());
				 dc.getResult();
				 deleteTreeChildConSQL  = new StringBuffer("delete from tree where child in (");
				 deleteTreeParentConSQL = new StringBuffer("delete from tree where parent in (");
				 inodesAux.clear();
				 first = true;
			 }
			 i++;
		}


		if(!(inodes.size() % offset == 0) && inodesAux.size()>0)
		{
		  deleteTreeChildConSQL.append(")");
		  deleteTreeParentConSQL.append(")");
		  dc.setSQL(deleteTreeChildConSQL.toString());
		  dc.getResult();
		  dc.setSQL(deleteTreeParentConSQL.toString());
		  dc.getResult();
		}
	}

	/**
	 * Will replace text in the DB within the following tables.
	 * NOTE: This is intended to replace code so we only go after columns/tables that could have HTML or Velocity code in them.
	 * Contentlet - text and text_area columns
	 * Containers - code,pre_loop and post_loop
	 * Template - body
	 * Field - field_values for the widget code
	 * Link - url
	 *
	 * @param textToSearchFor - Cannot be NULL or Empty String
	 * @param textToReplaceWith - Cannot be NULL
	 *
	 * @return boolean if the method found errors.  It will catch DB errors so that the replace will run everywhere it can.
	 */
	public static boolean DBSearchAndReplace(String textToSearchFor, String textToReplaceWith){
		boolean hasErros = false;
		if(!UtilMethods.isSet(textToSearchFor)){
			Logger.info(MaintenanceUtil.class, "Returning because text to search for is null or empty");
		}
		if(textToReplaceWith == null){
			Logger.info(MaintenanceUtil.class, "Returning because text to replace is null");
		}
		DotConnect dc = new DotConnect();
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE COLUMNS text[1-25] ON THE CONTENTLET TABLE");
		int count = 1;		
		StringBuilder SQL = new StringBuilder("UPDATE contentlet SET ");
		while(count<26){
			if(count>1){
				SQL.append(",");
			}
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				SQL.append("text" + count + " = replace(cast(text" + count + " as varchar(max)),?,?)");
			}else{
				SQL.append("text" + count + " = replace(text" + count + ",?,?)");
			}
			count++;
		}

		dc.setSQL(SQL.toString() + " WHERE contentlet.inode = (SELECT working_inode FROM contentlet_version_info cvi WHERE (cvi.working_inode = contentlet.inode OR cvi.live_inode =contentlet.inode)) ");
		count = 1;
		while(count<26){
			dc.addParam(textToSearchFor);
			dc.addParam(textToReplaceWith);
			count++;
		}
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating contentlet table : " + e.getMessage(),e);
		}

		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE COLUMNS text_area[1-25] ON THE CONTENTLET TABLE");
		count = 1;
		SQL = new StringBuilder("UPDATE contentlet SET ");
		while(count<26){
			if(count>1){
				SQL.append(",");
			}
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				SQL.append("text_area" + count + " = replace(cast(text_area" + count + " as varchar(max)),?,?)");
			}else{
				SQL.append("text_area" + count + " = replace(text_area" + count + ",?,?)");
			}
			count++;
		}

		dc.setSQL(SQL.toString() + "WHERE contentlet.inode = (SELECT working_inode FROM contentlet_version_info cvi WHERE (cvi.working_inode = contentlet.inode OR cvi.live_inode =contentlet.inode)) ");
		count = 1;
		while(count<26){
			dc.addParam(textToSearchFor);
			dc.addParam(textToReplaceWith);
			count++;
		}
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating contentlet table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE COLUMNS code, pre_loop, and post_loop ON THE containers TABLE");
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dc.setSQL("UPDATE containers SET code=replace(cast(code as varchar(max)),?,?),pre_loop=replace(cast(pre_loop as varchar(max)),?,?),post_loop=replace(cast(post_loop as varchar(max)),?,?) WHERE containers.inode = (SELECT working_inode FROM container_version_info cvi WHERE (cvi.working_inode = containers.inode OR cvi.live_inode =containers.inode)) ");
		}else{
			dc.setSQL("UPDATE containers SET code=replace(code,?,?),pre_loop=replace(pre_loop,?,?),post_loop=replace(post_loop,?,?) WHERE containers.inode = (SELECT working_inode FROM container_version_info cvi WHERE (cvi.working_inode = containers.inode OR cvi.live_inode =containers.inode)) ");
		}
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating containers table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE body COLUMN ON THE template TABLE");
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dc.setSQL("UPDATE template SET body=replace(cast(body as varchar(max)),?,?) WHERE template.inode = (SELECT working_inode FROM template_version_info tvi WHERE (tvi.working_inode = template.inode OR tvi.live_inode = template.inode)) ");
		}else{
			dc.setSQL("UPDATE template SET body=replace(body,?,?) WHERE template.inode = (SELECT working_inode FROM template_version_info tvi WHERE (tvi.working_inode = template.inode OR tvi.live_inode = template.inode)) ");
		}
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating template table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE field_values COLUMN ON THE field TABLE");
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dc.setSQL("UPDATE field SET field_values=replace(cast(field_values as varchar(max)),?,?)");
		}else{
			dc.setSQL("UPDATE field SET field_values=replace(field_values,?,?)");
		}
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating field table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE url COLUMN ON THE links TABLE");
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dc.setSQL("UPDATE links SET url=replace(cast(url as varchar(max)),?,?) WHERE links.inode = (SELECT working_inode FROM link_version_info lvi WHERE (lvi.working_inode = links.inode OR lvi.live_inode = links.inode)) ");
		}else{
			dc.setSQL("UPDATE links SET url=replace(url,?,?) WHERE links.inode = (SELECT working_inode FROM link_version_info lvi WHERE (lvi.working_inode = links.inode OR lvi.live_inode = links.inode)) ");
		}
		dc.addParam(textToSearchFor);
		dc.addParam(textToReplaceWith);
		try{
			dc.loadResult();
		} catch (DotDataException e) {
			hasErros = true;
			Logger.error(MaintenanceUtil.class,"Problem updating links table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "Finished Updating DB");
		return hasErros;
	}

	/**
	 * Will find all working/live Text in the DB and Search/Replace them. so the mimetype in db would need to be text/...
	 * @param textToSearchFor
	 * @param textToReplaceWith
	 * @return boolean if there is an error found while running
	 */
	public static boolean textAssetsSearchAndReplace(String textToSearchFor, String textToReplaceWith){
		return fileAssetSearchAndReplace(textToSearchFor, textToReplaceWith, "text");
	}

	/**
	 * Will find all working/live VTLs in the DB and Search/Replace them
	 * @param textToSearchFor
	 * @param textToReplaceWith
	 * @return boolean if there is an error found while running
	 */
	public static boolean VTLSearchAndReplace(String textToSearchFor, String textToReplaceWith){
		return fileAssetSearchAndReplace(textToSearchFor, textToReplaceWith, "text/velocity");
	}

	/**
	 * Will pull only working/live from the DB to search/replace on the FS
	 * @param textToSearchFor
	 * @param textToReplaceWith
	 * @param mimeTypesOfFile Will search for things like text/Velocity in the DB. It will put a % after what you pass so text would pass text% in the like to the DB
	 * @return
	 */
	private static boolean fileAssetSearchAndReplace(String textToSearchFor, String textToReplaceWith,String mimeTypesOfFile){
		Logger.info(MaintenanceUtil.class, "Starting Search and Replace");
		boolean hasErrors = false;
		DotConnect dc = new DotConnect();
		dc.setSQL("SELECT inode,file_name FROM file_asset fa, fileasset_version_info fvi WHERE mime_type LIKE '"+ mimeTypesOfFile +"%' AND (fa.inode = fvi.working_inode OR fa.inode = fvi.live_inode)");
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		try {
			results = dc.loadResults();
		} catch (DotDataException e) {
			Logger.error(MaintenanceUtil.class,"Unable to pull files from DB to search for on filesystem : " + e.getMessage(),e);
			hasErrors = true;
			Logger.info(MaintenanceUtil.class, "Finished Search and Replace With Errors");
			return true;
		}
		File f = null;
		String s = null;
		for (Map<String,Object> result : results) {
			if(!UtilMethods.isSet(result.get("inode").toString())){
				hasErrors = true;
				Logger.error(MaintenanceUtil.class, "Empty or null file inode found");
				continue;
			}
			try{
				f = new File(fileAPI.getRealAssetPath(result.get("inode").toString(), UtilMethods.getFileExtension(result.get("file_name").toString())));
			}catch (Exception e) {
				hasErrors = true;
				Logger.error(MaintenanceUtil.class, "Unable to load the file with inode " + result.get("inode").toString() + " : " + e.getMessage(),e);
				f=null;
				continue;
			}
			if(!f.exists() || f.length() > 1310712000){
				hasErrors = true;
				Logger.error(MaintenanceUtil.class, "Unable to load the file with inode " + result.get("inode").toString());
				f=null;
				continue;
			}
			try{
				s = FileUtils.readFileToString(f,"UTF-8");
				s = s.replace(textToSearchFor, textToReplaceWith);
				FileUtils.writeStringToFile(f, s,"UTF-8");
			}catch (Exception e) {
				hasErrors = true;
				Logger.error(MaintenanceUtil.class, "Unable to replace file contents for " + f.getPath());
			}
			s = null;
			f = null;
		}
		Logger.info(MaintenanceUtil.class, "Finished Search and Replace");
		return hasErrors;
	}

	/**
	 * Delete from db all content with no identifier.  It will try to update identifiers from tree table first though
	 */
	private static int deleteAssetsWithNoIdentifier(String tableNameOfAsset){
		final String countSQL = "select count(*) as count from " + tableNameOfAsset + " t";
		final String selectNullIdentsSQL = "select inode from " + tableNameOfAsset + " where identifier IS NULL";

		DotConnect dc = new DotConnect();
		dc.setSQL(countSQL);
		List<HashMap<String, String>> result =null;
		int before = 0;
		try {
			result = dc.getResults();
			before = Integer.parseInt(result.get(0).get("count"));
			dc.setSQL(selectNullIdentsSQL);
			List<HashMap<String, String>> results = dc.getResults();
			List<String> inodesToClean = new ArrayList<String>();
			boolean runDelete = false;
			for (HashMap<String, String> r : results) {
				inodesToClean.add(r.get("inode"));
				runDelete = true;
			}
			if(runDelete){
				deleteAssets(inodesToClean, tableNameOfAsset,500);
			}
			dc.setSQL(countSQL);
			result = dc.getResults();
		} catch (Exception e) {
			Logger.error(MaintenanceUtil.class, e.getMessage(), e);
		}
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;
	}


	/**
	 * Deleting all inodes of the assets from inode table in case that inode in the table does not exist any more
	 */
	@SuppressWarnings("unchecked")
	public static void deleteAssetsWithNoInode() throws DotDataException {
		String assetsPath = fileAPI.getRealAssetsRootPath();
		File assetsRootFolder = new File(assetsPath);
		String reportsPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			reportsPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH");
		} else {
			reportsPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH"));
		}
		File reportsFolder = new File(reportsPath);

		String messagesPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			messagesPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + "messages";
		} else {
			messagesPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + "messages");
		}
		File messagesFolder = new File(messagesPath);

		DotConnect dc = new DotConnect();
		int counter = 0;
		File file = null;
		final String selectInodesSQL = "select i.inode from inode i where type = 'file_asset'";
		dc.setSQL(selectInodesSQL);
		List<HashMap<String, String>> results = dc.loadResults();

		List<Object> filesAssetsCanBeParsed = new ArrayList<Object>();
		try{
			filesAssetsCanBeParsed = findFileAssetsCanBeParsed();
		}catch(Exception ex){
			Logger.error(MaintenanceUtil.class, ex.getMessage(),ex);
		}
		List<String> fileAssetsListFromFileSystem = (List<String>) filesAssetsCanBeParsed.get(0);
		List<String> fileAssetsInodesListFromFileSystem = (List<String>) filesAssetsCanBeParsed.get(1);
		List<String> fileAssetsInodesList = new ArrayList<String>();
		for(HashMap<String, String> r : results){
			fileAssetsInodesList.add(r.get("inode").toString());
		}
		results = null;
		for(int i = 0; i < fileAssetsInodesListFromFileSystem.size(); i++){
			if(!fileAssetsInodesList.contains(fileAssetsInodesListFromFileSystem.get(i))){
				file = new File(fileAssetsListFromFileSystem.get(i));
				if(!file.getPath().startsWith(assetsRootFolder.getPath()+java.io.File.separator+"license")
						&&  !file.getPath().startsWith(reportsFolder.getPath())
						&&  !file.getPath().startsWith(messagesFolder.getPath())){
					Logger.info(MaintenanceUtil.class, "Deleting " + file.getPath() + "...");
					file.delete();
					counter++;
				}
			}
		}
		Logger.info(MaintenanceUtil.class, "Deleted " + counter + " files");
	}

	/**
	 *
	 * @param folder This is the folder where the assets are stored
	 * @param fileAssetsList This is the list of all the file assets
	 * @return
	 */
	private static List<String> findFileAssetsList(File folder, List<String> fileAssetsList){
		File[] files = folder.listFiles();
		if (0 < files.length) {
			List<Structure> structures = StructureFactory.getStructures();
			List<Field> binaryFields = new ArrayList<Field>();
			List<Field> fields;
			for (Structure structure: structures) {
				fields = FieldsCache.getFieldsByStructureInode(structure.getInode());

				for (Field field: fields) {
					if (field.getFieldType().equals(Field.FieldType.BINARY.toString()))
						binaryFields.add(field);
				}
			}

			boolean isBinaryField;
	        for(int j = 0; j < files.length; j++) {
	            fileAssetsList.add(files[j].getPath());
	            if(files[j].isDirectory()) {

	            	isBinaryField = false;
	            	for (Field field: binaryFields) {
	            		if (field.getVelocityVarName().equals(files[j].getName())) {
	            			isBinaryField = true;
	            			break;
	            		}
	            	}

	            	if (!isBinaryField)
	            		findFileAssetsList(files[j], fileAssetsList);
	            }
	        }
		}
        return fileAssetsList;
	}

	/**
	 * This method returns a list which keeps two lists. The first one is the list of file
	 * asset files of which names without the extension can be parsed to a long, meaning that
	 * they are inodes. The second list keeps those inodes obtained by taking the left parts
	 * of the . in the file asset file names
	 * @return
	 */
	public static List<Object> findFileAssetsCanBeParsed() {
		String fileName = null;
		List<String> fileAssetsList = new ArrayList<String>();
        File folder = new File(fileAPI.getRealAssetPath());
		fileAssetsList = findFileAssetsList(folder, fileAssetsList);
		List<String> fileAssets = new ArrayList<String>();
		List<String> fileAssetsInodes = new ArrayList<String>();
		File file = null;
		for(int i = 0; i < fileAssetsList.size(); i++){
			file = new File((String)fileAssetsList.get(i));
			fileName = file.getName();
			String [] fileSplitted = fileName.split("\\.");
			if(fileSplitted.length > 2 || file.isDirectory()){
				continue;
			}
			try{
				if(fileSplitted[0].indexOf("resized") != -1  || fileSplitted[0].indexOf("thumb") != -1){
					String [] underscoreSplitted = fileSplitted[0].split("_");
					fileAssetsInodes.add(underscoreSplitted[0]);
				}else{
					fileAssetsInodes.add(fileSplitted[0]);
				}
				fileAssets.add((String)fileAssetsList.get(i));

			}catch(NumberFormatException numberFormatException){
				Logger.info(MaintenanceUtil.class, "File " + fileName + " is not an inode");
			}catch(Exception exception){
				Logger.error(MaintenanceUtil.class, exception.getMessage(), exception);
			}
		}
		List<Object> assetFilesAndInodes = new ArrayList<Object>();
		assetFilesAndInodes.add(fileAssets);
		assetFilesAndInodes.add(fileAssetsInodes);
		return assetFilesAndInodes;
	}

	public static void fixImagesTable() throws SQLException{
		DotConnect dc = new DotConnect();
		List<String> imageIds = new ArrayList<String>();
		final String selectImageIdsSQL = "select i.imageid from image i";
		String deleteImageSQL = "delete from image where imageid like '' or imageid is null";
		dc.setSQL(selectImageIdsSQL);
		List<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(MaintenanceUtil.class,e.getMessage(), e);
		}
		for(HashMap<String, String> r : results){
			imageIds.add(r.get("imageid").toString());
		}

		for(int i = 0; i < imageIds.size(); i++){
			if(!UtilMethods.isSet(imageIds.get(i))){
				dc.setSQL(deleteImageSQL);
				dc.getResult();
			}

		}
	}

}
