package com.dotmarketing.portlets.categories.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rest.api.FailedResultView;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import java.util.*;
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
		this(FactoryLocator.getCategoryFactory(), APILocator.getPermissionAPI());
	}

	@VisibleForTesting
	public CategoryAPIImpl (final CategoryFactory categoryFactory, final PermissionAPI permissionAPI) {
		this.categoryFactory = categoryFactory;
		this.permissionAPI = permissionAPI;
	}

	/**
	 * Checks if a user has permissions to view a category
	 *
	 * @param category Category to check permissions
	 * @param user user making the request
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can read/view a category.
	 * @throws DotDataException
	 */
	public boolean canUseCategory(final Category category, final User user,
								  final boolean respectFrontendRoles) throws DotDataException {
		return permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles);
	}

	@WrapInTransaction
	public void delete(final Category category, final User user,
					   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have permission to edit Category '%s'",
					null != user ? user.getUserId() : null, category.getInode()));
        }
		categoryFactory.delete(category);
	}

	@WrapInTransaction
	public void deleteAll(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Category> all = findAll(user, respectFrontendRoles);
		for(Category category : all) {
			removeChildren(category, user, respectFrontendRoles);
			delete(category, user, respectFrontendRoles);
		}

	}

	@CloseDBIfOpened
	public Category find(final String id, final User user,
						 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.find(id);
		if(category != null && !permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have READ permission on Category '%s'",
                    null != user ? user.getUserId() : null, category.getInode()));
        }
		return category;
	}

	@CloseDBIfOpened
	public List<Category> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Category> categories = categoryFactory.findAll();
		return permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
	}

	/**
	 * Saves a category
	 *
	 * When saving a new category the parent should be passed to the API
	 * to check if the user has permissions to add children to the parent
	 * and the parent will be associated to the passed category
	 *
	 * @param parent Parent can be null if saving an top level category
	 * @param category Category to be saved
	 * @param user user that is performing the save
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@WrapInTransaction
	public void save(final Category parent,
					 Category category,
					 final User user,
					 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        // Checking that we have a unique key.
	    category = checkUniqueKey(category, user);

	    final boolean isANewCategory = UtilMethods.isNotSet(category.getInode());

	    if(isANewCategory) {
			//If parent is null is a top level category, we need to check permissions over the SYSTEM_HOST
			//the permissions that the user requires are: ADD CHILDREN and PUBLISH over the CATEGORY type
			if (!UtilMethods.isSet(parent)) {
				if (!permissionAPI.doesUserHavePermission(APILocator.systemHost(),
						PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles) ||
						!permissionAPI.doesUserHavePermissions(APILocator.systemHost().getIdentifier(),PermissionableType.CATEGORY,
								PermissionAPI.PERMISSION_PUBLISH, user)) {
                    final String errorMsg = String.format("User '%s' doesn't have ADD CHILDREN and PUBLISH " +
                            "permissions to save Category '%s' at the top level.", null != user ? user.getUserId() : null, category
                            .getInode());
                    Logger.error(this, errorMsg);
					throw new DotSecurityException(errorMsg);
				}
			} else {
				if (!permissionAPI
						.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user,
								respectFrontendRoles)) {
                    final String errorMsg = String.format("User '%s' doesn't have EDIT permissions to save child " +
                            "Category '%s' under parent Category '%s'.", null != user ? user.getUserId() : null, category.getInode(),
                            parent.getInode());
                    Logger.error(this, errorMsg);
					throw new DotSecurityException(errorMsg);
				}
			}
		} else {
	    	//Category already exists, just check permissions over the category itself
			if (!permissionAPI
					.doesUserHavePermission(category, PermissionAPI.PERMISSION_EDIT, user,
							respectFrontendRoles)) {
                final String errorMsg = String.format("User '%s' doesn't have EDIT permissions to save Category " +
                        "'%s'", null != user ? user.getUserId() : null, category.getInode());
                Logger.error(this, errorMsg);
				throw new DotSecurityException(errorMsg);
			}
		}

		category.setModDate(new Date());
		categoryFactory.save(category, parent);

		//if is a new category and is not top level, relate the category to the parent category
		if(isANewCategory && parent != null) {
			categoryFactory.addChild(parent, category, null);
			permissionAPI.copyPermissions(parent, category);
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

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to save child " +
                    "Category '%s' under parent Category '%s'", null != user ? user.getUserId() : null, child.getInode(), parent
                    .getCategoryId()));
        }
		categoryFactory.addChild(parent, child, null);
	}

	@WrapInTransaction
	@Override
	public void addChild(final Categorizable parent, final Category child,
						 final String relationType, final User user,
						 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to save child " +
                    "Category '%s' under parent Category '%s'", null != user ? user.getUserId() : null, child.getInode(), parent
                    .getCategoryId()));
        }
		categoryFactory.addChild(parent, child, relationType);
	}

	@WrapInTransaction
	@Override
	public void addParent(final Categorizable child, final Category parent,
						  final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission on child Category " +
                    "'%s' to save parent Category '%s'", null != user ? user.getUserId() : null, child.getCategoryId(), parent.getInode()));
        }
		categoryFactory.addParent(child, parent);
	}

	@CloseDBIfOpened
	public Category findByKey(final String key, final User user,
							  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.findByKey(key);

		if(null == category || !InodeUtils.isSet(category.getCategoryId())){
			return null;
		}
		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have READ permission on Category with key" +
                    " '%s'", null != user ? user.getUserId() : null, category.getKey()));
        }
		return category;
	}

	@CloseDBIfOpened
	public Category findByName(final String name, final User user,
							   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Category category = categoryFactory.findByName(name);

		if(category == null) {
			return null;
		}
		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have READ permission on Category with " +
                    "name '%s'", null != user ? user.getUserId() : null, category.getCategoryName()));
        }
		return category;
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
		if(categories.isEmpty()) {
		    return categories;
		}
		
		if(onlyActive) {
			List<Category> resultList = new ArrayList<>();
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
			List<Category> resultList = new ArrayList<>();
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
			List<Category> resultList = new ArrayList<>();
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
			List<Category> resultList = new ArrayList<>();
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
			List<Category> resultList = new ArrayList<>();
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

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to remove child " +
                    "Category '%s' from parent Category '%s'",null != user ? user.getUserId() : null, child.getInode(), parent.getCategoryId()));
        }
		categoryFactory.removeChild(parent, child, null);
	}

	@WrapInTransaction
	public void removeChild(final Categorizable parent, final Category child,
							final String relationType, final User user,
							final boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to remove child " +
                    "Category '%s' from parent Category '%s'", null != user ? user.getUserId() : null, child.getInode(), parent.getCategoryId()));
        }
		categoryFactory.removeChild(parent, child, relationType);
	}

	@WrapInTransaction
	public void removeChildren(final Categorizable parent, final User user,
							   final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to remove all " +
                    "children from parent Category '%s'", null != user ? user.getUserId() : null, parent.getCategoryId()));
        }
		categoryFactory.removeChildren(parent);
	}

	@WrapInTransaction
	public void removeParent(final Categorizable child, final Category parent,
							 final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to remove parent " +
                    "Category '%s' from child Category '%s'", null != user ? user.getUserId() : null, parent.getInode(), child.getCategoryId()));
        }
		categoryFactory.removeParent(child, parent);
	}

	@WrapInTransaction
	public void removeParents(final Categorizable child, final User user,
							  final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to remove all " +
                    "parents from child Category '%s'",null != user ? user.getUserId() : null, child.getCategoryId()));
        }
		categoryFactory.removeParents(child);
	}

	@WrapInTransaction
	public void setChildren(final Categorizable parent, final List<Category> children,
							final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have EDIT permission to set any children " +
                    "to parent Category '%s'",null != user ? user.getUserId() : null, parent.getCategoryId()));
        }
		categoryFactory.setChildren(parent, children);
	}

	@WrapInTransaction
	public void setParents(final Categorizable child, final List<Category> parents, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(!permissionAPI.doesUserHavePermission(child, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)){
			final List<Role> rolesPublish = permissionAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_PUBLISH, Role.CMS_OWNER_ROLE, 0, -1);
			final List<Role> rolesWrite = permissionAPI.getRoles(child.getCategoryId(), PermissionAPI.PERMISSION_EDIT, Role.CMS_OWNER_ROLE, 0, -1);

			final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
			boolean isCMSOwner = false;
			if (!rolesPublish.isEmpty() || !rolesWrite.isEmpty()) {
				for (final Role role : rolesPublish) {
					if(role.getId().equals(cmsOwner.getId())){
						isCMSOwner = true;
						break;
					}
				}
				if(!isCMSOwner){
					for (final Role role : rolesWrite) {
						if(role.getId().equals(cmsOwner.getId())){
							isCMSOwner = true;
							break;
						}
					}
				}
				if(!isCMSOwner){
                    final String errorMsg = String.format("User '%s' doesn't have the correct permissions to the " +
                            "object the Category is being assigned to, or to the Category '%s'",
							null != user ? user.getUserId() : null, child.getCategoryId());
                    Logger.error(this, errorMsg);
					throw new DotSecurityException(errorMsg);
				}
			}else{
                final String errorMsg = String.format("User '%s' doesn't have EDIT or CMS Owner permissions to the " +
                        "object the Category is being assigned to, or to the Category '%s'",
						null != user ? user.getUserId() : null, child.getCategoryId());
				Logger.error(this, errorMsg);
				throw new DotSecurityException(errorMsg);
			}
		}
		categoryFactory.setParents(child, parents);
	}

    @CloseDBIfOpened
    public List<Category> getAllChildren(final Category category, final User user, final boolean respectFrontendRoles)
                    throws DotDataException, DotSecurityException {

        return permissionAPI.filterCollection(categoryFactory.getAllChildren(category), PermissionAPI.PERMISSION_READ,
                        respectFrontendRoles, user);

    }

	@WrapInTransaction
	public HashMap<String, Category> deleteCategoryAndChildren(final List<String> categoriesToDelete, final User user,
			final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		final HashMap<String, Category> parentCategoryUnableToDelete = new HashMap<>();

		for(final String parentCategoryInode : categoriesToDelete) {

			final Category parentCategory = categoryFactory.find(parentCategoryInode);

			if(parentCategory != null) {
				if (!permissionAPI.doesUserHavePermission(parentCategory,
						PermissionAPI.PERMISSION_EDIT,
						user, respectFrontendRoles)) {
					throw new DotSecurityException(
							String.format("User '%s' doesn't have permission to edit Category '%s'",
									null != user ? user.getUserId() : null,
									parentCategory.getInode()));
				}

				final List<Category> childrenCategoriesToDelete = getChildren(parentCategory, user,
						false);
				childrenCategoriesToDelete.forEach((category) -> {
					try {
						delete(category, user, false);
					} catch (final DotDataException | DotSecurityException e) {
						Logger.error(this, String.format(
								"Child Category '%s' has dependencies. It couldn't be removed from "
										+
										"parent Category '%s'", category.getInode(),
								parentCategory.getInode()));
						parentCategoryUnableToDelete.put(parentCategory.getInode(),parentCategory);
					}
				});

				try {
					if (!parentCategoryUnableToDelete.containsKey(parentCategory.getInode())) {
						categoryFactory.delete(parentCategory);
					}
				} catch (final DotDataException e) {
					Logger.error(this, String.format(
							"Parent Category '%s' couldn't be removed", parentCategory.getInode(),
							parentCategory.getInode()));
					parentCategoryUnableToDelete.put(parentCategory.getInode(), parentCategory);
				}
			}
			else{
				Category notFound = new Category();
				notFound.setInode(parentCategoryInode);
				parentCategoryUnableToDelete.put(parentCategoryInode, notFound);
			}
		}

		return parentCategoryUnableToDelete;
	}

	@CloseDBIfOpened
	public List<Category> removeAllChildren(final Category parentCategory, final User user,
										 final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		final List<Category> unableToDelete = Collections.unmodifiableList(new ArrayList<>());

		final List<Category> categoriesToDelete = getChildren(parentCategory, user, false);
		categoriesToDelete.forEach((category)-> {
			try {
				delete(category, user, false);
			} catch (final DotDataException | DotSecurityException e) {
                Logger.error(this, String.format("Child Category '%s' has dependencies. It couldn't be removed from " +
                        "parent Category '%s'", category.getInode(), parentCategory.getInode()));
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
		return getCategoryTree(child, new ArrayList<>(), user, respectFrontendRoles);
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
		} catch (final DotDataException | DotSecurityException e) {
            Logger.warnAndDebug(CategoryAPI.class, String.format("An error occurred when determining if Category '%s'" +
                    " is the parent of Category '%s': %s", givenParent.getInode(), givenChild.getInode(), e.getMessage()), e);
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

		if( null == category || !InodeUtils.isSet(category.getCategoryId())) {
			return null;
		}

		if(!permissionAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException(String.format("User '%s' doesn't have READ permission to Category '%s'.",
					null != user ? user.getUserId() : null, category.getInode()));
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

								} catch (final DotDataException e) {
                                    Logger.error(this, String.format("An error occurred when retrieving Categories " +
                                            "from content '%s' in field '%s': %s", contentlet.getIdentifier(),
                                            categoryIdKeyOrVar, e.getMessage()), e);
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
		} catch (final DotSecurityException | DotDataException e) {
            Logger.error(getClass(), String.format("User '%s' couldn't get the Category from field '%s': %s",
					null != user ? user.getUserId() : null, categoryField.id(), e.getMessage()), e);
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

	/**
	 * Default implementation.
	 *
	 * @param searchCriteria Searching criteria
	 * @param user User to check Permission
	 * @param respectFrontendRoles true if you must respect Frontend Roles
	 *
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@CloseDBIfOpened
	@Override
	public PaginatedCategories findAll(final CategorySearchCriteria searchCriteria,
									   final User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if (searchCriteria.limit < 1) {
			throw new IllegalArgumentException("Limit must be greater than 0");
		}

		final List<Category> allCategories = new ArrayList<>(categoryFactory.findAll(searchCriteria));

		final List<Category> categories = permissionAPI.filterCollection(allCategories,
				PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

		return getCategoriesSubList(searchCriteria.offset, searchCriteria.limit, categories, null);
	}

	/**
	 * Default implementation of {@link CategoryAPI}
	 *
	 * @param keys List of keys to search
	 * @return
	 *
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	@Override
	public List<HierarchyShortCategory> findHierarchy(final Collection<String> keys) throws DotDataException {
		Logger.debug(this, "Getting parentList for the follow Categories: " + keys);
		return categoryFactory.findHierarchy(keys);
	}
}
