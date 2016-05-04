package com.dotmarketing.portlets.structure.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.repackage.org.jboss.util.Strings;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.action.EditFieldAction;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;


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
		List<Map<String,Object>> wids = new ArrayList<Map<String,Object>>();
		List<Structure> wstructures = new ArrayList<Structure>();
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (PortalException e) {
			Logger.error(this,e.getMessage(),e);
		} catch (SystemException e) {
			Logger.error(this,e.getMessage(),e);
		}
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

	public String getStructureFields (String structureInode) {
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

	public List<Map> getSearchableStructureFields (String structureInode) {
		Structure st = StructureFactory.getStructureByInode(structureInode);
		List<Field> fields = st.getFields();
		ArrayList<Map> searchableFields = new ArrayList<Map> ();
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

	public List<Map> getStructureSearchFields (String structureInode) {
		Structure st = StructureFactory.getStructureByInode(structureInode);
		List<Field> fields = st.getFields();
		ArrayList<Map> searchableFields = new ArrayList<Map> ();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
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
		Structure structure = StructureFactory.getStructureByInode(structureInode);
		req.getSession().setAttribute("selectedStructure", structureInode);

		return searchableFields;
	}

	public Map<String,Object> getKeyStructureFields (String structureInode) {
		Map<String,Object> result = new HashMap<String, Object>();
		boolean allowImport = true;
		
		Structure struct = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
		List<Field> fields = struct.getFields();
		ArrayList<Map> searchableFields = new ArrayList<Map> ();
		for (Field field : fields) {
			if (!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) &&
					!field.getFieldType().equals(Field.FieldType.FILE.toString()) &&
					!field.getFieldType().equals(Field.FieldType.IMAGE.toString())&&
					!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) &&
					!field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
				try {
					Map fieldMap = field.getMap();
					searchableFields.add(fieldMap);
				} catch (Exception e) {
					Logger.error(this, "Error getting the map of properties of a field: " + field.getInode());
				}
			}
		}

		try {
			WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(struct);
			if(scheme.isMandatory() && !UtilMethods.isSet(scheme.getEntryActionId())){
				allowImport = false;
			}
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		}

		result.put("keyStructureFields",searchableFields);
		result.put("allowImport", allowImport);

		return result;
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

		if(!InodeUtils.isSet(structureInode))
			return new ArrayList<Map>();

		Structure st = (Structure) InodeFactory.getInode(structureInode, Structure.class);

		List<Map> catsMaps = new ArrayList<Map>();

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
	 * Returns all the categories and sub-categories of the given structure
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

		Structure st = (Structure) CacheLocator.getContentTypeCache().getStructureByName(structureName);
		List<Map<String, Object>> maps = new ArrayList<Map<String,Object>>();


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
		ArrayList<Map<String, Object>> categories = new ArrayList<Map<String, Object>>();
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

	public String getDropDownList (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
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

	public String getDropDownOptions (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
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

	public String getRadioButtons (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
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

	public String getCheckboxes (String fieldInode) {
		StringBuffer ddHtml = new StringBuffer ();
		Field f = FieldFactory.getFieldByInode(fieldInode);
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
	 * This methods is used to reorder the structure fields, and return this fields Map ordered.
	 * @param structureInode
	 * @return List<Map>
	 * @author Oswaldo Gallango
	 * @since 1.5
	 * @version 1.0
	 */
	@SuppressWarnings("deprecation")
	public String reorderfields(String structureInode, String inodeList){

		Structure structure = StructureFactory.getStructureByInode(structureInode);
		Company d = PublicCompanyFactory.getDefaultCompany();
		try
		{
			String[] params = inodeList.split(",");
			for(String tempField : params)
			{
				String[] tokens = tempField.split(" @ ");
				if(tokens ==null || tokens.length<2){
					continue;
				}
				String fieldInode = tokens[0];
				String parameterValue = tokens[1];
				Field field = FieldFactory.getFieldByInode(fieldInode);
				field.setSortOrder(Integer.parseInt(parameterValue));
				FieldFactory.saveField(field);
			}

            //Cleaning cache
            FieldsCache.removeFields( structure );
            CacheLocator.getContentTypeCache().remove( structure );

            //Save the structure in order to update the modDate
            StructureFactory.saveStructure( structure );

			//VirtualLinksCache.clearCache();
			//String message = "message.structure.reorderfield";
			//SessionMessages.add(request, "message",message);
			return LanguageUtil.get(d.getCompanyId(),d.getLocale(), "message.structure.reorderfield");
		}
		catch(Exception ex)
		{
			Logger.error(EditFieldAction.class,ex.toString());
			return "Fields-could-not-be-reorderd";
		}






	}

	public Map<String, Object> getStructureDetails(String StructureInode) throws PortalException, SystemException, DotDataException {
	    HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = userWebAPI.getLoggedInUser(req);
        
        // StructureInode is a special inode to get ALL content types (used in combo boxes)
        if(StructureInode.equals(Structure.STRUCTURE_TYPE_ALL)) {
            Map<String, Object> structureDetails = new HashMap<String, Object>();
            structureDetails.put("inode", Structure.STRUCTURE_TYPE_ALL);
            structureDetails.put("name", LanguageUtil.get(user, "all"));
            structureDetails.put("velocityVarName", Strings.EMPTY);
            return structureDetails;
        }

        // StructureInode is a valid inode different than ALL
        Structure str = CacheLocator.getContentTypeCache().getStructureByInode(StructureInode);

        if(!APILocator.getPermissionAPI().doesUserHavePermission(str, PERMISSION_READ, user, false))
            return  new HashMap<String, Object>();

		Map<String, Object> structureDetails = new HashMap<String, Object>();
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
		List<Structure> fullListStructures = new ArrayList<Structure>();
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
		Map<String, Object> results = new HashMap<String, Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>> ();
		for(Structure structure : fullListStructures) {
			Map<String, Object> stMap = structure.getMap();
			list.add(stMap);
		}
		results.put("totalResults", list.size());
		results.put("list", list);

		return results;
	}

	public Map<String, Object> fetchByIdentity(String id) throws DotDataException, DotSecurityException {
		Structure st = CacheLocator.getContentTypeCache().getStructureByInode(id);
		if(st!=null){
			return st.getMap();
		}
		return null;
	}

	public Map<Object,Object> checkDependencies(String structureInode) throws DotDataException{

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		
		Map<Object,Object> result = new HashMap<Object, Object>();
		List<Map<String,String>> containersList = new ArrayList<Map<String,String>>();
		
		// checking if there are containers using this structure
		List<Container> containers=APILocator.getContainerAPI().findContainersForStructure(structureInode);
		Map<String, Container> containersInUse = new HashMap<String, Container>();		
		
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
			Map<String,String> containerMap = new HashMap<String, String>();
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
		
		if(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(StructureVelocityVarName) != null){
			request.getSession().setAttribute("selectedStructure", CacheLocator.getContentTypeCache().getStructureByVelocityVarName(StructureVelocityVarName).getInode());	
		}
	}

}
