package com.dotcms.content.index.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Immutable domain representation of a single search result hit from any search engine.
 *
 * <p>This type provides a unified abstraction layer for individual search results, allowing the
 * application to work with search hits without depending on specific search engine libraries
 * (Elasticsearch, OpenSearch, etc.).</p>
 *
 * <p><strong>Two shapes, one contract.</strong> Content search results are the hot path — they must
 * stay as small as possible — while Site Search results carry an extra concern nothing else asks
 * for: the per-field highlight fragments used to render result snippets. Rather than widening every
 * hit with a field only one caller reads, this is a sealed interface over two records:</p>
 * <ul>
 *   <li>{@link ContentSearchHit} — the lean shape: six components, no highlight state at all.</li>
 *   <li>{@link SiteSearchHit} — the same six plus the highlight fragments.</li>
 * </ul>
 *
 * <p>Callers never choose: {@link Builder#build()} picks the shape from the data, returning the lean
 * record unless the engine actually returned highlight fragments. Since the only queries that request
 * highlighting are the Site Search ones ({@code ESSiteSearchAPI} / {@code OSSiteSearchAPI}), a content
 * hit is always a {@link ContentSearchHit} and pays nothing — not even a null reference — for a
 * concern it does not use. If a content query ever requests highlighting, it would get the
 * highlight-bearing shape and the names here would be worth revisiting.</p>
 *
 * <p>Highlights are read through {@link #highlightsFor(String)}, which defaults to an empty list, so
 * a caller holding a plain {@code SearchHit} never has to know or ask which shape it got.</p>
 *
 * <p>Accessors are bean-style ({@code getId()}, {@code getSourceAsMap()}, …) so the type works
 * directly from Velocity templates (e.g. {@code $hit.id}) without any extra alias methods. The record
 * components are named accordingly and the {@code JsonProperty} annotations keep the JSON contract
 * clean ({@code id}, {@code index}, …) for the caches/REST paths backed by this type. Deserializing the
 * interface goes through {@link #fromJson}, which reads the same {@code highlights} key the serialized
 * form already carries and rebuilds the matching shape — so a round-trip preserves the fragments
 * instead of silently dropping them, and no polymorphic type discriminator has to be added to a JSON
 * shape those caches depend on.</p>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Create from Elasticsearch hit
 * SearchHit hit = SearchHit.from(elasticsearchHit);
 *
 * // Create from OpenSearch hit
 * SearchHit hit = SearchHit.from(openSearchHit);
 *
 * // Access unified data
 * String docId = hit.getId();
 * Map&lt;String, Object&gt; content = hit.getSourceAsMap();
 * float relevanceScore = hit.getScore();
 *
 * // Site Search snippets — empty for every hit that was not highlighted
 * List&lt;String&gt; fragments = hit.highlightsFor("content");
 * </pre>
 *
 * @author Fabrizio Araya
 * @see ContentSearchHit
 * @see SiteSearchHit
 * @see SearchHits
 * @see com.dotcms.content.index.ContentFactoryIndexOperations
 */
public sealed interface SearchHit permits ContentSearchHit, SiteSearchHit {

    /**
     * @return the unique identifier of this search hit (the document ID)
     */
    String getId();

    /**
     * @return the index name where this search hit was found, or {@code null} if not available
     */
    String getIndex();

    /**
     * @return the source document as a map of field names to values, never {@code null}
     */
    Map<String, Object> getSourceAsMap();

    /**
     * @return the search relevance score for this hit, or {@code NaN} when the engine omitted it
     *         (field-sorted, non-relevance-scored queries)
     */
    float getScore();

    /**
     * @return the document fields retrieved by the search query (additional fields beyond the source
     *         document that were explicitly requested), empty if none were requested
     */
    Map<String, Object> getFields();

    /**
     * @return the per-hit sort values the engine returns when the query sorts by a field (e.g. the
     *         computed distance for a {@code _geo_distance} sort), in the order the {@code sort}
     *         clause declared; empty for relevance-only (unsorted) queries
     */
    List<Object> getSortValues();

    /**
     * The highlight fragments for a single field, in engine order. Only {@link SiteSearchHit} carries
     * any: a hit from a query that did not request highlighting yields an empty list here, which is
     * why callers can turn the result straight into an array without a null check.
     *
     * @param fieldName the field the highlight was requested for
     * @return that field's fragments, never {@code null}
     */
    default List<String> highlightsFor(final String fieldName) {
        return List.of();
    }

    /**
     * Creates a new SearchHit builder.
     *
     * @return a new builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Jackson entry point for the interface, which is not instantiable on its own.
     *
     * <p>It deliberately re-uses {@link Builder#build()} rather than pinning one implementation: the
     * serialized form already carries the {@code highlights} key when there were fragments, so the same
     * rule that picks the shape on the way in from the engine picks it on the way in from JSON. Pinning
     * {@code ContentSearchHit} instead would make a serialized {@link SiteSearchHit} come back without
     * its fragments and without an error — a silent loss, which is the failure mode this codebase has
     * already paid for elsewhere.</p>
     *
     * @return the shape matching the deserialized data
     */
    @JsonCreator
    static SearchHit fromJson(
            @JsonProperty("id") final String id,
            @JsonProperty("index") final String index,
            @JsonProperty("sourceAsMap") final Map<String, Object> sourceAsMap,
            @JsonProperty("score") final float score,
            @JsonProperty("fields") final Map<String, Object> fields,
            @JsonProperty("sortValues") final List<Object> sortValues,
            @JsonProperty("highlights") final Map<String, List<String>> highlights) {
        return builder()
                .id(id)
                .index(index)
                .sourceAsMap(sourceAsMap)
                .score(score)
                .fields(fields)
                .sortValues(sortValues)
                .highlights(highlights)
                .build();
    }

    /**
     * Creates a SearchHit from an Elasticsearch SearchHit.
     *
     * @param esSearchHit the Elasticsearch SearchHit to wrap
     * @return a new SearchHit instance
     */
    static SearchHit from(final org.elasticsearch.search.SearchHit esSearchHit) {
        final Object[] esSortValues = esSearchHit.getSortValues();
        return builder()
                .id(esSearchHit.getId())
                .sourceAsMap(esSearchHit.getSourceAsMap())
                .fields(esSearchHit.getFields())
                .score(esSearchHit.getScore())
                .index(esSearchHit.getIndex())
                .sortValues(esSortValues == null ? null : Arrays.asList(esSortValues))
                .highlights(fromEsHighlightFields(esSearchHit.getHighlightFields()))
                .build();
    }

    /**
     * Flattens Elasticsearch's {@code Map<String, HighlightField>} into the neutral
     * {@code Map<String, List<String>>}: each {@code HighlightField} carries its fragments as
     * {@code Text[]}, an ES-specific type that must not leak past this adapter.
     *
     * @param highlightFields the ES highlight fields, possibly {@code null} or empty
     * @return field name to fragments, empty when the hit carries no highlight
     */
    private static Map<String, List<String>> fromEsHighlightFields(
            final Map<String, org.elasticsearch.search.fetch.subphase.highlight.HighlightField> highlightFields) {
        if (highlightFields == null || highlightFields.isEmpty()) {
            return Map.of();
        }
        final Map<String, List<String>> highlights = new HashMap<>();
        highlightFields.forEach((fieldName, highlightField) -> {
            final org.elasticsearch.common.text.Text[] fragments = highlightField.fragments();
            if (fragments != null && fragments.length > 0) {
                highlights.put(fieldName, Arrays.stream(fragments)
                        .map(Object::toString)
                        .collect(Collectors.toList()));
            }
        });
        return highlights;
    }

    /**
     * Creates a SearchHit from an OpenSearch Hit.
     *
     * @param osHit the OpenSearch Hit to wrap
     * @return a new SearchHit instance
     */
    @SuppressWarnings("unchecked")
    static SearchHit from(final org.opensearch.client.opensearch.core.search.Hit<?> osHit) {
        // Extract source as Map - OpenSearch Hit.source() returns the typed source object
        Map<String, Object> sourceMap;
        Object source = osHit.source();
        if (source instanceof Map) {
            sourceMap = (Map<String, Object>) source;
        } else if (source instanceof org.opensearch.client.json.JsonData) {
            // top_hits aggregation hits carry their _source as JsonData (HitsMetadata<JsonData>),
            // not a Map — unwrap it so the document survives the conversion instead of being dropped.
            Map<String, Object> unwrapped;
            try {
                unwrapped = ((org.opensearch.client.json.JsonData) source).to(Map.class);
            } catch (final RuntimeException cannotMap) {
                unwrapped = null;
            }
            sourceMap = unwrapped != null ? unwrapped : Map.of();
        } else {
            // Unknown typed source — fall back to an empty map rather than failing the conversion.
            sourceMap = Map.of();
        }

        // OpenSearch returns per-hit sort values as a List<FieldValue> tagged union; unwrap each to
        // its raw scalar (Double/Long/Boolean/String, or null) so the neutral hit mirrors ES's Object[].
        final List<org.opensearch.client.opensearch._types.FieldValue> osSortValues = osHit.sort();
        final List<Object> sortValues = (osSortValues == null || osSortValues.isEmpty())
                ? null
                : osSortValues.stream()
                        .map(fieldValue -> (fieldValue == null || fieldValue.isNull())
                                ? null : fieldValue._get())
                        .collect(Collectors.toList());

        return builder()
                .id(osHit.id())
                .index(osHit.index())
                .sourceAsMap(sourceMap)
                // Map an absent OS score (field-sorted / non-relevance-scored hits, where OS omits
                // _score) to NaN, matching the ES client, which returns NaN for the same case. The
                // legacy JSON adapter coerces non-finite scores to null (#36478); defaulting to 0.0f
                // instead serialized a spurious 0.0 for field-sorted OS queries.
                .score(osHit.score() != null ? osHit.score().floatValue() : Float.NaN)
                .sortValues(sortValues)
                // OpenSearch already models highlights as field -> fragments, so no unwrapping is
                // needed here (unlike ES's Text[]-bearing HighlightField).
                .highlights(osHit.highlight())
                .build();
    }

    /**
     * Fluent builder for {@link SearchHit}. Unset collection attributes default to an empty map and
     * an unset score defaults to {@code 0.0f}, preserving the lenient behaviour of the former
     * Immutables builder.
     *
     * <p>{@link #build()} is where the two shapes are chosen: a hit whose engine response carried no
     * highlight fragments becomes a {@link ContentSearchHit}, which has no highlight state to carry.
     * That keeps the decision in one place instead of threading a "do I want highlights" flag down
     * through {@code rawSearch} → {@code ContentSearchResponse.from} → {@code SearchHits.from}, which
     * would be needed otherwise: Site Search and content search reach the neutral layer through the
     * very same factory.</p>
     */
    final class Builder {

        private String id;
        private String index;
        private Map<String, Object> sourceAsMap = Map.of();
        private float score;
        private Map<String, Object> fields = Map.of();
        private List<Object> sortValues = List.of();
        private Map<String, List<String>> highlights = Map.of();

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder index(final String index) {
            this.index = index;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder sourceAsMap(final Map<String, ?> sourceAsMap) {
            this.sourceAsMap = (Map<String, Object>) sourceAsMap;
            return this;
        }

        public Builder score(final float score) {
            this.score = score;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder fields(final Map<String, ?> fields) {
            this.fields = (Map<String, Object>) fields;
            return this;
        }

        public Builder sortValues(final List<Object> sortValues) {
            this.sortValues = sortValues == null ? List.of() : sortValues;
            return this;
        }

        public Builder highlights(final Map<String, List<String>> highlights) {
            this.highlights = highlights == null ? Map.of() : highlights;
            return this;
        }

        /**
         * @return a {@link SiteSearchHit} when highlight fragments are present, otherwise the lean
         *         {@link ContentSearchHit}
         */
        public SearchHit build() {
            if (highlights.isEmpty()) {
                return new ContentSearchHit(id, index, sourceAsMap, score, fields, sortValues);
            }
            return new SiteSearchHit(id, index, sourceAsMap, score, fields, sortValues, highlights);
        }
    }
}
