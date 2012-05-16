package com.dotmarketing.cache;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

/**
 * @author David
 * @author Jason Tesser
 */
public class FieldsCache {

    public static void addFields(Structure st, List<Field> fields){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		String inode = st.getInode();
		cache.put(getPrimaryGroup() + inode, fields,getPrimaryGroup());
        cache.put(getPrimaryGroup() + st.getVelocityVarName(), fields, getPrimaryGroup());
	}
    
    public static List<Field> getFieldsByStructureInode(String inode){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	List<Field> fields = null;
    	try{
    		fields = (List<Field>) cache.get(getPrimaryGroup() + inode, getPrimaryGroup());
	    }catch (DotCacheException e) {
			Logger.debug(FieldsCache.class, "Cache Entry not found", e);
		}
        if (fields == null) {
            Structure st = StructureCache.getStructureByInode(inode);
            fields = st.getFields();
            if(fields.size()>0)
                addFields(st, fields);
        }
        return fields;
	}

    /*public static List<Field> getFieldsByStructureInode(String inode) 
    {
        return getFieldsByStructureInode(Long.parseLong(inode));
    }*/

    /**
     * This methods retrieves the fields from the cache based in the 
     * structure velocity variable name, which is the safest way to reference
     * a structure since the velocity variable name never changes. 
     * 
     * This methods tries to retrieve the fields from the cache, if the
     * structure were not found in the cache, it would try to find it in database
     * and store the fields in cache.
     * 
     * @param velocityVarName velocity variable name of the structure
     * @return The fields of the structure
     * 
     * 
     */
    @SuppressWarnings("unchecked")
	public static List<Field> getFieldsByStructureVariableName(String velocityVarName){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	List<Field> fields = null;
        try{
        	fields = (List<Field>) cache.get(getPrimaryGroup() + velocityVarName, getPrimaryGroup());
        }catch (DotCacheException e) {
			Logger.debug(FieldsCache.class,"Cache Entry not found", e);
        }
        if (fields == null) {
        	synchronized (velocityVarName.intern()){
                try{
                	fields = (List<Field>) cache.get(getPrimaryGroup() + velocityVarName, getPrimaryGroup());
                }catch (DotCacheException e) {
        			Logger.debug(FieldsCache.class,"Cache Entry not found", e);
                }
        		if(fields ==null){
		            Structure st = StructureCache.getStructureByVelocityVarName(velocityVarName);
		            fields = FieldFactory.getFieldsByStructure(st.getInode());
		            if(fields.size()>0)
		                addFields(st, fields);
        		}
        	}
        }
        return new ArrayList(fields);
    }
    
    /**
     * This methods retrieves the fields from the cache based in the 
     * structure name. 
     * 
     * This methods tries to retrive the fields from the cache, if the
     * structure were not found in the cache, it would try to find it in database
     * and store the fields in cache.
     * 
     * <b>NOTE:</b> This method runs the same code than getFieldsByStructureType
     * the name and the type of a structure are synomyns
     * 
     * @param name Name of the structure
     * @return The fields of the structure
     * 
     * @deprecated referencing a structure by its name is not safe since the 
     * structure name can be changed by the user, used the structure velocity 
     * variable name instead
     * 
     */
    

    public boolean hasFieldsByStructureInode (String inode) {
        return getFieldsByStructureInode(inode) != null;
    }
    
    /*public boolean hasFieldsByStructureInode (long inode) {
        return getFieldsByStructureInode(inode) != null;
    }*/
    
    public static void removeFields(Structure st){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String inode = st.getInode();
        cache.remove(getPrimaryGroup() + inode, getPrimaryGroup());
        cache.remove(getPrimaryGroup() + st.getVelocityVarName(), getPrimaryGroup());
        cache.remove(getPrimaryGroup() + st.getVelocityVarName(), getPrimaryGroup());
        StructureCache.clearURLMasterPattern();
    }

	public static void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    cache.flushGroup(getPrimaryGroup());
	    StructureCache.clearURLMasterPattern();
	}
    
	public static String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public static String getPrimaryGroup() {
    	return "FieldsCache";
    }
    
    public static void addField(Field f){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();		
		String inode = f.getInode();
		cache.put(getPrimaryGroup() + inode, f, getPrimaryGroup());        
	}
    
	public static Field getField(String id) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Field field = null;
    	try{
    		field = (Field) cache.get(getPrimaryGroup() + id, getPrimaryGroup());
	    }catch (DotCacheException e) {
			Logger.debug(FieldsCache.class, "Cache Entry not found", e);
		}
        if (field == null) {
            field = FieldFactory.getFieldByInode(id);
            addField(field);
        }
        return field;
	}
	
	public static void removeField(Field field) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String id = field.getInode();
        cache.remove(getPrimaryGroup() + id,getPrimaryGroup());
    }
	
    public static void addFieldVariable(FieldVariable fVar){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String id = fVar.getId();
		cache.put(getPrimaryGroup() + id, fVar, getPrimaryGroup());
	}
    
	public static FieldVariable getFieldVariable(String id) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	FieldVariable fieldVar = null;
    	try{
    		fieldVar = (FieldVariable) cache.get(getPrimaryGroup() + id, getPrimaryGroup());
	    }catch (DotCacheException e) {
			Logger.debug(FieldsCache.class, "Cache Entry not found", e);
		}
        if (fieldVar == null) {
            fieldVar = FieldFactory.getFieldVariable(id);
            addFieldVariable(fieldVar);
        }
        return fieldVar;
	}

    public static void removeFieldVariable(FieldVariable fieldVar) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String id = fieldVar.getId();
        cache.remove(getPrimaryGroup() + id,getPrimaryGroup());
    }
}
