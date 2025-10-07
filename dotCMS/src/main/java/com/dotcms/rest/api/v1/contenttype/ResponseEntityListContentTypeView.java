package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.Pagination;
import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<ContentType> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListContentTypeView extends ResponseEntityView<List<ContentType>> {

    public ResponseEntityListContentTypeView(List<ContentType> entity) {
        super(entity);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListContentTypeView(List<ContentType> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}