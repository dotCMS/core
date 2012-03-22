package com.dotmarketing.portlets.categories.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;

/**
 * 
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public abstract class CategoryFactory {

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
	protected abstract void save(Category object) throws DotDataException;
	
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
	 * Deletes all the top level categories
	 * @return
	 */
	abstract protected void deleteTopLevelCategories();
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
	 * Deletes all the Children of a given parent inode
	 * @return
	 */
	abstract protected void deleteChildren(String inode);
	
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

	abstract protected  void clearCache();
	
}
