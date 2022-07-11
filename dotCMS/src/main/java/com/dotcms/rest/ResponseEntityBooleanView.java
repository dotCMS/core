package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Response entity view to wrap a boolean callback
 * @author jsanca
 */
public class ResponseEntityBooleanView extends ResponseEntityView<Boolean> {
    public ResponseEntityBooleanView(List<ErrorEntity> errors) {
        super(errors);
    }

    public ResponseEntityBooleanView(List<ErrorEntity> errors, Map<String, String> i18nMessagesMap) {
        super(errors, i18nMessagesMap);
    }

    public ResponseEntityBooleanView(List<ErrorEntity> errors, Boolean entity) {
        super(errors, entity);
    }

    public ResponseEntityBooleanView(List<ErrorEntity> errors, Boolean entity, Map<String, String> i18nMessagesMap) {
        super(errors, entity, i18nMessagesMap);
    }

    public ResponseEntityBooleanView(Boolean entity) {
        super(entity);
    }

    public ResponseEntityBooleanView(Boolean entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityBooleanView(Boolean entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityBooleanView(Boolean entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityBooleanView(Boolean entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityBooleanView(Boolean entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityBooleanView(Boolean entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityBooleanView(Boolean entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }
}
