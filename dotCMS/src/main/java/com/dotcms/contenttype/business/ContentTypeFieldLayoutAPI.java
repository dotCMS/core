package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.List;

/**
 * Create, delete, Update and Move Field making sure that the {@link ContentType} keep with a right layout after the operation
 *
 * @see FieldLayout
 */
public interface ContentTypeFieldLayoutAPI {

    /**
     * Update a field making sure that the {@link ContentType} keep with a right layout after the operation
     *
     * @param contentType field's {@link ContentType}
     * @param fieldToUpdate field to update
     * @param user who is making the change
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    FieldLayout updateField(final ContentType contentType, final Field fieldToUpdate, final User user)
            throws DotSecurityException, DotDataException;

    /**
     * Move fields, receive a {@link FieldLayout} and update the sortOrder of each {@link Field} to match the index into
     * the FieldLayout.
     *
     * @param contentType field's {@link ContentType}
     * @param newFieldLayout
     * @param user who is making the change
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    FieldLayout moveFields(final ContentType contentType, final FieldLayout newFieldLayout, final User user)
            throws DotSecurityException, DotDataException;

    /**
     * Return the {@link ContentType}'s layout
     *
     * @param contentTypeId
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    FieldLayout getLayout(final String contentTypeId, final User user)  throws DotDataException, DotSecurityException;

    /**
     * Delete a field making sure that the {@link ContentType} keep with a right layout after the operation
     *
     * @param contentType field's {@link ContentType}
     * @param fieldsID Fields'id to delete
     * @param user who is making the change
     * @return a DeleteFieldResult with the new {@link ContentType}'s layout and the fields id deleted
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    DeleteFieldResult deleteField(final ContentType contentType, final List<String> fieldsID, final User user)
            throws DotSecurityException, DotDataException;

    /**
     * Analyzes the field layout for the field that is being updated or deleted in order to check if
     * it also needs to be fixed. Layout inconsistencies include, for example: Having more than the
     * allowed number of columns, invalid or missing Row or Column fields, among others.
     *
     * @param contentType The {@link ContentType} whose field layout will be validated.
     * @param user        The {@link User} performing this action.
     * @return The fixed {@link FieldLayout} if necessary, or the original layout if no changes were
     * needed.
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified user doesn't have the required permissions to
     *                              perform this action.
     */
    FieldLayout fixLayoutIfNecessary(ContentType contentType, User user)
            throws DotDataException, DotSecurityException;

    /**
     * If the current field layout of a Content Type is not valid, this method takes care of
     * updating the required values and structures to fix that. For more information about valid
     * layouts, please see the Javadoc for the {@link FieldLayout} class.
     *
     * @param fieldLayout The {@link FieldLayout} for a given Content Type.
     * @param user        The {@link User} performing this action.
     * @return The fixed {@link FieldLayout}.
     * @throws DotSecurityException The specified user doesn't have the required permissions to
     *                              perform this action.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    FieldLayout fixLayout(final FieldLayout fieldLayout, final User user)
            throws DotDataException, DotSecurityException;

    class DeleteFieldResult {
        private FieldLayout layout;
        private Collection<String> fieldDeletedIds;

        public DeleteFieldResult(final FieldLayout layout, final Collection<String> fieldDeletedIds) {
            this.layout = layout;
            this.fieldDeletedIds = fieldDeletedIds;
        }

        public FieldLayout getLayout() {
            return layout;
        }

        public Collection<String> getFieldDeletedIds() {
            return fieldDeletedIds;
        }
    }
}
