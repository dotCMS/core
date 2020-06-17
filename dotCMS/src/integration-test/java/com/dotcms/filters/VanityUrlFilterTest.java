package com.dotcms.filters;

import java.io.File;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

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
     * this tests that the vanityURL proxies requests that are made to different hosts.
     * In this case, we will request a url from dotcms and check to see that we get the results from dotcms.com
     */

    @Test
    public void test_that_vanity_url_filter_handles_proxy_requests() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/testing_forward" + System.currentTimeMillis();
        String forwardTo = "https://dotcms.com";
        int action = 200;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        final HttpServletRequest request = new MockHttpRequest(defaultHost.getHostname(), uri).request();
  
        final File tmp = File.createTempFile("testingVanity", "test");
        tmp.deleteOnExit();
        
        final HttpServletResponse response = new MockHttpCaptureResponse(new MockHttpResponse().response(), tmp).response();

        final VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);
        
        assert(tmp.exists());
        String content = FileUtil.read(tmp);
        assert(content!=null);
        assert(content.contains("All rights reserved"));
        assert(content.contains("<meta property=\"og:url\" content=\"https://dotcms.com/\">"));
        

    }

    /**
     * this tests that vanity url redirects work as expected
     * @throws Exception
     */
    @Test
    public void test_that_vanity_url_filter_handles_redirects() throws Exception {


        String title = "VanityURL"  + System.currentTimeMillis();
        String site = defaultHost.getIdentifier();
        String uri = "/testing_301" + System.currentTimeMillis();
        String forwardTo = "https://dotcms.com";
        int action = 301;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);


        final HttpServletRequest request = new MockHttpRequest(defaultHost.getHostname(), uri).request();
        final HttpServletResponse response = new MockHttpStatusResponse(new MockHeaderResponse(new MockHttpResponse().response()).response()).response();

        
        VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);

        Collection<String> list=  response.getHeaderNames();
        assert(response.getHeader("Location").equals(forwardTo));
        assert(response.getStatus()==301);

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

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(contentlet1);

        HttpServletRequest request = new MockHttpRequest(defaultHost.getHostname(), uri).request();
        HttpServletResponse response = new MockHttpStatusResponse(new MockHttpResponse().response()).response();

        VanityURLFilter filter = new VanityURLFilter();
        
        filter.doFilter(request, response, null);

        assert(response.getStatus()==302);
        
        // clear cache
        CacheLocator.getVanityURLCache().clearCache();
        
        // try again
        request = new MockHttpRequest(defaultHost.getHostname(), uri).request();
        response = new MockHttpStatusResponse(new MockHttpResponse().response()).response();


        filter.doFilter(request, response, null);
        assert(response.getStatus()==302);
        
        
        
        

    }

}
