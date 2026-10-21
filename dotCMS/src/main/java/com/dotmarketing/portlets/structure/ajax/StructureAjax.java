package com.dotmarketing.portlets.structure.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.portlets.contentlet.util.ContentletUtil.isFieldTypeAllowedOnImportExport;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldReorderHelper;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;


/**
 * @author David
 */
public class StructureAjax {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private FieldAPI fAPI = APILocator.getFieldAPI();
	private WidgetAPI wAPI = APILocator.getWidgetAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

    public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public List<Map<String, Object>> getWidgets(){
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = null;
		List<Map<String,Object>> wids = new ArrayList<>();
		List<Structure> wstructures = new ArrayList<>();

			user = com.liferay.portal.util.PortalUtil.getUser(req);

		try {
			wstructures = wAPI.findAll(user, false);
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(this,e.getMessage(),e);
		}
		for (Structure structure : wstructures) {
			wids.add(structure.getMap());
		}
		return wids;
	}

	/**
	 * Lists the fields of a content type, except dividers, as a JavaScript array literal.
	 * A user who cannot read the content type, or an unknown content type, gets the same text a
	 * type without fields produces.
	 *
	 * @param structureInode inode of the content type
	 * @return the fields as {@code "[ { fieldinode:..., fieldname:..., ... }, ... ]"}
	 */
	public String getStructureFields (String structureInode) {
		if (!canRead(loggedInUser(), structureInode)) {
			return "[  ]";
		}
		Structure st = StructureFactory.getStructureByInode(structureInode);
		List fields = st.getFields();
		Iterator it = fields.iterator();
		StringBuffer names = new StringBuffer ();
		names.append("[ ");
		for (int i = 0; i < fields.size(); i++) {
			Field field = (Field)it.next();
			if (!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) &&
				  !field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
			names.append("{ fieldinode:\"" + field.getInode() + "\", fieldname:\"" + field.getFieldName() + "\", fielddbname:\"" + field.getFieldContentlet()  + "\", velocityname:\"" + field.getVelocityVarName() + "\" }");
			if (i < fields.size() - 1)
				names.append(", ");
		 }
		}
		names.append(" ]");
		return names.toString();
	}

	/**
	 * Returns the searchable and indexed fields of a content type, used by the content search and
	 * relationship screens. A user who cannot read the content type, or an unknown content type,
	 * gets an empty list.
	 *
	 * @param structureInode inode of the content type
	 * @return the property maps of the searchable fields, never {@code null}
	 */
	public List<Map> getSearchableStructureFields (String structureInode) {
		if (!canRead(loggedInUser(), structureInode)) {
			return new ArrayList<>();
		}
		Structure st = StructureFactory.getStructureByInode(structureInode);
		List<Field> fields = st.getFields();
		ArrayList<Map> searchableFields = new ArrayList<> ();
		for (Field field : fields) {
		  if (!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) &&
					  !field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
			if (field.isSearchable() && field.isIndexed()) {
				try {
					Map fieldMap = field.getMap();
					searchableFields.add(fieldMap);
				} catch (Exception e) {
					Logger.error(this, "Error getting the map of properties of a field: " + field.getInode());
				}
			}
		  }
		}

		return searchableFields;
	}


	public Map<String,Object> getKeyStructureFields (final String contentTypeInode) throws SystemException, PortalException, DotDataException, DotSecurityException {
		final Map<String,Object> result = new HashMap<>();
		final boolean allowImport = true;

		//Retrieving the current user
		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest request = ctx.getHttpServletRequest();
		final User user = PortalUtil.getUser(request);

		final ContentType contentTypeToImport = APILocator.getContentTypeAPI(user).find(contentTypeInode);
		final List<com.dotcms.contenttype.model.field.Field> fields = contentTypeToImport.fields();
		final ArrayList<Map> searchableFields = new ArrayList<> ();

		fields.stream().filter(field -> !(field instanceof RelationshipField))
				.filter(field -> !(field instanceof RelationshipsTabField))
				.map(field -> new LegacyFieldTransformer(field).asOldField())
				.filter(field -> isFieldTypeAllowedOnImportExport(field))
				.forEach(field -> addKeyFieldsToImport(field,searchableFields));

		result.put("keyStructureFields",searchableFields);
		result.put("allowImport", allowImport);

		return result;
	}

	private ArrayList<Map> addKeyFieldsToImport(final Field field, final ArrayList<Map> searchableFields){
		try {
			Map fieldMap = field.getMap();
			searchableFields.add(fieldMap);
		} catch (Exception e) {
			Logger.error(this, "Error getting the map of properties of a field: " + field.getInode());
		}

		return searchableFields;
	}

	public List<Map> getStructureCategories (String structureInode) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = PortalUtil.getUser(request);
		boolean respectFrontendRoles = false;
		if(user == null) {
			//Assuming is a front-end access
			respectFrontendRoles = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}

		if(!InodeUtils.isSet(structureInode) || structureInode.equals(Structure.STRUCTURE_TYPE_ALL)) {
			return new ArrayList<>();
		}

		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

		//Search for the given ContentType inode
		Structure st = null;
		ContentType foundContentType = contentTypeAPI.find(structureInode);
		if ( null != foundContentType ) {
			//Transform the found content type to a Structure
			st = new StructureTransformer(foundContentType).asStructure();
		}


		List<Map> catsMaps = new ArrayList<>();

		List<Field> fields = st.getFields();
		for (Field field : fields) {
			if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
				try {
					Category category = categoryAPI.find(field.getValues(), user, respectFrontendRoles);
					if(category != null && !catsMaps.contains(category.getMap()) && field.isSearchable())
						catsMaps.add(category.getMap());
				} catch (DotSecurityException e) {
					Logger.debug(this, "Ignoring a category the user has no permission on");
				}
			}
		}

		return catsMaps;
	}

	/**
	 * Returns all the categories and sub-categories of the category fields of a content type, used
	 * by the calendar category filter. A user who cannot read the content type, or an unknown
	 * variable name, gets an empty list and no category is looked up. READ on the content type is
	 * checked with the same front-end role rule as the category lookups: a back-end session does
	 * not use front-end roles, a front-end session does.
	 *
	 * @param structureName velocity variable name of the content type
	 * @param filter        regular expression the category names must match; blank keeps all
	 * @return the category maps, indented by level
	 * @throws DotDataException when the permissions or the categories cannot be loaded
	 * @throws PortalException  when the logged-in user cannot be resolved
	 * @throws SystemException  when the logged-in user cannot be resolved
	 */
	public List<Map<String, Object>> getCategoriesTree (String structureName, String filter)
		throws DotDataException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		PermissionAPI perAPI = APILocator.getPermissionAPI();

		//Retrieving the current user
		User user = PortalUtil.getUser(request);
		boolean respectFrontendRoles = false;
		if(user == null) {
			//Assuming is a front-end access
			respectFrontendRoles = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}

		Structure st = (Structure) CacheLocator.getContentTypeCache().getStructureByVelocityVarName(structureName);
		List<Map<String, Object>> maps = new ArrayList<>();
		if (null == st) {
			return maps;
		}
		if (!perAPI.doesUserHavePermission(st, PERMISSION_READ, user, respectFrontendRoles)) {
			logReadDenied(user);
			return maps;
		}

		List<Field> fields = st.getFields();
		for (Field field : fields) {
			if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
				try {
					Category category = categoryAPI.find(field.getValues(), user, respectFrontendRoles);

					if(!perAPI.doesUserHavePermission(category, perAPI.PERMISSION_READ, user))
						continue;

					Map<String, Object> catMap = category.getMap();
					String categoryName = (String) catMap.get("categoryName");
					catMap.put("categoryOrigName", categoryName);
					categoryName = "+ " + categoryName;
					catMap.put("categoryLevel",0);
					maps.add(catMap);
					maps.addAll(getChildrenCategories(category, 1, filter));
				} catch (DotSecurityException e) {
					Logger.debug(this, "getCategoriesTree - User " + (user == null?"anonymous":user.getUserId()) + ", can't access category field = " + field.getFieldName() +
							" on structure = " + structureName);
				}
			}
		}
		return maps;
	}

	private int maxLevel = 10;
	private List<Map<String, Object>> getChildrenCategories(Category category, int level, String filter)
	throws DotDataException, DotSecurityException, PortalException, SystemException
	{

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = PortalUtil.getUser(request);
		boolean respectFrontendRoles = false;
		if(user == null) {
			//Assuming is a front-end access
			respectFrontendRoles = true;
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}

		String separator = "    ";
		ArrayList<Map<String, Object>> categories = new ArrayList<>();
		if(level <= maxLevel)
		{
			int nextLevel = level + 1;
			List<Category> childCategories = categoryAPI.getChildren(category, user, respectFrontendRoles);
			//Get the separator
			String finalSeparator = "";
			for(int i = 0;i < level;i++)
			{
				finalSeparator += separator;
			}
			//Get the children categories of each child
			for(Category categoryAux : childCategories)
			{
				Map<String, Object> categoryMap = categoryAux.getMap();
				String categoryName = (String) categoryMap.get("categoryName");
				categoryMap.put("categoryOrigName", categoryName);
				categoryName = finalSeparator + "+ " + categoryName;
				categoryMap.put("categoryName",categoryName);
				categoryMap.put("categoryLevel",level);

				List<Map<String, Object>> children = getChildrenCategories(categoryAux,nextLevel, filter);
				if(!UtilMethods.isSet(filter) || (categoryName.matches(filter) || children.size() > 0)) {
					categories.add(categoryMap);
					categories.addAll(children);
				}
			}
		}
		return categories;
	}

	/**
	 * Renders a select or multi-select field as an HTML {@code select} with its options, used by
	 * the container code editor. A user who cannot read the field's content type, or an unknown
	 * field, gets an empty string.
	 *
	 * @param fieldInode inode of the field
	 * @return the HTML of the select, or an empty string
	 */
	public String getDropDownList (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
		if (!canReadTypeOf(f)) {
			return StringPool.BLANK;
		}
		if (f.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()))
			ddHtml.append("<select multiple size=\"4\" id=\"" + f.getVelocityVarName() + "\" name=\"" + f.getVelocityVarName() + "\">\n");
		else
			ddHtml.append("<select id=\"" + f.getVelocityVarName() + "\" name=\"" + f.getVelocityVarName() + "\">\n");
		String valuesSt = f.getValues();
		String[] values = valuesSt!=null ? valuesSt.split("[(\\r\\n)\\n\\s]") : new String[0];
		for (String value : values) {
			if (value.trim().equals("")) continue;
			String[] temp = value.split("\\|");
			String val = "", label = "";
			if (temp.length > 0)
				label = temp[0];
			if (temp.length > 1)
				val = temp[1];
			else
				val = temp[1];

			ddHtml.append("    <option value=\"" + val + "\" #if($UtilMethods.hasValue($!{" + f.getVelocityVarName() + "}, '" + val +"')) selected #end>"+label+"</option>\n");

		}
		ddHtml.append("</select>\n");
		return ddHtml.toString();
	}

	/**
	 * Renders the options of a select field as HTML {@code option} elements, used by the
	 * container code editor. A user who cannot read the field's content type, or an unknown
	 * field, gets an empty string.
	 *
	 * @param fieldInode inode of the field
	 * @return the HTML of the options, or an empty string
	 */
	public String getDropDownOptions (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
		if (!canReadTypeOf(f)) {
			return StringPool.BLANK;
		}
		String valuesSt = f.getValues();
		String[] values = valuesSt!=null ? valuesSt.split("[(\\r\\n)\\n\\s]") : new String[0];
		for (String value : values) {
			if (value.trim().equals("")) continue;
			String[] temp = value.split("\\|");
			String val = "", label = "";
			if (temp.length > 0)
				label = temp[0];
			if (temp.length > 1)
				val = temp[1];
			else
				val = temp[1];

			ddHtml.append("<option value=\"" + val + "\">"+label+"</option>\n");

		}
		return ddHtml.toString();
	}

	/**
	 * Renders the options of a radio field as HTML radio inputs, used by the container code
	 * editor. A user who cannot read the field's content type, or an unknown field, gets an empty
	 * string.
	 *
	 * @param fieldInode inode of the field
	 * @return the HTML of the radio inputs, or an empty string
	 */
	public String getRadioButtons (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
		if (!canReadTypeOf(f)) {
			return StringPool.BLANK;
		}
		String valuesSt = f.getValues();
		String[] values = valuesSt !=null ? valuesSt.split("[(\\r\\n)\\n\\s]") : new String[0];
		for (String value : values) {
			if (value.trim().equals("")) continue;
			String[] temp = value.split("\\|");
			String val = "", label = "";
			if (temp.length > 0)
				label = temp[0];
			if (temp.length > 1)
				val = temp[1];
			else
				val = temp[1];

			ddHtml.append("<input type=\"radio\" value=\"" + val + "\" id=\"" + f.getVelocityVarName() + "\" name=\"" + f.getVelocityVarName() + "\" #if($!{" + f.getVelocityVarName() + "} == '" + val +"') checked #end> " + label + "<br>\n");

		}
		return ddHtml.toString();
	}

	/**
	 * Renders the options of a checkbox field as HTML checkbox inputs, used by the container code
	 * editor. A user who cannot read the field's content type, or an unknown field, gets an empty
	 * string.
	 *
	 * @param fieldInode inode of the field
	 * @return the HTML of the checkbox inputs, or an empty string
	 */
	public String getCheckboxes (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
		if (!canReadTypeOf(f)) {
			return StringPool.BLANK;
		}
		String valuesSt = f.getValues();
		String[] values = valuesSt!=null ? valuesSt.split("[(\\r\\n)\\n\\s]") : new String[0];
		for (String value : values) {
			if (value.trim().equals("")) continue;
			String[] temp = value.split("\\|");
			String val = "", label = "";
			if (temp.length > 0)
				label = temp[0];
			if (temp.length > 1)
				val = temp[1];
			else
				val = temp[1];

			ddHtml.append("<input type=\"checkbox\" value=\"" + val + "\" id=\"" + f.getVelocityVarName() + "\" name=\"" + f.getVelocityVarName() + "\" #if($UtilMethods.hasValue($!{" + f.getVelocityVarName() + "}, '" + val +"')) checked #end> " + label + "<br>\n");

		}
		return ddHtml.toString();
	}

	/**
	 * Saves a new sort order for fields of a content type, as sent by the content type editor.
	 * <p>The logged-in user needs EDIT_PERMISSIONS on the content type and every field in the
	 * list must belong to it. {@link FieldReorderHelper} checks the user against the content type
	 * before it looks at the fields or the sort orders, validates every entry before saving any
	 * field, and saves the fields as the logged-in user, which also updates the content type's
	 * modification date. A request that breaks a rule throws a {@link DotSecurityException} and
	 * changes nothing. A request from a user who may reorder the fields, with a sort order that is
	 * not a whole number, also changes nothing and returns the "could not be reordered" text.</p>
	 *
	 * @param structureInode inode of the content type whose fields are reordered
	 * @param inodeList      comma-separated {@code "<field inode> @ <sort order>"} entries;
	 *                       entries without the separator are skipped
	 * @return the localized success message, or {@code "Fields-could-not-be-reorderd"} when the
	 *         order could not be read or saved
	 * @throws DotSecurityException when no user is logged in or the request breaks a reorder rule
	 * @throws PortalException      when the logged-in user cannot be resolved
	 * @throws SystemException      when the logged-in user cannot be resolved
	 */
	@SuppressWarnings("deprecation")
	public String reorderfields(final String structureInode, final String inodeList)
			throws DotSecurityException, PortalException, SystemException {

		final User user = DwrUtil.getLoggedInUser();
		final Company company = PublicCompanyFactory.getDefaultCompany();
		try {
			FieldReorderHelper.reorder(user, structureInode, sortOrdersByFieldInode(inodeList));

			return LanguageUtil.get(company.getCompanyId(), company.getLocale(),
					"message.structure.reorderfield");
		} catch (final DotSecurityException e) {
			throw e;
		} catch (final NumberFormatException e) {
			Logger.info(StructureAjax.class,
					"Fields were not reordered: every sort order must be a whole number");
			return "Fields-could-not-be-reorderd";
		} catch (final Exception e) {
			Logger.error(StructureAjax.class, "The fields could not be reordered", e);
			return "Fields-could-not-be-reorderd";
		}
	}

	/**
	 * Splits the {@code "<field inode> @ <sort order>"} entries sent by the content type editor
	 * into a sort order per field inode, keeping the values as sent. Entries without the
	 * separator, such as the empty one before the editor's leading comma, are skipped.
	 */
	private static Map<String, String> sortOrdersByFieldInode(final String inodeList) {
		final Map<String, String> sortOrders = new LinkedHashMap<>();
		if (null == inodeList) {
			return sortOrders;
		}
		for (final String entry : inodeList.split(",")) {
			final String[] tokens = entry.split(" @ ");
			if (tokens.length < 2) {
				continue;
			}
			sortOrders.put(tokens[0], tokens[1]);
		}
		return sortOrders;
	}

	public Map<String, Object> getStructureDetails(String StructureInode) throws PortalException, SystemException, DotDataException {
	    HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = userWebAPI.getLoggedInUser(req);
        
        // StructureInode is a special inode to get ALL content types (used in combo boxes)
        if(StructureInode.equals(Structure.STRUCTURE_TYPE_ALL)) {
            Map<String, Object> structureDetails = new HashMap<>();
            structureDetails.put("inode", Structure.STRUCTURE_TYPE_ALL);
            structureDetails.put("name", LanguageUtil.get(user, "all"));
            structureDetails.put("velocityVarName", StringPool.BLANK);
            return structureDetails;
        }

        // StructureInode is a valid inode different than ALL
        Structure str = CacheLocator.getContentTypeCache().getStructureByInode(StructureInode);

        if(!APILocator.getPermissionAPI().doesUserHavePermission(str, PERMISSION_READ, user, false))
            return  new HashMap<>();

		Map<String, Object> structureDetails = new HashMap<>();
		structureDetails.put("inode", StructureInode);
		structureDetails.put("name", str.getName());
		structureDetails.put("velocityVarName", str.getVelocityVarName());
		return structureDetails;
	}


	public Map<String, Object> fetchStructures (Map<String, String> query, Map<String, String> queryOptions, int start, int count,
			List<String> sort) throws PortalException, SystemException, DotDataException, DotSecurityException {

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		if(count<=0)count=10;
		List<Structure> fullListStructures = new ArrayList<>();
		try{
			String structureTypeStr = queryOptions.get("structureType");
			Integer structureType = null;
			if(UtilMethods.isSet(structureTypeStr)){
				structureType = Integer.parseInt(structureTypeStr);
			}
			String filter = query.get("name");
			if(UtilMethods.isSet(filter)){
				filter = filter.replaceAll("\\*", "");
				filter = filter.replaceAll("\\?", "");
			}
			fullListStructures.addAll(StructureFactory.findStructuresUserCanUse(user, filter, structureType, start, start>0?count:count+1));
		}catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		Map<String, Object> results = new HashMap<>();
		List<Map<String, Object>> list = new ArrayList<> ();
		for(Structure structure : fullListStructures) {
			Map<String, Object> stMap = structure.getMap();
			list.add(stMap);
		}
		results.put("totalResults", list.size());
		results.put("list", list);

		return results;
	}

	/**
	 * Returns the properties of one content type, used by the content type selectors. A user who
	 * cannot read the content type, or an unknown id, gets {@code null}, the answer the selectors
	 * already handle as "not found".
	 *
	 * @param id inode of the content type
	 * @return the content type properties, or {@code null}
	 */
	public Map<String, Object> fetchByIdentity(String id) {
		if (!canRead(loggedInUser(), id)) {
			return null;
		}
		Structure st = CacheLocator.getContentTypeCache().getStructureByInode(id);
		if(st!=null){
			return st.getMap();
		}
		return null;
	}

	/**
	 * Lists the containers that use a content type, shown before the type is deleted. A user who
	 * cannot read the content type, or an unknown content type, gets no containers and a size of
	 * zero, and the containers are not looked up. The containers are looked up with the inode of
	 * the content type that was loaded for the READ check. A failure to load the content type is
	 * thrown, not answered with a size of zero, because the delete dialog reads zero as "no
	 * container uses this type". The cause is logged, and the exception carries a fixed message,
	 * as DWR sends the message to the page.
	 *
	 * @param structureInode inode of the content type
	 * @return a map with the {@code containers} list and its {@code size}
	 * @throws DotDataException when the content type or the containers cannot be loaded
	 */
	public Map<Object,Object> checkDependencies(String structureInode) throws DotDataException{

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		Map<Object,Object> result = new HashMap<>();
		List<Map<String,String>> containersList = new ArrayList<>();
		final ContentType readableType;
		try {
			readableType = loadReadableType(loggedInUser(), structureInode);
		} catch (final DotDataException e) {
			Logger.error(StructureAjax.class,
					"The content type could not be loaded to list the containers that use it", e);
			throw new DotDataException("The content type could not be loaded");
		}
		if (null == readableType) {
			result.put("containers", containersList);
			result.put("size", 0);
			return result;
		}

		// checking if there are containers using this structure
		List<Container> containers=APILocator.getContainerAPI().findContainersForStructure(readableType.inode(), true);
		Map<String, Container> containersInUse = new HashMap<>();		
		
		for(Container c : containers) {
			try {
				containersInUse.put(c.getIdentifier(), c);
			} catch (Exception e) {
			}
 		}
		
		for(Container c : containersInUse.values()){
			String hostTitle = "";
			try {
				hostTitle = APILocator.getHostAPI().findParentHost(c, PortalUtil.getUser(request), false).getTitle();
			} catch (Exception e) {}
			Map<String,String> containerMap = new HashMap<>();
			containerMap.put("title", hostTitle + " : " + c.getTitle());
			containerMap.put("identifier", c.getIdentifier());
			containerMap.put("inode", c.getInode());
			containersList.add(containerMap);
		}
		
		result.put("containers", containersList);
		result.put("size",containersList.size());		
		return result;
	}
	
	public void setSelectedStructure(String StructureVelocityVarName){
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

	}

	/**
	 * Returns the user of the current DWR request as resolved by {@link UserWebAPI}, or
	 * {@code null} when nobody is logged in. {@link #fetchByIdentity(String)},
	 * {@link #checkDependencies(String)}, {@link #getStructureFields(String)},
	 * {@link #getSearchableStructureFields(String)} and the four field option readers use it, so a
	 * request without a user gets the same empty answer from them as a type the user cannot read,
	 * and no security log line is written for it. The other methods resolve their user themselves;
	 * {@link #reorderfields(String, String)} uses {@link DwrUtil#getLoggedInUser()}, which throws a
	 * {@link DotSecurityException} when nobody is logged in.
	 */
	@Nullable
	private User loggedInUser() {
		return userWebAPI.getLoggedInUser(WebContextFactory.get().getHttpServletRequest());
	}

	/**
	 * Tells whether the user may read a content type: {@code true} when
	 * {@link #findReadableType(User, String)} loads it.
	 */
	private boolean canRead(final User user, final String typeInode) {
		return null != findReadableType(user, typeInode);
	}

	/**
	 * Loads a content type the user may read. The type is loaded with
	 * {@code ContentTypeAPI.find}, the same READ check {@link #getKeyStructureFields(String)} and
	 * {@link #getStructureCategories(String)} rely on. That check honours front-end roles, unlike
	 * the older check in {@link #getStructureDetails(String)}: {@code APILocator.getContentTypeAPI(user)}
	 * is created with {@code respectFrontendRoles=false}, but {@code ContentTypeAPIImpl#find}
	 * checks READ with the three-argument {@code PermissionAPI#doesUserHavePermission}, which
	 * respects front-end roles. {@link #getCategoriesTree(String, String)} does not use it, as
	 * that method also serves front-end sessions and applies its own front-end role rule.
	 * <p>No user, a blank or unknown id, a type the user cannot read, and a failure to load the
	 * type all answer {@code null}. Only a type the user cannot read is a denial and writes one
	 * security log line. A failure to load the type is logged as a warning of this class, not as
	 * a denial. Neither line holds a value from the request.</p>
	 *
	 * @return the content type, or {@code null} in any of the cases above
	 */
	@Nullable
	private ContentType findReadableType(@Nullable final User user, final String typeInode) {
		try {
			return loadReadableType(user, typeInode);
		} catch (final DotDataException e) {
			Logger.warn(StructureAjax.class,
					"The content type could not be loaded to check READ permission", e);
			return null;
		}
	}

	/**
	 * Loads a content type the user may read, like {@link #findReadableType(User, String)}, but a
	 * failure to load the type is thrown to the caller instead of answering {@code null}. Used
	 * where an empty answer would be taken as a fact about the type, such as "no container uses
	 * it".
	 *
	 * @return the content type, or {@code null} for no user, a blank or unknown id, or a type the
	 *         user cannot read
	 * @throws DotDataException when the content type cannot be loaded
	 */
	@Nullable
	private ContentType loadReadableType(@Nullable final User user, final String typeInode)
			throws DotDataException {
		if (null == user || !UtilMethods.isSet(typeInode)) {
			return null;
		}
		try {
			return APILocator.getContentTypeAPI(user).find(typeInode);
		} catch (final NotFoundInDbException e) {
			return null;
		} catch (final DotSecurityException e) {
			logReadDenied(user);
			return null;
		}
	}

	/**
	 * Writes the single security log line for a caller who gets no content type data because READ
	 * permission on the content type is missing. Only the user id is logged.
	 */
	private static void logReadDenied(final User user) {
		SecurityLogger.logInfo(StructureAjax.class, String.format(
				"User %s was not given content type data: READ permission on the content type is required",
				null == user ? "[anonymous]" : user.getUserId()));
	}

	/**
	 * Tells whether the logged-in user may read the content type that owns a field; an unknown
	 * field, which has no content type, answers {@code false}.
	 */
	private boolean canReadTypeOf(final Field field) {
		return null != field && canRead(loggedInUser(), field.getStructureInode());
	}

}
