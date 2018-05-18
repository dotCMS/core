package com.dotcms.rest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Response Entity View encapsulates the response to include errors and the entity to be returned as part of the Jersey response
 * @author jsanca
 */
public class ResponseEntityView implements Serializable {

    public static final String OK = "Ok";

    private static final String EMPTY_ENTITY = "";

    private final List<ErrorEntity> errors;
    private final Object entity;
    private final List<MessageEntity> messages;
    private final Map<String, String> i18nMessagesMap;
    private final List<String> permissions;


    public ResponseEntityView(final List<ErrorEntity> errors) {

        this.errors          = errors;
        this.messages        = Collections.EMPTY_LIST;
        this.entity          = EMPTY_ENTITY;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final Map<String, String> i18nMessagesMap) {

        this.errors = errors;
        this.messages = Collections.EMPTY_LIST;
        this.entity = EMPTY_ENTITY;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final Object entity) {

        this.errors = errors;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final Object entity, final Map<String, String> i18nMessagesMap) {

        this.errors = errors;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity) {

        this.errors = Collections.EMPTY_LIST;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity, final String... permissions) {

        this.errors          = Collections.EMPTY_LIST;
        this.messages        = Collections.EMPTY_LIST;
        this.entity          = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Arrays.asList(permissions);
    }

    public ResponseEntityView(final Object entity, final Map<String, String> i18nMessagesMap) {

        this.errors = Collections.EMPTY_LIST;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity,
                              final List<MessageEntity> messages) {

        this.errors = Collections.EMPTY_LIST;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity,
                              final List<MessageEntity> messages,
                              final Map<String, String> i18nMessagesMap) {

        this.errors = Collections.EMPTY_LIST;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages) {

        this.errors = errors;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages, final Map<String, String> i18nMessagesMap) {

        this.errors = errors;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
    }

    public ResponseEntityView(final Object entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages, final Map<String, String> i18nMessagesMap, final List<String> permissions) {

        this.errors          = errors;
        this.messages        = messages;
        this.entity          = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = permissions;
    }

    public List<ErrorEntity> getErrors() {
        return errors;
    }

    public Object getEntity() {
        return entity;
    }

    public List<MessageEntity> getMessages() {
        return messages;
    }

    public Map<String, String> getI18nMessagesMap() {
        return i18nMessagesMap;
    }


    public List<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return "ResponseEntityView{" +
                "errors="     + errors +
                ", entity="   + entity +
                ", messages=" + messages +
                ", i18nMessagesMap=" + i18nMessagesMap +
                '}';
    }
} // E:O:F:ResponseEntityView.
