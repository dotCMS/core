package com.dotmarketing.portlets.categories.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *	This class is an specific implementation of the CategoryAPI API to manage
 *  dotCMS categories
 *
 * @author Jason Tesser & David Torres
 * @since 1.5.1.1
 *
 */
public class CategoryAPIImpl implements CategoryAPI {

	private final CategoryFactory categoryFactory;
	private final PermissionAPI permissionAPI;

	public CategoryAPIImpl () {
		categoryFactory = FactoryLocator.getCategoryFactory();
		permissionAPI = APILocator.getPermissionAPI();
	}

	/**
	 *
	 * @param category
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can use a category.
	 * @throws DotDataException
	 */
	public boolean canUseCategory(final Category category, final User user,
								  final boolean respectFrontendRoles) throws DotDataException {
		return permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles);
	}
	/**
	 *
	 * @param category
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can add a child category.
	 * @throws DotDataException
	 */
	public boolean canAddChildren(final Category category, final User user,
								  final boolean respectFrontendRoles) throws DotDataException {
		return permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles);
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
		return permissionAPI.doesUserHavePermission(cat, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles);
	}

	@WrapInTransaction
	public void delete(final Category object, final User user,
					   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(object, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit the category = " + object.getInode());

		categoryFactory.delete(object);

	}

	@WrapInTransaction
	public void deleteAll(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> all = findAll(user, respectFrontendRoles);
		for(Category category : all) {
			removeChildren(category, user, respectFrontendRoles);
			delete(category, user, respectFrontendRoles);
		}

	}

	/*public Category find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return find(Long.parseLong(id), user, respectFrontendRoles);
	}*/
	@CloseDBIfOpened
	public Category find(final String id, final User user,
						 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.find(id);
		if(category != null && !permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to read the category = " + category.getInode());
		return category;

	}

	@CloseDBIfOpened
	public List<Category> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findAll();
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	@WrapInTransaction
	public void save(final Category parent,
					 Category object,
					 final User user,
					 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        // Checking that we have a unique key.
	    object = checkUniqueKey(object, user);

	    boolean isANewCategory = false;

		//Checking permissions
		if(InodeUtils.isSet(object.getInode()) || parent == null) {
			//Object is not new or is a top level category
			//if it is a new top level category the user should be a cms administrator
			// and that's checked in the permissions api
			 if(!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId())){
              if(!permissionAPI.doesUserHavePermission(object, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles))
				throw new DotSecurityException("User doesn't have permission to edit the category = " + object.getInode());
			 }
		} else {
			//Object is new and a parent was provided so we check in the parent permissions
			if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontendRoles))
				throw new DotSecurityException("User doesn't have permission to save this category = " +
						object.getInode() + " having as parent the category = " + parent.getInode());

			isANewCategory = true;
		}

		object.setModDate(new Date());
		categoryFactory.save(object);

		if(isANewCategory && parent != null) {
			categoryFactory.addChild(parent, object, null);
			permissionAPI.copyPermissions(parent, object);
		}

	}

	@WrapInTransaction
    @Override
	public void saveRemote(final Category parent,
						   final Category object,
						   final User user,
						   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        object.setModDate(new Date());
	    categoryFactory.save(object);

        if(parent != null) {
            categoryFactory.addChild(parent, object, null);
            permissionAPI.copyPermissions(parent, object);
        }
    }

	@WrapInTransaction
	@Override
	public void publishRemote(final Category parent, final Category object,
							  final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		categoryFactory.saveRemote(object);

		if(parent != null) {
			categoryFactory.addChild(parent, object, null);
			permissionAPI.copyPermissions(parent, object);
		}
	}

	@WrapInTransaction
	@Override
	public void addChild(final Categorizable parent, final Category child,
						 final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the category = " + parent.getCategoryId());

		categoryFactory.addChild(parent, child, null);

	}

	@WrapInTransaction
	@Override
	public void addChild(final Categorizable parent, final Category child,
						 final String relationType, final User user,
						 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the category = " + parent.getCategoryId());

		categoryFactory.addChild(parent, child, relationType);
	}

	@WrapInTransaction
	@Override
	public void addParent(final Categorizable child, final Category parent,
						  final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getCategoryId() + " having as parent the category = " + parent.getInode());

		categoryFactory.addParent(child, parent);
	}

	@CloseDBIfOpened
	public Category findByKey(final String key, final User user,
							  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.findByKey(key);

		if(!InodeUtils.isSet(category.getCategoryId()))
			return null;

		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					category.getInode() + " having as parent the category = " + category.getInode());

		return category;
	}

	@CloseDBIfOpened
	public Category findByName(final String name, final User user,
							   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.findByName(name);

		if(category == null)
			return null;

		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					category.getInode() + " having as parent the category = " + category.getInode());

		return category;
	}

	@WrapInTransaction
	public void deleteTopLevelCategories(final User user) throws DotSecurityException, DotDataException {

		if(!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId())){
			throw new DotSecurityException("User doesn't have permission to edit Top Level Categories ");
		}

		categoryFactory.deleteTopLevelCategories();
	}

	@CloseDBIfOpened
	public List<Category> findTopLevelCategories(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> categories = categoryFactory.findTopLevelCategories();
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	@CloseDBIfOpened
	public List<Category> findTopLevelCategories(final User user, final boolean respectFrontendRoles,
												 final String filter) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findTopLevelCategoriesByFilter(filter, null);
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	@CloseDBIfOpened
	public PaginatedCategories findTopLevelCategories(final User user, final boolean respectFrontendRoles,
													  final int start, final int count,
													  final String filter, final String sort) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findTopLevelCategoriesByFilter(filter, sort);
		categories = permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return getCategoriesSubList(start, count, categories, filter);
	}

	@WrapInTransaction
	public void deleteChilren(final String inode) {
		categoryFactory.deleteChildren(inode);
	}

	@CloseDBIfOpened
	public PaginatedCategories findChildren(final User user, final String inode,
											final boolean respectFrontendRoles,
											final int start, final int count,
											final String filter, final String sort) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findChildrenByFilter(inode, filter, sort);
		categories = permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return getCategoriesSubList(start, count, categories, filter);
	}

	@CloseDBIfOpened
	public List<Category> findChildren(final User user, final String inode,
									   final boolean respectFrontendRoles,
									   final String filter) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findChildrenByFilter(inode, filter, null);
		categories = permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		return categories;
	}

	public List<Category> getChildren(final Categorizable parent, final User user,
									  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getChildren(parent, false, user, respectFrontendRoles);
	}

	@CloseDBIfOpened
	public List<Category> getChildren(final Categorizable parent, final boolean onlyActive,
									  final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.getChildren(parent);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}

		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	@CloseDBIfOpened
	public List<Category> getChildren(final Categorizable parent, final String relationType,
									  final boolean onlyActive, final String orderBy,
									  final User user,	final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.getChildren(parent, orderBy, relationType);
		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	@CloseDBIfOpened
	public List<Category> getChildren(final Categorizable parent, final boolean onlyActive,
									  final String orderBy, final User user,
									  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.getChildren(parent, orderBy);
		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	public List<Category> getChildren(Categorizable parent, String orderBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		return getChildren(parent, false, orderBy, user, respectFrontendRoles);
	}

	public List<Category> getParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getParents(child, false, user, respectFrontendRoles);
	}

	@CloseDBIfOpened
	public List<Category> getParents(final Categorizable child, final boolean onlyActive,
									 final String relationType, final User user,
									 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.getParents(child, relationType);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	@CloseDBIfOpened
	public List<Category> getParents(final Categorizable child, final boolean onlyActive,
									 final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.getParents(child);

		if(onlyActive) {
			List<Category> resultList = new ArrayList<Category>();
			for (Category cat : categories) {
				if(cat.isActive())
					resultList.add(cat);
			}
			categories = resultList;
		}
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);


	}

	@WrapInTransaction
	public void removeChild(final Categorizable parent, final Category child,
							final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the inode = " + parent.getCategoryId());

		categoryFactory.removeChild(parent, child, null);

	}

	@WrapInTransaction
	public void removeChild(final Categorizable parent, final Category child,
							final String relationType, final User user,
							final boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this category = " +
					child.getInode() + " having as parent the inode = " + parent.getCategoryId());

		categoryFactory.removeChild(parent, child, relationType);
	}

	@WrapInTransaction
	public void removeChildren(final Categorizable parent, final User user,
							   final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit this inode = " +
					parent.getCategoryId());

		categoryFactory.removeChildren(parent);

	}

	@WrapInTransaction
	public void removeParent(final Categorizable child, final Category parent,
							 final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this inoe = " +
					child.getCategoryId() + " having as parent the category = " + parent.getInode());

		categoryFactory.removeParent(child, parent);

	}

	@WrapInTransaction
	public void removeParents(final Categorizable child, final User user,
							  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to save this inode = " +
					child.getCategoryId() + " having as parent the category = " + child.getCategoryId());

		categoryFactory.removeParents(child);
	}

	@WrapInTransaction
	public void setChildren(final Categorizable parent, final List<Category> children,
							final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
			throw new DotSecurityException("User doesn't have permission to edit this inode = " +
					parent.getCategoryId());

		categoryFactory.setChildren(parent, children);

	}

	@WrapInTransaction
	public void setParents(Categorizable child, List<Category> parents, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)){
			List<Role> rolesPublish = permissionAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
			List<Role> rolesWrite = permissionAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1);

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

		categoryFactory.setParents(child, parents);
	}

	@CloseDBIfOpened
	public List<Category> getAllChildren(final Category category, final User user,
										 final boolean respectFrontendRoles)
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

	@CloseDBIfOpened
	public List<Category> removeAllChildren(final Category parentCategory, final User user,
										 final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		List<Category> unableToDelete = Collections.unmodifiableList(new ArrayList<>());

		List<Category> categoriesToDelete = getChildren(parentCategory, user, false);
		categoriesToDelete.forEach((category)-> {
			try {
				delete(category, user, false);
			} catch (DotDataException | DotSecurityException e) {
				Logger.error(this, "Category has dependencies. Category name: " + category.getCategoryName());
				unableToDelete.add(category);
			}
		});

		return unableToDelete;
	}

	public void clearCache() {
		categoryFactory.clearCache();
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

	@CloseDBIfOpened
	public boolean  hasDependencies(Category cat) throws DotDataException {
		return categoryFactory.hasDependencies(cat);
	}

	@CloseDBIfOpened
	public void sortTopLevelCategories() throws DotDataException {
		categoryFactory.sortTopLevelCategories();
	}

	@CloseDBIfOpened
	public void sortChildren(String inode) throws DotDataException {
		categoryFactory.sortChildren(inode);
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

	public boolean isParent(final Category givenChild, final Category givenParent, final User user) {
		return isParent(givenChild,givenParent,user,false);
	}

	@CloseDBIfOpened
	public boolean isParent(final Category givenChild, final Category givenParent, final User user, final boolean respectFrontendRoles) {

		try {
			final List<Category> parents = getParents(givenChild, user, respectFrontendRoles);

			if(parents==null || parents.isEmpty()) {
				return false;
			}

			for(final Category localParent: parents) {
				if(localParent.getCategoryId().equals(givenParent.getCategoryId())) {
					return true;
				} else {
					return isParent(localParent, givenParent, user,respectFrontendRoles);
				}
			}
		} catch (DotDataException e) {
			Logger.warnAndDebug(CategoryAPI.class,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.warnAndDebug(CategoryAPI.class,e.getMessage(),e);
		}


		return false;
	}

	@CloseDBIfOpened
	public synchronized String suggestVelocityVarName(final String categoryVelVarName) throws DotDataException {
	    if (!UtilMethods.isSet(categoryVelVarName)) {
	        return UUID.randomUUID().toString();
	    } else {
	        return categoryFactory.suggestVelocityVarName(categoryVelVarName);
	    }
	}

    /**
     * Util method to check and generate (if necessary) a unique key for the Category.
     *
     * @return {@link Category} with a unique key.
     */
    private Category checkUniqueKey(Category category, User user)
            throws DotDataException, DotSecurityException {

        // If the category is new or if the category doesn't have any key: Let's generate a potential
        // key and test until we have a unique one.
        if (!InodeUtils.isSet(category.getInode()) || !UtilMethods.isSet(category.getKey())) {
            final String potentialKey = getPotentialKeyFromCategory(category);
            final String uniqueKey = getUniqueKey(potentialKey, user, 1);
            category.setKey(uniqueKey);
        } else {
            // If the category is already in the DB, let's double check that the key is unique,
            // maybe the the user is editing the category and changing it's key and that key
            // already used by another Category.
            final Category categoryInDB = findByKey(category.getKey(), user, false);
            if (UtilMethods.isSet(categoryInDB)
                    && !category.getInode().equals(categoryInDB.getInode())) {
                final String uniqueKey = getUniqueKey(category.getKey(), user, 1);
                category.setKey(uniqueKey);
            }
        }
        return category;
    }

	/**
     * Util method to check is a {@link String} key is unique among the other Category keys. In case
     * it is repeated ths method will concat "-" + a consecutive number.
     */
    private String getUniqueKey(String key, User user, Integer consecutive)
            throws DotDataException, DotSecurityException {

        if (findByKey(key, user, false) != null) {
            key = getUniqueKey(key + "-" + consecutive, user, ++consecutive);
        }

        return key;
    }

    /**
     * Util method to explore potential keys in this order:
     * 1. Category Key.
     * 2. Category Variable Name.
     * 3. "key" string.
     */
    private String getPotentialKeyFromCategory(Category category) {
        if (UtilMethods.isSet(category.getKey())) {
            return category.getKey();
        } else {
            return UtilMethods.isSet(category.getCategoryVelocityVarName()) ? category
                    .getCategoryVelocityVarName() : "key";
        }
    }

	/**
	 * This method will look for all the fields of type 'Category' within a Structure and will get you all the associated Category types available for a given a user.
	 * @param contentType
	 * @param user
	 * @return
	 */
	public List<Category> findCategories(final ContentType contentType, final User user)
			throws DotSecurityException, DotDataException {

		if(!hasCategoryFields(contentType)){
			return ImmutableList.of();
		}

		final List<Category> filteredTopCategories = permissionAPI
				.filterCollection(findCategoryFields(contentType).stream()
						.map(field -> findCategory(CategoryField.class.cast(field), user))
						.filter(Objects::nonNull)
						.collect(Collectors.toList()), PermissionAPI.PERMISSION_READ, false, user
				);

		final ImmutableList.Builder<Category> builder = new ImmutableList.Builder<>();

	    for(final Category category: filteredTopCategories){
			builder.add(category).addAll(
		 			getAllChildren(category, user, false)
			);
		}
		return builder.build();

	}

	@CloseDBIfOpened
	@Override
	public Category findByVariable(final String variable, final User user,
								   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.findByVar(variable);

		if(!InodeUtils.isSet(category.getCategoryId())) {
			return null;
		}

		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_USE, user, respectFrontendRoles)) {
			throw new DotSecurityException("User doesn't have permission to use this category = " +
					category.getInode());
		}

		return category;
	}

	@CloseDBIfOpened
    @Override
    public List<Category> getCategoriesFromContent(Contentlet contentlet, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		final List<Category> categories = new ArrayList<>();

		if(!UtilMethods.isSet(contentlet.getContentType())) {
			return categories;
		}

		final List<com.dotmarketing.portlets.structure.model.Field> fields = new LegacyFieldTransformer(
				APILocator.getContentTypeAPI(APILocator.systemUser()).
						find(contentlet.getContentType().inode()).fields()).asOldFieldList();

		for (com.dotmarketing.portlets.structure.model.Field field : fields) {
			if (field.getFieldType().equals(com.dotmarketing.portlets.structure.model.Field.FieldType.CATEGORY.toString())) {
				final String catValue = contentlet.getStringProperty(field.getVelocityVarName());
				if (UtilMethods.isSet(catValue)) {
					for (final String categoryIdKeyOrVar : catValue.split("\\s*,\\s*")) {
						// take it as catId
						Category category = APILocator.getCategoryAPI()
								.find(categoryIdKeyOrVar, user, respectFrontendRoles);
						if (category != null && InodeUtils.isSet(category.getCategoryId())) {
							categories.add(category);
						} else {
							// try it as catKey
							category = APILocator.getCategoryAPI()
									.findByKey(categoryIdKeyOrVar, user, respectFrontendRoles);
							if (category != null && InodeUtils
									.isSet(category.getCategoryId())) {
								categories.add(category);
							} else {
								try {
									category = findByVariable(categoryIdKeyOrVar, user, respectFrontendRoles);

									if (category != null && InodeUtils.isSet(category.getCategoryId())) {
										categories.add(category);
									}

								} catch (DotDataException e) {
									Logger.error(this, "Error finding category by variable. " +
											"Var name: " + categoryIdKeyOrVar, e);
								}

							}
						}

					}
				}
			}
		}

		return UtilMethods.isSet(categories)?categories:null;
    }



    /**
	 * given a field previously determined to be of type Category this method will look up the respective CategoryField type.
	 * @param categoryField
	 * @param user
	 * @return
	 */
	private Category findCategory(final CategoryField categoryField, final User user) {
		Category category = null;
		try {
			category = find(categoryField.values(), user, false);
		} catch (DotSecurityException | DotDataException e) {
			Logger.error(getClass(),
					String.format("Unable to get category for field '%s' ", categoryField), e);
		}
		return category;
	}

	/**
	 * Given a contentType this method will look into the fields and get you all the ones of type CategoryField
	 * @param contentType
	 * @return
	 */
	private List<Field> findCategoryFields(final ContentType contentType) {
		return contentType.fields()
				.stream().filter(CategoryField.class::isInstance)
				.collect(CollectionsUtils.toImmutableList());
	}

	/**
	 *
	 * @param contentType
	 * @return
	 */
	private boolean hasCategoryFields(final ContentType contentType) {
		return contentType.fields()
				.stream().anyMatch(CategoryField.class::isInstance);

	}

}
