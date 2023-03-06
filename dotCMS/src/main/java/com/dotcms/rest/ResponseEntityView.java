package com.dotcms.rest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected entity and related
 * information such as pagination parameters, errors, i18n messages, etc. for them to be returned as part of the Jersey
 * response
 *
 * @author jsanca
 * @since Jul 7th, 2016
 */
public class ResponseEntityView <T> implements EntityView<T>, Serializable {

    public static final String OK = "Ok";

    private static final Object EMPTY_ENTITY = "";

    private final List<ErrorEntity> errors;
    private final T entity;
    private final List<MessageEntity> messages;
    private final Map<String, String> i18nMessagesMap;
    private final List<String> permissions;
    private final Pagination pagination;

    public ResponseEntityView(final List<ErrorEntity> errors) {
        this.errors          = errors;
        this.messages        = Collections.EMPTY_LIST;
        this.entity          = (T)EMPTY_ENTITY;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final Map<String, String> i18nMessagesMap) {
        this.errors          = errors;
        this.messages        = Collections.EMPTY_LIST;
        this.entity          = (T)EMPTY_ENTITY;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final T entity) {
        this.errors = errors;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final List<ErrorEntity> errors, final T entity, final Map<String, String> i18nMessagesMap) {
        this.errors = errors;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity) {
        this.errors = Collections.EMPTY_LIST;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final Pagination pagination) {
        this.errors = Collections.EMPTY_LIST;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = pagination;
    }

    public ResponseEntityView(final T entity, final String... permissions) {
        this.errors          = Collections.EMPTY_LIST;
        this.messages        = Collections.EMPTY_LIST;
        this.entity          = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Arrays.asList(permissions);
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final Map<String, String> i18nMessagesMap) {
        this.errors = Collections.EMPTY_LIST;
        this.messages = Collections.EMPTY_LIST;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity,
                              final List<MessageEntity> messages) {
        this.errors = Collections.EMPTY_LIST;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity,
                              final List<MessageEntity> messages,
                              final Map<String, String> i18nMessagesMap) {
        this.errors = Collections.EMPTY_LIST;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages) {
        this.errors = errors;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = Collections.EMPTY_MAP;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages, final Map<String, String> i18nMessagesMap) {
        this.errors = errors;
        this.messages = messages;
        this.entity = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = Collections.EMPTY_LIST;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final List<ErrorEntity> errors,
                              final List<MessageEntity> messages, final Map<String, String> i18nMessagesMap, final List<String> permissions) {
        this.errors          = errors;
        this.messages        = messages;
        this.entity          = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = permissions;
        this.pagination = null;
    }

    public ResponseEntityView(final T entity, final List<ErrorEntity> errors, final List<MessageEntity> messages, final Map<String, String> i18nMessagesMap, final List<String> permissions, final Pagination pagination) {
        this.errors          = errors;
        this.messages        = messages;
        this.entity          = entity;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions     = permissions;
        this.pagination = pagination;
    }

    public List<ErrorEntity> getErrors() {
        return errors;
    }

    public T getEntity() {
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

    public Pagination getPagination() {
        return this.pagination;
    }

    @Override
    public String toString() {
        return "ResponseEntityView{" +
                "errors="     + errors +
                ", entity="   + entity +
                ", messages=" + messages +
                ", i18nMessagesMap=" + i18nMessagesMap +
                ", pagination=" + this.pagination +
                '}';
    }

}
