package com.dotcms.content.elasticsearch.business;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Identifier;
import com.liferay.portal.model.User;

/**
 * This class represents a generic multipurpose search criteria object. It can be used by several
 * APIs to return specific filtered or paginated information. It also helps keeping method
 * signatures from having lost of parameters.
 * <p>Developers can add as many filtering or search attributes as they need to.</p>
 *
 * @author Jose Castro
 * @since Aug 4th, 2025
 */
public class SearchCriteria {

    private final Identifier identifier;

    private final User user;

    private final boolean bringOldVersions;

    private final int limit;
    private final int offset;
    private final OrderDirection orderDirection;

    private final boolean respectFrontendRoles;

    private SearchCriteria(final Builder builder) {
        this.identifier = builder.identifier;
        this.user = builder.user;
        this.bringOldVersions = builder.bringOldVersions;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.orderDirection = builder.orderDirection;
        this.respectFrontendRoles = builder.respectFrontendRoles;
    }

    /**
     * Returns the {@link Identifier} object that belongs to the contentlet being searched.
     *
     * @return The {@link Identifier} of the contentlet.
     */
    public Identifier identifier() {
        return this.identifier;
    }

    /**
     * Returns the user that is making the search.
     *
     * @return The {@link User}.
     */
    public User user() {
        return this.user;
    }

    /**
     * Determines whether to include old versions of the contentlet, and not only its live/working
     * version. If {@code false}, only the live or working version will be returned for each
     * language it's available in.
     *
     * @return {@code true} if old versions should be included, {@code false} otherwise.
     */
    public boolean bringOldVersions() {
        return this.bringOldVersions;
    }

    /**
     * Returns the limit for the number of records to be returned, for pagination purposes.
     *
     * @return The limit.
     */
    public int limit() {
        return this.limit;
    }

    /**
     * Returns the result offset, for pagination purposes.
     *
     * @return The offset.
     */
    public int offset() {
        return this.offset;
    }

    /**
     * Returns the {@link OrderDirection} used to sort the results by.
     *
     * @return The {@link OrderDirection}.
     */
    public OrderDirection orderDirection() {
        return this.orderDirection;
    }

    /**
     * Determines whether to include only contentlets that are visible to the user making the
     * search.
     *
     * @return {@code true} if the search should respect the frontend roles, {@code false}
     * otherwise.
     */
    public boolean respectFrontendRoles() {
        return this.respectFrontendRoles;
    }

    @Override
    public String toString() {
        return "SearchCriteria{" +
                "identifier=" + identifier +
                ", user=" + user +
                ", bringOldVersions=" + bringOldVersions +
                ", limit=" + limit +
                ", offset=" + offset +
                ", orderDirection=" + orderDirection +
                ", respectFrontendRoles=" + respectFrontendRoles +
                '}';
    }

    public static class Builder {

        private Identifier identifier;

        private User user;

        private boolean bringOldVersions;

        private int limit = -1;
        private int offset = 0;
        private OrderDirection orderDirection = OrderDirection.DESC;

        private boolean respectFrontendRoles = true;

        /**
         * Specifies the {@link Identifier} object that belongs to a contentlet.
         *
         * @param identifier The {@link Identifier} of the contentlet to filter by.
         *
         * @return The {@link Builder} instance.
         */
        public Builder withIdentifier(final Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withUser(final User user) {
            this.user = user;
            return this;
        }

        /**
         * Determines whether to include old versions of the contentlet, and not only its
         * live/working version. If set to {@code false}, only the live or working version will be
         * returned for each language it's available in.
         *
         * @param bringOldVersions If old versions should be returned, set to {@code true}.
         *
         * @return The {@link Builder} instance.
         */
        public Builder withBringOldVersions(final boolean bringOldVersions) {
            this.bringOldVersions = bringOldVersions;
            return this;
        }

        /**
         * Sets the maximum number of records to be returned, for pagination purposes.
         *
         * @param limit The maximum number of records.
         *
         * @return The {@link Builder} instance
         */
        public Builder withLimit(final int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the result offset, for pagination purposes.
         *
         * @param offset The result offset.
         *
         * @return The {@link Builder} instance.
         */
        public Builder withOffset(final int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Sets the sorting direction of the results being returned.
         *
         * @param orderDirection The {@link OrderDirection}
         *
         * @return The {@link Builder} instance.
         */
        public Builder withOrderDirection(final OrderDirection orderDirection) {
            this.orderDirection = orderDirection;
            return this;
        }

        /**
         * Sets whether the search should respect the frontend roles of the specified user.
         *
         * @param respectFrontendRoles Whether the search should respect the frontend roles.
         *
         * @return The {@link Builder} instance.
         */
        public Builder withRespectFrontendRoles(final boolean respectFrontendRoles) {
            this.respectFrontendRoles = respectFrontendRoles;
            return this;
        }

        /**
         * Builds the {@link SearchCriteria} object.
         *
         * @return The {@link SearchCriteria} instance.
         */
        public SearchCriteria build() {
            return new SearchCriteria(this);
        }

    }

}
