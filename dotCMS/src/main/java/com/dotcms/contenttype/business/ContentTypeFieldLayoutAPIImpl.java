package com.dotcms.contenttype.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.field.layout.FieldUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;


public class ContentTypeFieldLayoutAPIImpl implements ContentTypeFieldLayoutAPI {

    private final FieldAPI fieldAPI;

    public ContentTypeFieldLayoutAPIImpl() {
        this.fieldAPI = APILocator.getContentTypeFieldAPI();
    }

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

    @WrapInTransaction
    @Override
    public FieldLayout moveFields(final ContentType contentType, final FieldLayout newFieldLayout, final User user)
            throws DotSecurityException, DotDataException {
        newFieldLayout.validate();

        final FieldLayout fieldLayout = new FieldLayout(contentType);
        this.deleteUnecessaryLayoutFields(fieldLayout, user);

        fieldAPI.saveFields(newFieldLayout.getFields(), user);

        return newFieldLayout;
    }

    @Override
    public FieldLayout getLayout(final String contentTypeId, final User user)
            throws DotDataException, DotSecurityException {

        final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeId);
        return new FieldLayout(contentType);
    }

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

    private FieldLayout fixLayoutIfNecessary(final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException {

        final FieldLayout fieldLayout = new FieldLayout(contentType);

        if (!fieldLayout.isValidate()) {
            this.fixLayout(fieldLayout, user);
            return fieldLayout.getLayoutFixed();
        } else {
            return fieldLayout;
        }
    }

    private Optional<Field> checkFieldExists(final Field fieldToUpdate, final ContentType contentType) {
        return contentType.fields()
                .stream()
                .filter(field -> field.id().equals(fieldToUpdate.id()))
                .findFirst();
    }

    private void fixLayout(final FieldLayout fieldLayout, final User user)
            throws DotSecurityException, DotDataException {

        deleteUnecessaryLayoutFields(fieldLayout, user);

        fieldAPI.saveFields(fieldLayout.getLayoutFieldsToCreateOrUpdate(), user);
    }

    private void deleteUnecessaryLayoutFields(FieldLayout fieldLayout, User user) throws DotDataException, DotSecurityException {
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
