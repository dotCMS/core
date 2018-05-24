package com.dotcms.rest;

import com.dotcms.util.pagination.Pagination;
import com.liferay.util.StringPool;
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

    private static final String EMPTY_ENTITY = StringPool.BLANK;

    private final List<ErrorEntity> errors;
    private final Object entity;
    private final List<MessageEntity> messages;
    private final Map<String, String> i18nMessagesMap;
    private final List<String> permissions;
    private final Pagination pagination;

     private ResponseEntityView(
             final List<ErrorEntity> errors,
             final Object entity,
             final List<MessageEntity> messages,
             final Map<String, String> i18nMessagesMap,
             final List<String> permissions,
             final Pagination pagination) {
        this.errors = errors;
        this.entity = entity;
        this.messages = messages;
        this.i18nMessagesMap = i18nMessagesMap;
        this.permissions = permissions;
        this.pagination = pagination;
    }

    private ResponseEntityView(final Builder builder) {
         this(builder.errors, builder.entity, builder.messages, builder.i18nMessagesMap, builder.permissions, builder.pagination);
    }

    public ResponseEntityView(final List<ErrorEntity> errors) {
        this(errors, EMPTY_ENTITY, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), null);
    }

    public ResponseEntityView(final Object entity) {
        this(Collections.emptyList(), entity, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), null);
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

    public Pagination getPagination() {
        return pagination;
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


    public static final class Builder {

        private Object entity = EMPTY_ENTITY;
        private List<ErrorEntity> errors = Collections.emptyList();
        private List<MessageEntity> messages = Collections.emptyList();
        private Map<String, String> i18nMessagesMap = Collections.emptyMap();
        private List<String> permissions = Collections.emptyList();
        private Pagination pagination = null;

        public Builder entity(final Object entity){
            this.entity = entity;
            return this;
        }

        public Builder errors(final List<ErrorEntity> errors){
            this.errors = errors;
            return this;
        }

        public Builder messages(final List<MessageEntity> messages){
            this.messages = messages;
            return this;
        }

        public Builder i18nMessagesMap(final Map<String, String> i18nMessagesMap){
            this.i18nMessagesMap = i18nMessagesMap;
            return this;
        }

        public Builder permissions(final List<String> permissions){
            this.permissions = permissions;
            return this;
        }

        public Builder permissions(final String... permissions){
            this.permissions = Arrays.asList(permissions);
            return this;
        }

        public Builder pagination(final Pagination pagination){
            this.pagination = pagination;
            return this;
        }

        public ResponseEntityView build(){
            return new ResponseEntityView(this);
        }

    }


} // E:O:F:ResponseEntityView.
