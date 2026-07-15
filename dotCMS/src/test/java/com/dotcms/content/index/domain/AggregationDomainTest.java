package com.dotcms.content.index.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Test;

/**
 * Fast unit coverage for the vendor-neutral aggregation model introduced for
 * <a href="https://github.com/dotCMS/core/issues/36026">#36026</a>. Runs without any search
 * engine: it builds the neutral types directly and verifies the contract the Velocity layer and
 * the flat-map Java callers depend on.
 */
public class AggregationDomainTest {

    /**
     * The bean-style accessors on {@link SearchHit} / {@link SearchHits} must serialize cleanly
     * (these types back several caches/REST paths) — exactly one {@code id} property, full round-trip.
     */
    @Test
    public void searchHit_jacksonRoundTrip() throws Exception {
        // Guava module so Immutables' ImmutableMap source maps deserialize (dotCMS's default
        // mapper registers the same module).
        final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());

        final SearchHit hit = SearchHit.builder()
                .id("abc123")
                .index("live_idx")
                .sourceAsMap(Map.of("title", "hello"))
                .score(1.5f)
                .build();

        final String json = mapper.writeValueAsString(hit);
        assertTrue("serialized json must carry the id", json.contains("\"id\":\"abc123\""));
        assertEquals("id must not be serialized twice", json.indexOf("\"id\""), json.lastIndexOf("\"id\""));

        final SearchHit back = mapper.readValue(json, SearchHit.class);
        assertEquals("abc123", back.getId());
        assertEquals("hello", back.getSourceAsMap().get("title"));

