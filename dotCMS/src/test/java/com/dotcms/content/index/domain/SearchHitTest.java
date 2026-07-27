package com.dotcms.content.index.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
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
}
