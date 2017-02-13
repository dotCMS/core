package com.dotmarketing.portlets.structure.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.EmptyField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;


@Deprecated
public class FieldFactory {

    private static FieldAPI fapi(){
        return APILocator.getContentTypeFieldAPI();
    }
	//### READ ###
	public static Field getFieldByInode(String inode) 
	{

		try {
            return new LegacyFieldTransformer(APILocator.getContentTypeFieldAPI().find(inode)).asOldField();
        } catch (DotStateException | DotDataException e) {
            return new Field();
        }

	}

	@SuppressWarnings("unchecked")
	public static List<Field> getFieldsByStructure(String structureInode)
	{
	       try {
	            return new LegacyFieldTransformer(APILocator.getContentTypeFieldAPI().byContentTypeId(structureInode)).asOldFieldList();
	        } catch (DotStateException | DotDataException e) {
	            return ImmutableList.of();
	        }
	    
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getFieldsByStructureSortedBySortOrder(String structureInode)
	{
	    return getFieldsByStructure(structureInode);
	}

	public static boolean isTagField(String fieldLuceneName, Structure st)
	{
	    try {
            com.dotcms.contenttype.model.field.Field f = fapi().byContentTypeIdAndVar(st.getInode(), fieldLuceneName);
            return (f instanceof TagField);
        } catch (DotDataException e) {
            return false;
        }

	}



	public static Field getFieldByVariableName(String structureInode, String velocityVarName)
	{
	    velocityVarName = StringUtils.camelCaseLower(velocityVarName);
        try {
            com.dotcms.contenttype.model.field.Field f = fapi().byContentTypeIdAndVar(structureInode, velocityVarName);
            return new LegacyFieldTransformer(f).asOldField();
        } catch (DotDataException e) {
            return new Field();
        }
	}

    public static Field getFieldByStructure(String structureInode, String fieldName){
        try{
            List<com.dotcms.contenttype.model.field.Field> fields = fapi().byContentTypeId(structureInode);
            for(com.dotcms.contenttype.model.field.Field field : fields){
                if(field.name().equals(fieldName)){
                    return new LegacyFieldTransformer(field).asOldField();
                }
            }
        }
        catch(DotDataException e){
            Logger.error(FieldFactory.class, e.getMessage(),e);
        }
        return new Field();
    }


	//### CREATE AND UPDATE ###
	public static Field saveField(Field oldField) throws DotHibernateException
	{
	    
        if (oldField.getFieldType().equals("host or folder") || oldField.getFieldType().equals("line_divider") || oldField.getFieldType().equals("tab_divider")
                || oldField.getFieldType().equals("categories_tab")
                || oldField.getFieldType().equals("permissions_tab") 
                || oldField.getFieldType().equals( "relationships_tab")
                || oldField.getFieldType().equals("category")
                || oldField.getFieldType().equals("tag")
             ) {

            oldField.setFieldContentlet(DataTypes.SYSTEM.toString());
            
        }
	    
	    com.dotcms.contenttype.model.field.Field field = new LegacyFieldTransformer(oldField).from();
	    try {
            return new LegacyFieldTransformer(
            	APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser())
            ).asOldField();
        } catch (DotDataException | DotSecurityException e) {
            throw new DotHibernateException(e.getMessage(),e);
        }
	}

	public static void saveField(Field oldField, String existingId) throws DotHibernateException
	{
	    oldField.setInode(existingId);

        saveField(oldField);


	}

	//### DELETE ###
	public static void deleteField(String inode) throws DotHibernateException
	{
		Field field = getFieldByInode(inode);
		deleteField(field);
	}

	public static void deleteField(Field oldField) throws DotHibernateException
	{
        com.dotcms.contenttype.model.field.Field field = new LegacyFieldTransformer(oldField).from();
	    try {
            fapi().delete(field);
        } catch (DotDataException e) {
            throw new DotHibernateException(e.getMessage(),e);
        }

	}

	public static String getNextAvaliableFieldNumber (String dataType, String currentFieldInode, String structureInode) {
	    
        try{
            com.dotcms.contenttype.model.field.Field proxy = FieldBuilder.builder(EmptyField.class)
                    .contentTypeId(structureInode)
                    .id(currentFieldInode)
                    .name("fake")
                    .variable("fake")

                    .dataType(DataTypes.getDataType(dataType)).build();
            

            return fapi().nextAvailableColumn(proxy);
        }
        catch(DotDataException e){
            Logger.error(FieldFactory.class, e.getMessage(),e);
        }
        return null;
	}

	public static FieldVariable saveFieldVariable(FieldVariable fieldVar){

	    com.dotcms.contenttype.model.field.FieldVariable var= new FieldVariableTransformer(fieldVar).newfield();
	    
        try {
            return new FieldVariableTransformer(fapi().save(var, APILocator.systemUser())).oldField();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(FieldFactory.class, e.getMessage());
        }
        return new FieldVariable();
	}

	public static FieldVariable getFieldVariable(String id){
	    try {
            return new FieldVariableTransformer(fapi().loadVariable(id)).oldField();
        } catch (DotStateException | DotDataException e) {
            Logger.error(FieldFactory.class, e.getMessage());
        }
        return new FieldVariable();
	}

	public static void deleteFieldVariable(String id){
		FieldVariable fieldVar = getFieldVariable(id);
		deleteFieldVariable(fieldVar);
	}

	public static void deleteFieldVariable(FieldVariable fieldVar){
	       try {
	           fapi().delete(new FieldVariableTransformer(fieldVar).newfield());
	        } catch (DotStateException | DotDataException e) {
	            Logger.error(FieldFactory.class, e.getMessage());
	        }
	
	}


	public static List<FieldVariable> getFieldVariablesForField (String fieldId ){

		Field proxy = new Field();
		proxy.setInode(fieldId);
		return getFieldVariablesForField(proxy);
	}

	public static List<FieldVariable> getFieldVariablesForField (Field field ){
	    
	       try {
	           com.dotcms.contenttype.model.field.Field newfield = fapi().find(field.getInode());
	           List<com.dotcms.contenttype.model.field.FieldVariable > fl = fapi().loadVariables(newfield);
	           return new FieldVariableTransformer(fl).oldFieldList();
	        } catch (DotStateException | DotDataException e) {
	            Logger.error(FieldFactory.class, e.getMessage());
	        }
	        return new ArrayList<FieldVariable>();
	    
	}

}
