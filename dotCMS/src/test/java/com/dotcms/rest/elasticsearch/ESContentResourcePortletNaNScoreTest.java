package com.dotcms.rest.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.index.domain.Relation;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.domain.TotalHits;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Regression coverage for <a href="https://github.com/dotCMS/core/issues/36478">#36478</a>.
 *
 * <p>After the phase-aware SearchAPI cutover (#36398), {@code /api/es/search} rebuilds the legacy
 * Elasticsearch-wire response through dotCMS's {@link JSONObject}. Elasticsearch returns a
 * non-finite {@code _score} ({@code NaN}) for hits that are not relevance-scored (field-sorted
 * queries, filter/{@code constant_score}/aggregation-only contexts). {@code JSONObject.put(...)}
 * rejects non-finite numbers with "JSON does not allow non-finite numbers", producing an HTTP 500.
 *
 * <p>These fast unit tests drive the private {@code hitsToLegacyJson} mapping directly and assert
 * that a {@code NaN} score serializes as {@code null} (matching ES's native wire format) instead of
 * throwing, while finite scores are preserved unchanged.
 */
public class ESContentResourcePortletNaNScoreTest {

    private static JSONObject hitsToLegacyJson(final SearchHits hits) throws Exception {
        final Method method = ESContentResourcePortlet.class
                .getDeclaredMethod("hitsToLegacyJson", SearchHits.class);
        method.setAccessible(true);
        try {
            return (JSONObject) method.invoke(null, hits);
        } catch (final InvocationTargetException e) {
            // Surface the real cause (e.g. the pre-fix JSONException) to the test.
            throw (Exception) e.getCause();
        }
    }

    private static SearchHits hitsWithScore(final float score) {
        final SearchHit hit = SearchHit.builder()
                .id("abc123")
                .index("live_index")
                .score(score)
                .sourceAsMap(Map.of("title", "example"))
                .build();
        return new SearchHits(false, List.of(hit), new TotalHits(1L, Relation.EQUAL_TO));
    }

    /**
     * A {@code NaN} hit score (field-sorted / aggregation query) must not throw and must serialize
     * as {@code _score: null}. Before the fix this threw
     * {@code JSONException("JSON does not allow non-finite numbers.")}.
     */
    @Test
    public void test_nan_score_serializes_as_null() throws Exception {
        final JSONObject result = hitsToLegacyJson(hitsWithScore(Float.NaN));

        final JSONObject firstHit = result.getJSONArray("hits").getJSONObject(0);
        assertTrue("_score must be null for a non-finite (NaN) score", firstHit.isNull("_score"));

        // The whole response must be serializable — the production 500 happened on write/put.
        final String json = result.toString();
        assertTrue("serialized response must contain a null _score",
                json.contains("\"_score\":null") || json.contains("\"_score\": null"));
    }

    /** Positive/negative infinity is likewise coerced to {@code null} rather than throwing. */
    @Test
    public void test_infinite_score_serializes_as_null() throws Exception {
        final JSONObject result = hitsToLegacyJson(hitsWithScore(Float.POSITIVE_INFINITY));
        final JSONObject firstHit = result.getJSONArray("hits").getJSONObject(0);
        assertTrue("_score must be null for a non-finite (Infinity) score", firstHit.isNull("_score"));
    }

    /** A normal relevance score must be preserved unchanged. */
    @Test
    public void test_finite_score_is_preserved() throws Exception {
        final JSONObject result = hitsToLegacyJson(hitsWithScore(1.5f));

        final JSONArray arr = result.getJSONArray("hits");
        final JSONObject firstHit = arr.getJSONObject(0);
        assertTrue("_score must be present for a finite score", !firstHit.isNull("_score"));
        assertEquals(1.5d, firstHit.getDouble("_score"), 0.0001d);
    }
}
