package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 * Provides pagination data associated to the entity returned by any dotCMS REST Endpoint. This way, developers can
 * still access pagination data even when accessing our APIs through proxies that may remove the already existing
 * pagination headers.
 *
 * @author Jose Castro
 * @since Mar 3rd, 2023
 */
@JsonDeserialize(builder = Pagination.Builder.class)
public class Pagination implements Serializable {

    private final int currentPage;
    private final int perPage;
    private final long totalEntries;

    /**
     * Private constructor used to create an instance of this class.
     *
     * @param builder The {@link Builder} class for the pagination object.
     */
    private Pagination(final Builder builder) {
        this.currentPage = builder.currentPage;
        this.perPage = builder.perPage;
        this.totalEntries = builder.totalEntries;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public int getPerPage() {
        return this.perPage;
    }

    public long getTotalEntries() {
        return this.totalEntries;
    }

    @Override
    public String toString() {
        return "Pagination{" + "currentPage=" + this.currentPage + ", perPage=" + this.perPage + ", totalEntries=" + this.totalEntries + '}';
    }

    /**
     * This builder allows you to create an instance of the {@link Pagination} class.
     */
    public static class Builder {

        @JsonProperty
        private int currentPage;
        @JsonProperty
        private int perPage;
        @JsonProperty
        private long totalEntries;

        /**
         * Returns the currently selected results page, or the first one if not specified.
         *
         * @param currentPage The current results page.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder currentPage(int currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        /**
         * The maximum number of items that are included in a results page.
         *
         * @param perPage The maximum number of returned items.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder perPage(int perPage) {
            this.perPage = perPage;
            return this;
        }

        /**
         * The total number of results for a given data query. That is, the total list of <b>unfiltered items</b> for a
         * given query.
         *
         * @param totalEntries The total number of results.
         *
         * @return The current {@link Builder} instance.
         */
        public Builder totalEntries(long totalEntries) {
            this.totalEntries = totalEntries;
            return this;
        }

        /**
         * Creates an instance of the {@link Pagination} class.
         *
         * @return A new instance of the {@link Pagination} class.
         */
        public Pagination build() {
            return new Pagination(this);
        }

    }

}
