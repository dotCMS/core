package com.dotmarketing.portlets.structure.factories;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.EmptyField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dotcms.util.CollectionsUtils.set;

/**
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 * @deprecated As of dotCMS 4.1.0, this API has been deprecated. From now on,
 *             please use the {@link FieldAPI} interface via
 *             {@link APILocator#getContentTypeFieldAPI()} in order to interact
 *             with Content Type fields.
 *
 */
public class FieldFactory {

	/**
	 * 
	 * @return
	 */
    private static FieldAPI fapi(){
        return APILocator.getContentTypeFieldAPI();
    }

	//### READ ###
    /**
     * 
     * @param inode
     * @return
     */
	public static Field getFieldByInode(String inode) 
	{
		try {
            return new LegacyFieldTransformer(APILocator.getContentTypeFieldAPI().find(inode)).asOldField();
        } catch (DotStateException | DotDataException e) {
            return new Field();
        }
	}

	/**
	 * 
	 * @param structureInode
	 * @return
	 */
	public static List<Field> getFieldsByStructure(String structureInode)
	{
	       try {
	            return new LegacyFieldTransformer(
	                APILocator.getContentTypeAPI(APILocator.systemUser()).find(structureInode).fields()
	                ).asOldFieldList();
	        } catch (Exception e) {
	            return ImmutableList.of();
	        }
	}

	/**
	 * 
	 * @param structureInode
	 * @return
	 */
	public static List<Field> getFieldsByStructureSortedBySortOrder(String structureInode)
	{
	    return getFieldsByStructure(structureInode);
	}

	/**
	 * 
	 * @param fieldLuceneName
	 * @param st
	 * @return
	 */
	public static boolean isTagField(String fieldLuceneName, Structure st)
	{
	    try {
            com.dotcms.contenttype.model.field.Field f = fapi().byContentTypeIdAndVar(st.getInode(), fieldLuceneName);
            return (f instanceof TagField);
        } catch (DotDataException e) {
            return false;
        }
	}

	/**
	 * 
	 * @param structureInode
	 * @param velocityVarName
	 * @return
	 */
	public static Field getFieldByVariableName(String structureInode, String velocityVarName)
	{
        try {
            com.dotcms.contenttype.model.field.Field f = APILocator.getContentTypeAPI(APILocator.systemUser()).find(structureInode).fieldMap().get(velocityVarName);

            return new LegacyFieldTransformer(f).asOldField();
        } catch (Exception e) {
            return new Field();
        }
	}

	/**
	 * 
	 * @param structureInode
	 * @param fieldName
	 * @return
	 */
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
	/**
	 * Saves a field in a Content Type.
	 * 
	 * @param oldField
	 *            - The legacy Field object.
	 * @return The Field object that was saved.
	 * @throws DotHibernateException
	 *             An error occurred when saving the field.
	 */
	public static Field saveField(Field oldField) throws DotHibernateException {
		final Set<String> systemFieldsSet = set(Field.FieldType.HOST_OR_FOLDER.toString(),
				Field.FieldType.LINE_DIVIDER.toString(), Field.FieldType.TAB_DIVIDER.toString(),
				Field.FieldType.CATEGORIES_TAB.toString(), Field.FieldType.PERMISSIONS_TAB.toString(),
				Field.FieldType.RELATIONSHIP.toString(), Field.FieldType.RELATIONSHIPS_TAB.toString(),
				Field.FieldType.CATEGORY.toString(), Field.FieldType.TAG.toString(),
				Field.FieldType.HIDDEN.toString());
		if (systemFieldsSet.contains(oldField.getFieldType())) {
			oldField.setFieldContentlet(DataTypes.SYSTEM.toString());
		}
        
        //The Host or Folder Field and the Tag Field needs to be always indexed (issue #11128)
        if(Field.FieldType.HOST_OR_FOLDER.toString().equals(oldField.getFieldType()) || Field.FieldType.TAG.toString().equals(oldField.getFieldType())){
        	oldField.setIndexed(true);
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

	/**
	 * 
	 * @param oldField
	 * @param existingId
	 * @throws DotHibernateException
	 */
	public static void saveField(Field oldField, String existingId) throws DotHibernateException
	{
	    oldField.setInode(existingId);
        saveField(oldField);
	}

	//### DELETE ###
	/**
	 * 
	 * @param inode
	 * @throws DotHibernateException
	 */
	public static void deleteField(String inode) throws DotHibernateException
	{
		Field field = getFieldByInode(inode);
		deleteField(field);
	}

	/**
	 * 
	 * @param oldField
	 * @throws DotHibernateException
	 */
	public static void deleteField(Field oldField) throws DotHibernateException
	{
        com.dotcms.contenttype.model.field.Field field = new LegacyFieldTransformer(oldField).from();
	    try {
            fapi().delete(field);
        } catch (DotDataException e) {
            throw new DotHibernateException(e.getMessage(),e);
        }
	}

	/**
	 * 
	 * @param dataType
	 * @param currentFieldInode
	 * @param structureInode
	 * @return
	 */
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

	/**
	 * 
	 * @param fieldVar
	 * @return
	 */
	public static FieldVariable saveFieldVariable(FieldVariable fieldVar){
	    com.dotcms.contenttype.model.field.FieldVariable var= new FieldVariableTransformer(fieldVar).newfield();
	    
        try {
            return new FieldVariableTransformer(fapi().save(var, APILocator.systemUser())).oldField();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(FieldFactory.class, e.getMessage());
        }
        return new FieldVariable();
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public static FieldVariable getFieldVariable(String id){
	    try {
            return new FieldVariableTransformer(FactoryLocator.getFieldFactory().loadVariable(id)).oldField();
        } catch (DotStateException | DotDataException e) {
            Logger.error(FieldFactory.class, e.getMessage());
        }
        return new FieldVariable();
	}

	/**
	 * 
	 * @param id
	 */
	public static void deleteFieldVariable(String id){
		FieldVariable fieldVar = getFieldVariable(id);
		deleteFieldVariable(fieldVar);
	}

	/**
	 * 
	 * @param fieldVar
	 */
	public static void deleteFieldVariable(FieldVariable fieldVar){
	       try {
	           fapi().delete(new FieldVariableTransformer(fieldVar).newfield());
	        } catch (final DotStateException | UniqueFieldValueDuplicatedException | DotDataException e) {
	            Logger.error(FieldFactory.class, e.getMessage());
	        }
	}

	/**
	 * 
	 * @param fieldId
	 * @return
	 */
	public static List<FieldVariable> getFieldVariablesForField (String fieldId ){
		Field proxy = new Field();
		proxy.setInode(fieldId);
		return getFieldVariablesForField(proxy);
	}

	/**
	 * 
	 * @param field
	 * @return
	 */
	public static List<FieldVariable> getFieldVariablesForField (Field field ){
	       try {
	           com.dotcms.contenttype.model.field.Field newfield = fapi().find(field.getInode());
	           List<com.dotcms.contenttype.model.field.FieldVariable > fl = newfield.fieldVariables();
	           return new FieldVariableTransformer(fl).oldFieldList();
	        } catch (DotStateException | DotDataException e) {
	            Logger.error(FieldFactory.class, e.getMessage());
	        }
	        return new ArrayList<>();
	}

}
