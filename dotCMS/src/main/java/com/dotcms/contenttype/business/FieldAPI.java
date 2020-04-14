package com.dotcms.contenttype.business;

import com.dotcms.business.CloseDBIfOpened;

import java.util.*;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
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
	 * Returns a field based on the Content Type Id and the Field Relation Type
	 * @param contentTypeId
	 * @param fieldRelationType
	 * @return
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	Optional<Field> byContentTypeAndFieldRelationType(final String contentTypeId,
			final String fieldRelationType) throws DotDataException;

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
     * Returns the dataType and the number of the column of that field. e.g bool1
     * Only one Host Field and Tag Field per Content Type is allowed.
     * By default only 25 fields of each dataType is allowed, can be modified by this property db.number.of.contentlet.columns.per.datatype
     * 
     * @param field 
     * @return String that is the combination of the dataType and the number of the column.
     * @throws DotDataException Error occurred when performing the action.
     */
    String nextAvailableColumn(Field field) throws DotDataException;

	/**
	 * Delete a bunch of fields
	 *
	 * @param fieldsID fields's id to delete
	 * @param user user who delete the fields
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Collection<String> deleteFields(final List<String> fieldsID, final User user) throws DotDataException, DotSecurityException;

	/**
	 * Save a bunch of fields
	 *
	 * @param fields fields to save
	 * @param user user who save the fields
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
    void saveFields(final List<Field> fields, final User user) throws DotSecurityException, DotDataException;

	/**
	 * Apply Internationalization to field property using a Language Variable, for each field property set into
	 * fieldMap a Language Variable with the name contentTypeVariable.fieldVariable.propertyName is searched, if it
	 * exists then the field's property value is replaced by the language variable value
	 *
	 * @param contentType
	 * @param contentTypeInternationalization set the mode, language and user to search the Language Variable
	 * @param fieldMap field properties
	 * @return  The Field's properties with the new internationalization values
	 */
	public Map<String, Object> getFieldInternationalization(
			final ContentType contentType,
			final ContentTypeInternationalization contentTypeInternationalization,
			final Map<String, Object> fieldMap
	);

	/**
	 * Apply Internationalization to field property using a Language Variable, for each field property set into
	 * fieldMap a Language Variable with the name contentTypeVariable.fieldVariable.propertyName is searched, if it
	 * exists then the field's property value is replaced by the language variable value
	 *
	 * @param contentType
	 * @param contentTypeInternationalization set the mode, language and user to search the Language Variable
	 * @param fieldMap field properties
	 * @return  The Field's properties with the new internationalization values
	 */
	public Map<String, Object> getFieldInternationalization(
			final ContentType contentType,
			final ContentTypeInternationalization contentTypeInternationalization,
			final Map<String, Object> fieldMap,
			final User user
	);
}
