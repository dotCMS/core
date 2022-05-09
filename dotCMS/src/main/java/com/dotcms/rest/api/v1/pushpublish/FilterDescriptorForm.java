package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(builder = FilterDescriptorForm.Builder.class)
public class FilterDescriptorForm  extends Validated {

    private final String key;
    private final String title;
    private final boolean defaultFilter;
    private final String roles;
    private final Map<String,Object> filters;

    private FilterDescriptorForm(final FilterDescriptorForm.Builder builder) {

        this.key           = builder.key;
        this.title         = builder.title;
        this.defaultFilter = builder.defaultFilter;
        this.roles         = builder.roles;
        this.filters       = builder.filters;
        checkValid();
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDefaultFilter() {
        return defaultFilter;
    }

    public String getRoles() {
        return roles;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        return "FilterDescriptorForm{" +
                "key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", defaultFilter=" + defaultFilter +
                ", roles='" + roles + '\'' +
                ", filters=" + filters +
                '}';
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String key;
        @JsonProperty(required = true)
        private String title;
        @JsonProperty
        private boolean defaultFilter;
        @JsonProperty(required = true)
        private String roles;
        @JsonProperty(required = true)
        private  Map<String,Object> filters;

        public Builder key(final String key) {
            this.key = key;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder defaultFilter(final boolean defaultFilter) {
            this.defaultFilter = defaultFilter;
            return this;
        }

        public Builder roles(final String roles) {
            this.roles = roles;
            return this;
        }

        public Builder filters(final Map<String,Object> filters) {
            this.filters = filters;
            return this;
        }

        public FilterDescriptorForm build() {
            return new FilterDescriptorForm(this);
        }
    }

}
