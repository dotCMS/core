package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

public class ContentTypeCache2Impl implements ContentTypeCache2 {

    private final DotCacheAdministrator cache;

    public ContentTypeCache2Impl() {
        this.cache = CacheLocator.getCacheAdministrator();
    }
    public ContentTypeCache2Impl(DotCacheAdministrator cacheAdmin) {
    	 this.cache = cacheAdmin;
    }
    public String getContainerStructureGroup() {
        return containerStructureGroup;
    }
	
    @Override
    public void add(ContentType type){

		cache.put(primaryGroup + type.inode(), type, primaryGroup);
        cache.put(primaryGroup + type.velocityVarName().toLowerCase(), type, primaryGroup);
    }

    @Override
    public String getURLMasterPattern() throws DotCacheException {
		return (String) cache.get(primaryGroup + MASTER_STRUCTURE,primaryGroup);
    }
    
    @Override
    public void clearURLMasterPattern(){
    	synchronized (MASTER_STRUCTURE) {
        	cache.remove(primaryGroup + MASTER_STRUCTURE,primaryGroup);	
		}
    }
    @Override
    public void addURLMasterPattern(String pattern){
        cache.put(primaryGroup + MASTER_STRUCTURE, pattern, primaryGroup);
	}
    @Override
    public void addContainerStructures(List<ContainerStructure> containerStructures, String containerIdentifier, String containerInode){
        cache.put(containerStructureGroup + containerIdentifier + containerInode, containerStructures, containerStructureGroup);
	}
    @Override
    @SuppressWarnings("unchecked")
	public List<ContainerStructure> getContainerStructures(String containerIdentifier, String containerInode){
    	List<ContainerStructure> containerStructures = null;
    	
		try{
			containerStructures = (List<ContainerStructure>) cache.get(containerStructureGroup + containerIdentifier + containerInode, containerStructureGroup);
			return containerStructures;
			
		} catch (DotCacheException e) {
			Logger.debug(ContentTypeCache2.class, "Cache Entry not found", e);
			return null;
    	}
	}
    @Override
    public void removeContainerStructures(String containerIdentifier, String containerInode) {
        cache.remove(this.containerStructureGroup + containerIdentifier + containerInode, containerStructureGroup);
    }
    
    
    @Override
    public void clearCache(){
	    //clear the cache
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
	}
    
    @Override
	public String[] getGroups() {
    	return groups;
    }
    @Override
    public String getPrimaryGroup() {
    	return primaryGroup;
    }

    @Override
	public void remove(ContentType type) {
        cache.remove(primaryGroup + type.inode(),primaryGroup);
        cache.remove(primaryGroup + type.velocityVarName(),primaryGroup);
        clearURLMasterPattern();
	}

    @Override
	public ContentType byInode(String inode) {
		try {
			return (ContentType) cache.get(primaryGroup + inode, primaryGroup);
		} catch (DotCacheException e) {
			Logger.debug(ContentTypeCache2.class,"Cache Entry not found", e);
			return null;
		}
	}

    @Override
	public ContentType byVar(String var) {
        try{
        	return (ContentType) cache.get(primaryGroup + var,primaryGroup);
        }catch (Exception e) {
			Logger.debug(ContentTypeCache2.class,"Cache Entry not found", e);
			return null;
    	}

	}
}
