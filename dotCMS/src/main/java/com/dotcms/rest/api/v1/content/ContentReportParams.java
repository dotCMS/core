package com.dotcms.rest.api.v1.content;

import com.dotcms.util.pagination.OrderDirection;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the parameters that may be used to generate a Content Report for a given
 * dotCMS object. This class exposes the most common filtering criteria, and allows you to extend
 * them via the {@link #extraParams()} method.
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public class ContentReportParams implements Serializable {

    private final User user;
    private final String filter;
    private final int page;
    private final int perPage;
    private final String orderBy;
    private final OrderDirection orderDirection;
    private final Map<String, Object> extraParams;

    /**
     * Private constructor for creating an instance of the {@link ContentReportParams} object.
     *
     * @param builder The Builder instance.
     */
    private ContentReportParams(final Builder builder) {
        this.user = builder.user;
        this.filter = builder.filter;
        this.page = builder.page;
        this.perPage = builder.perPage;
        this.orderBy = builder.orderBy;
        this.orderDirection = builder.orderDirection;
        this.extraParams = builder.extraParams;
    }

    /**
     * Returns the User that is generating the Content Report.
     *
     * @return The {@link User} generating the report.
     */
    public User user() {
        return this.user;
    }

    /**
     * Returns the filter to be used when generating the Content Report. This may be used in SQL
     * queries
     *
     * @return The filter to be used when generating the Content Report.
     */
    public String filter() {
        return this.filter;
    }

    /**
     * Returns the page number of the result set, for pagination purposes.
     *
     * @return The page number.
     */
    public int page() {
        return this.page;
    }

    /**
     * Returns the maximum number of returned items in the result set, for pagination purposes.
     *
     * @return The maximum number of returned items.
     */
    public int perPage() {
        return this.perPage;
    }

    /**
     * Returns the order-by clause used to sort the results.
     *
     * @return The order-by clause.
     */
    public String orderBy() {
        return this.orderBy;
    }

    /**
     * Returns the order direction used to sort the results.
     *
     * @return The order direction.
     */
    public OrderDirection orderDirection() {
        return this.orderDirection;
    }

    /**
     * Returns any extra parameters that cannot be set via the exposed attributes, but may be
     * necessary when generating the Content Report. Any additional filters or metadata can be
     * specified via this Map.
     *
     * @return A map of extra parameters.
     */
    public Map<String, Object> extraParams() {
        return this.extraParams;
    }

    /**
     * Returns the value of the extra parameter with the specified key.
     *
     * @param key The key of the extra parameter.
     *
     * @return The value of the extra parameter, or {@code null} if it doesn't exist.
     */
    public String extraParam(final String key) {
        return extraParam(key, null);
    }

    /**
     * Returns the value of the extra parameter with the specified key, or the specified default
     * value.
     *
     * @param key          The key of the extra parameter.
     * @param defaultValue The default value to return if the extra parameter doesn't exist.
     *
     * @return The value of the extra parameter, or the specified default value if it doesn't exist.
     */
    public String extraParam(final String key, final String defaultValue) {
        if (this.extraParams.containsKey(key)) {
            return this.extraParams.get(key).toString().trim();
        }
        return defaultValue;
    }

    /**
     * Allows you to create an instance of the {@link ContentReportParams} class.
     */
    public static final class Builder {

        User user;
        String filter;
        int page;
        int perPage;
        String orderBy;
        OrderDirection orderDirection;
        Map<String, Object> extraParams;

        /**
         * Allows you to create an instance of the {@link ContentReportParams} class.
         */
        public Builder() {
            // Default Builder constructor
        }

        /**
         * Sets the User that is generating the Content Report.
         *
         * @param user The {@link User} generating the report.
         *
         * @return The {@link Builder} instance.
         */
        public Builder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the filter to be used when generating the Content Report. This may be used in SQL
         * queries
         *
         * @param filter The filter to be used when generating the Content Report.
         *
         * @return The {@link Builder} instance.
         */
        public Builder filter(final String filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets the page number of the result set, for pagination purposes.
         *
         * @param page The page number.
         *
         * @return The {@link Builder} instance.
         */
        public Builder page(final int page) {
            this.page = page;
            return this;
        }

        /**
         * Sets the maximum number of returned items in the result set, for pagination purposes.
         *
         * @param perPage The maximum number of returned items.
         *
         * @return The {@link Builder} instance.
         */
        public Builder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        /**
         * Sets the order-by clause used to sort the results.
         *
         * @param orderBy The order-by clause.
         *
         * @return The {@link Builder} instance.
         */
        public Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /**
         * Sets the order direction used to sort the results.
         *
         * @param orderDirection The order direction.
         *
         * @return The {@link Builder} instance.
         */
        public Builder orderDirection(final OrderDirection orderDirection) {
            this.orderDirection = orderDirection;
            return this;
        }

        /**
         * Sets any extra parameters that cannot be set via the exposed attributes, but may be
         * necessary when generating the Content Report. Any additional filters or metadata can be
         * specified via this Map.
         *
         * @param extraParams A map of extra parameters.
         *
         * @return The {@link Builder} instance.
         */
        public Builder extraParams(final Map<String, Object> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        /**
         * Builds an instance of the {@link ContentReportParams} class.
         *
         * @return An instance of the {@link ContentReportParams} class
         */
        public ContentReportParams build() {
            this.extraParams = null != this.extraParams ? this.extraParams : new HashMap<>();
            return new ContentReportParams(this);
        }

    }

}
