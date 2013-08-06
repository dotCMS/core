package com.dotmarketing.portlets.categories.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
/**
 *	This class is an specific implementation of the CategoryAPI API to manage
 *  dotCMS categories
 *
 * @author Jason Tesser & David Torres
 * @since 1.5.1.1
 *
 */
public class CategoryAPIImpl implements CategoryAPI {

	private CategoryFactory catFactory;
	private PermissionAPI perAPI;

	public CategoryAPIImpl () {
		catFactory = FactoryLocator.getCategoryFactory();
		perAPI = APILocator.getPermissionAPI();
	}

	/**
	 *
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can use a category.
	 * @throws DotDataException
	 */
	public boolean canUseCategory(Category cat, User user, boolean respectFrontendRoles) throws DotDataException{
		return perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles);
	}
	/**
	 *
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can add a child category.
	 * @throws DotDataException
	 */
	public boolean canAddChildren(Category cat, User user, boolean respectFrontendRoles) throws DotDataException{
		return perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles);
	}

	/**
	 *
	 * @param user
	 * @return Whether the user can add a category to the top level.  If it is a top parent category.
	 */
	public boolean canAddToTopLevel(User user){
		try {
			return com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
		} catch (DotDataException e) {
			Logger.error(CategoryAPIImpl.class,e.getMessage(),e);
			return false;
		}

	}

	/**
	 *
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can edit a category.
	 * @throws DotDataException
	 */
	public boolean canEditCategory(Category cat, User user, boolean respectFrontendRoles) throws DotDataException{
		return perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles);
	}

	public void delete(Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(object, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the category = " + object.getInode());

		catFactory.delete(object);

	}

	public void deleteAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> all = findAll(user, respectFrontendRoles);
		for(Category category : all) {
			removeChildren(category, user, respectFrontendRoles);
			delete(category, user, respectFrontendRoles);
		}

	}

	/*public Category find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return find(Long.parseLong(id), user, respectFrontendRoles);
	}*/

	public Category find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Category cat = catFactory.find(id);
		if(cat != null && !perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the category = " + cat.getInode());
		return cat;

	}

	public List<Category> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = catFactory.findAll();
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	public void save(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		boolean isANewCategory = false;

		//Checking permissions
		if(InodeUtils.isSet(object.getInode()) || parent == null) {
			//Object is not new or is a top level category
			//if it is a new top level category the user should be a cms administrator
			// and that's checked in the permissions api
			 if(!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId())){
              if(!perAPI.doesUserHavePermission(object, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
				throw new DotSecurityException("User doesn't have permission to edit the category = " + object.getInode());
			 }
		} else {
			//Object is new and a parent was provided so we check in the parent permissions
			if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles))
				throw new DotSecurityException("User doesn't have permission to save this category = " +
						object.getInode() + " having as parent the category = " + parent.getInode());

			isANewCategory = true;
		}

		object.setModDate(new Date());
		catFactory.save(object);

		if(isANewCategory && parent != null) {
			catFactory.addChild(parent, object, null);
			perAPI.copyPermissions(parent, object);
		}

	}

	public void publishRemote(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		catFactory.saveRemote(object);

		if(parent != null) {
			catFactory.addChild(parent, object, null);
			perAPI.copyPermissions(parent, object);
		}
	}

	public void addChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the category = " + parent.getCategoryId());

		catFactory.addChild(parent, child, null);

	}

	public void addChild(Categorizable parent, Category child,	String relationType, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the category = " + parent.getCategoryId());

		catFactory.addChild(parent, child, relationType);

	}

	public void addParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getCategoryId() + " having as parent the category = " + parent.getInode());

		catFactory.addParent(child, parent);

	}

	public Category findByKey(String key, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Category cat = catFactory.findByKey(key);

		if(!InodeUtils.isSet(cat.getCategoryId()))
			return null;

		if(!perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					cat.getInode() + " having as parent the category = " + cat.getInode());

		return cat;
	}

	public Category findByName(String name, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Category cat = catFactory.findByName(name);

		if(cat == null)
			return null;

		if(!perAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					cat.getInode() + " having as parent the category = " + cat.getInode());
		return cat;
	}

	public void deleteTopLevelCategories(User user) throws DotSecurityException, DotDataException {
		if(!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId())){
			throw new DotSecurityException("User doesn't have permission to edit Top Level Categories ");
		}
		catFactory.deleteTopLevelCategories();
	}

	public List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.findTopLevelCategories();
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	public List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.findTopLevelCategoriesByFilter(filter, null);
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	public PaginatedCategories findTopLevelCategories(User user, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.findTopLevelCategoriesByFilter(filter, sort);
		categories = perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return getCategoriesSubList(start, count, categories, filter);
	}

	public void deleteChilren(String inode) {
		catFactory.deleteChildren(inode);
	}

	public PaginatedCategories findChildren(User user, String inode, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.findChildrenByFilter(inode, filter, sort);
		categories = perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return getCategoriesSubList(start, count, categories, filter);
	}

	public List<Category> findChildren(User user, String inode, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.findChildrenByFilter(inode, filter, null);
		categories = perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return categories;
	}

	public List<Category> getChildren(Categorizable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getChildren(parent, false, user, respectFrontendRoles);
	}

	public List<Category> getChildren(Categorizable parent, boolean onlyActive, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		List<Category> categories = catFactory.getChildren(parent);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}

		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	public List<Category> getChildren(Categorizable parent,	String relationType, boolean onlyActive, String orderBy, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		List<Category> categories = catFactory.getChildren(parent, orderBy, relationType);
		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	public List<Category> getChildren(Categorizable parent, boolean onlyActive,
			String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.getChildren(parent, orderBy);
		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	public List<Category> getChildren(Categorizable parent, String orderBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		return getChildren(parent, false, orderBy, user, respectFrontendRoles);
	}

	public List<Category> getParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getParents(child, false, user, respectFrontendRoles);
	}

	public List<Category> getParents(Categorizable child, boolean onlyActive, String relationType,User user, boolean respectFrontendRoles) throws DotDataException,		DotSecurityException {
		List<Category> categories = catFactory.getParents(child, relationType);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	public List<Category> getParents(Categorizable child, boolean onlyActive, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		List<Category> categories = catFactory.getParents(child);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return perAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);


	}

	public void removeChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the inode = " + parent.getCategoryId());

		catFactory.removeChild(parent, child, null);

	}

	public void removeChild(Categorizable parent, Category child, String relationType, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the inode = " + parent.getCategoryId());

		catFactory.removeChild(parent, child, relationType);
	}

	public void removeChildren(Categorizable parent, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit this inode = " +
					parent.getCategoryId());

		catFactory.removeChildren(parent);

	}

	public void removeParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this inoe = " +
					child.getCategoryId() + " having as parent the category = " + parent.getInode());

		catFactory.removeParent(child, parent);

	}

	public void removeParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this inode = " +
					child.getCategoryId() + " having as parent the category = " + child.getCategoryId());

		catFactory.removeParents(child);
	}

	public void setChildren(Categorizable parent, List<Category> children, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit this inode = " +
					parent.getCategoryId());

		catFactory.setChildren(parent, children);

	}

	public void setParents(Categorizable child, List<Category> parents, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!perAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)){
			List<Role> rolesPublish = perAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
			List<Role> rolesWrite = perAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1);

			Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
			boolean isCMSOwner = false;
			if(rolesPublish.size() > 0 || rolesWrite.size() > 0){
				for (Role role : rolesPublish) {
					if(role.getId().equals(cmsOwner.getId())){
						isCMSOwner = true;
						break;
					}
				}
				if(!isCMSOwner){
					for (Role role : rolesWrite) {
						if(role.getId().equals(cmsOwner.getId())){
							isCMSOwner = true;
							break;
						}
					}
				}
				if(!isCMSOwner){
					Logger.info(this, "User didn't have permissions to the object the category was being assigned to or to the category with inode " + child.getCategoryId());
					throw new DotSecurityException("User didn't have permissions to the object the category was being assigned to or to the category with inode " + child.getCategoryId());
					//throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
				}
			}else{
				Logger.info(this, "User didn't have permissions to the object the category was being assigned to or to the category with inode " + child.getCategoryId());
				throw new DotSecurityException("User didn't have permissions to the object the category was being assigned to or to the category with inode " + child.getCategoryId());
				//throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}

		}

		catFactory.setParents(child, parents);

	}

	public List<Category> getAllChildren(Category category, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		List<Category> categoryTree = new ArrayList<Category>();
		LinkedList<Category> children = new LinkedList<Category>(getChildren(category, user, respectFrontendRoles));
		if (children != null) {
			while(children.size() > 0) {
				Category child = children.poll();
				children.addAll(getChildren(child, user, respectFrontendRoles));
				categoryTree.add(child);
			}
		}
		return categoryTree;
	}

	public void clearCache() {
		catFactory.clearCache();
	}

	public List<Category> getCategoryTreeUp(Category child, User user, boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException {
		return getCategoryTree(child, new ArrayList<Category>(), user, respectFrontendRoles);
	}

	public List<Category> getCategoryTreeDown(Categorizable categorizable,Category catToSearchFrom, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException {
		List<Category> catList = getAllChildren(catToSearchFrom, user, false);
		return catList;
	}

	private List<Category> getCategoryTree(Category child, List<Category> l, User user, boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException {

		if (InodeUtils.isSet(child.getInode())) {
			l.add(0, child);
		}
		List<Category> parents = getParents(child, user, respectFrontendRoles);
		if (parents.size() > 0) {
			Category parent = (Category) parents.get(0);
			return getCategoryTree(parent, l, user, respectFrontendRoles);
		} else {
			Category fakeCat = new Category();
			fakeCat.setCategoryName("Top Level");
			l.add(0, fakeCat);
		}
		return l;
	}

	public boolean  hasDependencies(Category cat) throws DotDataException {
		return catFactory.hasDependencies(cat);
	}

	public void sortTopLevelCategories() throws DotDataException {
		catFactory.sortTopLevelCategories();
	}

	public void sortChildren(String inode) throws DotDataException {
		catFactory.sortChildren(inode);
	}

	public void flushChildrenCache(){
		CategoryCache catCache = CacheLocator.getCategoryCache();
		catCache.clearChildrenCache();
	}


	private PaginatedCategories getCategoriesSubList(int start, int count, List<Category> categories, String filter) {
		List<Category> aux = null;
		Integer totalCount = 0;

		if(!categories.isEmpty()) {

			if(UtilMethods.isSet(filter)) {
				if(!UtilMethods.isSet(start))
					start = 0;
			}
			totalCount = categories.size();
			int limit = start+count;
			limit = limit>totalCount?totalCount:limit;
			aux = categories.subList(start, limit);
			categories = null;
		}
		return new PaginatedCategories(aux, totalCount);
	}

	public boolean isParent(Category givenChild, Category givenParent, User user) {

		CategoryAPI catAPI = APILocator.getCategoryAPI();
		List<Category> parents;

		try {
			parents = catAPI.getParents(givenChild, user, false);

			if(parents==null || parents.isEmpty()) {
				return false;
			}

			for(Category localParent: parents) {
				if(localParent.getCategoryId().equals(givenParent.getCategoryId())) {
					return true;
				} else {
					return isParent(localParent, givenParent, user);
				}
			}
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return false;
	}





}
