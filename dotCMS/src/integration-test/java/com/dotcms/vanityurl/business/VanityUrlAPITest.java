package com.dotcms.vanityurl.business;


import java.util.List;
import java.util.Optional;


import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.VanityUrlDataGen;

import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

/**
 * This class test the {@link VanityUrlAPI} methods
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 22, 2017
 */
public class VanityUrlAPITest {

    private static ContentletAPI contentletAPI;
    private static VanityUrlAPI vanityUrlAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static User user;
    private static Language defaultLanguage;
    private static VanityUrlCache vanityUrlCache;
    private static FiltersUtil filtersUtil;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        /* Default user */
        user = APILocator.systemUser();

        /* APIs initialization */
        hostAPI = APILocator.getHostAPI();
        contentletAPI = APILocator.getContentletAPI();
        vanityUrlAPI = APILocator.getVanityUrlAPI();
        filtersUtil = FiltersUtil.getInstance();

        /* Load Cache */
        vanityUrlCache = CacheLocator.getVanityURLCache();

        /* Default variables */
        defaultHost = hostAPI.findDefaultHost(user, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    /**
     * Creates a vanity url but no publish it, gets it and checks there is not in cache.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getWorkingVanityUrl() throws Exception{

        Contentlet contentlet1 = null;


            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = new VanityUrlDataGen()
                .title(title)
                .site(site)
                .uri(uri)
                .forwardTo(forwardTo)
                .action(action)
                .order(order)
                .language(defaultLanguage.getId())
                .nextPersisted();

            VanityUrl vanity1 = vanityUrlAPI.fromContentlet(contentletAPI.find(contentlet1.getInode(), user, false));

            Assert.assertNotNull(vanity1);
            Assert.assertNotNull(vanity1.getInode());
            Assert.assertEquals(title, vanity1.getTitle());
            Assert.assertEquals(site, vanity1.getSite());
            Assert.assertEquals(uri, vanity1.getURI());
            Assert.assertEquals(forwardTo, vanity1.getForwardTo());
            Assert.assertEquals(action, vanity1.getAction());

            Optional<CachedVanityUrl> vanityCached = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);

            // not published so not live
            Assert.assertFalse(vanityCached.isPresent());

            // added to the 404 cache
            Assert.assertTrue(vanityUrlCache.is404(defaultHost, defaultLanguage, uri));

    }

    /**
     * Creates a couple of vanity urls, publish only the first one.
     * Checks that the first one is in cache and the second one is not.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getLiveVanityUrl() throws Exception{

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
 
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test2_1_" + i;
            String forwardTo = "/products/";
            int action = 200;
            int order = 1;

            contentlet1 = new VanityUrlDataGen()
                .title(title)
                .site(site)
                .uri(uri)
                .forwardTo(forwardTo)
                .action(action)
                .order(order)
                .language(defaultLanguage.getId())
                .nextPersisted();

            filtersUtil.publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test2_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = new VanityUrlDataGen()
                .title(title2)
                .site(site2)
                .uri(uri2)
                .forwardTo(forwardTo2)
                .action(action2)
                .order(order2)
                .language(defaultLanguage.getId())
                .nextPersisted();
                
   

            Optional<CachedVanityUrl> vanityCached = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);

            Assert.assertTrue(vanityCached.isPresent());

            Assert.assertEquals(site, vanityCached.get().getSiteId());
            Assert.assertEquals(uri, vanityCached.get().getUrl());
            Assert.assertEquals(forwardTo, vanityCached.get().getForwardTo());
            Assert.assertEquals(action, vanityCached.get().getResponse());


            vanityCached = vanityUrlAPI.resolveVanityUrl(uri2, defaultHost, defaultLanguage);

            Assert.assertFalse(vanityCached.isPresent());


    }

    /**
     * Testing {@link VanityUrlAPI#getVanityUrlFromContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getVanityUrlFromContentlet() throws Exception{
        Contentlet contentlet1 = null;

            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test3_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 301;
            int order = 1;

            contentlet1 = new VanityUrlDataGen()
                .title(title)
                .site(site)
                .uri(uri)
                .forwardTo(forwardTo)
                .action(action)
                .order(order)
                .language(defaultLanguage.getId())
                .nextPersisted();

            filtersUtil.publishVanityUrl(contentlet1);

            VanityUrl vanity1 = vanityUrlAPI
                    .fromContentlet(contentlet1);

            Assert.assertNotNull(vanity1);
            Assert.assertEquals(title, vanity1.getTitle());
            Assert.assertEquals(site, vanity1.getSite());
            Assert.assertEquals(uri, vanity1.getURI());
            Assert.assertEquals(forwardTo, vanity1.getForwardTo());
            Assert.assertEquals(action, vanity1.getAction());


    }

    /**
     * This test creates a vanity url, gets it so it's cached.
     * Then modify the uri, and checks if the old one is removed from cache.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void updateUriVanityURLcheckCacheTest() throws DotDataException, DotSecurityException {
  
        
        final String url = "/testing" + System.currentTimeMillis();
        
        Contentlet vanityURLContentlet = new VanityUrlDataGen()
                .title("test Vanity Url " + url)
                .site(defaultHost.getIdentifier())
                .uri(url)
                .forwardTo("https://www.google.com")
                .action(200)
                .order(1)
                .language(defaultLanguage.getId()).nextPersisted();
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            CachedVanityUrl vanityURLCached = vanityUrlCache
                .getCachedVanityUrls(defaultHost, defaultLanguage)
                .stream()
                .filter(vc->vc.getUrl().equals(url))
                .findFirst().get();

            Assert.assertNotNull(vanityURLCached);

            Optional<CachedVanityUrl> vanity = vanityUrlAPI.resolveVanityUrl(url, defaultHost, defaultLanguage);

            Assert.assertFalse(vanityUrlCache.is404(defaultHost, defaultLanguage, url));
            Assert.assertTrue(vanity.isPresent());
            Assert.assertEquals(vanityURLCached,vanity.get());

            Assert.assertEquals(url, vanityURLCached.getUrl());

            final String url2 = "/testing" + System.currentTimeMillis();
            Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setStringProperty("uri", url2);
            vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            filtersUtil.publishVanityUrl(vanityURLContentletUpdated);

            vanity = vanityUrlAPI.resolveVanityUrl(url, defaultHost, defaultLanguage);

            vanityURLCached = vanityUrlCache
                .getCachedVanityUrls(defaultHost, defaultLanguage)
                .stream()
                .filter(vc->vc.getUrl().equals(url2))
                .findFirst().get();

            Assert.assertNotNull(vanityURLCached);

            Assert.assertEquals(url2, vanityURLCached.getUrl());

    }

    @Test
    public void Test_Vanity_URI_Ending_With_Forward_Slash_Handles_Non_forward_Slash_Ending_URL()
            throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final String hostIdentifier = host.getIdentifier();

        final long timeMillis = System.currentTimeMillis();
        final String title = "VanityURL" + timeMillis;
        final String uri = "/test1_" + timeMillis + "/" ;
        final String forwardTo = "/about-us/lol" ;
        final int action = 200;
        final int order = 1;

        final Contentlet vanityURL = filtersUtil.createVanityUrl(title, hostIdentifier, uri,
                forwardTo, action, order, defaultLanguage.getId());
        Assert.assertNotNull(vanityURL);
        filtersUtil.publishVanityUrl(vanityURL);

        final String testUri1 = "/test1_" + timeMillis + "/" ;
        final String testUri2 = "/test1_" + timeMillis  ;

        final Optional<CachedVanityUrl> v1 = vanityUrlAPI.resolveVanityUrl(testUri1, host, defaultLanguage);
        Assert.assertNotNull(v1);
        Assert.assertTrue(v1.isPresent());

        Assert.assertEquals(v1.get().getForwardTo(),forwardTo);

        final Optional<CachedVanityUrl> v2 = vanityUrlAPI.resolveVanityUrl(testUri2, host, defaultLanguage);
        Assert.assertNotNull(v2);
        Assert.assertTrue(v2.isPresent());
        Assert.assertEquals(v2.get().getForwardTo(),forwardTo);

        final List<CachedVanityUrl> vanityURLCached = vanityUrlCache.getCachedVanityUrls(host, defaultLanguage);

        Assert.assertNotNull(vanityURLCached);
        Assert.assertTrue(!vanityURLCached.isEmpty());
        Assert.assertTrue(vanityURLCached.contains(v1.get()));
    }

}