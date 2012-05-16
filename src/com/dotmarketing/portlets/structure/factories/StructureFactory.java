package com.dotmarketing.portlets.structure.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.cmis.QueryResult;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionedWebAssetUtil;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class StructureFactory {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	private static HostAPI hostAPI = APILocator.getHostAPI();


	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	//### READ ###

	/**
	 * Gets the structure by inode
	 * @deprecated  Use StructureCache.getStructureByInode instead
	 * @param inode is the contentlet inode
	 */
	public static Structure getStructureByInode(String inode)
	{
		return (Structure) InodeFactory.getInode(inode,Structure.class);
	}
	/**
	 * Gets the structure by Type
	 * @deprecated  Use StructureCache.getStructureByName instead
	 * @param type is the name of the structure
	 */
	public static Structure getStructureByType(String type)
	{
		Structure structure = null;
		String condition = " name = '" + type + "'";
		List list = InodeFactory.getInodesOfClassByCondition(Structure.class,condition);
		if (list.size() > 0)
		{
			structure = (Structure) list.get(0);
		}
		else
		{
			structure = new Structure();
		}
		return structure;
	}

	/**
	 * Gets the structure by variable name
	 * @param type is the name of the structure
	 */
	@SuppressWarnings("unchecked")
	public static Structure getStructureByVelocityVarName(String varName)
	{
		if(varName ==null) return new Structure();
		Structure structure = null;
		String condition = " lower(velocity_var_name) = '" + varName.toLowerCase() + "'";
		List<Structure> list = InodeFactory.getInodesOfClassByCondition(Structure.class,condition);
		if (list.size() > 0)
		{
			structure = (Structure) list.get(0);
		}
		else
		{
			structure = new Structure();
		}
		return structure;
	}

	public static Structure getDefaultStructure()
	{
		Structure structure = null;
		String dbTrue = com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		String condition = "default_structure = " + dbTrue;
		List list = InodeFactory.getInodesOfClassByCondition(Structure.class,condition);
		if (list.size() > 0)
		{
			structure = (Structure) list.get(0);
		}
		else
		{
			structure = new Structure();
		}
		return structure;
	}

	/**
	 * This method return the structures s
	 * @return List<String>
	 */
	public static List<String> getAllStructuresNames()
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> temp = getStructures(orderBy,limit);

		List<String> results = new ArrayList<String>();
		for(Structure st : temp){
			results.add(st.getName());
		}
		return results;

	}

	public static List<String> getAllVelocityVariablesNames()
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> temp = getStructures(orderBy,limit);

		List<String> results = new ArrayList<String>();
		for(Structure st : temp){
			results.add(st.getVelocityVarName());
		}
		return results;

	}

	public static List<SimpleStructureURLMap> findStructureURLMapPatterns()throws DotDataException{
		List<SimpleStructureURLMap> res = new ArrayList<SimpleStructureURLMap>();
		DotConnect dc = new DotConnect();
		if (DbConnectionFactory.isOracle()) {
			dc.setSQL("SELECT inode, url_map_pattern from structure where url_map_pattern is not null order by url_map_pattern desc");
		} else {
			dc.setSQL("SELECT inode, url_map_pattern from structure where url_map_pattern is not null and url_map_pattern <> '' order by url_map_pattern desc");
		}
		List<Map<String, String>> rows = dc.loadResults();
		for (Map<String, String> row : rows) {
			res.add(new SimpleStructureURLMap(row.get("inode"), row.get("url_map_pattern")));
		}
		return res;
	}

	public static List<Structure> getStructures()
	{
		String orderBy = "name";
		int limit = -1;
		return getStructures(orderBy,limit);
	}

	public static List<Structure> getStructuresByUser(User user, String condition, String orderBy,int limit,int offset,String direction) {

		PaginatedArrayList<Structure> structures = new PaginatedArrayList<Structure>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		List<Structure> resultList = new ArrayList<Structure>();
		HibernateUtil dh = new HibernateUtil(Structure.class);
		int countLimit = 100;
		int size = 0;
		try {
			String type = ((Inode) Structure.class.newInstance()).getType();
			String query = "from inode in class " + Structure.class.getName();
			// condition
			query += (UtilMethods.isSet(condition) ? " where inode.type ='"+type+"' and " + condition : " where inode.type ='"+type+"'");
			// order
			query +=  (UtilMethods.isSet(orderBy) ? " order by " + orderBy + "" : "");
			query += ((UtilMethods.isSet(orderBy) && UtilMethods.isSet(direction)) ? " " + direction : "");

			dh.setQuery(query.toString());

			while(!done) {
				dh.setFirstResult(internalOffset);
				dh.setMaxResults(internalLimit);
				resultList = dh.list();
				PermissionAPI permAPI = APILocator.getPermissionAPI();
				toReturn.addAll(permAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if(countLimit > 0 && toReturn.size() >= countLimit + offset)
					done = true;
				else if(resultList.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}

			if(offset > toReturn.size()) {
				size = 0;
			} else if(countLimit > 0) {
				int toIndex = offset + countLimit > toReturn.size()?toReturn.size():offset + countLimit;
				size = toReturn.subList(offset, toIndex).size();
			} else if (offset > 0) {
				size = toReturn.subList(offset, toReturn.size()).size();
			}
			structures.setTotalResults(size);
			int from = offset<toReturn.size()?offset:0;
			int pageLimit = 0;
			for(int i=from;i<toReturn.size();i++){
				if(pageLimit<limit || limit<=0){
					structures.add((Structure) toReturn.get(i));
					pageLimit+=1;
				}else{
					break;
				}

			}

		} catch (Exception e) {

			Logger.warn(StructureFactory.class, "getStructuresByUser failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return structures;
	}


	public static List<Structure> getStructuresWithWritePermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> all = getStructures(orderBy,limit);
		List<Structure> retList = new ArrayList<Structure> ();
		for (Structure st : all) {
			if (permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, user, respectFrontendRoles)) {
				retList.add(st);
			}
		}
		return retList;
	}

	public static List<Structure> getStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> all = getStructures(orderBy,limit);
		List<Structure> retList = new ArrayList<Structure> ();
		for (Structure st : all) {
			if (permissionAPI.doesUserHavePermission(st, PERMISSION_READ, user, respectFrontendRoles)) {
				retList.add(st);
			}
		}
		return retList;
	}

	public static List<Structure> getNoSystemStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		String orderBy = "structuretype,upper(name)";
		int limit = -1;
		List<Structure> all = getStructures(orderBy,limit);
		List<Structure> retList = new ArrayList<Structure> ();
		for (Structure st : all) {
			if (permissionAPI.doesUserHavePermission(st, PERMISSION_READ, user, respectFrontendRoles) && !st.isSystem()) {
				retList.add(st);
			}
		}
		return retList;
	}

	public static List<Structure> getStructuresByWFScheme(WorkflowScheme scheme, User user, boolean respectFrontendRoles) throws DotDataException
	{

		int limit = -1;

		HibernateUtil dh = new HibernateUtil(Structure.class);


		try{
			String type = ((Inode) Structure.class.newInstance()).getType();
			String query = "from inode in class " + Structure.class.getName() + " workflow_scheme_x_structure ";
			// condition
			query += " where inode.type ='"+type+"' and structure.inode = workflow_scheme_x_structure.structure_id and workflow_scheme_x_structure.scheme_id = ?";
			// order
			query +=  " order by name" ;
			dh.setQuery(query);

			List<Structure> resultList = dh.list();

			List<Structure> retList = new ArrayList<Structure> ();
			for (Structure st : retList) {
				if (permissionAPI.doesUserHavePermission(st, PERMISSION_READ, user, respectFrontendRoles)) {
					retList.add(st);
				}
			}
			return retList;
		}
		catch(Exception e){
			Logger.error(StructureFactory.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage());

		}
	}

	public static List getStructures(int limit)
	{
		String orderBy = "name";
		return getStructures(orderBy,limit);
	}

	@SuppressWarnings("unchecked")
	public static List<Structure> getStructures(String orderBy,int limit)
	{
		String direction = "asc";
		return getStructures(orderBy,limit,direction);
	}

	public static List<Structure> getStructures(String orderBy,int limit,String direction)
	{
		List<Structure> list = new ArrayList<Structure>();
		String condition = "";
		list = InodeFactory.getInodesOfClassByConditionAndOrderBy(Structure.class,condition,orderBy,limit,0,direction);
		return list;
	}

	public static List<Structure> getStructures(String condition, String orderBy,int limit,int offset,String direction)
	{
		List<Structure> list = new ArrayList<Structure>();
		list = InodeFactory.getInodesOfClassByConditionAndOrderBy(Structure.class,condition,orderBy,limit,offset,direction);
		return list;
	}


	//### CREATE AND UPDATE
	public static void saveStructure(Structure structure) throws DotHibernateException
	{
		structure.setUrlMapPattern(cleanURLMap(structure.getUrlMapPattern()));
		Date now = new Date();
		structure.setiDate(now);
		HibernateUtil.saveOrUpdate(structure);
	}

	//### DELETE ###
	public static void deleteStructure(String inode) throws DotHibernateException, DotDataException
	{
		Structure structure = getStructureByInode(inode);
		deleteStructure(structure);
	}

	public static void deleteStructure(Structure structure) throws DotHibernateException, DotDataException
	{

		WorkFlowFactory wff = FactoryLocator.getWorkFlowFactory();
		wff.deleteSchemeForStruct(structure.getInode());
		InodeFactory.deleteInode(structure);
	}

	public static void disableDefault() throws DotHibernateException
	{
		Structure defaultStructure = getDefaultStructure();
		if (InodeUtils.isSet(defaultStructure.getInode()))
		{
			defaultStructure.setDefaultStructure(false);
			saveStructure(defaultStructure);
		}
	}

	public static int getTotalDates(Structure structure)
	{
		String typeField = Field.FieldType.DATE.toString();
		int intDate = getTotals(structure,typeField);
		typeField = Field.FieldType.DATE_TIME.toString();
		int intDateTime = getTotals(structure,typeField);
		return intDate + intDateTime;
	}

	public static int getTotalImages(Structure structure)
	{
		String typeField = Field.FieldType.IMAGE.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalFiles(Structure structure)
	{
		String typeField = Field.FieldType.FILE.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalTextAreas(Structure structure)
	{
		String typeField = Field.FieldType.TEXT_AREA.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotalWYSIWYG(Structure structure)
	{
		String typeField = Field.FieldType.WYSIWYG.toString();
		return getTotals(structure,typeField);
	}

	public static int getTotals(Structure structure,String typeField)
	{
		List fields = structure.getFields();
		int total = 0;
		for(int i = 0; i < fields.size();i++)
		{
			Field field = (Field) fields.get(i);
			if(field.getFieldType().equals(typeField))
			{
				total++;
			}
		}
		return total;
	}

	/**
	 * Create the default cms contentlet structure
	 * @throws DotHibernateException
	 *
	 */
	public static void createDefaultStructure () throws DotHibernateException {
		Structure st = StructureFactory.getDefaultStructure();
		if (st == null || !InodeUtils.isSet(st.getInode())) {

			Logger.info(StructureFactory.class, "Creating the default cms contentlet structure");

			st.setDefaultStructure(true);
			st.setFixed(false);
			st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
			st.setDescription("Default CMS Content Structure");
			st.setName("Web Page Content");
			StructureFactory.saveStructure(st);

			Field field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text1");
			field.setFieldName("Title");
			field.setFieldType(Field.FieldType.TEXT.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(0);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("ContentletTitle");
			field.setIndexed(true);
			field.setSearchable(true);
			field.setListed(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);


			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area1");
			field.setFieldName("Body");
			field.setFieldType(Field.FieldType.WYSIWYG.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(8);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setIndexed(true);
			field.setVelocityVarName("Body");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);



			/* Let's create a News Items structure as well */

			st = new Structure();
			st.setName("News Item");
			st.setDescription("News Items and Press Releases");
			st.setFixed(false);
			st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
			StructureFactory.saveStructure(st);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text1");
			field.setFieldName("Headline");
			field.setFieldType(Field.FieldType.TEXT.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(0);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsHeadline");
			field.setIndexed(true);
			field.setListed(true);
			field.setSearchable(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area1");
			field.setFieldName("Short Summary");
			field.setFieldType(Field.FieldType.TEXT_AREA.toString());
			field.setHint("This is teaser copy shown on listing pages");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(1);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsSummary");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("date1");
			field.setFieldName("Publish Date");
			field.setFieldType(Field.FieldType.DATE_TIME.toString());
			field.setHint("<br>The date this news item will be displayed");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(2);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsPublishDate");
			field.setListed(true);
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);

			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("date2");
			field.setFieldName("Expire Date");
			field.setFieldType(Field.FieldType.DATE_TIME.toString());
			field.setHint("<br>The date this item will expire");
			field.setRegexCheck("");
			field.setRequired(false);
			field.setSortOrder(3);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setVelocityVarName("NewsExpireDate");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);


			field = new Field ();
			field.setDefaultValue("");
			field.setFieldContentlet("text_area2");
			field.setFieldName("Body");
			field.setFieldType(Field.FieldType.WYSIWYG.toString());
			field.setHint("");
			field.setRegexCheck("");
			field.setRequired(true);
			field.setSortOrder(4);
			field.setStructureInode(st.getInode());
			field.setFieldRelationType("");
			field.setIndexed(true);
			field.setVelocityVarName("NewsBody");
			field.setFixed(false);
			field.setReadOnly(false);
			FieldFactory.saveField(field);




		}
	}

	/**
	 * Gets the list of fields of a structure which type it is TAG
	 * @param structureInode inode of the structure owner of the fields to get
	 * @return a list of fields of a structure which type it is TAG
	 */
	public static ArrayList<Field> getTagsFields(String structureInode) {
		ArrayList<Field> tagFields = new ArrayList<Field>();
		List<Field> listFields = FieldsCache.getFieldsByStructureInode(structureInode);

		for (Field f : listFields) {
			if (f.getFieldType().equals(Field.FieldType.TAG.toString())) {
				String fieldValues = f.getFieldContentlet();
				if(UtilMethods.isSet(fieldValues)) {
					tagFields.add(f);
				}
			}
		}

		return tagFields;
	}

	public static int getStructuresCount(String condition)
	{
		DotConnect db = new DotConnect();

		StringBuffer sb = new StringBuffer();
		try {

			sb.append("select count(distinct structure.inode ) as count ");
			sb.append(" from structure ");
			if(condition != null && UtilMethods.isSet(condition)){
				sb.append(" where " + condition);
			}
			Logger.debug(StructureFactory.class, sb.toString());
			db.setSQL(sb.toString());
			return db.getInt("count");

		} catch (Exception e) {
			Logger.error(WebAssetFactory.class, "getStructuresCount failed:" + e, e);
		}

		return 0;
	}

	/**
	 * Get the list of image fields of a structure having a value in a list of parameters
	 * @param structure The structure whose fields will be compared to the list of values given
	 * @param parametersName A list with the velocity name of the fields to be compared to the structure
	 * @param values A list with the values of the fields to be compared to the structure
	 * @return List<Field>
	 */
	public static List<Field> getImagesFieldsList(Structure structure, List<String> parametersName, List<String[]> values){
		List<Field> imageList = new ArrayList<Field>();
		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			Field field = structure.getFieldVar(fieldname);
			if(UtilMethods.isSet(field) && APILocator.getFieldAPI().valueSettable(field)){
				if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) && UtilMethods.isSet(fieldValue)){
					imageList.add(field);
				}
			}
		}
		return imageList;
	}

	/**
	 * Get the list of file fields of a structure having a value in a list of parameters
	 * @param structure The structure whose fields will be compared to the list of values given
	 * @param parametersName A list with the velocity name of the fields to be compared to the structure
	 * @param values A list with the values of the fields to be compared to the structure
	 * @return List<Field>
	 */
	public static List<Field> getFilesFieldsList(Structure structure, List<String> parametersName, List<String[]> values){
		List<Field> fileList = new ArrayList<Field>();
		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			Field field = structure.getFieldVar(fieldname);
			if(UtilMethods.isSet(field) && APILocator.getFieldAPI().valueSettable(field)){
				if(field.getFieldType().equals(Field.FieldType.FILE.toString()) && UtilMethods.isSet(fieldValue)){
					fileList.add(field);
				}
			}
		}
		return fileList;
	}

	public static List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {

		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

		if(UtilMethods.isSet(query.getSelectAttributes())){

			if(!query.getSelectAttributes().contains("name")){
				query.getSelectAttributes().add("name" + " as " + QueryResult.CMIS_TITLE);
			}
		}else{
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("name" + " a" +
					"s " + QueryResult.CMIS_TITLE);
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}

	private static String cleanURLMap(String urlMap){
		if(!UtilMethods.isSet(urlMap)){
			return null;
		}
		urlMap = urlMap.trim();
		if(!urlMap.startsWith("/")){
			urlMap = "/" + urlMap;
		}
		return urlMap;
	}

	public static void updateFolderReferences(Folder folder) throws DotDataException{

		HibernateUtil dh = new HibernateUtil(Structure.class);
        dh.setQuery("select inode from inode in class " + Structure.class.getName() + " where inode.folder = ?");
	    dh.setParam(folder.getInode());
		List<Structure> results = dh.list();
		for(Structure  structure : results){
			if(UtilMethods.isSet(folder.getHostId()) && !hostAPI.findSystemHost().getIdentifier().equals(folder.getHostId())){
				structure.setHost(folder.getHostId());
			}else{
				structure.setHost("SYSTEM_HOST");
			}
			structure.setFolder("SYSTEM_FOLDER");
			HibernateUtil.saveOrUpdate(structure);
			StructureCache.removeStructure(structure);
			permissionAPI.resetPermissionReferences(structure);

		}

	}

	public static void updateFolderFileAssetReferences(Structure st) throws DotDataException{
		Structure defaultFileAssetStructure = getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
		if(defaultFileAssetStructure!=null &&
				UtilMethods.isSet(defaultFileAssetStructure.getInode())){
			DotConnect dc = new DotConnect();
			dc.setSQL("update folder set default_file_type = ? where default_file_type = ?");
			dc.addParam(defaultFileAssetStructure.getInode());
			dc.addParam(st.getInode());
			dc.loadResult();
		}

	}

	public static List<Structure> findStructuresUserCanUse(User user, String query, Integer structureType, int offset, int limit) throws DotDataException, DotSecurityException {
		return PermissionedWebAssetUtil.findStructuresForLimitedUser(query, structureType, "name", offset, limit, PermissionAPI.PERMISSION_READ, user, false);
	}


}
