package com.dotmarketing.portlets.categories.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
/**
 * 
 * @author David Torres
 * @author Jason Tesser
 * @since 1.5.1.1
 *
 */
public class CategoryCacheImpl extends CategoryCache {

	private DotCacheAdministrator cache;
	
	private String primaryGroup = "CategoryCache";
	private String categoryByKeyCacheGroup = "categoryByKeyCache";
    private String categoryChildrenCacheGroup = "categoryChildrenCache";
    private String categoryParentsCacheGroup = "categoryParentsCache";
	
    // region's name for the cache
    private String[] groupNames = {primaryGroup, categoryByKeyCacheGroup, categoryChildrenCacheGroup, categoryParentsCacheGroup};
    
    public CategoryCacheImpl() {
    	cache = CacheLocator.getCacheAdministrator();
	}
    
	@Override
	protected Category get(String id) throws DotDataException {
		try{
			return (Category) cache.get(getPrimaryGroup() + id, getPrimaryGroup());
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
			return null;
    	}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<String> getChildren(Categorizable parent) throws DotDataException {
		List<String> childrenIds = null;
		try{
			childrenIds = (List<String>) cache.get(categoryChildrenCacheGroup + parent.getCategoryId(),categoryChildrenCacheGroup);
			return childrenIds;
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
			return null;
    	}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<String> getParents(Categorizable child) throws DotDataException {
		
		List<String> catsIds = null;
		try{
			catsIds = (List<String>) cache.get(categoryParentsCacheGroup + child.getCategoryId(), categoryParentsCacheGroup);
			return catsIds;
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
			return null;
    	}
		
	}

	@Override
	
	
	protected void put(Category object) throws DotDataException, DotCacheException {

		cache.put(primaryGroup + object.getInode(), object, primaryGroup);		

		//DOTCMS-2765
		if(UtilMethods.isSet(object.getKey())){
			cache.put(categoryByKeyCacheGroup + object.getKey(), object, categoryByKeyCacheGroup);
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putChildren(Categorizable parent, List<Category> children)
			throws DotDataException, DotCacheException {
		
		List<String> catsIds = new ArrayList<String>();
		for(Category cat : children) {
			catsIds.add(cat.getInode());
		}
		cache.put(categoryChildrenCacheGroup + parent.getCategoryId(), catsIds, categoryChildrenCacheGroup);

		//Putting the children cats on the plain cache
		for(Category cat : children) {
			put(cat);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void putParents(Categorizable child, List<Category> parents)throws DotDataException, DotCacheException {
		
		List<String> catsIds = new ArrayList<String>();
		for(Category cat : parents) {
			catsIds.add(cat.getInode());
		}
		cache.put(categoryParentsCacheGroup + child.getCategoryId(), catsIds, categoryParentsCacheGroup);

		//Putting the children cats on the plain cache
		for(Category cat : parents) {
			put(cat);
		}
	
	}

	@SuppressWarnings("unchecked")
	@Override
	
	protected void remove(Category object) throws DotDataException, DotCacheException {
		
		Category oldCat = this.get(object.getCategoryId());
		if(oldCat != null && UtilMethods.isSet(oldCat.getKey())){
			cache.remove(categoryByKeyCacheGroup + oldCat.getKey(), categoryByKeyCacheGroup);
		}
		
		if(object != null && UtilMethods.isSet(object.getKey())){
			cache.remove(categoryByKeyCacheGroup + object.getKey(), categoryByKeyCacheGroup);
		}
		cache.remove(primaryGroup + object.getCategoryId(),primaryGroup);
		
		
		
		

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void removeChildren(Categorizable parent) throws DotDataException, DotCacheException {
		List<String> childrenIds = null;
		try{
			childrenIds = (List<String>) cache.get(categoryChildrenCacheGroup + parent.getCategoryId(),categoryChildrenCacheGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
    	}
		cache.remove(categoryChildrenCacheGroup + parent.getCategoryId(),categoryChildrenCacheGroup);
		
		//Updating the associated parent caches to keep it consistent
		if(childrenIds != null) {
			for(String child : childrenIds) {
				cache.remove(categoryParentsCacheGroup + child, categoryParentsCacheGroup);
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void removeParents(Categorizable child)throws DotDataException, DotCacheException {
		List<String> parentIds = null;
		try{
			parentIds = (List<String>) cache.get(categoryParentsCacheGroup + child.getCategoryId(), categoryParentsCacheGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
    	}
		cache.remove(categoryParentsCacheGroup + child.getCategoryId(), categoryParentsCacheGroup);
		
		//Updating the associated parent caches to keep it consistent
		if(parentIds != null) {
			for(String parentId : parentIds) {
				cache.remove(categoryChildrenCacheGroup + parentId, categoryChildrenCacheGroup);
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addChild(Categorizable parent, Category child, List<Category> children)throws DotDataException, DotCacheException {
		if(children == null)
			children = new ArrayList<Category>();
		else
			children = new ArrayList<Category>(children);
		
		List<String> childrenIds = new ArrayList<String>();
		for(Category childcat : children) {
			childrenIds.add(childcat.getInode());
		}
		
		childrenIds.add(child.getCategoryId());
		cache.put(categoryChildrenCacheGroup + parent.getCategoryId(), childrenIds, categoryChildrenCacheGroup);
		
		//putting the parent in the plain category cache if it's a category type
		if(parent instanceof Category){
			put((Category)parent);
		}
		
		//putting the parent in the plain category cache if it's a category type
		put(child);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addParent(Categorizable child, Category parent, List<Category> parents)throws DotDataException, DotCacheException {
		List<String> parentsInodes = new ArrayList<String>();
		parentsInodes.add(parent.getInode());
		if(parents != null){
			for(Category p : parents){
				parentsInodes.add(p.getInode());
			}
		}
		
		cache.put(categoryParentsCacheGroup + child.getCategoryId(), parentsInodes, categoryParentsCacheGroup);
		
		//putting the parent in the plain category cache if it's a category type
		if(child instanceof Category)
			put((Category)child);

		//putting the parent in the plain category cache if it's a category type
		put(parent);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void removeChild(Categorizable parent, Category child)throws DotDataException, DotCacheException {
		cache.remove(categoryChildrenCacheGroup + parent.getCategoryId(), categoryChildrenCacheGroup);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void removeParent(Categorizable child, Category parent)throws DotDataException, DotCacheException {
		cache.remove(categoryParentsCacheGroup + child.getCategoryId(), categoryParentsCacheGroup);
		
		//updating children list of the parent as well
		if(child instanceof Category) {
			cache.remove(categoryChildrenCacheGroup + parent.getCategoryId(), categoryChildrenCacheGroup);
		}
	}

	@Override
	protected Category getByKey(String catKey) throws DotDataException {
		try{
			return (Category) cache.get(categoryByKeyCacheGroup + catKey,categoryByKeyCacheGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
			return null;
    	}
	}

	@Override
	public void clearCache() {
		cache.flushGroup(primaryGroup);
		cache.flushGroup(categoryChildrenCacheGroup);
		cache.flushGroup(categoryByKeyCacheGroup);
		cache.flushGroup(categoryParentsCacheGroup);		
	}
	
	@Override
	protected void clearChildrenCache(){
		cache.flushGroup(categoryChildrenCacheGroup);
	}
	
	public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    @Override
    public String getCategoryByKeyGroup() {
    	return categoryByKeyCacheGroup;
    }
    
    @Override
    public String getCategoryChildrenGroup() {
    	return categoryChildrenCacheGroup;
    }
    
    @Override
    public String getCategoryParentsGroup() {
    	return categoryParentsCacheGroup;
    }
}
