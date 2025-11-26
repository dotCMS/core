package com.dotmarketing.portlets.categories.business;


import java.util.Collection;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.HierarchedCategory;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;


/**
 * 
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public abstract class CategoryFactory {
    final static String ALL_CHILDREN_SUFFIX=":all-children";
	/**
	 * Totally removes a category from the system
	 * @param object
	 * @throws DotDataException
	 */
	protected abstract void delete(Category object) throws DotDataException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * in cache a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	protected abstract Category find(String id) throws DotDataException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * in cache a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	protected abstract Category findByKey(String key) throws DotDataException;

	/**
	 * This tries to get a Category object with the given variable from
	 * @param variable
	 * @return
	 * @throws DotDataException
	 */

	protected abstract Category findByVar(String variable) throws DotDataException;

	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * in cache a null value is returned
	 * @param name
	 * @return 
	 * @throws DotDataException
	 */
	protected abstract Category findByName(String name) throws DotDataException;
	
	/**
	 * Retrieves the list of all the categories in the system
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Category> findAll() throws DotDataException;
	
	/**
	 * This method saves a category in the system
	 * @param object
	 * @throws DotDataException
	 */
	public abstract void save(Category object) throws DotDataException;

	/**
	 * This save requires of a parent in order to calculate the deterministic identifier
 	 * @param object
	 * @param parent
	 * @throws DotDataException
	 */
	public abstract void save(Category object, Category parent) throws DotDataException;
	
	/**
	 * This method saves a category in the system coming by a Remote publishing.
	 * @param object
	 * @throws DotDataException
	 */
	protected abstract void saveRemote(Category object) throws DotDataException;	
	
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
	abstract protected List<Category> getChildren(Categorizable parent) throws DotDataException;

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
	 * @deprecated this version doesn't leverage cache at all (!)
	 */
	abstract protected List<Category> getChildren(Categorizable parent, String orderBy) throws DotDataException;
	
	/**
	 * Retrieves the list of children categories associated
	 * to the given id/inode, this method can be used
	 * to retrived associated categories to another
	 * type of objects like categories associated to 
	 * contentlets
	 * 
	 * @param relationType
	 * @param parent
	 * @param orderBy - can be null
	 * @return
	 * @throws DotDataException
	 * @deprecated this version doesn't leverage cache at all (!)
	 */
	abstract protected List<Category> getChildren(Categorizable parent, String orderBy, String relationType) throws DotDataException;

	/**
	 * This methods associates the given children list to the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void setChildren(Categorizable parent, List<Category> children) throws DotDataException;

	/**
	 * This method adds the given category to parent children list
	 * @param parentId
	 * @param children
	 * @param relationType
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void addChild(Categorizable parent, Category child, String relationType) throws DotDataException;

	/**
	 * Removes all the associated children categories
	 * of the given parent
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void removeChildren(Categorizable parent) throws DotDataException;
	
	/**
	 * Removes from the list of children categories the 
	 * given child
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void removeChild(Categorizable parent, Category child,String relationType) throws DotDataException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Category> getParents(Categorizable child) throws DotDataException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @param relationType
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Category> getParents(Categorizable child, String relationType) throws DotDataException;

	/**
	 * Associates to the given list of categories as parents of the child id/inode
	 * Older parents gets replaced by the new list
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void setParents(Categorizable child, List<Category> parents) throws DotDataException;

	/**
	 * Adds the given category as a parent of the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void addParent(Categorizable child, Category parent) throws DotDataException;

	/**
	 * Removes the parents associated to the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void removeParents(Categorizable child) throws DotDataException;
	
	/**
	 * Associates the given the list of categories as parents of the given children id
	 * Older parents gets removed from the list
	 * 
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 */
	abstract protected void removeParent(Categorizable child, Category parent) throws DotDataException;

	/**
	 * Returns the first level of categories in the system
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Category> findTopLevelCategories() throws DotDataException;
	
	/**
	 * Returns the first level of categories in the system filtered by a string
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Category> findTopLevelCategoriesByFilter(String filter, String sort) throws DotDataException;
	
	/**
	 * Returns the children categories of the category with the supplied inode filtered by a string  
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Category> findChildrenByFilter(String inode, String filter, String sort) throws DotDataException;
	
	/**
	 * Returns true if the category has dependences
	 * @return
	 * @throws DotDataException
	 */
	abstract boolean  hasDependencies(Category cat) throws DotDataException;
	
	abstract void sortTopLevelCategories()  throws DotDataException;
	
	abstract void sortChildren(String inode)  throws DotDataException;
	
	abstract protected String suggestVelocityVarName (String categoryVelVarName) throws DotDataException;

	abstract protected  void clearCache();

    /**
     * This method recurses down the category tree returns all the children, grandchildren, great
     * grandchildren, etc under this category
     * 
     * @param parent
     * @return
     * @throws DotDataException
     */
    abstract protected List<Category> getAllChildren(Categorizable parent) throws DotDataException;

	/**
	 * Return a collection of {@link Category} objects, filtered based on the criteria specified in {@link CategorySearchCriteria}
	 *
	 * @param searchCriteria Search Criteria
	 *
	 * @return List of Category
	 */
	public abstract List<HierarchedCategory> findAll(final CategorySearchCriteria searchCriteria) throws DotDataException, DotSecurityException;

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
	public abstract List<HierarchyShortCategory> findHierarchy(final Collection<String> keys) throws DotDataException;
}
