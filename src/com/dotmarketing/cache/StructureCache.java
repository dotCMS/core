package com.dotmarketing.cache;

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
public class StructureCache {

	public static final String MASTER_STRUCTURE = "dotMaster_Structure";
	
    public static void addStructure(Structure st){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		String inode = st.getInode();
        String structureName = st.getName();
        String velocityVarName = st.getVelocityVarName();
		cache.put(getPrimaryGroup() + inode, st, getPrimaryGroup());
        cache.put(getPrimaryGroup() + structureName, st, getPrimaryGroup());
        cache.put(getPrimaryGroup() + velocityVarName, st, getPrimaryGroup());
        if (UtilMethods.isSet(velocityVarName))
        	cache.put(getPrimaryGroup() + velocityVarName.toLowerCase(), st, getPrimaryGroup());
	}
    
    /*public static Structure getStructureByInode(long inode){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Structure st = null;
    	try{
    		st = (Structure) cache.get(getPrimaryGroup() + inode,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(StructureCache.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByInode(inode);
            addStructure(st);
        }
        return st;
	}*/

    public static Structure getStructureByInode(String inode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Structure st = null;
    	try{
    		st = (Structure) cache.get(getPrimaryGroup() + inode,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(StructureCache.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByInode(inode);
            addStructure(st);
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
    public static Structure getStructureByName(String name) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        Structure st = null;
        try{
        	st = (Structure) cache.get(getPrimaryGroup() + name,getPrimaryGroup());
        }catch (DotCacheException e) {
			Logger.debug(StructureCache.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByType(name);
            addStructure(st);
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
    public static Structure getStructureByVelocityVarName(String variableName) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        Structure st = null;
        try{
        	st = (Structure) cache.get(getPrimaryGroup() + variableName,getPrimaryGroup());
        }catch (DotCacheException e) {
			Logger.debug(StructureCache.class,"Cache Entry not found", e);
    	}
        if (st == null) {
            st = StructureFactory.getStructureByVelocityVarName(variableName);
            addStructure(st);
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
    public static Structure getStructureByType(String type){
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
    
    public static void removeStructure(Structure st) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String inode = st.getInode();
        String structureName = st.getName();
        cache.remove(getPrimaryGroup() + inode,getPrimaryGroup());
        cache.remove(getPrimaryGroup() + structureName,getPrimaryGroup());
        clearURLMasterPattern();
    }

    public static String getURLMasterPattern() throws DotCacheException {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		return (String)cache.get(getPrimaryGroup() + MASTER_STRUCTURE,getPrimaryGroup());
    }
    
    public static void clearURLMasterPattern(){
    	synchronized (StructureCache.MASTER_STRUCTURE) {
    		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        	cache.remove(getPrimaryGroup() + MASTER_STRUCTURE,getPrimaryGroup());	
		}
    }
    
    public static void addURLMasterPattern(String pattern){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(getPrimaryGroup() + MASTER_STRUCTURE, pattern, getPrimaryGroup());
        
	}
    
    public static void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup());
	}
	public static String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public static String getPrimaryGroup() {
    	return "StructureCache";
    }  
}
