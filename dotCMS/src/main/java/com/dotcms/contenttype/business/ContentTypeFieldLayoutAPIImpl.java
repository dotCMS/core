package com.dotcms.contenttype.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.field.layout.FieldUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Create, delete, Update and Move Field making sure that the {@link ContentType} keep with a right layout after the operation
 * If before the operation the {@link ContentType} already has a wrong layout then it is fix before the operation.
 * If after the operation the {@link ContentType} would has a wrong layout then a
 * {@link com.dotcms.contenttype.model.field.layout.FieldLayoutValidationException} is thrown.
 *
 * @see FieldLayout
 */
public class ContentTypeFieldLayoutAPIImpl implements ContentTypeFieldLayoutAPI {

    private final FieldAPI fieldAPI;

    public ContentTypeFieldLayoutAPIImpl() {
        this.fieldAPI = APILocator.getContentTypeFieldAPI();
    }

    /**
     * Updates a field in the specified {@link ContentType}. If, before the update, the
     * {@link ContentType} has an invalid layout, it will be fixed before the update.
     * <p>The {@code fieldToUpdate}'s sort order attribute will be ignored. If you want to change
     * such a value for any field, you must call the
     * {@link ContentTypeFieldLayoutAPI#moveFields(ContentType, FieldLayout, User)} method. If the
     * {@code fieldToUpdate}'s sort order needs to be changed to fix a problem in the
     * {@link ContentType}, then this method will replace it with the right value.</p>
     *
     * @param contentType   The {@link ContentType} that the updated field belongs to.
     * @param fieldToUpdate The {@link Field} being updated.
     * @param user          The {@link User} that is updating the Field.
     *
     * @return The {@link FieldLayout} object with the updated Field.
     *
     * @throws DotSecurityException The User does not have the required permissions for execute this
     *                              action.
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws NotFoundException    The field to update was not found in the Content Type or in the
     *                              Field Layout.
     */
    @WrapInTransaction
    @Override
    public FieldLayout updateField(final ContentType contentType, final Field fieldToUpdate, final User user)
            throws DotSecurityException, DotDataException {
        if (null == fieldToUpdate) {
            throw new IllegalArgumentException("Field to update cannot be null");
        }
        if (null == contentType) {
            throw new IllegalArgumentException("Content type cannot be null");
        }
        final Optional<Field> optionalField = checkFieldExists(fieldToUpdate, contentType);

        if (optionalField.isPresent()) {
            final FieldLayout fixedFieldLayout = this.fixLayoutIfNecessary(contentType, user);
            final Optional<Field> fieldFromFixedLayout = fixedFieldLayout.getFields()
                    .stream()
                    .filter(field -> Objects.equals(fieldToUpdate.id(), field.id()))
                    .findFirst();
            if (fieldFromFixedLayout.isPresent()) {
                final Field fieldToUpdateWithSortOrder = setRightSortOrder(fieldFromFixedLayout.get(), fieldToUpdate);
                final Field savedField = fieldAPI.save(fieldToUpdateWithSortOrder, user);
                fieldAPI.save(fieldToUpdate.fieldVariables(), savedField);
                // Re-load the updated field from the database
                final Field updatedWithFieldVars = fieldAPI.find(savedField.id());
                return Objects.nonNull(updatedWithFieldVars)
                        ? fixedFieldLayout.update(list(updatedWithFieldVars))
                        : fixedFieldLayout;
            } else {
                throw new NotFoundException(String.format("Field '%s' was not found in Field Layout", fieldToUpdate.variable()));
            }
        } else {
            throw new NotFoundException(String.format("Field '%s' does not exist", fieldToUpdate.variable()));
        }
    }

