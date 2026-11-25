package com.dotcms.rest.api.v1.index;

import static com.dotmarketing.sitesearch.business.SiteSearchAPI.ES_SITE_SEARCH_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the site-search indices handled by {@link ESIndexResource}: an
 * administrator activates, deactivates, closes, opens and deletes them through
 * {@code PUT /api/v1/esindex/{indexName}?action=...} and {@code DELETE /api/v1/esindex/{indexName}},
 * which is what the Site Search portlet's index menu calls. Deleting a site-search index was already
 * handled by {@code DELETE /api/v1/esindex/{indexName}} and is not covered here.
 *
 * <p>Each test creates its own site-search indices and removes them afterwards; the default
 * site-search index the environment had before the test is restored.</p>
 *
 * @author hassandotcms
 */
public class ESIndexResourceSiteSearchIntegrationTest {

    private static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    private static ESIndexResource resource;
    private static SiteSearchAPI siteSearchAPI;

    private final List<String> created = new ArrayList<>();
    private Optional<String> previousDefault = Optional.empty();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        resource = new ESIndexResource();
        siteSearchAPI = APILocator.getSiteSearchAPI();
    }

    @AfterClass
    public static void closeTokens() {
        TOKEN_UTILS.close();
    }

    /**
     * Restores the default site-search index the test found, then deletes every index the test
     * created.
     */
    @After
    public void cleanup() {
        for (final String index : created) {
            Try.run(() -> {
                if (siteSearchAPI.isDefaultIndex(index)) {
                    siteSearchAPI.deactivateIndex(index);
                }
            });
            Try.run(() -> APILocator.getESIndexAPI().openIndex(index));
            Try.run(() -> siteSearchAPI.deleteIndex(index)).onFailure(e -> Logger.warn(
                    ESIndexResourceSiteSearchIntegrationTest.class,
                    "Could not clean up site-search index " + index, e));
        }
        previousDefault.ifPresent(index -> Try.run(() -> siteSearchAPI.activateIndex(index)));
    }

    /**
     * Creates a site-search index owned by the current test and remembers the default index to
     * restore afterwards.
     *
     * @return the name of the new index
     */
    private String newSiteSearchIndex() throws Exception {
        if (created.isEmpty()) {
            previousDefault = siteSearchAPI.listIndices().stream()
                    .filter(index -> Try.of(() -> siteSearchAPI.isDefaultIndex(index)).getOrElse(false))
                    .findFirst();
        }
        final String suffix = String.valueOf(System.nanoTime());
        final String indexName = ES_SITE_SEARCH_NAME + "_" + suffix;
        siteSearchAPI.createSiteSearchIndex(indexName, "alias_" + suffix, 1);
        created.add(indexName);
        return indexName;
    }

    /**
     * Builds a request authenticated as the administrator with an API token.
     *
     * @return the request
     */
    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization", TOKEN_UTILS.adminBearerHeader());
        return request;
    }

    /**
     * Calls {@code PUT /api/v1/esindex/{indexName}?action=...} as the administrator.
     *
     * @param indexName the index to change
     * @param action    the action name
     * @return the HTTP status of the response
     */
    private static int modIndex(final String indexName, final String action) throws Exception {
        final Response response = resource.modIndex(adminRequest(), new MockHttpResponse().response(),
                indexName, action);
        return response.getStatus();
    }

    /**
     * Reads the state of an index straight from the search engine, through the Elasticsearch
     * client the index API uses in this environment. {@link SiteSearchAPI} cannot be used for
     * this: on Elasticsearch its list of closed indices only covers content indices.
     *
     * @param indexName the site-search index name
     * @return {@code open} or {@code close}, or an empty string when the index does not exist
     */
    private static String engineState(final String indexName) throws Exception {
        final String physicalName = APILocator.getESIndexAPI().getNameWithClusterIDPrefix(indexName);
        try {
            final org.elasticsearch.client.Response response = RestHighLevelClientProvider.getInstance()
                    .getClient().getLowLevelClient()
                    .performRequest(new Request("GET", "/_cat/indices/" + physicalName + "?h=status"));
            return EntityUtils.toString(response.getEntity()).trim();
        } catch (final ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return "";
            }
            throw e;
        }
    }

    /**
     * Given : an administrator and a site-search index that is not the default
     * When  : the index is activated
     * Then  : 200, and the index is the default site-search index
     */
    @Test
    public void admin_activatesSiteSearchIndex() throws Exception {
        final String index = newSiteSearchIndex();

        assertEquals(200, modIndex(index, "activate"));
        assertTrue(siteSearchAPI.isDefaultIndex(index));
    }

    /**
     * Given : an administrator and the default site-search index
     * When  : the index is deactivated
     * Then  : 200, and the index is no longer the default
     */
    @Test
    public void admin_deactivatesSiteSearchIndex() throws Exception {
        final String index = newSiteSearchIndex();
        siteSearchAPI.activateIndex(index);

        assertEquals(200, modIndex(index, "deactivate"));
        assertFalse(siteSearchAPI.isDefaultIndex(index));
    }

    /**
     * Given : an administrator and an open site-search index
     * When  : the index is closed, and then opened
     * Then  : 200 each time; the index is closed, then open
     */
    @Test
    public void admin_closesAndOpensSiteSearchIndex() throws Exception {
        final String index = newSiteSearchIndex();

        assertEquals(200, modIndex(index, "close"));
        assertEquals("close", engineState(index));

        assertEquals(200, modIndex(index, "open"));
        assertEquals("open", engineState(index));
    }

    /**
     * Given : an administrator and a site-search index
     * When  : the index is cleared, which site-search indices do not support
     * Then  : 400, and the index is still listed
     */
    @Test
    public void admin_cannotClearSiteSearchIndex() throws Exception {
        final String index = newSiteSearchIndex();

        assertEquals(400, modIndex(index, "clear"));
        assertTrue(siteSearchAPI.listIndices().contains(index));
    }

    /**
     * Given : an administrator and a site-search index name that does not exist
     * When  : the index is activated
     * Then  : 404
     */
    @Test
    public void admin_getsNotFoundForUnknownSiteSearchIndex() throws Exception {
        assertEquals(404, modIndex(ES_SITE_SEARCH_NAME + "_1", "activate"));
    }
}
