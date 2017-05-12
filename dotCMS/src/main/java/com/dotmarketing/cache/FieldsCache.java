package com.dotmarketing.cache;

import java.util.List;

import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
        Logger.warn(FieldsCache.class, "addFields no longer implemented");
	}
    public static void addField(Field fields){
        Logger.warn(FieldsCache.class, "addField no longer implemented");
    }
    public static List<Field> getFieldsByStructureInode(String inode){
        try {
            return new LegacyFieldTransformer(APILocator.getContentTypeAPI(APILocator.getUserAPI().getSystemUser(), true).find(inode).fields()).asOldFieldList();
        } catch (DotStateException | DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }
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
        try {
            return new LegacyFieldTransformer(APILocator.getContentTypeAPI(APILocator.getUserAPI().getSystemUser(), true).find(velocityVarName).fields()).asOldFieldList();
        } catch (DotStateException | DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }
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

    }

	public static void clearCache(){

	}
    
	public static String[] getGroups() {
    	return new String[0];

    }
    
    public static String getPrimaryGroup() {
    	return "FieldsCache";
    }
    
    public static String getFieldsVarGroup() {
    	return "FieldsVarCache";
    }
    
    
	public static Field getField(String id) {
        try {
            return new LegacyFieldTransformer(APILocator.getContentTypeFieldAPI().find(id)).asOldField();
        } catch (DotStateException | DotDataException e) {
            throw new DotStateException(e);
        }
	}
	
	public static void removeField(Field field) {

    } 
    
	public static List<FieldVariable> getFieldVariables(Field field) {
        try {
        	
            return new FieldVariableTransformer(
            		new LegacyFieldTransformer(field).from().fieldVariables()        
            ).oldFieldList();
        } catch (DotStateException e) {
            throw new DotStateException(e);
        }
    }
    
	public static void addFieldVariables(Field field, List<FieldVariable> vars) {


    }
	
	
	public static void removeFieldVariables(Field field) {

    }
    
    
}