    /**
     * Moves fields around inside a Content Type, making sure it ends up with a correct layout after
     * the operation. This method receives a {@link ContentType} with a new {@link FieldLayout}, and
     * updates all the fields' sort order to match it (the new sort order values are calculated
     * automatically).
     * <p>
     * If before the operation the {@link ContentType} already has a wrong layout, it is fixed
     * before the move operation. For instance, if you have a legacy {@link ContentType} without
     * rows and columns, then these are added first.
     *
     * @param contentType    The {@link ContentType} that the {@link FieldLayout} belongs to.
     * @param newFieldLayout The {@link FieldLayout} that must be updated.
     * @param user           The {@link User} performing these changes.
     *
     * @return The updated {@link FieldLayout}.
     *
     * @throws DotSecurityException The specified User doesn't have the required permissions to
     *                              perform this action.
     * @throws DotDataException     An error occurred when interacting with the database.
     */
    @WrapInTransaction
    @Override
    public FieldLayout moveFields(final ContentType contentType, final FieldLayout newFieldLayout, final User user)
            throws DotSecurityException, DotDataException {
        newFieldLayout.validate();
        final FieldLayout fieldLayout = new FieldLayout(contentType);
        this.deleteUnnecessaryLayoutFields(fieldLayout, user);
        final List<Field> fields = addSkipForRelationshipCreation(newFieldLayout.getFields());
        final List<Field> persistedFields = saveFields(contentType.id(), fields, user);
        this.processFieldVariables(newFieldLayout, persistedFields);
        // Re-load the updated fields from the database
        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(user).find(contentType.id());
        return new FieldLayout(contentTypeFromDB);
    }

    /**
     * Save the fields that belong to a specific Content Type, and returns such fields after they've
     * been properly saved and hydrated.
     *
     * @param contentTypeId The ID of the Content type that the fields belong to.
     * @param fields        The list of {@link Field} objects being saved.
     * @param user          The {@link User} performing this action.
     *
     * @return The list of saved fields, including their respective IDs and any other data generated
     * by dotCMS under the covers.
     *
     * @throws DotSecurityException The specified User does not have the required permissions to
     *                              execute this action.
     * @throws DotDataException     An error occurred when interacting with the database.
     */
    private List<Field> saveFields(final String contentTypeId, final List<Field> fields, final User user) throws DotSecurityException, DotDataException {
        fieldAPI.saveFields(fields, user);
        final ContentType contentTypeFromDB = APILocator.getContentTypeAPI(user).find(contentTypeId);
        return contentTypeFromDB.fields();
    }

    /**
     * Processes the Fields that include Field Variables and associate them to either new or
     * existing fields in their respective Content Type. Comparing them to the list of saved fields
     * allows this method to associate potential Field Variables to them, and store them as
     * expected based on their position in the field layout.
     *
     * @param newFieldLayout The {@link FieldLayout} object that specifies how fields are displayed
     *                       in a Content Type.
     * @param savedFields    The list of {@link Field} objects that have been saved to the
     *                       database.
     */
    private void processFieldVariables(final FieldLayout newFieldLayout, final List<Field> savedFields) {
        final List<Field> updatedFields = newFieldLayout.getFields();
        final Map<Integer, List<FieldVariable>> fieldVariablesMap = new HashMap<>();
        for (int i = 0; i < updatedFields.size(); i++) {
            final List<FieldVariable> fieldVariables = updatedFields.get(i).fieldVariables();
            if (UtilMethods.isSet(fieldVariables)) {
                fieldVariablesMap.put(i, fieldVariables);
            }
        }
        fieldVariablesMap.forEach((key, fieldVariables) -> {
            if (!fieldVariables.isEmpty()) {
                fieldAPI.save(fieldVariables, savedFields.get(key));
            }
        });
    }

