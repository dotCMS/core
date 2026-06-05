package com.dotcms.content.index.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
}
