package com.dotmarketing.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author David
 */
public class ContentTypeCacheImpl extends ContentTypeCache {

	public static final String MASTER_STRUCTURE = "dotMaster_Structure";
	
    private DotCacheAdministrator cache;
    private final String primaryGroup = "StructureCache";
    private final String containerStructureGroup = "ContainerStructureCache";
    private final String structuresByTypeGroup = "StructuresByTypeCache";
    private final String recentGroup = "RecentCache";

    // region's name for the cache
    private final String[] groups = { primaryGroup, containerStructureGroup, structuresByTypeGroup, recentGroup };

    public ContentTypeCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    public String getContainerStructureGroup() {
        return containerStructureGroup;
    }
	
    public void add(Structure st){
    	if(st==null || !UtilMethods.isSet(st.getInode())){
    		return;
    	}
		// we use the identifier uri for our mappings.
		String inode = st.getInode();
        String structureName = st.getName();
        String velocityVarName = st.getVelocityVarName();
		cache.put(primaryGroup + inode, st, primaryGroup);
        cache.put(primaryGroup + structureName, st, primaryGroup);
        cache.put(primaryGroup + velocityVarName, st, primaryGroup);
        if (UtilMethods.isSet(velocityVarName))
        	cache.put(primaryGroup + velocityVarName.toLowerCase(), st, primaryGroup);
        removeStructuresByType(st.getStructureType());
	}
    
    /*public static Structure getStructureByInode(long inode){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Structure st = null;
    	try{
    		st = (Structure) cache.get(primaryGroup + inode,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(ContentTypeCacheImpl.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByInode(inode);
            addStructure(st);
        }
        return st;
	}*/

    public Structure getStructureByInode(String inode) {
    	Structure st = null;
    	try{
    		st = (Structure) cache.get(primaryGroup + inode,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(ContentTypeCacheImpl.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByInode(inode);
            if(st != null && UtilMethods.isSet(st.getInode()))
            	add(st);
            else
            	return null;
        }
        return st;
    }

    /**
     * This methods retrieves the structure from the cache based in the 
     * structure name. 
     * 
     * This methods tries to retrieve the structure from the cache, if the
     * structure were not found in the cache, it would try to find it in database
     * and store it in cache.
     * 
     * <b>NOTE:</b> This method runs the same code than getStructureByType
     * the name and the type of a structure are synonyms
     * 
     * @param name Name of the structure
     * @return The structure from cache
     * 
     * @deprecated getting the structure by its name might not be safe, since the 
     * structure name can be changed by the user, use getStructureByVelocityVarName
     */
     public Structure getStructureByName(String name) {
        Structure st = null;
        try{
        	st = (Structure) cache.get(primaryGroup + name,primaryGroup);
        }catch (DotCacheException e) {
			Logger.debug(ContentTypeCacheImpl.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByType(name);
            add(st);
        }
        return st;
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
    	
        Structure st = null;
        try{
        	st = (Structure) cache.get(primaryGroup + variableName,primaryGroup);
        }catch (DotCacheException e) {
			Logger.debug(ContentTypeCacheImpl.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByVelocityVarName(variableName);
            add(st);
        }
        return st;
    }
    /**
     * @see getStructureByName(String)
     * 
     * @param type Type of the structure
     * @return The structure from cache

     * @deprecated getting the structure by its name might not be safe, since the 
     * structure name can be changed by the user, use getStructureByVelocityVarName
     */
    public Structure getStructureByType(String type){
        return getStructureByName(type);
    }

    /**
     * @deprecated getting the structure by its name might not be safe, since the 
     * structure name can be changed by the user, use getStructureByVelocityVarName
     */
    public boolean hasStructureByType (String name) {
        return getStructureByType(name) != null;
    }
    
    /**
     * @deprecated getting the structure by its name might not be safe, since the 
     * structure name can be changed by the user, use getStructureByVelocityVarName
     */
    public boolean hasStructureByName (String name) {
        return getStructureByName(name) != null;
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
        String inode = st.getInode();
        String structureName = st.getName();
        cache.remove(primaryGroup + inode,primaryGroup);
        cache.remove(primaryGroup + structureName,primaryGroup);
        clearURLMasterPattern();
        removeStructuresByType(st.getStructureType());
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
    
    public String getStructuresByTypeGroup() {
        return structuresByTypeGroup;
    }
    
    @SuppressWarnings("unchecked")
    public List<Structure> getStructuresByType(int structureType) {
        List<Structure> structuresByType = null;
        
        try{
            structuresByType = (List<Structure>) cache.get(structuresByTypeGroup + structureType, structuresByTypeGroup);
            return structuresByType;
            
        } catch (DotCacheException e) {
            Logger.debug(ContentTypeCacheImpl.class, "Cache Entry not found: ", e);
            return null;
        }
    }
    
    public void addStructuresByType(List<Structure> structures, int structureType){
        cache.put(structuresByTypeGroup + structureType, structures, structuresByTypeGroup);
    }
    
    public void removeStructuresByType(int structureType) {
        cache.remove(structuresByTypeGroup + structureType, structuresByTypeGroup);
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
    public void addRecents(Structure.Type type, User user, int nRecents, Collection<Map<String, Object>> recents) {
        try {
            Map<String, Collection<Map<String, Object>>> userRecent = getFromRecentCache(user);
            String mapKey = String.format("%s-%d", type.toString(), nRecents);
            userRecent.put(mapKey, recents);
        } catch (DotCacheException e) {
            Logger.debug(ContentTypeCacheImpl.class, "Cache Entry not found: ", e);
        }

    }

    public Collection<Map<String, Object>> getRecents(Structure.Type type, User user, int nRecents){
        try {
            Map<String, Collection<Map<String, Object>>> userRecent = getFromRecentCache(user);
            String mapKey = String.format("%s-%d", type.toString(), nRecents);
            return userRecent.get(mapKey);
        } catch (DotCacheException e) {
            Logger.debug(ContentTypeCacheImpl.class, "Cache Entry not found: ", e);
            return null;
        }
    }

    public void clearRecents(String userId){
        cache.remove(userId, recentGroup);
    }

    private Map<String, Collection<Map<String, Object>>> getFromRecentCache(User user) throws DotCacheException {
        Map<String, Collection<Map<String, Object>>> userRecent =
                (Map<String, Collection<Map<String, Object>>>) cache.get(user.getUserId(), recentGroup);

        if (userRecent == null){
            userRecent = new HashMap<>();
            cache.put(user.getUserId(), userRecent, recentGroup);
        }
        return userRecent;
    }
}
