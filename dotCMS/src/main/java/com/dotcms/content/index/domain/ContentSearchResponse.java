package com.dotcms.content.index.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Vendor-neutral representation of a raw search response.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.action.search.SearchResponse} in
 * application code. Carries the search hits, timing metadata, scroll ID, and the aggregations.</p>
 *
 * <p>Aggregations are exposed two ways:</p>
 * <ul>
 *   <li>{@link #aggregations()} — a flat {@code Map<String, List<AggregationBucket>>} of the
 *       first-level {@code terms} aggregations. This is the convenient shape for Java callers that
 *       only need bucket keys and counts (e.g. {@code ContentTypeAPIImpl}, {@code WorkflowHelper}).</li>
 *   <li>{@link #aggregationTree()} — the full neutral aggregation tree, preserving nested
 *       sub-aggregations and the {@code top_hits} metric aggregation. This is what
 *       {@code ContentSearchResults#getAggregations()} exposes to Velocity so legacy templates that
 *       walk {@code .buckets} / {@code getKeyAsNumber()} / {@code getAggregations()} keep working.</li>
 * </ul>
 *
 * <p>The flat map is always derived from the tree (see {@link #flatten(Map)}) so the two
 * views can never disagree.</p>
 *
 * <p>Factory methods ({@code from(ES)}, {@code from(OS)}) map vendor types to this
 * neutral DTO; they are the only places where vendor imports are allowed in this file.</p>
 *
 * @param hits           neutral search hits (already vendor-independent)
 * @param scrollId       scroll ID returned by the cluster, or {@code null} when not a scroll request
 *                       (ES: {@code SearchResponse.getScrollId()} / OS: {@code SearchResponse.scrollId()})
 * @param tookMillis     time the cluster took to execute the query, in milliseconds
 * @param aggregationTree full neutral aggregation tree, keyed by aggregation name (nested
 *                        sub-aggregations and {@code top_hits} preserved); Velocity resolves
 *                        {@code $results.aggregations.<name>} through this map
 */
// The Velocity-only aliases {@code aggregations}, {@code tookInMillis} and {@code suggest} are
// suppressed for Jackson at the CLASS level (not with method-level {@code @JsonIgnore} on the
// getters) on purpose. Each is redundant on the neutral Jackson wire (the tree is serialized as
// {@code aggregationTree}, timing as {@code tookMillis}, and suggestions are wire-omitted), but a
// method-level {@code @JsonIgnore} would ALSO hide them from the reflection-based
// {@code com.dotmarketing.util.json.JSONObject} bean constructor that {@code JSONTool.generate(Object)}
// uses — it honours {@code @JsonIgnore}. That reflection path is exactly what
// {@code $json.generate($response)} templates walk, so a method-level ignore silently drops the data
// there (issue #36435 — originally only {@code aggregations} was reported, {@code tookInMillis} and
// {@code suggest} shared the same latent regression). A class-level {@code @JsonIgnoreProperties} is
// read by Jackson but NOT by the vendored JSONObject, so it keeps the wire shape unchanged while
// leaving the {@code getX()} aliases visible to Velocity's json tool.
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"aggregations", "tookInMillis", "suggest"})
public record ContentSearchResponse(
        SearchHits hits,
        @Nullable String scrollId,
        long tookMillis,
        Map<String, Aggregation> aggregationTree,
        Map<String, Object> suggest) {

    /**
     * Canonical constructor. {@code aggregationTree} and {@code suggest} default to an empty map when
     * {@code null} (mirrors the previous Immutables collection defaults).
     */
    public ContentSearchResponse {
        aggregationTree = aggregationTree == null ? Collections.emptyMap() : aggregationTree;
        suggest = suggest == null ? Collections.emptyMap() : suggest;
    }

    /**
     * First-level terms aggregations, keyed by aggregation name.
     * Derived from {@link #aggregationTree()}; only aggregations that have buckets are included.
     */
    public Map<String, List<AggregationBucket>> aggregations() {
        return flatten(aggregationTree());
    }

    // -------------------------------------------------------------------------
    // Backward-compat accessors (Velocity)
    // -------------------------------------------------------------------------
    // {@code $dotcontent.raw(...)} hands this record straight to VTL. Velocity's property syntax
    // ({@code $r.hits}) only resolves {@code getX()}/{@code isX()}, not the bare record accessors
    // ({@code hits()}), so legacy templates would silently get {@code null}. These aliases restore
    // that access WITHOUT changing the JSON wire shape:
    //  - getHits()/getScrollId() return the same values as the record components, so Jackson merges
    //    them into the existing "hits"/"scrollId" fields (no new key, no duplicate).
    //  - getTookInMillis()/getAggregations()/getSuggest() are Velocity-only and must NOT add a field
    //    to the neutral JSON; that suppression lives in the class-level @JsonIgnoreProperties above
    //    (Jackson-only) rather than a method-level @JsonIgnore, so they stay visible to the
    //    reflection-based $json.generate() path (issue #36435).

    /** Velocity/back-compat alias for {@link #hits()}; serializes as the same {@code hits} field. */
    public SearchHits getHits() {
        return hits;
    }

    /** Velocity/back-compat alias for {@link #scrollId()}; serializes as the same {@code scrollId} field. */
    public String getScrollId() {
        return scrollId;
    }

    /**
     * Velocity/back-compat alias mirroring Elasticsearch {@code SearchResponse.getTookInMillis()}.
     * Deliberately NOT annotated {@code @JsonIgnore}: it must stay visible to the reflection-based
     * {@code $json.generate($response)} path (issue #36435). The neutral Jackson JSON is kept
     * unchanged (timing is serialized only as {@code tookMillis}) by the class-level
     * {@code @JsonIgnoreProperties("tookInMillis")}.
     */
    public long getTookInMillis() {
        return tookMillis;
    }

    /**
     * Velocity/back-compat alias exposing the aggregation tree as {@code $r.aggregations}, matching
     * {@code ContentSearchResults#getAggregations()} so {@code $dotcontent.raw(...)} and
     * {@code $dotcontent.search(...)} templates walk aggregations the same way.
     *
     * <p>Deliberately NOT annotated {@code @JsonIgnore}: this accessor must stay visible to the
     * reflection-based {@code com.dotmarketing.util.json.JSONObject} bean constructor behind
     * {@code JSONTool.generate(Object)}, so {@code $json.generate($response).aggregations...} templates
     * keep working (issue #36435). The neutral Jackson JSON is kept unchanged (the tree is serialized
     * only as {@code aggregationTree}) by the class-level {@code @JsonIgnoreProperties("aggregations")}
     * instead — Jackson honours it, the vendored JSONObject does not.</p>
     */
    public Map<String, Aggregation> getAggregations() {
        return aggregationTree;
    }

    /**
     * Velocity/back-compat alias for {@link #suggest()}, exposing search suggestions as
     * {@code $r.suggest}. Deliberately NOT annotated {@code @JsonIgnore}: it must stay visible to the
     * reflection-based {@code $json.generate($response)} path (issue #36435). The neutral JSON shape of
     * {@code /api/es/raw} is unchanged (suggestions are wire-omitted) via the class-level
     * {@code @JsonIgnoreProperties("suggest")}; the ES-wire {@code suggest} block is emitted only by the
     * {@code /api/es/search} legacy adapter.
     */
    public Map<String, Object> getSuggest() {
        return suggest;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Derives the flat first-level terms map from an aggregation tree: every bucket aggregation
     * (e.g. {@code terms}) contributes its bucket list under its name — <b>including when the bucket
     * list is empty</b>, so callers can rely on the key being present for a declared aggregation.
     * Metric aggregations such as {@code top_hits} (which carry hits, not buckets) are skipped,
     * mirroring the legacy behaviour where only {@code terms} aggregations were mapped.
     *
     * <p>The discriminator is {@link Aggregation#getHits()}: bucket aggregations leave it
     * {@code null}; {@code top_hits} sets it (possibly empty). Filtering on {@code getHits() == null}
     * — not on whether buckets are empty — keeps empty-result terms aggregations in the map.</p>
     */
    static Map<String, List<AggregationBucket>> flatten(final Map<String, Aggregation> tree) {
        if (tree == null || tree.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, List<AggregationBucket>> map = new LinkedHashMap<>();
        for (final Aggregation aggregation : tree.values()) {
            if (aggregation.getHits() == null) {
                map.put(aggregation.getName(), aggregation.getBuckets());
            }
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // ES factory
    // -------------------------------------------------------------------------

    public static ContentSearchResponse from(
            final org.elasticsearch.action.search.SearchResponse esResponse) {

        return builder()
                .hits(esResponse.getHits() != null
                        ? SearchHits.from(esResponse.getHits())
                        : SearchHits.empty())
                .scrollId(esResponse.getScrollId())
                .tookMillis(esResponse.getTook() != null ? esResponse.getTook().getMillis() : 0L)
                .aggregationTree(Aggregation.from(esResponse.getAggregations()))
                .suggest(suggestFrom(esResponse.getSuggest()))
                .build();
    }

    /**
     * Converts an Elasticsearch {@code Suggest} into a vendor-neutral {@code Map<String, Object>}
     * mirroring the ES suggest JSON shape: {@code { <suggesterName>: [ { text, offset, length,
     * options: [ { text, score } ] } ] } }. Vendor imports are confined to this factory (like the
     * other {@code from(...)} methods). Returns an empty map when there are no suggestions.
     */
    @SuppressWarnings("rawtypes")
    private static Map<String, Object> suggestFrom(
            final org.elasticsearch.search.suggest.Suggest esSuggest) {
        if (esSuggest == null) {
            return Collections.emptyMap();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final org.elasticsearch.search.suggest.Suggest.Suggestion suggestion : esSuggest) {
            final List<Map<String, Object>> entries = new ArrayList<>();
            for (final Object entryObj : suggestion.getEntries()) {
                final org.elasticsearch.search.suggest.Suggest.Suggestion.Entry entry =
                        (org.elasticsearch.search.suggest.Suggest.Suggestion.Entry) entryObj;
                final List<Map<String, Object>> options = new ArrayList<>();
                for (final Object optionObj : entry.getOptions()) {
                    final org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option option =
                            (org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option) optionObj;
                    final Map<String, Object> opt = new LinkedHashMap<>();
                    opt.put("text", option.getText() != null ? option.getText().string() : null);
                    opt.put("score", finiteScoreOrNull(option.getScore()));
                    options.add(opt);
                }
                final Map<String, Object> entryMap = new LinkedHashMap<>();
                entryMap.put("text", entry.getText() != null ? entry.getText().string() : null);
                entryMap.put("offset", entry.getOffset());
                entryMap.put("length", entry.getLength());
                entryMap.put("options", options);
                entries.add(entryMap);
            }
            result.put(suggestion.getName(), entries);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // OS factory
    // -------------------------------------------------------------------------

    public static ContentSearchResponse from(
            final org.opensearch.client.opensearch.core.SearchResponse<Object> osResponse) {

        return builder()
                .hits(osResponse.hits() != null
                        ? SearchHits.from(osResponse.hits())
                        : SearchHits.empty())
                .scrollId(osResponse.scrollId())
                .tookMillis(osResponse.took())
                .aggregationTree(Aggregation.fromOS(osResponse.aggregations()))
                .suggest(suggestFrom(osResponse.suggest()))
                .build();
    }

    /**
     * Converts the OpenSearch suggest map into the same vendor-neutral shape as the ES
     * {@link #suggestFrom(org.elasticsearch.search.suggest.Suggest)} overload:
     * {@code { <suggesterName>: [ { text, offset, length, options: [ { text, score } ] } ] } }.
     * Handles the term / phrase / completion union variants. Vendor imports are confined to this
     * factory. Returns an empty map when there are no suggestions.
     */
    private static Map<String, Object> suggestFrom(
            final Map<String, ? extends List<? extends
                    org.opensearch.client.opensearch.core.search.Suggest<?>>> osSuggest) {
        if (osSuggest == null || osSuggest.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final Map.Entry<String, ? extends List<? extends
                org.opensearch.client.opensearch.core.search.Suggest<?>>> entry : osSuggest.entrySet()) {
            final List<Map<String, Object>> entries = new ArrayList<>();
            for (final org.opensearch.client.opensearch.core.search.Suggest<?> suggest : entry.getValue()) {
                final Map<String, Object> entryMap = osSuggestEntry(suggest);
                if (entryMap != null) {
                    entries.add(entryMap);
                }
            }
            result.put(entry.getKey(), entries);
        }
        return result;
    }

    /**
     * Maps one OpenSearch {@code Suggest} union (term/phrase/completion) to the neutral entry shape.
     * The three variants share the {@code text}/{@code offset}/{@code length} metadata (all inherited
     * from {@code SuggestBase}) and differ only in their option element type, so the common shaping is
     * factored into {@link #suggestEntry(org.opensearch.client.opensearch.core.search.SuggestBase,
     * List, Function)}.
     */
    private static Map<String, Object> osSuggestEntry(
            final org.opensearch.client.opensearch.core.search.Suggest<?> suggest) {
        if (suggest.isTerm()) {
            final org.opensearch.client.opensearch.core.search.TermSuggest s = suggest.term();
            return suggestEntry(s, s.options(), o -> suggestOption(o.text(), o.score()));
        } else if (suggest.isPhrase()) {
            final org.opensearch.client.opensearch.core.search.PhraseSuggest s = suggest.phrase();
            return suggestEntry(s, s.options(), o -> suggestOption(o.text(), o.score()));
        } else if (suggest.isCompletion()) {
            final org.opensearch.client.opensearch.core.search.CompletionSuggest<?> s = suggest.completion();
            return suggestEntry(s, s.options(), o -> suggestOption(o.text(), o.score()));
        }
        return null;
    }

    /**
     * Builds a neutral suggest entry from the shared {@code SuggestBase} metadata plus the variant's
     * options, mapped to the neutral {@code {text, score}} shape by {@code optionMapper}.
     */
    private static <O> Map<String, Object> suggestEntry(
            final org.opensearch.client.opensearch.core.search.SuggestBase base,
            final List<O> opts,
            final Function<O, Map<String, Object>> optionMapper) {
        final Map<String, Object> entryMap = new LinkedHashMap<>();
        entryMap.put("text", base.text());
        entryMap.put("offset", base.offset());
        entryMap.put("length", base.length());
        final List<Map<String, Object>> options = new ArrayList<>();
        for (final O o : opts) {
            options.add(optionMapper.apply(o));
        }
        entryMap.put("options", options);
        return entryMap;
    }

    private static Map<String, Object> suggestOption(final String text, final double score) {
        final Map<String, Object> option = new LinkedHashMap<>();
        option.put("text", text);
        option.put("score", finiteScoreOrNull(score));
        return option;
    }

    /**
     * Coerces a suggest-option score to {@code null} when it is non-finite ({@code NaN}/Infinity).
     * The suggest map is serialized via {@code new JSONObject(map)}, and dotCMS's JSON writer rejects
     * non-finite numbers ("JSON does not allow non-finite numbers", see #36478), so a stray NaN would
     * fail the whole response. Returning {@code null} lets {@code JSONObject.wrap} emit a JSON null.
     */
    private static Object finiteScoreOrNull(final double score) {
        return Double.isFinite(score) ? score : null;
    }

    /**
     * Fluent builder for {@link ContentSearchResponse}. An unset {@code aggregationTree} defaults to
     * an empty map and an unset {@code scrollId} to {@code null}, preserving the lenient behaviour of
     * the former Immutables builder.
     */
    public static final class Builder {

        private SearchHits hits;
        private String scrollId;
        private long tookMillis;
        private Map<String, Aggregation> aggregationTree = Collections.emptyMap();
        private Map<String, Object> suggest = Collections.emptyMap();

        public Builder hits(final SearchHits hits) {
            this.hits = hits;
            return this;
        }

        public Builder scrollId(final String scrollId) {
            this.scrollId = scrollId;
            return this;
        }

        public Builder tookMillis(final long tookMillis) {
            this.tookMillis = tookMillis;
            return this;
        }

        public Builder aggregationTree(final Map<String, Aggregation> aggregationTree) {
            this.aggregationTree = aggregationTree;
            return this;
        }

        public Builder suggest(final Map<String, Object> suggest) {
            this.suggest = suggest;
            return this;
        }

        public ContentSearchResponse build() {
            return new ContentSearchResponse(hits, scrollId, tookMillis, aggregationTree, suggest);
        }
    }
}
