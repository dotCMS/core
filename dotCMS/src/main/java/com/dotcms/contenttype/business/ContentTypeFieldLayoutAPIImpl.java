package com.dotcms.contenttype.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * Update a field.
     *
     * If before the update the {@link ContentType} already has a wrong layout then it is fix before the update.
     *
     * The fieldToUpdate's sortOrder attribute will be ignore, if you want to change any field's sortOrder then use
     * {@link this#moveFields(ContentType, FieldLayout, User)} method.
     * If the fieldToUpdate's sortOtder need to be changed to fix the {@link ContentType},
     * then it would be change for the right value by this method.
     *
     * @param contentType field's {@link ContentType}
     * @param fieldToUpdate field to update
     * @param user who is making the change
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @WrapInTransaction
    @Override
    public FieldLayout updateField(final ContentType contentType, final Field fieldToUpdate, final User user)
            throws DotSecurityException, DotDataException {

        final Optional<Field> optionalField = checkFieldExists(fieldToUpdate, contentType);

        if (optionalField.isPresent()) {
            final FieldLayout fieldLayout = this.fixLayoutIfNecessary(contentType, user);
            final Optional<Field> fielFromFixLayout = fieldLayout.getFields()
                    .stream()
                    .filter(field -> fieldToUpdate.id().equals(field.id()))
                    .findFirst();

            final Field fieldToUpdateWithSortOrder = setRightSortOrder(fielFromFixLayout.get(), fieldToUpdate);

            fieldAPI.save(fieldToUpdateWithSortOrder, user);

            return fieldLayout.update(list(fieldToUpdateWithSortOrder));
        } else {
            throw new NotFoundException(String.format("Field %s does not exists", fieldToUpdate.variable()));
        }
    }

    /**
     * Move fields into the content types making sure that the {@link ContentType} keep with a right layout after the operation.
     * This method receive a {@link ContentType}'s new {@link FieldLayout} and update all the fields's sort order to match
     * this new layout (the new sort order values are calcalutes automatically).
     *
     * If before the operation the {@link ContentType} already has a wrong layout then it is fix before the operation.
     * For example if you have a legacy {@link ContentType} without rows and columns the these are add.
     *
     * @param contentType field's {@link ContentType}
     * @param newFieldLayout
     * @param user who is making the change
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @WrapInTransaction
    @Override
    public FieldLayout moveFields(final ContentType contentType, final FieldLayout newFieldLayout, final User user)
            throws DotSecurityException, DotDataException {
        newFieldLayout.validate();

        final FieldLayout fieldLayout = new FieldLayout(contentType);
        this.deleteUnecessaryLayoutFields(fieldLayout, user);
        final List<Field> fields = addSkipForRelationshipCreation(newFieldLayout.getFields());
        fieldAPI.saveFields(fields, user);

        final ContentType contentTypeFfromDB = APILocator.getContentTypeAPI(user).find(contentType.id());
        return new FieldLayout(contentTypeFfromDB);
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
        deleteUnecessaryLayoutFields(fieldLayout, user);
        final List<Field> fields = addSkipForRelationshipCreation(fieldLayout.getLayoutFieldsToCreateOrUpdate());
        fieldAPI.saveFields(fields, user);
    }

    /**
     * Finds any Relationship field in the list of {@link Field} objects and checks of the id is set, if is not set
     * means that is a new Field so the Relationship needs to be created, if is set changes its setting to skip the creation
     * of the Relationship, which is not necessary at all when fixing/adjusting the field layout. Such an operation is
     * executed when fields are moved or deleted from a Content Type, where creating/re-creating a relationship is NOT
     * necessary at all.
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
    private void deleteUnecessaryLayoutFields(final FieldLayout fieldLayout, final User user)
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