    /**
     * Return a {@link ContentType}'s layout, if the {@link ContentType} has a wrong layout then it is fix before.
     * Any change make for  the {@link ContentType}'s layout is not saved into data base.
     *
     * @param contentTypeId
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public FieldLayout getLayout(final String contentTypeId, final User user)
            throws DotDataException, DotSecurityException {

        final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeId);
        return new FieldLayout(contentType);
    }

    /**
     * Deleta a set fields making sure that the {@link ContentType} keep with a right layout after the delete.
     *
     * If before the operation the {@link ContentType} already has a wrong layout then it is fix before the delete.
     * If after the delete the {@link ContentType} would has a wrong layout then a
     * {@link com.dotcms.contenttype.model.field.layout.FieldLayoutValidationException} is thrown, for example if
     * we have this layout: ROW_FIELD - COLUMN_FIELD - TEXT_FIELD. if ypu try to remove the ROW_FIELD the the
     * exception would be thrown.
     *
     * @param contentType field's {@link ContentType}
     * @param fieldsID Fields'id to delete
     * @param user who is making the change
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @WrapInTransaction
    @Override
    public DeleteFieldResult deleteField(final ContentType contentType, final List<String> fieldsID, final User user)
            throws DotSecurityException, DotDataException {
        final FieldLayout fieldLayout = this.fixLayoutIfNecessary(contentType, user);
        final FieldLayout fieldLayoutUpdated = fieldLayout.remove(fieldsID);
        fieldLayoutUpdated.validate();

        final Collection<String> deletedIds = fieldAPI.deleteFields(fieldsID, user);

        return new DeleteFieldResult(fieldLayoutUpdated, deletedIds);
    }

    @Override
    public FieldLayout fixLayoutIfNecessary(final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException {

        final FieldLayout fieldLayout = new FieldLayout(contentType);

        if (!fieldLayout.isValidate()) {
            return this.fixLayout(fieldLayout, user);
        } else {
            return fieldLayout;
        }
    }

    @Override
    public FieldLayout fixLayout(final FieldLayout fieldLayout, final User user)
            throws DotDataException, DotSecurityException {

        this.internalFixLayout(fieldLayout, user);
        return fieldLayout.getLayoutFixed();
    }

    private Optional<Field> checkFieldExists(final Field fieldToUpdate, final ContentType contentType) {
        return contentType.fields()
                .stream()
                .filter(field -> field.id().equals(fieldToUpdate.id()))
                .findFirst();
    }

    /**
     * If the current field layout of a Content Type is not valid, this method takes care of updating the required
     * values and structures to fix that. For more information about valid layouts, please see the Javadoc for the
     * {@link FieldLayout} class.
     *
     * @param fieldLayout The {@link FieldLayout} for a given Content Type.
     * @param user        The {@link User} performing this action.
     *
     * @throws DotSecurityException The specified user doesn't have the required permissions to perform this action.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    private void internalFixLayout(final FieldLayout fieldLayout, final User user)
            throws DotSecurityException, DotDataException {
        deleteUnnecessaryLayoutFields(fieldLayout, user);
        final List<Field> fields = addSkipForRelationshipCreation(fieldLayout.getLayoutFieldsToCreateOrUpdate());
        fieldAPI.saveFields(fields, user);
    }

    /**
     * Finds any {@link RelationshipField} objects in the list of {@link Field} objects and performs
     * the following check on each of them:
     * <ol>
     *     <li>If the ID <b>is NOT set</b>, it means that it's a new Field, and the Relationship
     *     needs to be created.</li>
     *     <li>If the ID <b>IS set</b>, we change its setting to skip the creation of the
     *     Relationship, which is not necessary at all when fixing/adjusting the field layout.</li>
     * </ol>
     * Such an operation is executed when fields are moved or deleted from a Content Type, where
     * creating/re-creating a relationship is NOT necessary at all.
     *
     * @param layoutFields The list of Fields that will be updated.
     *
     * @return The updated list of Fields, with the appropriate setting for Relationship fields.
     */
    private List<Field> addSkipForRelationshipCreation(final List<Field> layoutFields) {
        return layoutFields.stream()
                .map(field -> {
                    if (field instanceof RelationshipField &&
                    UtilMethods.isSet(field.id())) {
                        return RelationshipFieldBuilder.builder(field).skipRelationshipCreation(true).build();
                    }
                    return field;
                }).collect(Collectors.toList());
    }

    /**
     * Based on the field layout validation process, the unnecessary fields in a Content Type are deleted altogether, if
     * any.
     *
     * @param fieldLayout The {@link FieldLayout} containing potential fields that need to be deleted.
     * @param user        The {@link User} performing this action.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified user doesn't have the required permissions to perform this action.
     */
    private void deleteUnnecessaryLayoutFields(final FieldLayout fieldLayout, final User user)
            throws DotDataException, DotSecurityException {

        fieldAPI.deleteFields(
                fieldLayout.getLayoutFieldsToDelete().stream().map(field -> field.id()).collect(Collectors.toList()),
                user
        );
    }

    private Field setRightSortOrder(
            final Field fieldFromDB,
            final Field fieldToUpdate
    ) {

        if (Field.SORT_ORDER_DEFAULT_VALUE == fieldToUpdate.sortOrder() || fieldToUpdate.sortOrder() != fieldFromDB.sortOrder()) {
            return FieldUtil.copyField(fieldToUpdate, fieldFromDB.sortOrder());
        } else {
            return fieldToUpdate;
        }

    }
}
