package com.dotmarketing.portlets.categories.business;

import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;

/**
 * 
 * @author David Torres
 * @since 1.5.1.1
 *
 */
public abstract class CategoryCache implements Cachable{

	/**
	 * This method removes the category entry from the cache
	 * based on the category inode
	 * @param object
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void remove(Category object) throws DotDataException, DotCacheException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * in cache a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	abstract protected Category get(String id) throws DotDataException;
	
	/**
	 * This method get a category object from the cache based
	 * on the passed inode, if the object does not exist
	 * in cache a null value is returned
	 * @param id
	 * @return 
	 * @throws DotDataException
	 */
	abstract protected Category getByKey(String catKey) throws DotDataException;
	
	/**
	 * This method puts a category object in cache
	 * using the category inode as key this method also
	 * triggers the removal of children and parents from the cache
	 * @param object
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void put(Category object) throws DotDataException, DotCacheException;

	/**
	 * Retrieves children categories of the given 
	 * id(inode or identifier) this method can be used
	 * to associate not only children of categories
	 * but also children of other type of entities
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<String> getChildren(Categorizable parentId) throws DotDataException;

	/**
	 * Sets the list of children based on the given parent id/inode
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void putChildren(Categorizable parentId, List<Category> children) throws DotDataException, DotCacheException;

	/**
	 * Sets the list of children based on the given parent id/inode
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void addChild(Categorizable parentId, Category child, List<Category> children) throws DotDataException, DotCacheException;

	/**
	 * Removes the list of children categories based 
	 * using the given parent id/inode
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void removeChildren(Categorizable parentId) throws DotDataException, DotCacheException;
	
	/**
	 * Removes the list of children categories based 
	 * using the given parent id/inode
	 * @param parentId
	 * @param children
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void removeChild(Categorizable parentId, Category child) throws DotDataException, DotCacheException;
	
	/**
	 * Retrieves the list of parents categories associated to the given
	 * id/inode
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<String> getParents(Categorizable childId) throws DotDataException;

	/**
	 * Sets the list of parent categories of the given child id/inode
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void putParents(Categorizable child, List<Category> parents) throws DotDataException, DotCacheException;

	/**
	 * Sets the list of parent categories of the given child id/inode
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void addParent(Categorizable child, Category parent, List<Category> parents) throws DotDataException, DotCacheException;

	/**
	 * Removes the parents associated to the given children category
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void removeParents(Categorizable child) throws DotDataException, DotCacheException;
	
	/**
	 * Sets the list of parent categories of the given child id/inode
	 * @param children
	 * @param parents
	 * @return
	 * @throws DotDataException
	 * @throws DotCacheException 
	 */
	abstract protected void removeParent(Categorizable child, Category parent) throws DotDataException, DotCacheException;

	/**
	 * Removes all entries from cache
	 */
	public abstract void clearCache();
	
	/**
	 * Removes all the child entries from the cache
	 */
	abstract protected void clearChildrenCache();
	
	/**
	 * use to get the group name used in the cache
	 * @return
	 */
	abstract public String getCategoryByKeyGroup();
	
	/**
	 * use to get the group name used in the cache
	 * @return
	 */
	abstract public String getCategoryChildrenGroup();
	
	/**
	 * use to get the group name used in the cache
	 * @return
	 */
	abstract public String getCategoryParentsGroup();

	
}
