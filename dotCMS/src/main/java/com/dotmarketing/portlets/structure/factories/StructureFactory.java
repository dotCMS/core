package com.dotmarketing.portlets.structure.factories;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.api.system.event.*;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.exception.BaseRuntimeInternationalizationException;

import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.beans.Host;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;


import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.folders.model.Folder;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


/**
 * Provides access to information related to Content Types and the different
 * ways it is related to other types of objects in dotCMS. The term "Structure" 
 * is deprecated, it has been changed to "Content Type" now. 
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
@Deprecated
public class StructureFactory {




	private static final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();
	private static final HttpServletRequestThreadLocal httpServletRequestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
	private static final ContentTypeUtil contentTypeUtil = ContentTypeUtil.getInstance();
	private static final ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

	//### READ ###

	/**
	 * Gets the structure by inode
	 * @deprecated  Use CacheLocator.getContentTypeCache().getStructureByInode instead
	 * @param inode is the contentlet inode
	 */
	public static Structure getStructureByInode(String inode) {
		try {
			return new StructureTransformer(typeAPI.find(inode)).asStructure();
		} catch (Exception e) {
			return new Structure();
		}
	}
	/**
	 * Gets the structure by Type
	 * @deprecated  Use CacheLocator.getContentTypeCache().getStructureByName instead
	 * @param type is the name of the structure
	 */
	public static Structure getStructureByType(String type)
	{
		type = SQLUtil.sanitizeParameter(type);
		String condition = " structure.name = '" + type + "'";
		try {
			return new StructureTransformer(typeAPI.search(condition, "mod_date desc", 1, 0)).asStructure();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}

	/**
	 * Gets the structure by variable name
	 * @param varName is the name of the structure
	 */
	@SuppressWarnings("unchecked")
	public static Structure getStructureByVelocityVarName(String varName)
	{

		try {
			return new StructureTransformer(typeAPI.find(varName)).asStructure();
		} catch (NotFoundInDbException  e){
			return new Structure();
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}

	public static Structure getDefaultStructure()
	{
		try {
			return new StructureTransformer(typeAPI.findDefault()).asStructure();
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}

	/**
	 * This method return the structures s
	 * @return List<String>
	 */
	@Deprecated
	public static List<String> getAllStructuresNames()
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> temp = getStructures(orderBy,limit);

		List<String> results = new ArrayList<>();
		for(Structure st : temp){
			results.add(st.getName());
		}
		return results;
	}
	
	@Deprecated
	public static List<String> getAllVelocityVariablesNames()
	{
		String orderBy = "name";
		int limit = -1;
		List<Structure> temp = getStructures(orderBy,limit);

		List<String> results = new ArrayList<>();
		for(Structure st : temp){
			results.add(st.getVelocityVarName());
		}
		return results;
	}
	
	@Deprecated
	public static List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException{
		return typeAPI.findStructureURLMapPatterns();
	}

	/**
	 * Retrieves a list of {@link Structure} objects that the current user is
	 * allowed to access. The result set will contain all possible values,
	 * grouped by Content Type and name, and in ascendent order. Depending on
	 * the license level, some Content Types might not be included as part of
	 * the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @return A list of permissioned {@link Structure} objects.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
	public static List<Structure> getStructures(User user, boolean respectFrontendRoles, boolean allowedStructsOnly)
			throws DotDataException {
	  
		List<ContentType> types = APILocator.getContentTypeAPI(user, respectFrontendRoles).findAll();
		return new StructureTransformer(types).asStructureList();
	}

	/**
	 * Retrieves a list of {@link Structure} objects that the current user is
	 * allowed to access. It also allows you to have more control on the
	 * filtering criteria for the result set. Depending on the license level,
	 * some Content Types might not be included as part of the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @param condition
	 *            - Any specific condition or filtering criteria for the
	 *            resulting Content Types.
	 * @param orderBy
	 *            - The column(s) to order the results by.
	 * @param limit
	 *            - The maximum number of records to return.
	 * @param offset
	 *            - The record offset for pagination purposes.
	 * @param direction
	 *            - The ordering of the results: <code>asc</code>, or
	 *            <code>desc</code>.
	 * @return A list of {@link Structure} objects based on the current user's
	 *         permissions and the system license.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
	public static List<Structure> getStructures(User user, boolean respectFrontendRoles, boolean allowedStructsOnly,
			String condition, String orderBy, int limit, int offset, String direction) throws DotStateException {
		
		try {
			List<ContentType> types = APILocator.getContentTypeAPI(user,respectFrontendRoles).search(condition, orderBy + " " + direction, limit, offset);
			return new StructureTransformer(types).asStructureList();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}
	
	public static List<Structure> getStructures()
	{
		try {
			return new StructureTransformer(typeAPI.search("1=1", "name desc", 10000, 0)).asStructureList();
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}
	
	   /**
     * Returns a list of Content Type according to a specific Type
     * These could be:
     * 1. Contents.
     * 2. Widgets.
     * 3. Forms.
     * 4. File Assets.
     * 5. Pages.
     * 6. Personas
     * @param structureType: Integer type, according to valid content types specified in Structure.java class
     * @return structures: List of Structures 
     */
	public static List<Structure> getAllStructuresByType(int structureType)
    {
		BaseContentType type = BaseContentType.getBaseContentType(structureType);
		try {
			return new StructureTransformer(typeAPI.findByType(type)).asStructureList();
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException(e);
		}
    }

	public static List<Structure> getStructuresByUser(User user, String condition, String orderBy,int limit,int offset,String direction) {

		return getStructures(user,  false,  false,
				 condition,  orderBy,  limit,  offset,  direction);
	}


	public static List<Structure> getStructuresWithWritePermissions(User user, boolean respectFrontendRoles) throws DotDataException{

		try {
			
			List<Structure>  structs= new StructureTransformer(APILocator.getContentTypeAPI(user,respectFrontendRoles).findAll()).asStructureList();
			return APILocator.getPermissionAPI().filterCollection(structs,PermissionAPI.PERMISSION_WRITE, respectFrontendRoles, user);
		
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException(e);
		}
	}

	public static List<Structure> getStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		try {
			
			return new StructureTransformer(typeAPI.findAll()).asStructureList();
	
		} catch (DotStateException | DotDataException e) {
			throw new DotStateException(e);
		}
	}

	public static List<Structure> getNoSystemStructuresWithReadPermissions(User user, boolean respectFrontendRoles) throws DotDataException
	{
		String orderBy = "structuretype,upper(name)";
		int limit = -1;
		String condition = " structure.system= " + DbConnectionFactory.getDBFalse();
		List<ContentType> types = APILocator.getContentTypeAPI(user,respectFrontendRoles).search(condition, orderBy, limit, 0);
		return new StructureTransformer(types).asStructureList();

	}

	public static List<Structure> getStructuresUnderHost(Host h, User user, boolean respectFrontendRoles) throws DotDataException
	{



		try{
			String condition = " host = '" + h.getIdentifier() + "'";
			int limit = -1;
			List<ContentType> types = APILocator.getContentTypeAPI(user,respectFrontendRoles).search(condition, "mod_date desc", limit, 0);
			return new StructureTransformer(types).asStructureList();
		}
		catch(Exception e){
			Logger.error(StructureFactory.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage(),e);

		}
	}

    public static List<Structure> getStructuresByWFScheme(WorkflowScheme scheme, User user, boolean respectFrontendRoles) throws DotDataException {

        try {
            // Use WorkflowAPI to fetch content types associated with the given scheme
            final List<ContentType> types = APILocator.getWorkflowAPI().findContentTypesForScheme(scheme);
            return new StructureTransformer(types).asStructureList();
        } catch (Exception e) {
            Logger.error(StructureFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
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

	public static List<Structure> getStructures(String orderBy,int limit,String direction){
		return getStructures("1=1 ", "mod_date", limit, 0,direction);
		
	}

	public static List<Structure> getStructures(String condition, String orderBy,int limit,int offset,String direction) {

        // Community edition filtering is now enforced at the ContentTypeFactory layer.
        // Remove legacy NOT IN condition appends to keep queries within the simplified safe grammar.

		try{
			List<ContentType> types = typeAPI.search(condition, orderBy + " " +direction, limit, offset);
			return new StructureTransformer(types).asStructureList();
		}
		catch(Exception e){
			throw new DotStateException(e);

		}
	}

	
	protected static void fixFolderHost(Structure st) {
	    if(!UtilMethods.isSet(st.getFolder())) {
	        st.setFolder(Folder.SYSTEM_FOLDER);
	    }
	    if(!UtilMethods.isSet(st.getHost())) {
	        st.setHost(Host.SYSTEM_HOST);
	    }
	}

	//### CREATE AND UPDATE
	public static void saveStructure(Structure structure) throws DotHibernateException{
        boolean isNew = !UtilMethods.isSet(structure.getInode());
		try {
			ContentType type = new StructureTransformer(structure).from();
			type = typeAPI.save(type);
			structure.setInode(type.inode());
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotHibernateException(e.getMessage(),e);
		}



		pushSaveUpdateEvent(structure, isNew);
	}

	private static void pushSaveUpdateEvent(final Structure structure, final boolean isNew) {

		ContentType type=new StructureTransformer(structure).from();

		final DotSubmitter dotSubmitter =
				SystemEventsFactory.getInstance().getDotSubmitter();
		
		final String actionUrl = isNew ? contentTypeUtil.getActionUrl(type) : null;

		dotSubmitter.execute(() -> {

			final SystemEventType systemEventType = isNew ?
					SystemEventType.SAVE_BASE_CONTENT_TYPE : SystemEventType.UPDATE_BASE_CONTENT_TYPE;

			try {

				ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper(actionUrl, type);
				systemEventsAPI.push(systemEventType, new Payload(contentTypePayloadDataWrapper,  Visibility.PERMISSION,
	                            PermissionAPI.PERMISSION_READ));
			} catch (DotDataException e) {
				throw new RuntimeException( e );
			}
		});
	}

	public static void saveStructure(Structure structure, String existingId) throws DotHibernateException
	{
		try {
			ContentType type = typeAPI.save(new StructureTransformer(structure).from());
			structure.setInode(type.inode());
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotHibernateException(e.getMessage(),e);
		}
	}

	//### DELETE ###
	public static void deleteStructure(String inode) throws DotDataException
	{
		Structure structure = getStructureByInode(inode);
		deleteStructure(structure);
	}

	public static void deleteStructure(final Structure structure) throws DotDataException
	{
		final DotSubmitter dotSubmitter =
				SystemEventsFactory.getInstance().getDotSubmitter();

		ContentType type=new StructureTransformer(structure).from();

		try {

			typeAPI.delete(type);

			if (null != dotSubmitter) {
				
				final String actionUrl = contentTypeUtil.getActionUrl(type);

				dotSubmitter.execute(() -> {

					try {
						ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper(actionUrl, type);
						systemEventsAPI.push(SystemEventType.DELETE_BASE_CONTENT_TYPE, new Payload(contentTypePayloadDataWrapper,  Visibility.PERMISSION, PermissionAPI.PERMISSION_READ));
					} catch (DotDataException e) {
						throw new BaseRuntimeInternationalizationException( e );
					}
				});
			}
		} catch (DotStateException | DotSecurityException e) {
			Logger.error(StructureFactory.class, e.getMessage(), e);


		} catch (DotDataException e) {

			throw new BaseRuntimeInternationalizationException( e );
		}
	}

    /**
     *
     * @throws com.dotmarketing.exception.DotHibernateException
     * 
     * @deprecated Do not use this method anymore, Instead, use {@link com.dotcms.contenttype.business.ContentTypeAPI#setAsDefault(ContentType)}
     * 
     * @see com.dotcms.contenttype.business.ContentTypeAPI#setAsDefault(ContentType)
     */
	public static void disableDefault() throws DotHibernateException
	{
		throw new DotHibernateException("You cannot disbale the default without setting a new one");
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
	public static void createDefaultStructure () {
		Structure st = StructureFactory.getDefaultStructure();
		if (st == null || !InodeUtils.isSet(st.getInode())) {





		}
	}

	/**
	 * Gets the list of fields of a structure which type it is TAG
	 * @param structureInode inode of the structure owner of the fields to get
	 * @return a list of fields of a structure which type it is TAG
	 */
	public static ArrayList<Field> getTagsFields(String structureInode) {
		ArrayList<Field> tagFields = new ArrayList<>();
		List<Field> listFields = FieldsCache.getFieldsByStructureInode(structureInode);

		for (Field f : listFields) {
			if (f.getFieldType().equals(Field.FieldType.TAG.toString())) {
				tagFields.add(f);
			}
		}

		return tagFields;
	}

    /**
     * Counts the amount of structures in DB filtering by the given condition
     * 
     * @param condition to be used
     * @return Amount of structures found
     */
    public static int getStructuresCount(String condition) {
        DotConnect db = new DotConnect();

        StringBuffer sb = new StringBuffer();

        condition = (UtilMethods.isSet(condition.trim())) ? condition + " AND " : "";
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            condition += " structuretype NOT IN (" + Structure.STRUCTURE_TYPE_FORM + ", "
                    + Structure.STRUCTURE_TYPE_PERSONA + ") AND ";
        }

        condition += " 1=1 ";

        try {

            sb.append("select count(distinct structure.inode ) as count ");
            sb.append(" from structure ");
            if (condition != null && UtilMethods.isSet(condition)) {
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
		List<Field> imageList = new ArrayList<>();
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
		List<Field> fileList = new ArrayList<>();
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

		Map<String, String> dbColToObjectAttribute = new HashMap<>();

		if(UtilMethods.isSet(query.getSelectAttributes())){

			if(!query.getSelectAttributes().contains("name")){
				query.getSelectAttributes().add("name");
			}
		}else{
			List<String> atts = new ArrayList<>();
			atts.add("*");
			atts.add("name");
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}





	public static List<Structure> findStructuresUserCanUse(User user, String query, Integer structureType, int offset, int limit) throws DotDataException, DotSecurityException {
		BaseContentType baseType = BaseContentType.getBaseContentType(structureType);
		List<ContentType> listContentTypes = APILocator.getContentTypeAPI(user).search(query,baseType, "name", limit, offset);
		return new StructureTransformer(listContentTypes).asStructureList();
	}
}
