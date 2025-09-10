package com.dotcms.util;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.liferay.util.StringPool.BLANK;

/**
 * This class allows developers to easily specify the different filtering and pagination parameters
 * to Paginators.
 *
 * @author Jose Castro
 * @since Aug 1st, 2025
 */
public class PaginationUtilParams<T, R> {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final User user;
    private final String filter;
    private final int page;
    private final int perPage;
    private final String orderBy;
    private final OrderDirection direction;
    private final Map<String, Object> extraParams;
    private final Function<PaginatedArrayList<T>, R> function;

    private PaginationUtilParams(final Builder<T, R> builder) {
        this.request = builder.request;
        this.response = builder.response;
        this.user = builder.user;
        this.filter = builder.filter;
        this.page = builder.page;
        this.perPage = builder.perPage;
        this.orderBy = builder.orderBy;
        this.direction = builder.direction;
        this.extraParams = builder.extraParams;
        this.function = builder.function;
    }

    /**
     * Returns the current instance of the {@link HttpServletRequest}.
     *
     * @return The current instance of the {@link HttpServletRequest}.
     */
    public HttpServletRequest request() {
        return request;
    }

    /**
     * Returns the current instance of the {@link HttpServletResponse}.
     *
     * @return The current instance of the {@link HttpServletResponse}.
     */
    public HttpServletResponse response() {
        return response;
    }

    /**
     * Returns the {@link User} that is calling the Paginator.
     *
     * @return The {@link User} that is calling the Paginator.
     */
    public User user() {
        return user;
    }

    /**
     * Returns the optional filtering parameter. Every paginator can use or implement this parameter
     * as required.
     *
     * @return The optional filtering parameter.
     */
    public String filter() {
        return filter;
    }

    /**
     * Returns the result offset value, for pagination purposes.
     *
     * @return The offset.
     */
    public int page() {
        return page;
    }

    /**
     * Returns the number of items per page, for pagination purposes.
     *
     * @return The number of items per page.
     */
    public int perPage() {
        return perPage;
    }

    /**
     * Returns the criterion used to sort the returned values.
     *
     * @return The criterion used to sort the returned values.
     */
    public String orderBy() {
        return orderBy;
    }

    /**
     * Returns the {@link OrderDirection} used to sort the returned values.
     *
     * @return The {@link OrderDirection}.
     */
    public OrderDirection direction() {
        return direction;
    }

    /**
     * Returns the map with additional/uncommon extra parameters. All Paginators can use this map to
     * store any extra parameters that may be necessary to the specific Paginator implementation.
     *
     * @return The map with extra parameters.
     */
    public Map<String, Object> extraParams() {
        return extraParams;
    }

    /**
     * Returns the Functional Interface that will be used to transform every entry in the result set
     * into an expected value.
     *
     * @return The {@link Function}.
     */
    public Function<PaginatedArrayList<T>, R> function() {
        return function;
    }

    @Override
    public String toString() {
        return "PaginationUtilParams{" +
                "request=" + request +
                ", response=" + response +
                ", user=" + user +
                ", filter='" + filter + '\'' +
                ", page=" + page +
                ", perPage=" + perPage +
                ", orderBy='" + orderBy + '\'' +
                ", direction=" + direction +
                ", extraParams=" + extraParams +
                '}';
    }

    public static class Builder<T, R> {

        private HttpServletRequest request;
        private HttpServletResponse response;
        private User user;
        private String filter = BLANK;
        private int page = 0;
        private int perPage = 10;
        private String orderBy = BLANK;
        private OrderDirection direction = OrderDirection.ASC;
        private Map<String, Object> extraParams = new HashMap<>();
        private Function<PaginatedArrayList<T>, R> function;

        /**
         * Sets the current instance of the {@link HttpServletRequest}.
         *
         * @param request The current instance of the {@link HttpServletRequest}.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withRequest(final HttpServletRequest request) {
            this.request = request;
            return this;
        }

        /**
         * Sets the current instance of the {@link HttpServletResponse}.
         *
         * @param response The current instance of the {@link HttpServletResponse}.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withResponse(final HttpServletResponse response) {
            this.response = response;
            return this;
        }

        /**
         * Sets the {@link User} that is calling the Paginator.
         *
         * @param user The {@link User} that is calling the Paginator.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withUser(final User user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the optional filtering parameter. Every paginator can use or implement this
         * parameter as required.
         *
         * @param filter The optional filtering parameter.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withFilter(final String filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets the result offset value, for pagination purposes.
         *
         * @param page The offset.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withPage(final int page) {
            this.page = page;
            return this;
        }

        /**
         * Sets the number of items per page, for pagination purposes.
         *
         * @param perPage The number of items per page.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withPerPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        /**
         * Sets the criterion used to sort the returned values.
         *
         * @param orderBy The criterion used to sort the returned values.
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withOrderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /**
         * Sets the {@link OrderDirection} used to sort the returned values.
         *
         * @param direction The {@link OrderDirection}
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withDirection(final OrderDirection direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Sets any extra parameters that may be necessary to the specific Paginator
         * implementation.
         *
         * @param extraParams The map with extra parameters
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withExtraParams(final Map<String, Object> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        /**
         * Sets the Functional Interface that will be used to transform every entry in the result
         * set into an expected value.
         *
         * @param function The {@link Function}
         *
         * @return The current instance of the {@link Builder}.
         */
        public Builder<T, R> withFunction(final Function<PaginatedArrayList<T>, R> function) {
            this.function = function;
            return this;
        }

        /**
         * Builds an instance of the {@link PaginationUtilParams} class.
         *
         * @return An instance of the {@link PaginationUtilParams}.
         */
        public PaginationUtilParams<T, R> build() {
            return new PaginationUtilParams<T, R>(this);
        }

    }

}
