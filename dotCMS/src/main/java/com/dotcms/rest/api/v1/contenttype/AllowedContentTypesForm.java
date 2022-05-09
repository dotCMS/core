package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Provides the list of Content Types -- in the form of Velocity Variable Names -- that can be referenced inside a Story
 * Block field. Any other Content Type that is NOT part if this list cannot be referenced t all.
 *
 * @author Jose Castro
 * @since Apr 27th, 2022
 */
@JsonDeserialize(builder = AllowedContentTypesForm.Builder.class)
public class AllowedContentTypesForm extends Validated {

    private final String types;
    private final int page;
    private final int perPage;
    private final String orderBy;
    private final String direction;

    private static final String DEFAULT_ORDER_BY = "UPPER(name)";
    private static final String DEFAULT_DIRECTION = "ASC";

    /**
     * Internal class constructor meant to be used by the Builder class.
     *
     * @param builder The Builder object for the {@link AllowedContentTypesForm} class.
     */
    private AllowedContentTypesForm(final Builder builder) {
        this.types = builder.types;
        this.page = builder.page;
        this.perPage = builder.perPage;
        this.orderBy = builder.orderBy;
        this.direction = builder.direction;
    }

    /**
     * The comma-separated list of Velocity Variable Names for each Content Type that can be referenced in a Story Block
     * field.
     *
     * @return The comma-separated list of Velocity Variable Names.
     */
    public String getTypes() {
        return this.types;
    }

    /**
     * The result page that will be returned as part of the response, for pagination purposes.
     *
     * @return The result page.
     */
    public int getPage() {
        return this.page;
    }

    /**
     * The total amount of items per result page that will be returned as part of the response, for pagination
     * purposes.
     *
     * @return The total items per result page.
     */
    public int getPerPage() {
        return this.perPage;
    }

    /**
     * The criterion in which results will be ordered in the response.
     *
     * @return The ordering criterion.
     */
    public String getOrderBy() {
        return UtilMethods.isSet(this.orderBy) ? this.orderBy : DEFAULT_ORDER_BY;
    }

    /**
     * The ordering direction for the results in the response: {@code ASC} or {@code DESC}.
     *
     * @return The ordering direction.
     */
    public String getDirection() {
        return UtilMethods.isSet(this.direction) ? this.direction : DEFAULT_DIRECTION;
    }

    /**
     * Builder class used to create an instance of the {@link AllowedContentTypesForm} class.
     */
    public static final class Builder {

        @JsonProperty(required = true)
        private String types;
        @JsonProperty
        private int page;
        @JsonProperty
        private int perPage;
        @JsonProperty
        private String orderBy;
        @JsonProperty
        private String direction;

        private Builder() {
        }

        /**
         * The comma-separated list of Velocity Variable Names for each Content Type that can be referenced in a Story
         * Block field.
         *
         * @param types The comma-separated list of Velocity Variable Names.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder types(final String types) {
            this.types = types;
            return this;
        }

        /**
         * The result page that will be returned as part of the response, for pagination purposes.
         *
         * @param page The result page.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder page(final int page) {
            this.page = page;
            return this;
        }

        /**
         * The total amount of items per result page that will be returned as part of the response, for pagination
         * purposes.
         *
         * @param perPage The total items per result page.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        /**
         * The criterion in which results will be ordered in the response.
         *
         * @param orderBy The ordering criterion.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /**
         * The ordering direction for the results in the response: {@code ASC} or {@code DESC}.
         *
         * @param direction The ordering direction.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder direction(final String direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Creates an instance of the {@link AllowedContentTypesForm} class.
         *
         * @return An instance of the {@link AllowedContentTypesForm} class.
         */
        public AllowedContentTypesForm build() {
            return new AllowedContentTypesForm(this);
        }

    }

}
