package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class CategoriesWebAPI implements ViewTool {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();

	private HttpServletRequest request;
	Context ctx;
	User user = null;

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
		HttpSession ses = request.getSession(false);
		
		if (ses != null) {
			user = (User) ses.getAttribute(WebKeys.CMS_USER);
			if (user == null && ses.getAttribute("USER_ID") != null) {
				String userId = (String) ses.getAttribute("USER_ID");
				try {
					user = APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), false);
				} catch (NoSuchUserException e) {
					Logger.error(this, "A System error happend while trying to retrieve user  : " + userId, e);
				} catch (DotDataException e) {
					Logger.error(this, "A System error happend while trying to retrieve user  : " + userId, e);
				} catch (DotSecurityException e) {
					Logger.error(this, "A System error happend while trying to retrieve user  : " + userId, e);
				}
			}
		}
	}

	public List<Category> getChildrenCategoriesByKey(String key) {
		if (key == null) {
			return new ArrayList<Category>();
		}
		try {
			Category cat = categoryAPI.findByKey(key, user, true);
			if (!InodeUtils.isSet(cat.getInode())) {
				return new ArrayList<Category>();
			}
			return categoryAPI.getChildren(cat, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories with key : " + key);
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
		}
		return new ArrayList<Category>();

	}

	public Category getCategoryByKey(String key) {
		if(!UtilMethods.isSet(key)){
			return null;
		}
		try {
			return categoryAPI.findByKey(key, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the category");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve category : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
			return null;
		}
	}

	/**
	 * 
	 * @param name
	 * @return
	 * @deprecated Multiple categories can have the same name so this method
	 *             should be avoid to search a single category
	 */
	public Category getCategoryByName(String name) {
		if(!UtilMethods.isSet(name)){
			return null;
		}
		try {
			return categoryAPI.findByName(name, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the category");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve the category : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve the category : ", e);
			return null;
		}
	}

	public List<Category> getChildrenCategories(Category cat) {
		try {
			return categoryAPI.getChildren(cat, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Category> getChildrenCategories(Inode inode) {
		try {
			List<Category> categories = InodeFactory.getChildrenClass(inode, Category.class);
			return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, true, user);
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Category> getChildrenCategories(String inode) {
		try {
			Inode inodeObj = new Inode();
			inodeObj.setInode(inode);
			List<Category> categories = InodeFactory.getChildrenClass(inodeObj, Category.class);
			return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, true, user);
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}
	
	/**
	 * Retrieves the list of categories, their children categories and grand-children categories upto the specified maxDepth.
	 * 
	 * @param inode CategoryInode for which to get the children categories.
	 * @param includeGrandChildren 
	 * @param maxDepth
	 * @return
	 */
	public List<Category> getChildrenCategories(String inode, boolean includeGrandChildren, int maxDepth) {		
		try {
			List<Category> categories = new ArrayList<Category>();
			Category cat = categoryAPI.find(inode, user, true);
			List<Category> cats = categoryAPI.getChildren(cat, user, true);

			if(!UtilMethods.isSet(maxDepth))
				maxDepth = 5;
			for (Category childCat : cats) {
				categories.add(childCat);
				if(includeGrandChildren)
					categories.addAll(getChildrenCategories(childCat, 1 , maxDepth));
			}
			
			return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, true, user);
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}
	
	private List<Category> getChildrenCategories(Category parentCategory, int level, int maxDepth) throws DotDataException, DotSecurityException{
		
		List<Category> result = new ArrayList<Category>();
		
		if(level <= maxDepth)
		{
			int nextLevel = level + 1;
			List<Category> childCategories = categoryAPI.getChildren(parentCategory, user, true);
			//Get the children categories of each child
			for(Category categoryAux : childCategories)
			{
				result.add(categoryAux);
				List<Category> children = getChildrenCategories(categoryAux,nextLevel, maxDepth);
				if(children.size() > 0) {
					result.addAll(children);
				}
			}
		}
		return result;
	}

	public List<Category> getActiveChildrenCategories(Category cat) {
		try {
			return categoryAPI.getChildren(cat, true, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Category> getActiveChildrenCategoriesByKey(String key) {
		if (key == null) {
			return new ArrayList();
		}
		try {
			Category cat = categoryAPI.findByKey(key, user, true);
			if (!InodeUtils.isSet(cat.getInode())) {
				return new ArrayList();
			}
			return categoryAPI.getChildren(cat, true, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Category> getActiveChildrenCategories(Inode inode) {
		try {
			return categoryAPI.getChildren(inode, true, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getActiveChildrenCategories(String inode) {
		try {
			Inode inodeObj = new Inode();
			inodeObj.setInode(inode);
			return categoryAPI.getChildren(inodeObj, true, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getActiveChildrenCategoriesOrderByName(Category cat) {
		try {
			return categoryAPI.getChildren(cat, true, "category_name", user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getActiveChildrenCategoriesOrderByName(Inode inode) {
		try {
			return categoryAPI.getChildren(inode, true, "category_name", user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getActiveChildrenCategoriesOrderByName(String inode) {
		try {
			Inode inodeObj = new Inode();
			inodeObj.setInode(inode);
			return categoryAPI.getChildren(inodeObj, true, "category_name", user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getActiveChildrenCategoriesByParent(ArrayList<String> o) {
		try {
			List<Category> children = new ArrayList<Category>();
			for (String key : o) {
				if (UtilMethods.isSet(key)) {
					Category cat = getCategoryByKey(key);
					if (!InodeUtils.isSet(cat.getInode())) {
						cat = getCategoryByName(key);
					}
					children.addAll(categoryAPI.getChildren(cat, user, true));
				}
			}
			return children;
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	private List<Map<String, Object>> getAllActiveChildrenCategories(List<Category> children, int currentLevel)
			throws DotDataException, DotSecurityException {
		List<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
		for (Category ccat : children) {
			Map<String, Object> valMap = new HashMap<String, Object>();
			valMap.put("level", currentLevel);
			valMap.put("category", ccat);
			retList.add(valMap);
			List<Category> cchildren = categoryAPI.getChildren(ccat, true, user, true);
			if (cchildren.size() > 0) {
				List<Map<String, Object>> childrenMaps = getAllActiveChildrenCategories(cchildren, currentLevel + 1);
				retList.addAll(childrenMaps);
			}
		}
		return retList;
	}

	/**
	 * Retrieves a plain list of all the children categories (any depth) of the
	 * given parent category key The list returned is a list of maps, each map
	 * has the category and the level of this category belongs
	 * 
	 * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
	 * Presentations level: 2 cat: second level level: 1 cat: Marketing
	 * 
	 * @param key
	 *            parent category key
	 * @return
	 */

	public List<Map<String, Object>> getAllActiveChildrenCategoriesByKey(String key) {
		if (key == null) {
			return new ArrayList<Map<String, Object>>();
		}
		try {
			Category cat = categoryAPI.findByKey(key, user, true);
			if (!InodeUtils.isSet(cat.getInode())) {
				return new ArrayList<Map<String, Object>>();
			}
			List<Category> children = categoryAPI.getChildren(cat, true, user, true);
			return getAllActiveChildrenCategories(children, 1);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	/**
	 * Retrieves a plain list of all the children categories (any depth) of the
	 * given parent inode The list returned is a list of maps, each map has the
	 * category and the level of this category belongs
	 * 
	 * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
	 * Presentations level: 2 cat: second level level: 1 cat: Marketing
	 * 
	 * @param inode
	 *            parent inode
	 * @return
	 */

	public List<Map<String, Object>> getAllActiveChildrenCategories(Inode inode) {
		try {
			List<Category> children = categoryAPI.getChildren(inode, true, user, true);
			return getAllActiveChildrenCategories(children, 1);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	/**
	 * Retrieves a plain list of all the children categories (any depth) of the
	 * given parent inode The list returned is a list of maps, each map has the
	 * category and the level of this category belongs
	 * 
	 * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
	 * Presentations level: 2 cat: second level level: 1 cat: Marketing
	 * 
	 * @param inode
	 *            parent inode
	 * @return
	 */

	public List<Map<String, Object>> getAllActiveChildrenCategories(String inode) {
		try {
			Category parent = categoryAPI.find(inode, user, true);
			List<Category> children = categoryAPI.getChildren(parent, true, user, true);
			return getAllActiveChildrenCategories(children, 1);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getInodeCategories(String inode) {
		try {
			Inode inodeObj = new Inode();
			inodeObj.setInode(inode);
			return categoryAPI.getParents(inodeObj, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> getInodeCategories(Inode inodeObj) {
		try {
			return categoryAPI.getParents(inodeObj, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}
	
	public Category getCategoryByInode(String inode) {
		try {
			return (Category) categoryAPI.find(inode, user, true);
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the category");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve category : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
			return null;
		}
	}

	@Deprecated
	public Category getCategoryByInode(long inode) {
		try {
			return getCategoryByInode(String.valueOf(inode));
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
			return null;
		}
	}

	@Deprecated
	public String getCategoryKeyByContentlet(long contentletInode) {
		try {
			return getCategoryKeyByContentlet(String.valueOf(contentletInode));
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
			return null;
		}
	}

	public String getCategoryKeyByContentlet(String contentletInode) {
		try {
			Contentlet contentlet = new Contentlet();
			try {
				contentlet = conAPI.find(contentletInode, user, true);
			} catch (DotDataException e) {
				Logger.error(this, "Unable to look up contentlet with inode " + contentletInode, e);
			}
			List<Category> category = categoryAPI.getParents(contentlet, user, true);
			// Category category = (Category)
			// InodeFactory.getParentOfClass(contentlet,Category.class);
			String key = category.get(0).getKey();
			return key;
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the category");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve category : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Category> getCategoriesByUser(User user) {
		try {
			HttpSession session = request.getSession();
			List<Category> catsUser = (List<Category>) session.getAttribute(WebKeys.LOGGED_IN_USER_CATS);
			if (!UtilMethods.isSet(catsUser) || catsUser.size() == 0) {
				UserProxy up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,
						APILocator.getUserAPI().getSystemUser(), false);
				catsUser = categoryAPI.getChildren(up, user, true);
				request.getSession().setAttribute(WebKeys.LOGGED_IN_USER_CATS, catsUser);
			}
			return catsUser;
		} catch (DotSecurityException se) {
			Logger.info(this, "The logged in user cannot access the categories");
			return null;
		} catch (DotDataException de) {
			Logger.error(this, "An error happening while trying to retrieve categories : ", de);
			return null;
		} catch (Exception e) {
			Logger.error(this, "An unknown error happening while trying to retrieve categories : ", e);
			return null;
		}
	}

	public List<Category> filterCategoriesByUserPermissions(List<Object> catInodes) {
		List<Category> result = new ArrayList<Category>(30);
		try {
			// Needed to make the List Generic when we refactored to UUID to
			// handle backwards compat of categores being passed.
			for (Object cInode : catInodes) {
				String catInode = cInode.toString();
				try {
					result.add(categoryAPI.find(catInode, user, true));
				} catch (DotSecurityException se) {
				} catch (DotDataException de) {
					Logger.error(this, "An error happening while trying to retrieve category : ", de);
				} catch (Exception e) {
					Logger.error(this, "An unknown error happening while trying to retrieve category : ", e);
				}
			}
		} catch (Exception e) {
			Logger.warn(this, e.toString());
		}

		return result;
	}
	
	public List<String> fetchCategoriesInodes(List<Category> cats) {
	    List<String> inodes=new ArrayList<String>(cats.size());
	    for(Category cc : cats)
	        inodes.add(cc.getInode());
	    return inodes;
	}
	public List<String> fetchCategoriesNames(List<Category> cats) {
        List<String> inodes=new ArrayList<String>(cats.size());
        for(Category cc : cats)
            inodes.add(cc.getCategoryName());
        return inodes;
    }
	public List<String> fetchCategoriesKeys(List<Category> cats) {
        List<String> inodes=new ArrayList<String>(cats.size());
        for(Category cc : cats)
            if(UtilMethods.isSet(cc.getKey()))
                inodes.add(cc.getKey());
            else
                inodes.add("");
        return inodes;
    }
}