package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * This class help to encapsulate a list of something as a rest response
 * @author jsanca
 */
public class ResponseEntityListView<T> extends ResponseEntityView<List<T>> {

    public ResponseEntityListView(List<T> entity) {
        super(entity);
    }

    public ResponseEntityListView(List<T> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListView(List<T> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListView(List<T> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListView(List<T> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListView(List<T> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListView(List<T> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListView(List<T> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListView(List<T> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListView(List<T> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}
