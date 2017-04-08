package com.dotcms.contenttype.business;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * 
 * Through this API you will be able to access, delete, create and modify the {@link Field}, there are some
 * Field Variables Names that are already set by dotcms:
 * <p><ul>
 * <li> Inode
 * <li> languageId
 * <li> stInode
 * <li> lastReview
 * <li> nextReview
 * <li> reviewInternal
 * <li> disabledWYSIWYG
 * <li> locked
 * <li> archived
 * <li> live
 * <li> working
 * <li> modDate
 * <li> modUser
 * <li> owner
 * <li> identifier
 * <li> sortOrder
 * <li> host
 * <li> folder
 * </ul><p>
 * 
 * 
 * @author Will Ezell
 *
 */
public interface FieldAPI {

	static FieldAPI api = new FieldAPIImpl();

	default FieldAPI instance() {
		return api;
	}

	public static final Date VALIDATE_AFTER = new Date(1451606400000l);
	
	/**
	 * Retrieves the list of the base Fields Types
	 * 
	 * @return List of baseFieldTypes
	 */
	List<Class> fieldTypes();
	
	/**
	 * Register a field type to the list of field types. Still not implemented.
	 * 
	 * @param type Field that wants to be registered.
	 */
	void registerFieldType(Field type);

	/**
	 * Removes a field type to the list of field types. Still not implemented.
	 * 
	 * @param type Field that wants to be removed.
	 */
	void deRegisterFieldType(Field type);

	/**
	 * Deletes a field from a content type (as specified in the passed in Field object)
	 * 
	 * @param field Field that wants to be deleted.
	 * @throws DotDataException Error occurred when performing the action.
	 */
	void delete(Field field) throws DotDataException;

	/**
	 * Deletes a field from a content type (as specified in the passed in Field object)
	 * 
	 * @param field Field that wants to be deleted.
	 * @throws DotDataException Error occurred when performing the action.
	 */
	void delete(Field field, User user) throws DotDataException, DotSecurityException;

	/**
	 * Deletes all the fields related to the given Content Type
	 * 
	 * @param type Content Type that contains the fields.
	 * @throws DotDataException Error occurred when performing the action.
	 */
	void deleteFieldsByContentType(ContentType type) throws DotDataException;
	
	/**
	 * Returns a field based on the Content Type and the Field Variable
	 * 
	 * @param type Content Type to search on
	 * @param fieldVar Field Variable to search on
	 * @return Field Object that met the Field Variable and related to the Content Type
	 * @throws DotDataException Error occurred when performing the action.
	 */
	Field byContentTypeAndVar(ContentType type, String fieldVar) throws DotDataException;
	
	/**
	 * Retrieves a Field given its id
	 * 
	 * @param id Id of the field
	 * @return Field Object that is related to the id.
	 * @throws DotDataException Error occurred when performing the action.
	 */
	Field find(String id) throws DotDataException;
	
	/**
	 * Retrieves a List of Fields based on the Content Type Id.
	 * 
	 * @param typeId Content Type Id to search on.
	 * @return List of Field Objects that are related to the Content Type. 
	 * @throws DotDataException Error occurred when performing the action.
	 */
	List<Field> byContentTypeId(String typeId) throws DotDataException;

	/**
	 * Saves a new Field
	 * 
	 * @param field Field to be saved.
	 * @param user User that is going to save the Field
	 * @return Saved Field Object
	 * @throws DotDataException Error occurred when performing the action.
	 * @throws DotSecurityException The user does not have permissions to perform this action.
	 */
	Field save(Field field, User user) throws DotDataException, DotSecurityException;
	
	/**
	 * Saves a new Field Variable.
	 * 
	 * @param fieldVar Field Variable to be saved.
	 * @param user User that is going to save the Field Variable.
	 * @return Saved Field Variable Object
	 * @throws DotDataException Error occurred when performing the action.
	 * @throws DotSecurityException The user does not have permissions to perform this action.
	 */
	FieldVariable save(FieldVariable fieldVar, User user) throws DotDataException, DotSecurityException;

	static Set<String> RESERVED_FIELD_VARS= ImmutableSet.of(
			Contentlet.INODE_KEY,
			Contentlet.LANGUAGEID_KEY,
			Contentlet.STRUCTURE_INODE_KEY,
			Contentlet.LAST_REVIEW_KEY,
			Contentlet.NEXT_REVIEW_KEY,
			Contentlet.REVIEW_INTERNAL_KEY,
			Contentlet.DISABLED_WYSIWYG_KEY,
			Contentlet.LOCKED_KEY,
			Contentlet.ARCHIVED_KEY,
			Contentlet.LIVE_KEY,
			Contentlet.WORKING_KEY,
			Contentlet.MOD_DATE_KEY,
			Contentlet.MOD_USER_KEY,
			Contentlet.OWNER_KEY,
			Contentlet.IDENTIFIER_KEY,
			Contentlet.SORT_ORDER_KEY,
			Contentlet.HOST_KEY,
			Contentlet.FOLDER_KEY);

	/**
	 * Returns a field based on the Content Type Id and the Field Variable
	 * 
	 * @param type Content Type Id to search on
	 * @param fieldVar Field Variable to search on
	 * @return Field Object that met the Field Variable and related to the Content Type
	 * @throws DotDataException Error occurred when performing the action.
	 */
	Field byContentTypeIdAndVar(String id, String fieldVar) throws DotDataException;

    /**
	 * Deletes a field variable from a field (as specified in the passed in FieldVariable object)
	 * 
	 * @param field Field Variable that wants to be deleted.
	 * @throws DotDataException Error occurred when performing the action.
	 */
    void delete(FieldVariable fieldVar) throws DotDataException;

    /**
     * Retrieves the Field Variables related to a specific Field.
     * 
     * @param field Field to search on 
     * @return List of Field Variables Objects that are related to a Field
     * @throws DotDataException Error occurred when performing the action.
     */
    List<FieldVariable> loadVariables(Field field) throws DotDataException;
    
    /**
     * Retrieves a Field Variable given its id
     * 
     * @param id Id of the field Variable.
     * @return Field Variable related to the given id.
     * @throws DotDataException Error occurred when performing the action.
     */
    FieldVariable loadVariable(String id) throws DotDataException;

    /**
     * Returns the dataType and the number of the column of that field. e.g bool1
     * Only one Host Field and Tag Field per Content Type is allowed.
     * By default only 25 fields of each dataType is allowed, can be modified by this property db.number.of.contentlet.columns.per.datatype
     * 
     * @param field 
     * @return String that is the combination of the dataType and the number of the column.
     * @throws DotDataException Error occurred when performing the action.
     */
    String nextAvailableColumn(Field field) throws DotDataException;
	
}
