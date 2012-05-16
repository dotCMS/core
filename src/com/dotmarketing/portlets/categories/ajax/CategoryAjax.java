package com.dotmarketing.portlets.categories.ajax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.csvreader.CsvReader;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 * @author David
 */
public class CategoryAjax {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	private int maxLevel = 5;

//	/**
//	 * Returns all the categories and sub-categories of the given entity
//	 * @param entityName The name of the entity to search over
//	 * @param filter A regular expression to use as filter if null, then no filter will be applied
//	 * @return
//	 * @throws DotDataException
//	 * @throws DotSecurityException
//	 * @throws SystemException
//	 * @throws PortalException
//	 */
//	public List<Map<String, Object>> getEntityCategories (String entityName, String filter)
//			throws DotDataException, PortalException, SystemException, DotSecurityException {
//		List<Category>  categories = EntityFactory.getEntityCategories(EntityFactory.getEntity(entityName));
//		List<Map<String, Object>> maps = new ArrayList<Map<String,Object>>();
//		for (Category cat : categories) {
//			Map<String, Object> catMap = cat.getMap();
//			String categoryName = (String) catMap.get("categoryName");
//			catMap.put("categoryOrigName", categoryName);
//			categoryName = "+ " + categoryName;
//			catMap.put("categoryLevel",0);
//			maps.add(catMap);
//			maps.addAll(getChildrenCategories(cat, 1, filter));
//		}
//		return maps;
//	}

	/**
	 * Returns all the sub-categories multiple levels depth of the given category
	 * @param catName The inode/key/name of the parent category
	 * @return
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotSecurityException
	 */
	public List<Map<String, Object>> getSubCategories (String cat, String filter) throws DotDataException, PortalException, SystemException, DotSecurityException {
		return getChildrenCategories(cat, filter);
	}

	/**
	 * Returns all the sub-categories multiple levels depth of the given category
	 * @param catName The name of the parent category
	 * @return
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotSecurityException
	 * @deprecated use getChildrenCategories(String catName, String filter) instead
	 */
	public List<Map<String, Object>> getChildrenCategories (String catName) throws DotDataException, PortalException, SystemException, DotSecurityException {

		return getChildrenCategories(catName, null);

	}

	/**
	 * Returns all the sub-categories multiple levels depth of the given category
	 * @param category tries first to parse by inode, if not tries by key if not tries by name, passing the name of a category is
	 * 			discouraged and could be removed in later versions
	 * @param filter A filter regular expression
	 * @return
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("deprecation")
	public List<Map<String, Object>> getChildrenCategories (String categoryInode, String filter)
			throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

		Category cat = null;
		try {
			//int inode = Integer.parseInt(category);
			cat = categoryAPI.find(categoryInode, user, respectFrontendRoles);
		} catch (NumberFormatException e) { }
		if(cat == null)
			cat = categoryAPI.findByKey(categoryInode, user, respectFrontendRoles);
		if(cat == null)
			cat = categoryAPI.findByName(categoryInode, user, respectFrontendRoles);

		return findChildrenCategories(cat.getInode(), filter);
	}

	/**
	 * Returns all sub-categories multiple levels depth of the given parent category,
	 * it returns them on a single list adding to each category a property that denotes
	 * depth
	 * @param catName The name of the parent category
	 * @param filter A regular expression to use as filter if null, then no filter will be applied
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws SystemException
	 * @throws PortalException
	 */
	public List<Map<String, Object>> findChildrenCategories (String catInode, String filter)
			throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();

		//Retrieving the current user
		User user = userWebAPI.getLoggedInUser(request);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

