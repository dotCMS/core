package com.dotmarketing.util;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.util.FileUtil;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;

/**
 * This class provides access to utility methods that will search the database
 * for data inconsistencies and fix/remove the incorrect data to ensure dotCMS
 * works as expected.
 * 
 * @author root
 * @since Mar 22, 2012
 *
 */
public class MaintenanceUtil {

	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();

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
		CacheLocator.getNavToolCache().clearCache();
	}


	public static int cleanMultiTreeTable(){
		final String countSQL = "select count(*) as count from multi_tree t";
		final String deleteChildFromSQL ="delete from multi_tree where child not in (select i.id from identifier i)";
		final String deleteParent1FromSQL ="delete from multi_tree where parent1 not in (select i.id from identifier i)";
		final String deleteParent2FromSQL ="delete from multi_tree where parent2 not in (select i.id from identifier i)";
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
		count += deleteTemplatesWithNoIdentifier();
		return count;
	}

	public static void cleanInodeTableData()throws DotDataException{
		Map map = new HashMap();
		try {
			//Including Identifier.class because it is not mapped with Hibernate anymore
			map.put(Identifier.class, null);
			map.putAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata());

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
					String tableName = ((com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
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
					String tableName = ((com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
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
		return deleteAssetsWithNoIdentifier(Inode.Type.CONTAINERS.getTableName());
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
	 * Will replace text in the DB within the following com.dotcms.repackage.jruby.tables.
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
			if(DbConnectionFactory.isMsSql()){
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
			if(DbConnectionFactory.isMsSql()){
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
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE COLUMNS code, pre_loop, and post_loop ON THE " + Inode.Type.CONTAINERS.getTableName() + " TABLE");
		if(DbConnectionFactory.isMsSql()){
			dc.setSQL("UPDATE " + Inode.Type.CONTAINERS.getTableName() + " SET code=replace(cast(code as varchar(max)),?,?),pre_loop=replace(cast(pre_loop as varchar(max)),?,?),post_loop=replace(cast(post_loop as varchar(max)),?,?) WHERE " + Inode.Type.CONTAINERS.getTableName() + ".inode = (SELECT working_inode FROM container_version_info cvi WHERE (cvi.working_inode = " + Inode.Type.CONTAINERS.getTableName() + ".inode OR cvi.live_inode =" + Inode.Type.CONTAINERS.getTableName() + ".inode)) ");
		}else{
			dc.setSQL("UPDATE " + Inode.Type.CONTAINERS.getTableName() + " SET code=replace(code,?,?),pre_loop=replace(pre_loop,?,?),post_loop=replace(post_loop,?,?) WHERE " + Inode.Type.CONTAINERS.getTableName() + ".inode = (SELECT working_inode FROM container_version_info cvi WHERE (cvi.working_inode = " + Inode.Type.CONTAINERS.getTableName() + ".inode OR cvi.live_inode = " + Inode.Type.CONTAINERS.getTableName() + ".inode)) ");
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
			Logger.error(MaintenanceUtil.class,"Problem updating " + Inode.Type.CONTAINERS.getTableName() + " table : " + e.getMessage(),e);
		}
		Logger.info(MaintenanceUtil.class, "ABOUT TO UPDATE body COLUMN ON THE template TABLE");
		if(DbConnectionFactory.isMsSql()){
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
		if(DbConnectionFactory.isMsSql()){
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
		if(DbConnectionFactory.isMsSql()){
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

	@WrapInTransaction
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

	/**
	 * Locates the Content Type fields that are pointing to Content Types that
	 * no longer exist in the database and removes them.
	 * 
	 * @throws SQLException
	 *             An error occurred when executing the fix queries.
	 */
	public static void deleteOrphanContentTypeFields() throws SQLException {
	    String query = "DELETE FROM field WHERE NOT EXISTS (SELECT * FROM structure WHERE structure.inode = field.structure_inode)";
		DotConnect dc = new DotConnect();
		dc.executeStatement(query);
		query = String.format("DELETE FROM inode WHERE NOT EXISTS (SELECT * FROM field " + 
		        "WHERE field.inode = inode.inode) and inode.type = '%s' ",Inode.Type.FIELD.getTableName());
        dc.executeStatement(query);
	}

}
