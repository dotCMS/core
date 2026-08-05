package com.dotcms.content.index.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.search.Hit;

/**
 * Unit tests for {@link SearchHit} conversion from vendor types, focused on the per-hit
 * {@code sortValues} mapping restored for
 * <a href="https://github.com/dotCMS/core/issues/36581">#36581</a>. Exercises the OpenSearch
 * {@code Hit.sort()} unwrap branch (a {@code List<FieldValue>} tagged union), which the ES-only
 * integration environment does not cover.
 */
public class SearchHitTest {

    /**
     * Method to test: {@link SearchHit#from(Hit)}
     * Given scenario: an OpenSearch hit whose {@code sort()} carries a double and a string FieldValue.
     * Expected result: {@code getSortValues()} unwraps each FieldValue to its raw scalar, in order.
     */
    @Test
    public void from_openSearchHit_unwrapsSortValues() {
        final Hit<Object> osHit = Hit.of(builder -> builder
                .index("idx")
                .id("1")
                .sort(Arrays.asList(FieldValue.of(11.82d), FieldValue.of("abc"))));

        final SearchHit hit = SearchHit.from(osHit);

        final List<Object> sortValues = hit.getSortValues();
        assertEquals("both sort values must survive the conversion", 2, sortValues.size());
        assertEquals(11.82d, ((Number) sortValues.get(0)).doubleValue(), 0.0001d);
        assertEquals("abc", sortValues.get(1));
    }

    /**
     * Method to test: {@link SearchHit#from(Hit)}
     * Given scenario: an OpenSearch hit with no {@code sort()} values (a relevance-only query).
     * Expected result: {@code getSortValues()} is empty (never null), so serializers omit the
     *          {@code sort} key instead of emitting an empty array.
     */
    @Test
    public void from_openSearchHit_noSort_yieldsEmptySortValues() {
        final Hit<Object> osHit = Hit.of(builder -> builder.index("idx").id("1"));

        final SearchHit hit = SearchHit.from(osHit);

        assertTrue("a hit without a sort clause must expose empty sort values",
                hit.getSortValues().isEmpty());
    }

    /**
     * Method to test: {@link SearchHit#from(Hit)}
     * Given scenario: the {@code sort()} list contains a raw Java {@code null} element (defensive
     *          against a client that yields nulls).
     * Expected result: the conversion does not NPE and maps the null element to a null entry.
     */
    @Test
    public void from_openSearchHit_nullSortElement_doesNotThrow() {
        final Hit<Object> osHit = Hit.of(builder -> builder
                .index("idx")
                .id("1")
                .sort(Arrays.asList(FieldValue.of(1.0d), null)));

        final SearchHit hit = SearchHit.from(osHit);

        final List<Object> sortValues = hit.getSortValues();
        assertEquals(2, sortValues.size());
        assertEquals(1.0d, ((Number) sortValues.get(0)).doubleValue(), 0.0001d);
        assertTrue("a null FieldValue element must map to null, not throw", sortValues.get(1) == null);
    }

    /**
     * Method to test: {@link SearchHit#from(Hit)}
     * Given scenario: an OpenSearch hit carrying highlight fragments for the {@code content} field,
     *          as Site Search requests them.
     * Expected result: the fragments survive the conversion, so search-result snippets keep their
     *          emphasis in the phases where OpenSearch serves reads (issue #36360).
     */
    @Test
    public void from_openSearchHit_carriesHighlightFragments() {
        final Hit<Object> osHit = Hit.of(builder -> builder
                .index("idx")
                .id("1")
                .highlight(Map.of("content",
                        Arrays.asList("an <em>argue</em>ment", "and <em>argues</em> again"))));

        final SearchHit hit = SearchHit.from(osHit);

        assertTrue("a highlighted hit must take the highlight-bearing shape",
                hit instanceof SiteSearchHit);
        final List<String> fragments = hit.highlightsFor("content");
        assertEquals("both fragments must survive the conversion", 2, fragments.size());
        assertTrue(fragments.get(0).contains("<em>argue</em>"));
        assertTrue(fragments.get(1).contains("<em>argues</em>"));
    }

    /**
     * Method to test: {@link SearchHit#from(Hit)}
     * Given scenario: a hit from a query that never asked for highlighting — i.e. every content search.
     * Expected result: the lean {@link ContentSearchHit} shape, which carries no highlight state at
     *          all. This is the invariant that keeps the content path from paying for a Site Search
     *          concern, so it is asserted rather than assumed.
     */
    @Test
    public void from_openSearchHit_noHighlight_yieldsLeanContentShape() {
        final Hit<Object> osHit = Hit.of(builder -> builder.index("idx").id("1"));

        final SearchHit hit = SearchHit.from(osHit);

        assertTrue("an un-highlighted hit must take the lean shape",
                hit instanceof ContentSearchHit);
        assertTrue("the builder default must behave the same",
                SearchHit.builder().id("1").build() instanceof ContentSearchHit);
    }

    /**
     * Method to test: {@link SearchHit#highlightsFor(String)}
     * Given scenario: a hit with no highlighting at all, and a field that was never highlighted.
     * Expected result: an empty list either way — callers turn this straight into an array, so a
     *          null would NPE.
     */
    @Test
    public void highlightsFor_absentField_yieldsEmptyList() {
        final Hit<Object> osHit = Hit.of(builder -> builder.index("idx").id("1"));

        final SearchHit hit = SearchHit.from(osHit);

        assertTrue("an un-highlighted field must yield an empty list, not null",
                hit.highlightsFor("content").isEmpty());
        assertTrue("the builder default must behave the same",
                SearchHit.builder().id("1").build().highlightsFor("content").isEmpty());
    }

    /**
     * Method to test: {@link SearchHit#fromJson}
     * Given scenario: a Site Search hit carrying highlight fragments is serialized and read back
     *          through the {@link SearchHit} interface.
     * Expected result: it returns as the highlight-bearing shape with its fragments intact. Pinning
     *          deserialization to one implementation would drop them silently instead — the whole
     *          reason the creator rebuilds through the builder.
     */
    @Test
    public void jacksonRoundTrip_highlightedHit_keepsFragments() throws Exception {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
        final SearchHit hit = SearchHit.builder()
                .id("abc123")
                .index("sitesearch_1")
                .highlights(Map.of("content", List.of("an <em>argue</em>ment")))
                .build();
        assertTrue("precondition: the builder must pick the highlight-bearing shape",
                hit instanceof SiteSearchHit);

        final SearchHit back = mapper.readValue(mapper.writeValueAsString(hit), SearchHit.class);

        assertTrue("a highlighted hit must come back as the highlight-bearing shape",
                back instanceof SiteSearchHit);
        assertEquals("the fragments must survive the round-trip",
                List.of("an <em>argue</em>ment"), back.highlightsFor("content"));
        assertEquals("abc123", back.getId());
    }

    /**
     * Method to test: {@link SearchHit#fromJson}
     * Given scenario: a content hit — no highlight key in the JSON — read back through the interface.
     * Expected result: the lean shape, so the round-trip does not quietly widen every cached hit into
     *          the Site Search one.
     */
    @Test
    public void jacksonRoundTrip_contentHit_staysLean() throws Exception {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
        final SearchHit hit = SearchHit.builder().id("abc123").index("live_1").build();

        final SearchHit back = mapper.readValue(mapper.writeValueAsString(hit), SearchHit.class);

        assertTrue("an un-highlighted hit must come back lean", back instanceof ContentSearchHit);
        assertEquals("abc123", back.getId());
    }
}
