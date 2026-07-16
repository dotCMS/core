package com.dotcms.graphql.datafetcher.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration test class for the VanityURLFetcher GraphQL data fetcher.
 * This test suite verifies the behavior of VanityURLFetcher when resolving
 * vanity URLs based on page content properties.
 *
 * @see VanityURLFetcher
 * @see DataFetchingEnvironment
 */
public class VanityURLFetcherTest {

    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static UserAPI userAPI;
    private static User user;
    private static FiltersUtil filtersUtil;
    private static Language defaultLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();
        defaultHost = hostAPI.findDefaultHost(user, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        filtersUtil = FiltersUtil.getInstance();
    }

    /**
     * MethodToTest: {@link VanityURLFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link VanityURLFetcher} on a content that does not contain an url
     * field
     * Expected Result: Should return null since no URL is present to resolve a vanity URL
     */
    @Test
    public void testGet_WithNullURL() throws Exception {

        final var fetcher = new VanityURLFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);
        final Field field = new Field("url");

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(user).build()
        );
        Mockito.when(environment.getSource()).thenReturn(new Contentlet());
        Mockito.when(environment.getField()).thenReturn(field);

        final var result = fetcher.get(environment);
        assertNull(result);
    }

    /**
     * MethodToTest: {@link VanityURLFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link VanityURLFetcher} on a content that contains url and a
     * vanity URLs
     * Expected Result: Should return the matching vanity URL object with all properties correctly set
     */
    @Test
    public void testGet_WithExitingVanityURL() throws Exception {

        final var fetcher = new VanityURLFetcher();

        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        // Get dummy page content
        final var page = TestDataUtils.getPageContent(true, defaultLanguage.getId());
        final var pageURL = page.getStringProperty("url");
        assertNotNull(pageURL);
        assertFalse(pageURL.isEmpty());

        //Create vanity URL
        long i = System.currentTimeMillis();
        final String title = "VanityURL" + i;
        final String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
        final int action = 200;
        final int order = 1;

        final var vanityURL = filtersUtil.createVanityUrl(
                title, defaultHost, pageURL, forwardTo, action, order, defaultLanguage.getId()
        );
        filtersUtil.publishVanityUrl(vanityURL);

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(
                        APILocator.systemUser()
                ).build()
        );
        Mockito.when(environment.getSource()).thenReturn(page);

        final var cachedVanityUrl = fetcher.get(environment);
        assertNotNull(cachedVanityUrl);
        assertEquals(cachedVanityUrl.url, pageURL);
        assertEquals(cachedVanityUrl.languageId, defaultLanguage.getId());
        assertEquals(cachedVanityUrl.siteId, defaultHost.getIdentifier());
        assertEquals(cachedVanityUrl.forwardTo, forwardTo);
        assertEquals(cachedVanityUrl.response, action);
    }

    /**
     * MethodToTest: {@link VanityURLFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link VanityURLFetcher} on a content that contains url but no
     * matching vanity URL exists
     * Expected Result: Should return null since no vanity URL matches the given page URL
     */
    @Test
    public void testGet_WithNonExitingVanityURL() throws Exception {

        final var fetcher = new VanityURLFetcher();

        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        // Get dummy page content
        final var page = TestDataUtils.getPageContent(true, defaultLanguage.getId());
        final var pageURL = page.getStringProperty("url");
        assertNotNull(pageURL);
        assertFalse(pageURL.isEmpty());

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(
                        APILocator.systemUser()
                ).build()
        );
        Mockito.when(environment.getSource()).thenReturn(page);

        final var cachedVanityUrl = fetcher.get(environment);
        assertNull(cachedVanityUrl);
    }

    /**
     * MethodToTest: {@link VanityURLFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Context already includes a cached vanity URL
     * Expected Result: Should return the cached vanity URL without resolving it again
     */
    @Test
    public void testGet_WithCachedVanityUrlInContext() throws Exception {
        final var fetcher = new VanityURLFetcher();

        final var environment = Mockito.mock(DataFetchingEnvironment.class);
        final var preCachedVanityUrl = new CachedVanityUrl(
                "vanity-123",
                "/pre-cached-url",
                defaultLanguage.getId(),
                defaultHost.getIdentifier(),
                "/forwarded-destination",
                301,
                1
        );

        final var context = DotGraphQLContext.createServletContext()
                .with(APILocator.systemUser())
                .build();
        context.addParam("cachedVanityUrl", preCachedVanityUrl);

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(TestDataUtils.getPageContent(true, defaultLanguage.getId()));

        final var cachedVanityUrl = fetcher.get(environment);
        assertNotNull(cachedVanityUrl);
        assertEquals("/pre-cached-url", cachedVanityUrl.url);
        assertEquals("/forwarded-destination", cachedVanityUrl.forwardTo);
        assertEquals(301, cachedVanityUrl.response);
    }
}
