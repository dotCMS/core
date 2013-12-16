package com.dotmarketing.portlets.categories.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.liferay.portal.model.User;
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
	 * 
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can use a category.  
	 * @throws DotDataException 
	 */
	
	public boolean canUseCategory(Category cat, User user, boolean respectFrontendRoles) throws DotDataException;
	
	/**
	 * 
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can add a child category.  
	 * @throws DotDataException 
	 */
	public boolean canAddChildren(Category cat, User user, boolean respectFrontendRoles) throws DotDataException;
	
	/**
	 * 
	 * @param user
	 * @return Whether the user can add a category to the top level.  If it is a top parent category.
	 */
	public boolean canAddToTopLevel(User user);
	
	/**
	 * 
	 * @param cat
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean on whether or not a user can edit a category.  
	 * @throws DotDataException 
	 */
	public boolean canEditCategory(Category cat, User user, boolean respectFrontendRoles) throws DotDataException;
	
	/**
	 * Totally removes a category from the system
	 * @param object
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void delete(Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Remove all categories from the system
	 * @param object
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void deleteAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	
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
	public Category find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed key, if the object does not exist
	 * a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	public Category findByKey(String key, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public Category findByName(String name, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of all the categories in the system
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Deletes the top level categories
	 * @return
	 */
	
	public void deleteTopLevelCategories(User user) throws DotSecurityException, DotDataException ;
	/**
	 * Retrieves the list of all top level categories in the system
	 * @return
	 * @throws DotDataException
	 */
	
	public List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Retrieves a list of all top level categories in the system filtered by a String that can match the name, key, or variable. 
	 * @param filter  String used to filter the Categories. Compares to attributes name, key or variable.  
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> findTopLevelCategories(User user, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException;

	/**
	 *  Retrieves a list of all top level categories in the system filtered by a String that can match the name, key, or variable and delimited by start and count params.
	 * @return
	 * @throws DotDataException
	 */
	public PaginatedCategories findTopLevelCategories(User user, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a Paginated list of all the children of the Category with the given inode
	 * @return
	 * @throws DotDataException
	 */
	public PaginatedCategories findChildren(User user, String inode, boolean respectFrontendRoles, int start, int count, String filter, String sort) throws DotDataException, DotSecurityException;
	
	/**
	 * Deletes all the children of a given parent inode
	 * @return
	 * @deprecated this will mess up with your transaction. Use removeXXXX methods instead
	 */
	
	public void deleteChilren(String inode);
	/**
	 * Retrieves the list of all top level categories in the system
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> findChildren(User user, String inode, boolean respectFrontendRoles, String filter) throws DotDataException, DotSecurityException;

	
	/**
	 * Saves categories,
	 * When saving a new category the parent should be passed to the API
	 * to check if the user has permissions to add children to the parent
	 * and the parent will be associated to the passed category object
	 * @param parent Parent can be null if saving an old category
	 * @param object
	 * @param user
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void save(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Publish Remote categories,
	 * in this case we don't need to check if this category exists into the system but just save with its own inode.
	 * 
	 * Mar 6, 2013 - 10:12:47 AM
	 */
	public void publishRemote(Category parent, Category object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public List<Category> getChildren(Categorizable parent, boolean onlyActive, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public List<Category> getChildren(Categorizable parent, String relationType, boolean onlyActive,String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	public List<Category> getChildren(Categorizable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	public List<Category> getChildren(Categorizable parent, boolean onlyActive, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	public List<Category> getChildren(Categorizable parent, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * This methods associates the given children list to the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void setChildren(Categorizable parent, List<Category> children, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * This method adds the given category to parent children list
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void addChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This method adds the given category to parent children list
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void addChild(Categorizable parent, Category child, String relationType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Removes all the associated children categories
	 * of the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void removeChildren(Categorizable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Removes from the list of children categories the 
	 * given child
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void removeChild(Categorizable parent, Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public void removeChild(Categorizable parent, Category child, String relationType ,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> getParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param relationType
	 * @param id
	 * @param onlyActive
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> getParents(Categorizable child, boolean onlyActive, String relationType,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	public List<Category> getParents(Categorizable child, boolean onlyActive, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Associates to the given list of categories as parents of the child id/inode
	 * Older parents gets replaced by the new list
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void setParents(Categorizable child, List<Category> parents, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Adds the given category as a parent of the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void addParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Removes the parents associated to the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void removeParents(Categorizable child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public void removeParent(Categorizable child, Category parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Recursive Method that returns a list of categories
	 * given a parent category, it does not take into account
	 * the whole hierarchy, instead all categories are being added
	 * to the categories list being returned.
	 * @param category
	 * @return
	 */
	public List<Category> getAllChildren(Category category, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves a list all the line of parent categories of the given child category
	 * a final fake top category is added at the beginning of the list to represent the top of
	 * the hierarchy 
	 * @param cat
	 * @param l
	 * @return
	 * @throws DotDataException 
	 */
	public List<Category> getCategoryTreeUp(Category child, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	public List<Category> getCategoryTreeDown(Categorizable categorizable,Category catToSearchFrom, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * This is a low level method only intended to be use for maintenance purposes 
	 */
	public void clearCache();

	 
	
	/**
	 * check If the category has dependencies. 
	 * @param category
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public boolean hasDependencies(Category cat) throws DotDataException;
	
	public void sortTopLevelCategories()  throws DotDataException;
	
	public void sortChildren(String inode)  throws DotDataException;
	
	/**
	 * This method flushes the children cache 
	 */
	public void flushChildrenCache();
	
	
	/**
	 * Determines if a givenParent is parent/grandParent/... and so on of a givenSon, recursively
	 */
	public boolean isParent(Category givenChild, Category givenParent, User user);

}