		Category cat = categoryAPI.find(catInode, user, respectFrontendRoles);
		List<Category> cats = categoryAPI.getChildren(cat, user, respectFrontendRoles);
		List<Map<String, Object>> mapsList = new ArrayList<Map<String, Object>> ();
		for (Category childCat : cats) {
			Map<String, Object> childCategoryMap = childCat.getMap();
			String categoryName = (String) childCategoryMap.get("categoryName");
			childCategoryMap.put("categoryOrigName", categoryName);
			categoryName = "+ " + categoryName;
			childCategoryMap.put("categoryName",categoryName);
			childCategoryMap.put("categoryLevel",0);
			List<Map<String, Object>> children = getChildrenCategories(childCat, 1, filter);
			if(!UtilMethods.isSet(filter) || children.size() > 0 || ((String)childCategoryMap.get("categoryOrigName")).matches(filter)) {
				mapsList.add(childCategoryMap);
				mapsList.addAll(children);
			}
		}
		return mapsList;
	}

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
				if(!UtilMethods.isSet(filter) || (((String)categoryMap.get("categoryOrigName")).matches(filter) || children.size() > 0)) {
					categories.add(categoryMap);
					categories.addAll(children);
				}
			}
		}
		return categories;
	}

	public Integer deleteSelectedCategories(String[] inodes) throws Exception {
		Integer catsWithDependencies = 0;

		for (String inode : inodes) {
			int result = deleteCategory(inode);

			if(result==2) {
				catsWithDependencies++;
			}
		}


		if(catsWithDependencies>0) {
			return 1;
		}

		return 0;
	}

	public Integer deleteCategories(String contextInode) throws Exception {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = uWebAPI.getLoggedInUser(request);
		Integer catsWithDependencies = 0;

		if(UtilMethods.isSet(contextInode)) {
			Category contextCat = categoryAPI.find(contextInode, user, false);
			List<Category> catsToDelete =  categoryAPI.getChildren(contextCat, user, false);
			for (Category category : catsToDelete) {
				int result = deleteCategory(category.getInode());

				if(result==2) {
					catsWithDependencies++;
				}
			}
		} else {
			List<Category> catsToDelete =  categoryAPI.findTopLevelCategories(user, false);
			for (Category category : catsToDelete) {
				int result = deleteCategory(category.getInode());

				if(result==2) {
					catsWithDependencies++;
				}
			}
		}

		if(catsWithDependencies>0) {
			return 1;
		}

		return 0;
	}

	public Integer deleteCategory(String inode) throws Exception {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = uWebAPI.getLoggedInUser(request);
		Category cat = categoryAPI.find(inode, user, false);

		try {
			if(!categoryAPI.hasDependencies(cat)){
				categoryAPI.delete(cat, user, false);
				return 0;
			}
			else{
				return 2; // has children
			}
		} catch(DotDataException e) {
			return 1;
		}
	}

	public Integer saveOrUpdateCategory(Boolean save, String inode, String name, String var, String key, String keywords) throws Exception {
		return saveOrUpdateCategory(save, inode, name, var, key, keywords, null, !save);
	}

	private Integer saveOrUpdateCategory(Boolean save, String inode, String name, String var, String key, String keywords, String sort, boolean merge)
			throws Exception {

		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = uWebAPI.getLoggedInUser(request);
		Category parent = null;
		Category cat = new Category();
		cat.setCategoryName(name);
		cat.setKey(key);
		cat.setCategoryVelocityVarName(var);
		cat.setSortOrder(sort);
		cat.setKeywords(keywords);

		if(UtilMethods.isSet(inode) && !save) {
			cat.setInode(inode);
		}

		setVelocityVarName(cat, var, name);

		if(InodeUtils.isSet(inode) && save)
			parent = (Category) categoryAPI.find(inode, user, false);

		if(merge) { // add/edit

			if(save) { // Importing
				if(UtilMethods.isSet(key)) {
					cat = categoryAPI.findByKey(key, user, false);
						if(cat==null) {
							cat = new Category();
							cat.setKey(key);
						}

						cat.setCategoryName(name);
						setVelocityVarName(cat, var, name);
						cat.setSortOrder(sort);
					}
			} else { // Editing
				cat = categoryAPI.find(inode, user, false);
				cat.setCategoryName(name);
				setVelocityVarName(cat, var, name);
//				cat.setSortOrder(sort);
				cat.setKeywords(keywords);

				// avoiding editing key with a already used one
				Category aux = categoryAPI.findByKey(key, user, false);
				if(aux!=null && !aux.getInode().equals(cat.getInode())) {
					cat.setKey(getUniqueKey(key, user, 1));
				} else {
					cat.setKey(key);
				}

			}


		} else { // replace

			cat.setCategoryName(name);
			setVelocityVarName(cat, var, name);
			cat.setSortOrder(sort);

			if(UtilMethods.isSet(key)) {
				Category aux = categoryAPI.findByKey(key, user, false);
				if(aux!=null) {
					cat.setKey(getUniqueKey(key, user, 1));
				} else {
					cat.setKey(key);
				}
			}
		}

		try {
			categoryAPI.save(parent, cat, user, false);
		} catch (DotSecurityException e) {
			return 1;
		}

		return 0;
	}

	private void setVelocityVarName(Category cat, String catvelvar, String catName) throws DotDataException, DotSecurityException {
		List <Category> categories=categoryAPI.findAll(APILocator.getUserAPI().getSystemUser(), false);
		List <String> velocityvarnames=new ArrayList <String>();
		int found=0;
		Boolean Proceed=false;
		if(!UtilMethods.isSet(catvelvar)){
			catvelvar=VelocityUtil.convertToVelocityVariable(catName);
			Proceed=true;
		}
		if(!InodeUtils.isSet(cat.getInode())|| Proceed){
			if(VelocityUtil.isNotAllowedVelocityVariableName(catvelvar)){
				found++;
			}
			for(Category categ: categories){
				velocityvarnames.add(categ.getCategoryVelocityVarName());
			}
			for(String velvar: velocityvarnames){
				if(velvar!=null){
					if(catvelvar.equals(velvar)){
						found++;
					}
					else if (velvar.contains(catvelvar))
					{
						String number=velvar.substring(catvelvar.length());
						if(RegEX.contains(number,"^[0-9]+$")){
							found ++;
						}
					}
				}
			}
			if(found>0){
				catvelvar= catvelvar + Integer.toString(found);
				cat.setCategoryVelocityVarName(catvelvar);
			} else {
				cat.setCategoryVelocityVarName(catvelvar);
			}
		}
	}

	private String getUniqueKey(String key, User user, Integer consecutive) throws DotDataException, DotSecurityException {

		String validKey = key+"-"+consecutive;

		if(categoryAPI.findByKey(validKey, user, false)!=null) {
			validKey = getUniqueKey(key, user, ++consecutive);
		}

		return validKey;
	}

	public boolean getPermission(String inode) throws Exception {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = uWebAPI.getLoggedInUser(request);
		Category cat = categoryAPI.find(inode, user, false);

		try {
			if(!categoryAPI.hasDependencies(cat)){
				categoryAPI.delete(cat, user, false);
				return true;
			}
			else{
				return false;
			}
		} catch(DotDataException e) {
			return false;
		}

	}

	public boolean sortCategory(String inode, String sortOrder) throws Exception {
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		User user = uWebAPI.getLoggedInUser(request);
		Category cat = categoryAPI.find(inode, user, false);

		if(UtilMethods.isSet(cat)) {
			cat.setSortOrder(Integer.parseInt(sortOrder));
			try {
				categoryAPI.save(null, cat, user, false);
			} catch (DotSecurityException e) {
				return false;
			}
			return true;
		}
		return false;
	}


	public Integer importCategories(String contextInode, String filter, byte[] uploadFile, String exportType) {
		try {
			UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();
			User user = uWebAPI.getLoggedInUser(request);
			String content = new String(uploadFile);
			StringReader sr = new StringReader(content);
			BufferedReader br = new BufferedReader(sr);
			Integer catsWithDependencies = 0;

			if(exportType.equals("replace")) {
				if(UtilMethods.isSet(contextInode)) {
					Category contextCat = categoryAPI.find(contextInode, user, false);
					List<Category> catsToDelete =  categoryAPI.getChildren(contextCat, user, false);
					for (Category category : catsToDelete) {
						int result = deleteCategory(category.getInode());

						if(result==2) {
							catsWithDependencies++;
						}
					}
				} else {
					List<Category> catsToDelete =  categoryAPI.findTopLevelCategories(user, false);
					for (Category category : catsToDelete) {
						int result = deleteCategory(category.getInode());

						if(result==2) {
							catsWithDependencies++;
						}
					}
				}

				saveOrUpdateCat(contextInode, br, false);
			} else if(exportType.equals("merge")) {
				saveOrUpdateCat(contextInode, br, true);
			}

			br.close();

			if(catsWithDependencies>0) {
				return 1;
			}

			return 0;

		} catch(Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	private void saveOrUpdateCat(String contextInode, BufferedReader br, Boolean merge) throws IOException, Exception {
		CsvReader csvreader = new CsvReader(br);
		csvreader.setSafetySwitch(false);
		csvreader.readHeaders();
		String[] csvLine;

		while (csvreader.readRecord()) {
			csvLine = csvreader.getValues();
			try {
				saveOrUpdateCategory(true, contextInode, csvLine[0], csvLine[2], csvLine[1], null, csvLine[3], merge);

			} catch(Exception e) {
				Logger.error(this, "Error trying to save/update the categories csv row: name=" +csvLine[0]+ ", variable=" + csvLine[2] + ", key=" + csvLine[1] + ", sort=" + csvLine[3] , e);
			}
		}

		csvreader.close();
		br.close();
	}

}
