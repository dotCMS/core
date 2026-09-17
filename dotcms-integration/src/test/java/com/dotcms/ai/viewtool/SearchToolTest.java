package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiTest;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains unit tests for the SearchTool class.
 * Each method in this class corresponds to a method in the SearchTool class.
 *
 * The tests are designed to check the functionality of the SearchTool class
 * and ensure that it behaves as expected in various scenarios.
 *
 * @author vico
 */
public class SearchToolTest {

    /** Title carried by the probe embedding rows; must come back untouched (contentlet field). */
    private static final String PROBE_TITLE = AiTest.PROBE_MARKUP + " title";
    /** Dedicated index for the probe rows so they can never appear in another test's results. */
    private static final String PROBE_INDEX = "escape-probe";

    private Host host;
    private SearchTool searchTool;
    private SearchTool unsafeSearchTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        AiTest.aiAppSecretsWithProviderConfig(APILocator.systemHost(), AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
    }

    @Before
    public void before() throws Exception {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        host = new SiteDataGen().nextPersisted();
        searchTool = prepareSearchTool(viewContext);
        unsafeSearchTool = prepareSearchTool(viewContext, false);
        AiTest.aiAppSecretsWithProviderConfig(host, AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
    }

    /**
     * Feature: Query with Index Name
     * Scenario: User queries with a specific index name
     * Given a user has a query and a specific index name
     * When the user performs a search using the query and index name
     * Then the system should return the search results related to the query from the specified index
     */
    @Test
    public void test_query_withIndexName() {
        final String query = "Facts about Nikola Tesla";
        EmbeddingsDTODataGen.persistEmbeddings(query, null, "default");

        final JSONObject result = (JSONObject) searchTool.query(query, "default");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(query, result.getString("query"));
        assertFalse(result.getJSONArray("dotCMSResults").isEmpty());
    }

    /**
     * Feature: Query without Index Name
     * Scenario: User queries without specifying an index name
     * Given a user has a query but does not specify an index name
     * When the user performs a search using the query
     * Then the system should return the search results related to the query from the default index
     */
    @Test
    public void test_query() {
        final String query = "Facts about Nikola Tesla";
        EmbeddingsDTODataGen.persistEmbeddings(query, null, "default");

        final JSONObject result = (JSONObject) searchTool.query(query);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(query, result.getString("query"));
        assertFalse(result.getJSONArray("dotCMSResults").isEmpty());
    }

    /**
     * Feature: Query using Map
     * Scenario: User queries using a map of parameters
     * Given a user has a map of parameters including a query and possibly an index name, limit, and threshold
     * When the user performs a search using the map of parameters
     * Then the system should return the search results related to the query based on the parameters in the map
     */
    @Test
    public void test_query_usingMap() {
        final String query = "Brief description of theory of relativity";
        EmbeddingsDTODataGen.persistEmbeddings(query, null, "default");

        final JSONObject result = (JSONObject) searchTool.query(
                Map.of(
                        "query", query,
                        "indexName", "default",
                        "limit", 50,
                        "threshold", .25f));
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(query, result.getString("query"));
        assertFalse(result.getJSONArray("dotCMSResults").isEmpty());
    }

    /**
     * Feature: Related Content Search
     * Scenario: User retrieves related content for a specific Contentlet and index name
     * Given a user has a specific Contentlet and index name
     * When the user retrieves related content using the Contentlet and index name
     * Then the system should return the search results related to the Contentlet from the specified index
     */
    @Test
    public void test_related() {
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);
        final Field field = new FieldDataGen()
                .type(TextField.class)
                .indexed(true)
                .unique(true)
                .next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(htmlPageAsset.getIdentifier())
                .urlMapPattern("/test with blank space/{" + field.variable() + "}")
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "Testing_1")
                .nextPersisted();
        ContentletDataGen.publish(contentlet);

        final String query = "Elaborate about the 'Tour de France'";
        EmbeddingsDTODataGen.persistEmbeddings(query, null, "default");

