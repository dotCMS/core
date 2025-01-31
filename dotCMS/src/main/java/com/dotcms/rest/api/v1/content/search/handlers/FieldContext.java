package com.dotcms.rest.api.v1.content.search.handlers;

import com.dotcms.contenttype.model.type.ContentType;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

import static com.liferay.util.StringPool.BLANK;

/**
 * Provides all the context that a given field may need in order to generate the correct Lucene
 * query. Some fields might only need their Lucene search term -- i.e., the Content Type var name
 * with the field's var name and its value -- while others might need to access other APIs, user
 * permission validations, pagination information, Content Type-specific data, or any other piece
 * of information that an existing or new searchable field may require.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class FieldContext {

    private final ContentType contentType;
    private final User user;
    private final String fieldName;
    private final Object fieldValue;
    private final int page;
    private final int offset;
    private final String sortBy;
    private final Map<String, Object> extraParams;

    public FieldContext(final Builder builder) {
        this.contentType = builder.contentType;
        this.user = builder.user;
        this.fieldName = builder.fieldName;
        this.fieldValue = builder.fieldValue;
        this.page = builder.page;
        this.offset = builder.offset;
        this.sortBy = builder.sortBy;
        this.extraParams = builder.extraParams;
    }

    public ContentType contentType() {
        return contentType;
    }

    public User user() {
        return user;
    }

    public String fieldName() {
        return fieldName;
    }

    public Object fieldValue() {
        return fieldValue;
    }

    public int page() {
        return page;
    }

    public int offset() {
        return offset;
    }

    public String sortBy() {
        return sortBy;
    }

    public Map<String, Object> extraParams() {
        return extraParams;
    }

    public static class Builder {

        private ContentType contentType;
        private User user;
        private String fieldName;
        private Object fieldValue;
        private int page = 0;
        private int offset = 0;
        private String sortBy = BLANK;
        private Map<String, Object> extraParams = new HashMap<>();

        public Builder() {
        }

        public Builder withContentType(final ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withUser(final User user) {
            this.user = user;
            return this;
        }

        public Builder withFieldName(final String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder withFieldValue(final Object fieldValue) {
            this.fieldValue = fieldValue;
            return this;
        }

        public Builder withPage(final int page) {
            this.page = page;
            return this;
        }

        public Builder withOffset(final int offset) {
            this.offset = offset;
            return this;
        }

        public Builder withSortBy(final String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder withExtraParams(final Map<String, Object> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        public FieldContext build() {
            return new FieldContext(this);
        }

    }

}
