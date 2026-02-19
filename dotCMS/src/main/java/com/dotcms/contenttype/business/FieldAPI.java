package com.dotcms.contenttype.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 
 * Through this API you will be able to access, delete, create and modify the {@link Field}, there are some
 * Field Variables Names that are already set by dotcms:
 * <p><ul>
 * <li> Inode
 * <li> languageId
 * <li> stInode
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
	String FULLSCREEN_FIELD_FEATURE_FLAG = FeatureFlagName.FULLSCREEN_FIELD_FEATURE_FLAG;

	/**
	 * Retrieves the list of the base Fields Types
	 * 
	 * @return List of baseFieldTypes
	 */
	List<Class<? extends Field>> fieldTypes();
	
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
	 * Saves a {@link Field} object to the database. If the field already exists, it updates the
	 * existing field and will be automatically reordered in the field layout. This method also
	 * handles the necessary validations, reordering, and cache invalidations.
	 *
	 * @param field The {@link Field} being saved.
	 * @param user  The {@link User} performing the save operation.
	 *
	 * @return The saved {@link Field}.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The specified user does not have permissions to perform this
	 *                              action.
	 */
	Field save(final Field field, final User user) throws DotDataException, DotSecurityException;

	/**
	 * Saves a {@link Field} object to the database. If the field already exists, it updates the
	 * existing field. This method also handles the necessary validations, potential reordering,
	 * and cache invalidations.
	 *
	 * @param field         The {@link Field} being saved.
	 * @param user          The {@link User} performing the save operation.
	 * @param reorderIfNeed If set to {@code true}, all fields relative to the order of the field
	 *                      being saved will be recalculated.
	 *
	 * @return The saved {@link Field}.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The specified user does not have permissions to perform this
	 *                              action.
	 */
	Field save(final Field field, final User user, final boolean reorderIfNeed)
			throws DotDataException, DotSecurityException;

	/**
	 * Saves a new Field Variable. For this operation to succeed, the Field Variable must be
	 * already assigned to a field that actually exists in a Content Type.
	 *
	 * @param fieldVar The {@link FieldVariable} being saved.
	 * @param user     The {@link User} that is going to save the Field Variable.
	 *
	 * @return The saved {@link FieldVariable} object.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The specified user does not have permissions to perform this
	 *                              action.
	 */
	FieldVariable save(final FieldVariable fieldVar, final User user) throws DotDataException, DotSecurityException;

	/**
	 * Saves the list of {@link FieldVariable} objects to the specified {@link Field}.
	 *
	 * @param fieldVariables The list of new or existing Field Variables.
	 * @param field          The field that the Field Variables belong to.
	 *
	 * @throws DotStateException An error occurred when saving a Field Variable.
	 */
	void save(final List<FieldVariable> fieldVariables, final Field field);

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
	 * @param id Content Type Id to search on
	 * @param fieldVar Field Variable to search on
	 * @return Field Object that met the Field Variable and related to the Content Type
	 * @throws DotDataException Error occurred when performing the action.
	 */
	Field byContentTypeIdAndVar(String id, String fieldVar) throws DotDataException;

    /**
	 * Deletes a field variable from a field (as specified in the passed in FieldVariable object)
	 * 
	 * @param fieldVar Field Variable that wants to be deleted.
	 * @throws DotDataException Error occurred when performing the action.
	 */
    void delete(FieldVariable fieldVar) throws DotDataException, UniqueFieldValueDuplicatedException;

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
	 * Saves the specified list of fields that are assigned to a given Content Type.
	 *
	 * @param fields The list of {@link Field} objects being saved.
	 * @param user   The {@link User} saving such fields.
	 *
	 * @throws DotSecurityException The specified User does not have the required permissions to
	 *                              execute this action.
	 * @throws DotDataException     An error occurred when interacting with the database.
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

	/**
	 * Given a field load and return its variables.
	 *
	 * @param field field variables belong to
	 * @return list of variables
	 * @throws DotDataException when SQL error happens
	 */
	List<FieldVariable> loadVariables(Field field) throws DotDataException;

	/**
	 * Takes a field and returns if the field is a "fullScreen" if and only if:
	 * <ul>
	 *     <li>It's a multiline field.</li>
	 *     <li>It's the only field on its tab.</li>
	 *     <li>It does not have the field variable {@code fullScreenField=false}.</li>
	 * </ul>
	 *
	 * @param fieldIn The {@link Field} that will be checked.
	 *
	 * @return If the Field can be displayed in fullscreen, returns {@code true}.
	 */
	boolean isFullScreenField(Field fieldIn);

}
