package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;

public interface ContentTypeFieldLayoutAPI {

    FieldLayout updateField(ContentType contentType, Field fieldToUpdate, User user)
            throws DotSecurityException, DotDataException;

    FieldLayout moveFields(ContentType contentType, FieldLayout newFieldLayout, User user)
            throws DotSecurityException, DotDataException;

    FieldLayout getLayout(String contentTypeId, User user)  throws DotDataException, DotSecurityException;

    DeleteFieldResult deleteField(ContentType contentType, List<String> fieldsID, User user)
            throws DotSecurityException, DotDataException;

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
