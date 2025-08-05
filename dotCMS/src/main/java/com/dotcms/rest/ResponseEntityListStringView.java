package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<String> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListStringView extends ResponseEntityView<List<String>> {

    public ResponseEntityListStringView(List<String> entity) {
        super(entity);
    }

    public ResponseEntityListStringView(List<String> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListStringView(List<String> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListStringView(List<String> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListStringView(List<String> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListStringView(List<String> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListStringView(List<String> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListStringView(List<String> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListStringView(List<String> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListStringView(List<String> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}