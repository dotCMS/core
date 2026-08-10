package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for {@link OSSiteSearchAPI#searchBody(String, boolean, int, int)}, the OpenSearch
 * request body Site Search sends.
 *
 * <p>The subject is the {@code highlight} clause: its fragments are what put the {@code <em>}
 * emphasis in a result snippet, and losing them on the OpenSearch read path is issue #36360. The
 * text-query shape always asked for them; the JSON shape did not, while the Elasticsearch path
 * ({@code ESSiteSearchAPI}) adds a default clause for <em>both</em> — so the same query rendered
 * emphasised on ES and plain on OS.</p>
 */
public class OSSiteSearchBodyTest {

    private static final String HIGHLIGHTED_FIELD = "content";

    /** The field names a body's highlight clause covers. */
    private static JSONObject highlightedFields(final JSONObject body) {
        return body.getJSONObject("highlight").getJSONObject("fields");
    }

    /**
     * Method to test: {@link OSSiteSearchAPI#searchBody(String, boolean, int, int)}
     * Given scenario: a plain text query, the shape {@code $sitesearch.search($alias, "dotcms", …)}
     *          produces.
     * Expected result: the query is wrapped in a {@code query_string}, paging is applied, and the
     *          {@code content} field is highlighted with the explicit fragment size the ES path uses.
     */
    @Test
    public void searchBody_textQuery_highlightsContentWithFragmentSize() {
        final JSONObject body = OSSiteSearchAPI.searchBody("dotcms", false, 20, 10);

        assertEquals("dotcms",
                body.getJSONObject("query").getJSONObject("query_string").getString("query"));
        assertEquals(10, body.getInt("size"));
        assertEquals(20, body.getInt("from"));
        assertEquals("the fragment size must be the one the Elasticsearch path sends",
                255, highlightedFields(body).getJSONObject(HIGHLIGHTED_FIELD).getInt("fragment_size"));
    }

    /**
     * Method to test: {@link OSSiteSearchAPI#searchBody(String, boolean, int, int)}
     * Given scenario: a caller-supplied JSON body with no {@code highlight} clause — the common case
     *          for {@code $sitesearch.search($alias, $jsonQuery, …)} from a template.
     * Expected result: a default clause on {@code content} is added on the caller's behalf, exactly
     *          as {@code ESSiteSearchAPI} does. Without it the snippets come back unemphasised in
     *          every phase where OpenSearch serves reads (issue #36360).
     */
    @Test
    public void searchBody_jsonQueryWithoutHighlight_getsDefaultClause() {
        final String json = "{\"query\":{\"match_all\":{}}}";

        final JSONObject body = OSSiteSearchAPI.searchBody(json, true, 0, 0);

        assertTrue("a JSON body must still be highlighted, as the ES path does",
                highlightedFields(body).has(HIGHLIGHTED_FIELD));
        assertTrue("the caller's own query must be left intact", body.has("query"));
    }

    /**
     * Method to test: {@link OSSiteSearchAPI#searchBody(String, boolean, int, int)}
     * Given scenario: a JSON body that already declares its own {@code highlight} clause, on a
     *          different field and with its own fragment size.
     * Expected result: it is left exactly as the caller wrote it — the default is a fallback, not an
     *          override.
     */
    @Test
    public void searchBody_jsonQueryWithHighlight_isLeftUntouched() {
        final String json = "{\"query\":{\"match_all\":{}},"
                + "\"highlight\":{\"fields\":{\"title\":{\"fragment_size\":42}}}}";

        final JSONObject body = OSSiteSearchAPI.searchBody(json, true, 0, 0);

        final JSONObject fields = highlightedFields(body);
        assertEquals("the caller's highlight clause must survive untouched",
                42, fields.getJSONObject("title").getInt("fragment_size"));
        assertEquals("no field may be added to a caller-supplied clause", 1, fields.length());
    }

    /**
     * Method to test: {@link OSSiteSearchAPI#searchBody(String, boolean, int, int)}
     * Given scenario: a JSON body whose <em>document values</em> merely contain the word "highlight",
     *          with no highlight clause of its own.
     * Expected result: the default clause is still added. The Elasticsearch path greps the raw query
     *          string, so this query silently loses its highlighting there; reading the parsed body
     *          instead is what makes the check mean what it says.
     */
    @Test
    public void searchBody_jsonQueryMentioningHighlightInAValue_stillGetsDefaultClause() {
        final String json = "{\"query\":{\"query_string\":{\"query\":\"highlight\"}}}";

        final JSONObject body = OSSiteSearchAPI.searchBody(json, true, 0, 0);

        assertTrue("a document value must not be mistaken for a highlight clause",
                highlightedFields(body).has(HIGHLIGHTED_FIELD));
    }
}
