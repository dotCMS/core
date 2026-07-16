package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable domain representation of search result count metadata from any search engine.
 *
 * <p>This record provides a unified abstraction for total hit count information,
 * allowing the application to understand search result quantities without depending on
 * specific search engine libraries (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p><strong>Count Relations:</strong></p>
 * <ul>
 *   <li><strong>EQUAL_TO:</strong> the count is exact.</li>
 *   <li><strong>GREATER_THAN_OR_EQUAL_TO:</strong> the count is a lower bound (there are at least
 *       this many matching documents, possibly more).</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * TotalHits totalHits = TotalHits.from(elasticsearchTotalHits);
 * long count = totalHits.value();
 * if (totalHits.relation() == Relation.EQUAL_TO) { ... }
 * </pre>
 *
 * @param value    the total number of hits that match the query
 * @param relation whether the count is exact ({@link Relation#EQUAL_TO}) or a lower bound;
 *                 defaults to {@link Relation#EQUAL_TO}
 * @author Fabrizio Araya
 * @see SearchHits
 * @see Relation
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
public record TotalHits(
        @JsonProperty("value") long value,
        @JsonProperty("relation") Relation relation) {

    /**
     * Canonical constructor. {@code relation} defaults to {@link Relation#EQUAL_TO} when
     * {@code null} (mirrors the previous Immutables default).
     */
    public TotalHits {
        relation = relation == null ? Relation.EQUAL_TO : relation;
    }

    /**
     * Velocity/back-compat alias for {@link #value()}. Elasticsearch's {@code TotalHits.value} was a
     * public field, so legacy VTL walked {@code $r.hits.totalHits.value}; that resolves via this
     * getter on the record. Returns the same value as the {@code value} component, so Jackson merges
     * it into the existing {@code value} field and the JSON shape is unchanged.
     */
    public long getValue() {
        return value;
    }

    /**
     * Creates a new TotalHits builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty TotalHits with value 0.
     *
     * @return a new TotalHits instance with value 0
     */
    public static TotalHits empty() {
        return builder()
                .value(0L)
                .build();
    }

    /**
     * Creates a TotalHits from an Elasticsearch TotalHits.
     *
     * @param esTotalHits the Elasticsearch TotalHits to wrap
     * @return a new TotalHits instance
     */
    public static TotalHits from(org.apache.lucene.search.TotalHits esTotalHits) {
        if (esTotalHits == null) {
            return empty();
        }
        return builder()
                .value(esTotalHits.value)
                .relation(Relation.from(esTotalHits.relation))
                .build();
    }

    /**
     * Creates a TotalHits from an OpenSearch TotalHits.
     *
     * @param osTotalHits the OpenSearch TotalHits to wrap
     * @return a new TotalHits instance
     */
    public static TotalHits from(org.opensearch.client.opensearch.core.search.TotalHits osTotalHits) {
        if (osTotalHits == null) {
            return empty();
        }
        return builder()
                .value(osTotalHits.value())
                .relation(Relation.from(osTotalHits.relation()))
                .build();
    }

    /**
     * Fluent builder for {@link TotalHits}. An unset {@code relation} defaults to
     * {@link Relation#EQUAL_TO}, preserving the lenient behaviour of the former Immutables builder.
     */
    public static final class Builder {

        private long value;
        private Relation relation = Relation.EQUAL_TO;

        public Builder value(final long value) {
            this.value = value;
            return this;
        }

        public Builder relation(final Relation relation) {
            this.relation = relation;
            return this;
        }

        public TotalHits build() {
            return new TotalHits(value, relation);
        }
    }
}
