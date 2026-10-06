package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiTest;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EmbeddingsDTODataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    private static WireMockServer wireMockServer;

    private Host host;
    private SearchTool searchTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        // The failure-path tests below reach the mocked provider; the success tests are served
        // from persisted embeddings and never call it.
        IPUtils.disabledIpPrivateSubnet(true);
        wireMockServer = AiTest.prepareWireMock();
        AiTest.aiAppSecretsWithProviderConfig(APILocator.systemHost(), AiTest.providerConfigJson(AiTest.PORT, "gpt-4o-mini"));
    }

    @AfterClass
    public static void afterClass() {
        wireMockServer.stop();
        IPUtils.disabledIpPrivateSubnet(false);
    }

    @Before
    public void before() throws Exception {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        host = new SiteDataGen().nextPersisted();
        searchTool = prepareSearchTool(viewContext);
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

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Query when the embeddings provider fails
     * Given a query that has never been embedded (UUID suffix, so neither cache nor DB serves it)
     * And the query carries the embeddings-failure sentinel, so the mocked provider answers HTTP 500
     * When the user performs a search with the query
     * Then the result is a generic error payload with no stack trace and no exception detail
     */
    @Test
    public void test_query_providerFailure_returnsGenericError() {
        final String query = AiTest.FORCE_EMBEDDINGS_ERROR + " " + UUID.randomUUID();

        final Object result = searchTool.query(query);

        assertEmbeddingsProviderWasCalledWith(query);
        assertFalse("failure payload must not carry a 'stackTrace' key",
                result instanceof Map && ((Map<?, ?>) result).containsKey("stackTrace"));
        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Query with an index name when the embeddings provider fails
     * Given a never-embedded query carrying the embeddings-failure sentinel
     * When the user performs a search with the query and the "default" index
     * Then the result is a generic error payload with no stack trace and no exception detail
     */
    @Test
    public void test_query_withIndexName_providerFailure_returnsGenericError() {
        final String query = AiTest.FORCE_EMBEDDINGS_ERROR + " " + UUID.randomUUID();

        final Object result = searchTool.query(query, "default");

        assertEmbeddingsProviderWasCalledWith(query);
        assertFalse("failure payload must not carry a 'stackTrace' key",
                result instanceof Map && ((Map<?, ?>) result).containsKey("stackTrace"));
        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Query using a Map when the embeddings provider fails
     * Given a never-embedded query carrying the embeddings-failure sentinel, passed in a parameter map
     * When the user performs a search with the map
     * Then the result is a generic error payload with no stack trace and no exception detail
     */
    @Test
    public void test_query_usingMap_providerFailure_returnsGenericError() {
        final String query = AiTest.FORCE_EMBEDDINGS_ERROR + " " + UUID.randomUUID();

        final Object result = searchTool.query(
                Map.of(
                        "query", query,
                        "indexName", "default",
                        "limit", 50,
                        "threshold", .25f));

        assertEmbeddingsProviderWasCalledWith(query);
        assertFalse("failure payload must not carry a 'stackTrace' key",
                result instanceof Map && ((Map<?, ?>) result).containsKey("stackTrace"));
        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Query using a Map whose "query" value is not a String
     * Given a parameter map where "query" is an Integer, which EmbeddingsDTO.from casts to String
     * When the user performs a search with the map
     * Then the failure happens while the search arguments are built, before any provider call
     * And the result is still the generic error payload, not an exception escaping the viewtool
     */
    @Test
    public void test_query_usingMap_nonStringArgument_returnsGenericError() {
        final Object result = searchTool.query(Map.of("query", 42, "indexName", "default"));

        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Related content for a Contentlet when the embeddings provider fails
     * Given a contentlet whose indexed text carries the embeddings-failure sentinel and a UUID
     * When the user retrieves related content for the contentlet
     * Then the contentlet text is sent to the provider, which answers HTTP 500
     * And the result is a generic error payload with no stack trace and no exception detail
     */
    @Test
    public void test_related_contentlet_providerFailure_returnsGenericError() {
        final String text = AiTest.FORCE_EMBEDDINGS_ERROR + " " + UUID.randomUUID();
        final Contentlet contentlet = persistContentletWithText(text);

        final Object result = searchTool.related(contentlet, "default");

        assertEmbeddingsProviderWasCalledWith(text);
        assertFalse("failure payload must not carry a 'stackTrace' key",
                result instanceof Map && ((Map<?, ?>) result).containsKey("stackTrace"));
        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Feature: SearchTool failure handling (#37154)
     * Scenario: Related content for a ContentMap when the embeddings provider fails
     * Given a ContentMap wrapping a contentlet whose indexed text carries the embeddings-failure sentinel
     * When the user retrieves related content for the ContentMap
     * Then the result is a generic error payload with no stack trace and no exception detail
     */
    @Test
    public void test_related_contentMap_providerFailure_returnsGenericError() throws Exception {
        final String text = AiTest.FORCE_EMBEDDINGS_ERROR + " " + UUID.randomUUID();
        final Contentlet contentlet = persistContentletWithText(text);
        final ContentMap contentMap = new ContentMap(
                contentlet, APILocator.getUserAPI().getAnonymousUser(), PageMode.LIVE, host, mock(Context.class));

        final Object result = searchTool.related(contentMap, "default");

        assertEmbeddingsProviderWasCalledWith(text);
        assertFalse("failure payload must not carry a 'stackTrace' key",
                result instanceof Map && ((Map<?, ?>) result).containsKey("stackTrace"));
        AiTest.assertSafeErrorPayload(result);
    }

    /**
     * Proves the forced failure really came from the provider: the mocked embeddings endpoint must
     * have received a request carrying this test's unique text. Without this, a stub that stopped
     * matching could let a non-exception result pass the payload assertions for the wrong reason.
     */
    private static void assertEmbeddingsProviderWasCalledWith(final String text) {
        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/embeddings"))
                .withRequestBody(containing(text)));
    }

    /**
     * Persists and publishes a contentlet of a fresh content type with one long-text field holding
     * {@code text} padded past the embeddings minimum length, so {@code ContentToStringUtil}
     * selects it: {@code guessWhatFieldsToIndex} keeps only {@code LONG_TEXT} fields (a
     * {@code TextAreaField}, not a {@code TextField}) and {@code parseFields} drops text shorter
     * than {@code EMBEDDINGS_MINIMUM_TEXT_LENGTH_TO_INDEX} (default 64 characters).
     */
    private Contentlet persistContentletWithText(final String text) {
        final String padded = text + " This body exists only so the related-content search sends it to the"
                + " mocked provider, which is configured to fail for this sentinel. " + text;
        final Field field = new FieldDataGen()
                .type(TextAreaField.class)
                .indexed(true)
                .next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), padded)
                .nextPersisted();
        ContentletDataGen.publish(contentlet);
        return contentlet;
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
