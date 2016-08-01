package com.dotmarketing.cache;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.FromStructureTransformer;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author David
 */
public class ContentTypeCacheImpl extends ContentTypeCache {

	public static final String MASTER_STRUCTURE = "dotMaster_Structure";
	
    private DotCacheAdministrator cache;
    private final String primaryGroup = "StructureCache";
    private final String containerStructureGroup = "ContainerStructureCache";
    // region's name for the cache
    private final String[] groups = { primaryGroup, containerStructureGroup };

    public ContentTypeCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    public String getContainerStructureGroup() {
        return containerStructureGroup;
    }
	
    @Override
    public void add(ContentType type){
		// we use the identifier uri for our mappings.
		cache.put(primaryGroup + type.inode(), type, primaryGroup);
        cache.put(primaryGroup + type.velocityVarName().toLowerCase(), type, primaryGroup);
    }
    
    
    
    
    
    public void add(Structure st){
    	add(new FromStructureTransformer(st).from());
	}
    

    public Structure getStructureByInode(String inode) {
    	return new StructureTransformer(byInode(inode)).asStructure();
    }

    public Structure getStructureByName(String variableName) {
    	return new StructureTransformer(byVar(variableName)).asStructure();
    }
    /**
     * This methods retrieves the structure from the cache based in the 
     * structure velocity variable name which gets set once and never changes. 
     * 
     * This methods tries to retrieve the structure from the cache, if the
     * structure were not found in the cache, it would try to find it in database
     * and store it in cache.
     * 
     * @param variableName Name of the structure
     * @return The structure from cache
     * 
     */
    public Structure getStructureByVelocityVarName(String variableName) {
    	return new StructureTransformer(byVar(variableName)).asStructure();
       
    }

    
    public boolean hasStructureByVelocityVarName (String varname) {
        return getStructureByVelocityVarName(varname) != null;
    }
    
    public boolean hasStructureByInode (String inode) {
        return getStructureByInode(inode) != null;
    }
    
    /*public boolean hasStructureByInode (long inode) {
        return getStructureByInode(inode) != null;
    }*/
    
    public void remove(Structure st) {


    }

    public String getURLMasterPattern() throws DotCacheException {
		return (String)cache.get(primaryGroup + MASTER_STRUCTURE,primaryGroup);
    }
    
    public void clearURLMasterPattern(){
    	synchronized (ContentTypeCacheImpl.MASTER_STRUCTURE) {
        	cache.remove(primaryGroup + MASTER_STRUCTURE,primaryGroup);	
		}
    }
    
    public void addURLMasterPattern(String pattern){
        cache.put(primaryGroup + MASTER_STRUCTURE, pattern, primaryGroup);
	}
    
    public void addContainerStructures(List<ContainerStructure> containerStructures, String containerIdentifier, String containerInode){
        cache.put(containerStructureGroup + containerIdentifier + containerInode, containerStructures, containerStructureGroup);
	}
    
    @SuppressWarnings("unchecked")
	public List<ContainerStructure> getContainerStructures(String containerIdentifier, String containerInode){
    	List<ContainerStructure> containerStructures = null;
    	
		try{
			containerStructures = (List<ContainerStructure>) cache.get(containerStructureGroup + containerIdentifier + containerInode, containerStructureGroup);
			return containerStructures;
			
		} catch (DotCacheException e) {
			Logger.debug(ContentTypeCacheImpl.class, "Cache Entry not found", e);
			return null;
    	}
	}
    
    public void removeContainerStructures(String containerIdentifier, String containerInode) {
        cache.remove(containerStructureGroup + containerIdentifier + containerInode, containerStructureGroup);
    }
    
    

    public void clearCache(){
	    //clear the cache
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
	}
    
	public String[] getGroups() {
    	return groups;
    }
    
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentType byVar(String var) {
        try{
        	return (ContentType) cache.get(primaryGroup + var,primaryGroup);
        }catch (Exception e) {
			Logger.debug(ContentTypeCacheImpl.class,"Cache Entry not found", e);
			return null;
    	}

	}
}
