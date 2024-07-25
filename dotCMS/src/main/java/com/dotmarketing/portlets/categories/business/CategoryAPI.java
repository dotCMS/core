package com.dotmarketing.portlets.categories.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
/**
 * 
 * This class defines the API contract of methods usable to control cms categories
 * 
 * @author David Torres
 * @since 1.5.1.1
 * 
 *
 */
public interface CategoryAPI {

	/**
	 * Checks if a user has permissions to view a category
	 *
	 * @param category Category to check permissions
	 * @param user user making the request
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can read/view a category.
	 * @throws DotDataException
	 */
	
	boolean canUseCategory(Category category, User user, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Totally removes a category from the system
	 * @param object
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void delete(Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Remove all categories from the system
	 * @param object
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void deleteAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	
	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	/*public Category find(long id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;*/
	Category find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed key, if the object does not exist
	 * a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	Category findByKey(String key, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This method get a category object based on its name
	 * this method needs to be used carefully because 
	 * it might have more than one category with the same name
	 * @param name
	 * @return 
	 * @throws DotDataException
	 * @deprecated This method shouldn't be used because it might have more than one 
	 * 	category with the same name
	 * 
	 */
	Category findByName(String name, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of all the categories in the system
	 * @return
	 * @throws DotDataException
	 */
	List<Category> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of all top level categories in the system
	 * @return
	 * @throws DotDataException
	 */
	
	List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Retrieves a list of all top level categories in the system filtered by a String that can match the name, key, or variable. 
	 * @param filter  String used to filter the Categories. Compares to attributes name, key or variable.  
	 * @return
	 * @throws DotDataException
	 */
	List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException;

	/**
	 *  Retrieves a list of all top level categories in the system filtered by a String that can match the name, key, or variable and delimited by start and count params.
	 * @return
	 * @throws DotDataException
	 */
	PaginatedCategories findTopLevelCategories(User user, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a Paginated list of all the children of the Category with the given inode
	 * @return
	 * @throws DotDataException
	 */
	PaginatedCategories findChildren(User user, String inode, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of all top level categories in the system
	 * @return
	 * @throws DotDataException
	 */
	List<Category> findChildren(User user, String inode, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException;

	
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
	void save(Category parent, Category category, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Important: should be use only for Push Publish.
     *
     * Save Remote categories, in this case we don't need to check if this category exists into
     * the system but just save with its own inode.
     *
     */
    void saveRemote(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * * Important: should be use only for Push Publish.
     *
     * Publish Remote categories,
	 * in this case we don't need to check if this category exists into the system but just save with its own inode.
	 * 
	 * Mar 6, 2013 - 10:12:47 AM
	 */
	void publishRemote(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	List<Category> getChildren(Categorizable parent, boolean onlyActive, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param id
	 * @param relationType
	 * @param orderBy - can be null
	 * @return
	 * @throws DotDataException
	 * @deprecated this version doesn't leverage cache at all (!)
	 */
	List<Category> getChildren(Categorizable parent, String relationType, boolean onlyActive,String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	List<Category> getChildren(Categorizable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param parent
	 * @param orderBy
	 * @return
	 * @throws DotDataException
	 * @deprecated this version doesn't leverage cache at all (!)
	 */
	List<Category> getChildren(Categorizable parent, boolean onlyActive, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param parent
	 * @param orderBy
	 * @return
	 * @throws DotDataException
	 * @deprecated this version doesn't leverage cache at all (!)
	 */
	List<Category> getChildren(Categorizable parent, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * This methods associates the given children list to the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void setChildren(Categorizable parent, List<Category> children, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * This method adds the given category to parent children list
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void addChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This method adds the given category to parent children list
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void addChild(Categorizable parent, Category child, String relationType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Removes all the associated children categories
	 * of the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void removeChildren(Categorizable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Removes from the list of children categories the 
	 * given child
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void removeChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Removes from the list of children categories the 
	 * given child
	 * @param parent
	 * @param child
	 * @param user
	 * @param relationType
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void removeChild(Categorizable parent, Category child, String relationType ,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	List<Category> getParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param relationType
	 * @param id
	 * @param onlyActive
	 * @return
	 * @throws DotDataException
	 */
	List<Category> getParents(Categorizable child, boolean onlyActive, String relationType,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	List<Category> getParents(Categorizable child, boolean onlyActive, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Associates to the given list of categories as parents of the child id/inode
	 * Older parents gets replaced by the new list
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void setParents(Categorizable child, List<Category> parents, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Adds the given category as a parent of the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void addParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Removes the parents associated to the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void removeParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Associates the given the list of categories as parents of the given children id
	 * Older parents gets removed from the list
	 * 
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	void removeParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Recursive Method that returns a list of categories
	 * given a parent category, it does not take into account
	 * the whole hierarchy, instead all categories are being added
	 * to the categories list being returned.
	 * @param category
	 * @return
	 */
	List<Category> getAllChildren(Category category, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Recursive Method that deletes all the categories children of the
	 * given parent category
	 * @param parentCategory
	 * @return The List of categories that could not be deleted
	 */
	List<Category>  removeAllChildren(Category parentCategory, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Recursive Method that deletes all the parent categories along with their children
	 * @param categoriesToDelete
	 * @return The List of parent categories that could not be deleted
	 */
	HashMap<String, Category> deleteCategoryAndChildren(final List<String> categoriesToDelete, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Retrieves a list all the line of parent categories of the given child category
	 * a final fake top category is added at the beginning of the list to represent the top of
	 * the hierarchy 
	 * @param cat
	 * @param l
	 * @return
	 * @throws DotDataException 
	 */
	List<Category> getCategoryTreeUp(Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * Will return to you all category hierarchy (children and subchildren and so on) who are attached to the categorizable and are below the passed
	 * category
	 *  
	 * @param categorizable
	 * @param catToSearchFrom
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Category> getCategoryTreeDown(Categorizable categorizable,Category catToSearchFrom, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This is a low level method only intended to be use for maintenance purposes 
	 */
	void clearCache();

	 
	
	/**
	 * check If the category has dependencies. 
	 * @param category
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	boolean hasDependencies(Category cat) throws DotDataException;
	
	void sortTopLevelCategories()  throws DotDataException;
	
	void sortChildren(String inode)  throws DotDataException;
	
	/**
	 * This method flushes the children cache 
	 */
	void flushChildrenCache();
	
	
	/**
	 * Determines if a givenParent is parent/grandParent/... and so on of a givenSon, recursively
	 */
	boolean isParent(Category givenChild, Category givenParent, User user);

	/**
	 * Determines if a givenParent is parent/grandParent/... and so on of a givenSon, recursively
	 */
	boolean isParent(Category givenChild, Category givenParent, User user, boolean respectFrontendRoles);
	
	
	/**
	 * Returns a suggestion for the Velocity Variable Name.
	 * 
	 * @param categoryVelVarName Velocity Variable Name
	 * @return Suggestion for the Velocity Variable Name
	 * @throws DotDataException Error occurred when performing the action.
	 */
	String suggestVelocityVarName (String categoryVelVarName) throws DotDataException;


	/**
	 * This method will look for all the fields of type 'Category' within a ContentType and will get you all the associated Category types available for a given a user.
	 * @param contentType
	 * @param user
	 * @return
	 */
	List<Category> findCategories(final ContentType contentType, final User user)
			throws DotSecurityException, DotDataException;

	@CloseDBIfOpened
	Category findByVariable(final String variable, final User user,
							final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	List<Category> getCategoriesFromContent(final Contentlet contentlet, final User user, boolean respectFrontendRoles ) throws DotDataException, DotSecurityException;

	/**
	 * Return a list of Categories regardless of their levels.
	 *
	 * @param searchCriteria Searching criteria
	 * @param user User to check Permission
	 * @param respectFrontendRoles true if you must respect Frontend Roles
	 *
	 * @return List of Category filtered
	 */
	PaginatedCategories findAll(final CategorySearchCriteria searchCriteria, final User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	/**
	 * Find Categories by inodes and calculate its Hierarchy.
	 *
	 * For Example if you have the follows categories:
	 *
	 *
	 * | Inode  | Name        |  key        |Parent          |
	 * |--------|-------------|-------------|----------------|
	 * | 1      | Top Category| top         | null           |
	 * | 2      | Child       | child       | Top Category   |
	 * | 3      | Grand Child | grand_child | Child          |
	 *
	 * And you search by key 'grand_child' then you got:
	 *
	 * Inode: 3
	 * key: 'grand_child'
	 * categoryName: 'Grand Child'
	 * parentList <code>[
	 *   {
	 *       'categoryName':'Top Category',
	 *       'key': 'top',
	 *       'inode': '1'
	 *   },
	 *   {
	 *       'categoryName':'Child',
	 *       'key': 'child',
	 *       'inode': '2'
	 *   }
	 * ]</code>
	 *
	 * @param keys List of keys to search
	 * @return
	 * @throws DotDataException
	 */
	List<HierarchyShortCategory> findHierarchy(final Collection<String> keys) throws DotDataException;
}