        final SearchHits hits = SearchHits.builder()
                .addHits(hit)
                .totalHits(TotalHits.builder().value(1L).build())
                .build();
        final SearchHits hitsBack = mapper.readValue(mapper.writeValueAsString(hits), SearchHits.class);
        assertEquals(1, hitsBack.getHits().size());
        assertEquals("abc123", hitsBack.getHits().get(0).getId());
    }

    /** {@code getKeyAsNumber()} parses numeric keys leniently and returns null for non-numeric keys. */
    @Test
    public void aggregationBucket_keyAsNumber_isLenient() {
        assertEquals(42L, AggregationBucket.builder().key("42").docCount(1).build().getKeyAsNumber());
        assertEquals(3.14, AggregationBucket.builder().key("3.14").docCount(1).build().getKeyAsNumber());
        assertNull("non-numeric key must yield null, not throw",
                AggregationBucket.builder().key("Blog").docCount(1).build().getKeyAsNumber());
    }

    /**
     * The flat {@link ContentSearchResponse#aggregations()} map is derived from the tree and only
     * contains aggregations that have buckets — the legacy behaviour Java callers depend on.
     */
    @Test
    public void contentSearchResponse_flatMap_derivedFromTree() {
        final AggregationBucket blog = AggregationBucket.builder().key("Blog").docCount(7).build();
        final AggregationBucket news = AggregationBucket.builder().key("News").docCount(3).build();

        final Aggregation terms = Aggregation.builder()
                .name("content_types").type("sterms").buckets(List.of(blog, news)).build();
        // A metric-style aggregation with no buckets must be excluded from the flat map.
        final Aggregation topHits = Aggregation.builder()
                .name("top_content").type("top_hits").hits(SearchHits.empty()).build();

        final Map<String, Aggregation> tree = new LinkedHashMap<>();
        tree.put("content_types", terms);
        tree.put("top_content", topHits);

        final ContentSearchResponse response = ContentSearchResponse.builder()
                .hits(SearchHits.empty()).tookMillis(0).aggregationTree(tree).build();

        final Map<String, List<AggregationBucket>> flat = response.aggregations();
        assertTrue("terms agg must be flattened", flat.containsKey("content_types"));
        assertFalse("bucket-less top_hits must be excluded from the flat map",
                flat.containsKey("top_content"));
        assertEquals(2, flat.get("content_types").size());
        assertEquals(7L, flat.get("content_types").get(0).docCount());

        // Tree view keeps everything, including the metric aggregation.
        assertTrue(response.aggregationTree().containsKey("top_content"));
        assertNotNull(response.aggregationTree().get("top_content").getHits());
    }

    /**
     * A {@code terms} aggregation with an empty result must still appear in the flat map (with an
     * empty bucket list) — callers rely on the declared aggregation key being present even when no
     * documents matched. Regression guard for #36026 (the flat map must not drop empty-bucket terms).
     */
    @Test
    public void contentSearchResponse_flatMap_keepsEmptyTermsAggregation() {
        final Aggregation emptyTerms = Aggregation.builder()
                .name("entries").type("sterms").build(); // no buckets, no hits

        final Map<String, Aggregation> tree = new LinkedHashMap<>();
        tree.put("entries", emptyTerms);

        final ContentSearchResponse response = ContentSearchResponse.builder()
                .hits(SearchHits.empty()).tookMillis(0).aggregationTree(tree).build();

        assertTrue("empty terms aggregation key must be present",
                response.aggregations().containsKey("entries"));
        assertTrue("its bucket list must be empty", response.aggregations().get("entries").isEmpty());
    }

    /** Nested sub-aggregations survive on a bucket and are reachable via getAggregations(). */
    @Test
    public void aggregationBucket_nestedSubAggregations_areReachable() {
        final Aggregation nested = Aggregation.builder()
                .name("top_content").type("top_hits").hits(SearchHits.empty()).build();
        final AggregationBucket bucket = AggregationBucket.builder()
                .key("Blog").docCount(7)
                .subAggregations(Map.of("top_content", nested))
                .build();

        assertNotNull(bucket.getAggregations().get("top_content"));
        assertEquals("top_hits", bucket.getAggregations().get("top_content").getType());
    }

    /**
     * Jackson serialization contract for the vendor-neutral {@link ContentSearchResponse} DTO.
     *
     * <p>Note: {@code POST /api/es/raw} and the {@code esresponse} field of {@code /api/es/search} now
     * emit the legacy Elasticsearch-wire shape via {@code ESContentResourcePortlet#toLegacyEsJson()}
     * (for backward compatibility / rollback safety), so this neutral Jackson form is the DTO's
     * canonical/internal representation rather than a REST wire contract. This test still pins that
     * neutral form with the dotCMS default {@link ObjectMapper} so a change to the DTO (or the mapper
     * config) is caught here.</p>
     *
     * <p>Key guarantees: the top-level object carries {@code hits}, {@code tookMillis} and
     * {@code aggregationTree}; {@code hits} is a nested object with a {@code hits} array and
     * {@code totalHits} — NOT a bare array (a real risk because {@link SearchHits} implements
     * {@link Iterable}); and each hit exposes {@code id} and {@code sourceAsMap}.</p>
     */
    @Test
    public void contentSearchResponse_jacksonSerializesNeutralShape() throws Exception {
        // The dotCMS default mapper (the DTO's canonical neutral serialization).
        final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();

        final SearchHit hit = SearchHit.builder()
                .id("abc123").index("live_idx")
                .sourceAsMap(Map.of("title", "hello"))
                .score(1.5f).build();
        final SearchHits hits = SearchHits.builder()
                .addHits(hit)
                .totalHits(TotalHits.builder().value(1L).build())
                .build();
        final Aggregation terms = Aggregation.builder()
                .name("content_types").type("sterms")
                .buckets(List.of(AggregationBucket.builder().key("Blog").docCount(7).build()))
                .build();
        final ContentSearchResponse response = ContentSearchResponse.builder()
                .hits(hits).tookMillis(42L)
                .aggregationTree(Map.of("content_types", terms))
                .suggest(Map.of("titleSuggest", List.of()))
                .build();

        final JsonNode root = mapper.readTree(mapper.writeValueAsString(response));

        assertEquals("tookMillis must round-trip", 42L, root.get("tookMillis").asLong());
        assertTrue("aggregationTree must be present", root.has("aggregationTree"));
        assertTrue("declared aggregation must survive serialization",
                root.path("aggregationTree").has("content_types"));
        // The Velocity-only aliases getAggregations()/getTookInMillis()/getSuggest() must NOT leak into
        // the neutral Jackson JSON — the tree is single-sourced as aggregationTree, timing as tookMillis,
        // and suggestions are wire-omitted. The class-level @JsonIgnoreProperties (not method-level
        // @JsonIgnore) is what suppresses them here, so they stay visible to the reflection-based
        // $json.generate() path exercised in
        // jsonGenerate_ofResponse_preservesAggregationsForVelocityNavigation (issue #36435).
        assertFalse("getAggregations() must not double-emit an 'aggregations' key in the neutral JSON",
                root.has("aggregations"));
        assertFalse("getTookInMillis() must not add a 'tookInMillis' key (timing stays as tookMillis)",
                root.has("tookInMillis"));
        assertFalse("getSuggest() must not add a 'suggest' key to the neutral JSON",
                root.has("suggest"));

        // hits must be a nested object, not a bare array (SearchHits implements Iterable).
        final JsonNode hitsNode = root.get("hits");
        assertNotNull("hits object must be present", hitsNode);
        assertTrue("hits must serialize as an object, not an array", hitsNode.isObject());
        assertTrue("hits.hits array must be present", hitsNode.path("hits").isArray());
        assertTrue("totalHits must be present", hitsNode.has("totalHits"));
        assertEquals("the single hit id must be preserved",
                "abc123", hitsNode.path("hits").get(0).path("id").asText());
        assertEquals("the hit source document must be preserved",
                "hello", hitsNode.path("hits").get(0).path("sourceAsMap").path("title").asText());
    }

    /**
     * Anti-regression for the {@code value()} / {@code getValue()} pair on {@link TotalHits}. The
     * record component accessor ({@code value()}, annotated {@code @JsonProperty("value")}) and the
     * Velocity back-compat getter ({@code getValue()}) both map to the logical property {@code value},
     * so Jackson must merge them into a <b>single</b> {@code value} field — it must not emit a
     * duplicate nor throw a conflicting-getter/duplicate-property error. Serializes with the exact
     * default mapper the {@code /api/es} endpoints use, guarding against the (invalid) "duplicate
     * field" concern for good.
     */
    @Test
    public void totalHits_serializesValueOnce_despiteBackCompatGetter() throws Exception {
        final ObjectMapper mapper = DotObjectMapperProvider.createDefaultMapper();
        final TotalHits total = TotalHits.builder().value(7L).build();

        // Must not throw: value() + getValue() do not create a conflicting/duplicate property.
        final String json = mapper.writeValueAsString(total);

        // Exactly one "value" key — the back-compat getter must not double-emit it.
        final int valueKeyOccurrences = json.split("\"value\"", -1).length - 1;
        assertEquals("TotalHits must serialize 'value' exactly once (no duplicate from getValue())",
                1, valueKeyOccurrences);

        final JsonNode node = mapper.readTree(json);
        assertEquals("serialized 'value' must carry the correct number", 7L, node.get("value").asLong());
        assertEquals("value() and getValue() must return the same number",
                total.value(), total.getValue());
    }

    // =========================================================================
    // Elasticsearch factory conversion (Aggregation.from / AggregationBucket.from*)
    // =========================================================================
    //
    // These exercise the vendor-specific ES → neutral conversion deterministically (no search
    // engine, no container): the mocked ES aggregation objects mirror exactly what the live ES
    // client hands the factories, so the conversion is locked down here and not only end-to-end.

    /**
     * A {@code date_histogram} bucket key is a {@link ZonedDateTime} in Elasticsearch 7.x — NOT a
     * number. {@link AggregationBucket#fromHistogram} must normalize it to epoch-millis so that
     * {@code getKeyAsNumber()} (and the legacy {@code InternalWrapperCountDateHistogramFacet} that
     * reads it) returns a real timestamp rather than null/0. This is the trickiest branch of the
     * neutral conversion and the one with no obvious end-to-end equivalent, so it is pinned here.
     */
    @Test
    public void esFactory_dateHistogram_normalizesZonedDateTimeKeyToEpochMillis() {
        final ZonedDateTime day = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        final long expectedEpochMillis = day.toInstant().toEpochMilli();

        final Aggregations emptySubAggs = emptyEsAggregations();
        final Histogram.Bucket bucket = mock(Histogram.Bucket.class);
        when(bucket.getKey()).thenReturn(day);            // ES 7.x date-histogram key type
        when(bucket.getDocCount()).thenReturn(4L);
        when(bucket.getAggregations()).thenReturn(emptySubAggs);

        final Histogram histogram = mock(Histogram.class);
        when(histogram.getName()).thenReturn("by_day");
        when(histogram.getType()).thenReturn("date_histogram");
        doReturn(List.of(bucket)).when(histogram).getBuckets();

        final Aggregations esAggs = mock(Aggregations.class);
        when(esAggs.asList()).thenReturn(List.of(histogram));

        final Aggregation byDay = Aggregation.from(esAggs).get("by_day");
        assertNotNull("date_histogram aggregation must be mapped", byDay);
        assertEquals("date_histogram", byDay.getType());
        assertEquals("one bucket expected", 1, byDay.getBuckets().size());

        final AggregationBucket b = byDay.getBuckets().get(0);
        assertEquals("doc count must round-trip", 4L, b.getDocCount());
        assertEquals("a ZonedDateTime key must become epoch-millis, not a formatted date",
                expectedEpochMillis, b.getKeyAsNumber().longValue());
        assertEquals("getKeyAsString must expose the same epoch-millis",
                String.valueOf(expectedEpochMillis), b.getKeyAsString());
    }

    /**
     * A numeric {@code histogram} bucket key is a {@link Number} (a {@code Double} in ES); the
     * conversion must take the {@code longValue()} branch of {@code histogramKey} and yield that
     * number as the key.
     */
    @Test
    public void esFactory_numericHistogram_normalizesNumberKeyToLong() {
        final Aggregations emptySubAggs = emptyEsAggregations();
        final Histogram.Bucket bucket = mock(Histogram.Bucket.class);
        when(bucket.getKey()).thenReturn(Double.valueOf(50.0));   // ES numeric-histogram key type
        when(bucket.getDocCount()).thenReturn(2L);
        when(bucket.getAggregations()).thenReturn(emptySubAggs);

        final Histogram histogram = mock(Histogram.class);
        when(histogram.getName()).thenReturn("by_len");
        when(histogram.getType()).thenReturn("histogram");
        doReturn(List.of(bucket)).when(histogram).getBuckets();

        final Aggregations esAggs = mock(Aggregations.class);
        when(esAggs.asList()).thenReturn(List.of(histogram));

        final AggregationBucket b = Aggregation.from(esAggs).get("by_len").getBuckets().get(0);
        assertEquals("a numeric key must be preserved as a long", 50L, b.getKeyAsNumber().longValue());
        assertEquals("50", b.getKeyAsString());
    }

    /**
     * A {@code terms} aggregation maps every bucket through {@link AggregationBucket#from}: the
     * String key round-trips on {@code getKey()}/{@code getKeyAsString()}, a non-numeric key yields
     * a null number, doc counts survive, and a metric-less terms aggregation carries no top-hits.
     */
    @Test
    public void esFactory_terms_mapsBucketsAndIsHitsFree() {
        final Aggregations emptySubAggs = emptyEsAggregations();
        final Terms.Bucket esBucket = mock(Terms.Bucket.class);
        when(esBucket.getKeyAsString()).thenReturn("text/html");
        when(esBucket.getDocCount()).thenReturn(3L);
        when(esBucket.getAggregations()).thenReturn(emptySubAggs);

        final Terms terms = mock(Terms.class);
        when(terms.getName()).thenReturn("by_mime");
        when(terms.getType()).thenReturn("sterms");
        doReturn(List.of(esBucket)).when(terms).getBuckets();

        final Aggregations esAggs = mock(Aggregations.class);
        when(esAggs.asList()).thenReturn(List.of(terms));

        final Aggregation byMime = Aggregation.from(esAggs).get("by_mime");
        assertNotNull(byMime);
        assertEquals("sterms", byMime.getType());
        assertNull("a terms aggregation carries no top-hits", byMime.getHits());
        assertEquals(1, byMime.getBuckets().size());

        final AggregationBucket b = byMime.getBuckets().get(0);
        assertEquals("text/html", b.getKey());
        assertEquals("text/html", b.getKeyAsString());
        assertNull("a non-numeric key must yield a null number", b.getKeyAsNumber());
        assertEquals(3L, b.getDocCount());
        assertTrue("no nested sub-aggregations here", b.getAggregations().isEmpty());
    }

    /** A null Elasticsearch aggregation set maps to an empty tree rather than throwing. */
    @Test
    public void esFactory_nullAggregations_yieldEmptyMap() {
        assertTrue("null ES aggregations must map to an empty tree",
                Aggregation.from((Aggregations) null).isEmpty());
    }

    /**
     * The {@code meta} object set on an aggregation in the query is preserved on the neutral type
     * via {@code getMetadata()} — closing the last equivalence gap with the ES {@code Aggregation}
     * interface ({@code getName}/{@code getType}/{@code getMetadata}). This accessor is rollback-safe
     * because the ES type exposes the same method, so a template adopting {@code $agg.metadata}
     * resolves on both N (neutral) and N-1 (ES).
     */
    @Test
    public void esFactory_metadata_isPreserved() {
        final Map<String, Object> meta = Map.of("unit", "days", "version", 2);
        final Terms terms = mock(Terms.class);
        when(terms.getName()).thenReturn("by_day");
        when(terms.getType()).thenReturn("sterms");
        when(terms.getMetadata()).thenReturn(meta);
        doReturn(List.of()).when(terms).getBuckets();

        final Aggregations esAggs = mock(Aggregations.class);
        when(esAggs.asList()).thenReturn(List.of(terms));

        assertEquals("the aggregation meta map must round-trip from ES",
                meta, Aggregation.from(esAggs).get("by_day").getMetadata());
    }

    /** {@code getMetadata()} is never null — it defaults to an empty map when no meta was set. */
    @Test
    public void aggregation_metadata_defaultsToEmptyWhenUnset() {
        final Aggregation agg = Aggregation.builder().name("x").type("sterms").build();
        assertNotNull("metadata must never be null", agg.getMetadata());
        assertTrue("metadata defaults to empty when unset", agg.getMetadata().isEmpty());
    }

    // =========================================================================
    // $json.generate($rawResults.response) reflection navigation — issue #36435
    // =========================================================================
    //
    // Some templates do NOT navigate the Velocity object directly (the path #36026/#36027
    // restored); they first round-trip the raw response through JSONTool.generate(Object), i.e.
    // `#set($results = $json.generate($rawResults.response))`, and then walk the resulting
    // JSONObject: `$results.aggregations.<name>.buckets`. JSONTool.generate(Object) is literally
    // `new com.dotmarketing.util.json.JSONObject(bean)` — a reflection/bean constructor that only
    // picks up public zero-arg `getX()`/`isX()` accessors and (crucially) SKIPS any accessor
    // annotated with Jackson's @JsonIgnore. This test exercises that exact reflection path against
    // the neutral ContentSearchResponse so the regression is pinned as a fast unit test.

    /**
     * Reproduction / regression for <a href="https://github.com/dotCMS/core/issues/36435">#36435</a>.
     *
     * <p>A customer's {@code sitemap.vtl} does {@code #set($results = $json.generate($rawResults.response))}
     * and then {@code #foreach($folder in $results.aggregations.<name>.buckets)}. Before the ES→OS
     * migration the raw response was an ES {@code SearchResponse} bean and its {@code getAggregations()}
     * (and {@code getTookInMillis()}, {@code getSuggest()}) reflected into the JSON, so those survived
     * the {@code $json.generate()} hop. After the migration the raw response is
     * {@link ContentSearchResponse}; those Velocity aliases carried a method-level {@code @JsonIgnore}
     * (to keep the neutral Jackson wire single-sourced), and the dotCMS-vendored {@link JSONObject} bean
     * constructor honours {@code @JsonIgnore} too — so they vanished from the generated JSON and the
     * {@code #foreach} silently iterated zero times.</p>
     *
     * <p>Contract this pins: running the response through the same reflection constructor
     * {@code JSONTool.generate(Object)} uses must expose the Velocity-alias family — the aggregations
     * navigable as {@code aggregations.<name>.buckets} down to each bucket's {@code key}/{@code docCount},
     * plus {@code tookInMillis} and {@code suggest}. All three moved from a method-level
     * {@code @JsonIgnore} to a class-level {@code @JsonIgnoreProperties} that only Jackson honours.</p>
     */
    @Test
    public void jsonGenerate_ofResponse_preservesAggregationsForVelocityNavigation() {
        final AggregationBucket about = AggregationBucket.builder().key("/about-us/").docCount(12).build();
        final AggregationBucket products = AggregationBucket.builder().key("/products/").docCount(7).build();
        final Aggregation folders = Aggregation.builder()
                .name("folders").type("sterms").buckets(List.of(about, products)).build();

        final ContentSearchResponse response = ContentSearchResponse.builder()
                .hits(SearchHits.empty()).tookMillis(5L)
                .aggregationTree(Map.of("folders", folders))
                .suggest(Map.of("titleSuggest", List.of()))
                .build();

        // Exactly what JSONTool.generate(Object o) does: new JSONObject(o).
        final JSONObject json = new JSONObject(response);

        assertTrue("aggregations must survive the $json.generate() reflection hop (issue #36435)",
                json.has("aggregations"));

        final JSONObject aggs = json.getJSONObject("aggregations");
        assertTrue("the declared 'folders' aggregation must be present after JSON generation",
                aggs.has("folders"));

        final JSONArray buckets = aggs.getJSONObject("folders").getJSONArray("buckets");
        assertEquals("both folder buckets must survive", 2, buckets.length());
        assertEquals("/about-us/", buckets.getJSONObject(0).getString("key"));
        assertEquals(12L, buckets.getJSONObject(0).getLong("docCount"));
        assertEquals("/products/", buckets.getJSONObject(1).getString("key"));

        // The sibling Velocity aliases that shared the same latent regression (issue #36435) must also
        // survive the reflection hop: getTookInMillis() and getSuggest().
        assertEquals("tookInMillis must survive the $json.generate() reflection hop", 5L,
                json.getLong("tookInMillis"));
        assertTrue("suggest must survive the $json.generate() reflection hop", json.has("suggest"));
    }

    /** An empty (but non-null) Elasticsearch aggregation set whose buckets carry no sub-aggs. */
    private static Aggregations emptyEsAggregations() {
        final Aggregations aggs = mock(Aggregations.class);
        when(aggs.asList()).thenReturn(List.of());
        return aggs;
    }
}
