package com.dotcms.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.mockito.Mockito;

public class VanityUrlFilterTest {


    private static ContentletAPI contentletAPI;
    private static VanityUrlAPI vanityUrlAPI;
    private static Host defaultHost;
    private static User user;
    private static Language defaultLanguage;
    private static VanityUrlCache cache;
    private static FiltersUtil filtersUtil;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        /* Default user */
        user = APILocator.systemUser();

        contentletAPI = APILocator.getContentletAPI();
        vanityUrlAPI = APILocator.getVanityUrlAPI();
        filtersUtil = FiltersUtil.getInstance();

        cache=CacheLocator.getVanityURLCache();

        /* Default variables */
        defaultHost =  new SiteDataGen().nextPersisted();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    /**
     * Method to test: {@link VanityURLFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     * Given Scenario: try a success 301 to the same url with / at the end
     * ExpectedResult: response code = 301 and location is the forward to
     *
     */
    @Test
    public void test_vanity_trailing_slash_301_success() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/redirect-me/";
        String forwardTo = "/redirect-me";
        int action = 301;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), uri).request();

        final HttpServletResponse response = new MockHttpStatusAndHeadersResponse(
                new MockHttpResponse().response()).response();

        final VanityURLFilter filter = new VanityURLFilter();

        filter.doFilter(request, response, null);

        Assert.assertEquals(301,  response.getStatus());
        Assert.assertEquals(forwardTo,  response.getHeader("Location"));
        Assert.assertNotNull(response.getHeader("X-DOT-VanityUrl"));
    }

    /**
     * Method to test: {@link VanityURLFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     * Given Scenario: the url is the same of the forward to, so even if match the vanity does not need the redirect
     * ExpectedResult: response code = 200, the 301 is not needed
     *
     */
    @Test
    public void test_vanity_trailing_slash_200_success() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/redirect-me/";
        String forwardTo = "/redirect-me";
        int action = 301;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), forwardTo).request();

        final HttpServletResponse response = new MockHttpStatusAndHeadersResponse(
                new MockHttpResponse().response()).response();

        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        final VanityURLFilter filter = new VanityURLFilter();

        filter.doFilter(request, response, filterChain);

        Assert.assertEquals(200,  response.getStatus());
    }

    /**
     * this tests that the vanityURL proxies requests that are made to different hosts.
     * In this case, we will request an url from dotcms and check to see that we get the results from dotcms.com
     */
    @Test
    public void test_that_vanity_url_filter_handles_proxy_requests() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/testing_forward" + System.currentTimeMillis();
        String forwardTo = "https://www.dotcms.com";
        int action = 200;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), uri).request();
  
        final File tmp = File.createTempFile("testingVanity", "test");
        tmp.deleteOnExit();

        try(OutputStream outputStream = new FileOutputStream(tmp)) {
            final HttpServletResponse response = new MockHttpCaptureResponse(
                    new MockHttpResponse().response(), outputStream).response();

            final VanityURLFilter filter = new VanityURLFilter();

            filter.doFilter(request, response, null);

            assert (tmp.exists());
            String content = FileUtil.read(tmp);
            assert (content != null);
            assert (content.contains("All rights reserved"));
            assert (content.contains("<meta property=\"og:url\" content=\"https://www.dotcms.com/\">"));
        }
    }

    /**
     * this tests that a vanity url that has been run gets added to the request attributes
     * @throws Exception
     */
    @Test
    public void test_that_vanity_url_filter_adds_vanity_as_request_attribute() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/testing_request_attribute" + System.currentTimeMillis();
        String forwardTo = "https://google.com";
        int action = 200;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), uri).request();

        final HttpServletResponse response = new MockHttpResponse().response();

        final VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);
        
        final CachedVanityUrl resolvedVanity = (CachedVanityUrl) request.getAttribute(Constants.VANITY_URL_OBJECT);
        assert(resolvedVanity!=null);
        
        
        assert(resolvedVanity.vanityUrlId .equals(contentlet1.getIdentifier() ));



    }
    
    
    /**
     * this tests that vanity url redirects work as expected
     * @throws Exception
     */
    @Test
    public void test_that_vanity_url_filter_handles_redirects() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String baseURI = "/testing_301" + System.currentTimeMillis();
        String vanityURIPattern = baseURI + "/(.*)";
        String forwardBaseURI = "https://dotcms.com/redirected_301";
        String forwardTo = forwardBaseURI + "/$1";
        int action = 301;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, vanityURIPattern,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final String resource = "/test redirect 301".replaceAll(" ", "%20");
        final String queryWithFragment = "?param1=value 1&param2=value 2#test-fragment"
                .replaceAll(" ", "+");
        final String testURI = baseURI + resource + queryWithFragment;
        final HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), testURI).request();
        final HttpServletResponse response = new MockHttpStatusResponse(new MockHeaderResponse(new MockHttpResponse().response()).response()).response();

        VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);

        final String expectedLocation = forwardBaseURI + resource + queryWithFragment;
        Assert.assertEquals(expectedLocation, response.getHeader("Location"));
        Assert.assertEquals(301,response.getStatus());
        Assert.assertNotNull(response.getHeader("X-DOT-VanityUrl"));

    }

    /**
     * this tests that vanity urls still work after clearing the vanity url cache
     * @throws Exception
     */
    @Test
    public void test_cache_flush_does_not_break_vanities() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/testing_302again" + System.currentTimeMillis();
        String forwardTo = "https://dotcms.com";
        int action = 302;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, defaultHost, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        HttpServletRequest request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), uri).request();
        HttpServletResponse response = new MockHttpStatusResponse(new MockHttpResponse().response()).response();

        VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);

        assert(response.getStatus()==302);
        
        // clear cache
        CacheLocator.getVanityURLCache().clearCache();
        
        // try again
        request = new MockHttpRequestIntegrationTest(defaultHost.getHostname(), uri).request();
        response = new MockHttpStatusResponse(new MockHttpResponse().response()).response();


        filter.doFilter(request, response, null);
        assert(response.getStatus()==302);
        
        
        
        

    }

}
