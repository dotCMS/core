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
import com.dotmarketing.util.json.JSONObject;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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

    private Host host;
    private SearchTool searchTool;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        AiTest.aiAppSecrets(APILocator.systemHost(), "gpt-4o-mini", "dall-e-3", "text-embedding-ada-002");
    }

    @Before
    public void before() throws Exception {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        host = new SiteDataGen().nextPersisted();
        searchTool = prepareSearchTool(viewContext);
        AiTest.aiAppSecrets(host, "gpt-4o-mini", "dall-e-3", "text-embedding-ada-002");
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

    private SearchTool prepareSearchTool(final ViewContext viewContext) {
        return new SearchTool(viewContext) {
            @Override
            Host host() {
                return host;
            }
        };
    }

}
