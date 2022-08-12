package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

/**
 * Contains all the configuration parameters that make up a Push Publishing Filter via the REST API.
 * <p>You may create Push Publishing filters to control which content is pushed from your sending server to your
 * receiving server. The filters allow you to have fine-grained control over what content does and does not get pushed,
 * whether intentionally (when specifically selected) or by dependency.</p>
 * <p>
 * You may create as many filters as you wish. You can specify permissions for the filters, allowing you to control
 * what content and objects different users and Roles may push. For example, you can allow users with a specific Role
 * to only push content of a specific Content Type, or only push content in a specific location.</p>
 *
 * @author Jonathan Sanchez
 * @since May 9th, 2022
 */
@JsonDeserialize(builder = FilterDescriptorForm.Builder.class)
public class FilterDescriptorForm  extends Validated {

    private final String key;
    private final String title;
    private final String sort;
    private final boolean defaultFilter;
    private final String roles;
    private final Map<String,Object> filters;

    private FilterDescriptorForm(final FilterDescriptorForm.Builder builder) {
        this.key           = builder.key;
        this.title         = builder.title;
        this.sort          = builder.sort;
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

    /**
     * Returns the order in which this Filter Descriptor will be returned -- usually for UI purposes. If no value is
     * set, then the value specified by {@link com.dotcms.publishing.FilterDescriptor#DEFAULT_SORT_VALUE} will be
     * used instead.
     *
     * @return The sort value for the Filter Descriptor.
     */
    public String getSort() {
        return this.sort;
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
                ", sort='" + sort + '\'' +
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
        @JsonProperty()
        private String sort;
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

        /**
         * Sets the order in which this Filter Descriptor will be returned -- usually for UI purposes. If no value is
         * set, then the value specified by {@link com.dotcms.publishing.FilterDescriptor#DEFAULT_SORT_VALUE} will be
         * used instead.
         *
         * @param sort The sort value for the Filter Descriptor.
         *
         * @return The current Builder object.
         */
        public Builder sort(final String sort) {
            this.sort = sort;
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