        final JSONObject result = (JSONObject) searchTool.related(contentlet, "default");
        assertNotNull(result);
    }

    // ------------------------------------------------------------------------------------------
    // #37153: escaped by default, raw only through the unsafe construction
    // ------------------------------------------------------------------------------------------

    /**
     * Feature: Search output is escaped by default and raw through the unsafe tool (#37153, AC-002 / AC-003)
     * Given a row whose excerpt and title carry markup
     * When query(String,String) and query(Map) are called on the default tool
     * Then the query echo and the match's extractedText are HTML-escaped, the title is returned as stored
     * and the operator metadata is untouched; the unsafe tool returns the excerpt and the echo literally
     */
    @Test
    public void test_query_escapedByDefault_rawThroughUnsafe() {
        final String text = "Escaping probe search " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text, PROBE_INDEX);

        for (final JSONObject escaped : List.of(
                (JSONObject) searchTool.query(text, PROBE_INDEX),
                (JSONObject) searchTool.query(Map.of("query", text, "indexName", PROBE_INDEX)))) {
            assertEquals(Encode.forHtml(text), escaped.getString("query"));
            assertEquals("<=>", escaped.getString("operator"));
            final JSONObject probeResult = AiTest.findResultByTitle(escaped, PROBE_TITLE);
            assertEquals(PROBE_TITLE, probeResult.getString("title"));
            assertEquals(Encode.forHtml(text),
                    probeResult.getJSONArray("matches").getJSONObject(0).getString("extractedText"));
        }

        final JSONObject raw = (JSONObject) unsafeSearchTool.query(text, PROBE_INDEX);
        assertEquals(text, raw.getString("query"));
        assertEquals(text, AiTest.findResultByTitle(raw, PROBE_TITLE)
                .getJSONArray("matches").getJSONObject(0).getString("extractedText"));
    }

    /**
     * Feature: Stored visitor input in the cache index is escaped (#37153, AC-002, cache clause)
     * Given a row in the index named "cache" holding raw markup as extractedText (as the embeddings API
     * stores every query it embeds) and a template that searches that index
     * When query(text, "cache") is called on the default tool
     * Then every extractedText in the result is escaped; the unsafe tool returns the literal text
     */
    @Test
    public void test_query_cacheIndex_escapesStoredQueryText() {
        final String text = "Escaping probe cache " + AiTest.PROBE_MARKUP;
        seedProbeEmbeddings(text, "cache");

        final JSONObject escaped = (JSONObject) searchTool.query(text, "cache");
        final JSONObject raw = (JSONObject) unsafeSearchTool.query(text, "cache");

        final List<String> escapedTexts = extractedTexts(escaped);
        assertTrue("probe excerpt missing from " + escapedTexts, escapedTexts.contains(Encode.forHtml(text)));
        escapedTexts.forEach(AiTest::assertNoRawMarkup);
        assertTrue(extractedTexts(raw).contains(text));
    }

    // ------------------------------------------------------------------------------ helpers

    /** One row whose extractedText is exactly {@code text} (so the query reuses its vector) with a markup title. */
    private static void seedProbeEmbeddings(final String text, final String indexName) {
        final String inode = "probe-" + UUIDGenerator.generateUuid();
        new EmbeddingsDTODataGen().generate(inode, indexName, text).withTitle(PROBE_TITLE).nextPersisted();
    }

    private static List<String> extractedTexts(final JSONObject payload) {
        final List<String> texts = new ArrayList<>();
        final JSONArray results = payload.getJSONArray("dotCMSResults");
        for (int i = 0; i < results.length(); i++) {
            final JSONArray matches = results.getJSONObject(i).optJSONArray("matches");
            for (int j = 0; matches != null && j < matches.length(); j++) {
                texts.add(matches.getJSONObject(j).getString("extractedText"));
            }
        }
        return texts;
    }

    private SearchTool prepareSearchTool(final ViewContext viewContext, final boolean escapeOutput) {
        return new SearchTool(viewContext, escapeOutput) {
            @Override
            Host host() {
                return host;
            }
        };
    }

    private SearchTool prepareSearchTool(final ViewContext viewContext) {
        return new SearchTool(viewContext) {
            @Override
            Host host() {
                return host;
            }
        };
    }

}
