package com.dotcms.vanityurl.business;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.cache.VanityUrlCache;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.model.CacheVanityKey;
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
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    private static long defaultLanguageId;
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
        defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

    /**
     * Creates a vanity url but no publish it, gets it and checks there is not in cache.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getWorkingVanityUrl() {

        Contentlet contentlet1 = null;

        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            VanityUrl vanity1 = vanityUrlAPI.getVanityUrlFromContentlet(contentletAPI.find(contentlet1.getInode(), user, false));

            Assert.assertNotNull(vanity1);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity1.getInode());
            Assert.assertEquals(title, vanity1.getTitle());
            Assert.assertEquals(site, vanity1.getSite());
            Assert.assertEquals(uri, vanity1.getURI());
            Assert.assertEquals(forwardTo, vanity1.getForwardTo());
            Assert.assertEquals(action, vanity1.getAction());

            CachedVanityUrl vanityCached = vanityUrlAPI.getLiveCachedVanityUrl(uri, defaultHost, defaultLanguageId, user);

            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanityCached.getVanityUrlId());

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.destroy(contentlet1, user, false );
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Creates a couple of vanity urls, publish only the first one.
     * Checks that the first one is in cache and the second one is not.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getLiveVanityUrl() {

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test2_1_" + i;
            String forwardTo = "/products/";
            int action = 200;
            int order = 1;

            contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            filtersUtil.publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test2_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = filtersUtil.createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(new CacheVanityKey(
                                                                        contentlet1.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                                                        contentlet1.getLanguageId(),
                                                                        contentlet1.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                                                                    ));
            Assert.assertNull(vanityURLCached);

            CachedVanityUrl vanity = vanityUrlAPI.getLiveCachedVanityUrl(uri, defaultHost, defaultLanguageId, user);

            Assert.assertNotNull(vanity);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity.getVanityUrlId());
            Assert.assertEquals(site, vanity.getSiteId());
            Assert.assertEquals(uri, vanity.getUrl());
            Assert.assertEquals(forwardTo, vanity.getForwardTo());
            Assert.assertEquals(action, vanity.getResponse());

            vanityURLCached = vanityUrlCache.get(new CacheVanityKey(
                    contentlet1.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                    contentlet1.getLanguageId(),
                    contentlet1.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
            ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanityURLCached.getVanityUrlId());

            vanityURLCached = vanityUrlCache.get(new CacheVanityKey(
                    contentlet2.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                    contentlet2.getLanguageId(),
                    contentlet2.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
            ));
            Assert.assertNull(vanityURLCached);

            vanity = vanityUrlAPI.getLiveCachedVanityUrl(uri2, defaultHost, defaultLanguageId, user);

            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity.getVanityUrlId());

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.destroy(contentlet1, user, false );
                }

                if (contentlet2 != null) {
                    contentletAPI.destroy(contentlet2, user, false );
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getVanityUrlFromContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getVanityUrlFromContentlet() {
        Contentlet contentlet1 = null;
        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test3_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 301;
            int order = 1;

            contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            filtersUtil.publishVanityUrl(contentlet1);

            VanityUrl vanity1 = vanityUrlAPI
                    .getVanityUrlFromContentlet(contentlet1);

            Assert.assertNotNull(vanity1);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity1.getInode());
            Assert.assertEquals(title, vanity1.getTitle());
            Assert.assertEquals(site, vanity1.getSite());
            Assert.assertEquals(uri, vanity1.getURI());
            Assert.assertEquals(forwardTo, vanity1.getForwardTo());
            Assert.assertEquals(action, vanity1.getAction());

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.destroy(contentlet1, user, false );
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * This test creates a vanity url, gets it so it's cached.
     * Then modify the uri, and checks if the old one is removed from cache.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void updateUriVanityURLcheckCacheTest() throws DotDataException, DotSecurityException {
        long currentTime = System.currentTimeMillis();
        Contentlet vanityURLContentlet = null;
        try{
            vanityURLContentlet = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            "/testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals("/testing"+currentTime, vanityURLCached.getUrl());

            currentTime = System.currentTimeMillis();
            Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setStringProperty("uri", "/testing"+currentTime);
            vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            filtersUtil.publishVanityUrl(vanityURLContentletUpdated);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletUpdated.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletUpdated.getLanguageId(),
                            vanityURLContentletUpdated.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals("/testing"+currentTime, vanityURLCached.getUrl());

        } finally {
            contentletAPI.destroy(vanityURLContentlet, user, false );
        }
    }

    /**
     * This test creates a vanity url, gets it so it's cached.
     * Then modify the action, and checks if the old one is removed from cache.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void updateActionVanityURLcheckCacheTest() throws DotDataException, DotSecurityException {
        long currentTime = System.currentTimeMillis();
        Contentlet vanityURLContentlet = null;
        try{
            vanityURLContentlet = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            "/testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                        vanityURLContentlet.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                        vanityURLContentlet.getLanguageId(),
                        vanityURLContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals(200, vanityURLCached.getResponse());

            Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setLongProperty("action", 301);
            vanityURLContentletUpdated.setIndexPolicy(IndexPolicy.FORCE);
            vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            filtersUtil.publishVanityUrl(vanityURLContentletUpdated);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletUpdated.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletUpdated.getLanguageId(),
                            vanityURLContentletUpdated.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals(301, vanityURLCached.getResponse());

        }finally{
            contentletAPI.destroy(vanityURLContentlet, user, false );
        }
    }

    /**
     * Checks the proper validation of not allowed Action codes
     */
    @Test
    public void checkInvalidActionTest()
            throws DotDataException, DotSecurityException, LanguageException {

        long currentTime = System.currentTimeMillis();
        final String expectedInvalidCodeMessage = LanguageUtil
                .get("message.vanity.url.error.invalidAction");
        Contentlet vanityURLContentlet = null;
        try {
            vanityURLContentlet = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            "/testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);

            vanityURLContentlet.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet
                                    .getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet
                                    .getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing" + currentTime, defaultHost,
                    defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentlet
                                    .getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentlet.getLanguageId(),
                            vanityURLContentlet
                                    .getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            Assert.assertEquals(200, vanityURLCached.getResponse());

            //Now lets try to add an invalid action code, this should throw a DotContentletValidationException
            Contentlet vanityURLContentletUpdated = contentletAPI
                    .checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setLongProperty("action", 600);
            vanityURLContentletUpdated.setIndexPolicy(IndexPolicy.FORCE);
            try {
                contentletAPI.checkin(vanityURLContentletUpdated, user, false);
                fail("Using an invalid 600 action code, the checking method should fail...");
            } catch (Exception e) {
                e.printStackTrace();
                assertEquals(expectedInvalidCodeMessage, e.getMessage());
            }
        } finally {
            contentletAPI.destroy(vanityURLContentlet, user, false );
        }
    }

    /**
     * Creates a vanity url in defaultLanguage(English) and a version in Spanish, gets both so are cached.
     * Unpublish the one in the defaultLanguage, so it's removed from cache but the one in spanish is still in cache.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    @WrapInTransaction
    public void removeVanityURLDefaultLanguaguefromCacheTest() throws DotDataException, DotSecurityException {
        long currentTime = System.currentTimeMillis();
        Contentlet vanityURLContentletEnglish = null;
        Contentlet vanityURLContentletSpanish = null;
        final Language spanish = TestDataUtils.getSpanishLanguage();
        try{
            vanityURLContentletEnglish = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            "/testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentletEnglish);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletEnglish.getLanguageId(),
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletEnglish.getLanguageId(),
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());

            vanityURLContentletSpanish = contentletAPI.find(vanityURLContentletEnglish.getInode(), user, false);
            vanityURLContentletSpanish.setInode("");
            vanityURLContentletSpanish.setLanguageId(spanish.getId());
            vanityURLContentletSpanish = contentletAPI.checkin(vanityURLContentletSpanish, user, false);

            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, spanish.getId(), user).getVanityUrlId());
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletSpanish.getLanguageId(),
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());

            filtersUtil.publishVanityUrl(vanityURLContentletSpanish);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, spanish.getId(), user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletSpanish.getLanguageId(),
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());

            filtersUtil.unpublishVanityURL(vanityURLContentletEnglish);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletEnglish.getLanguageId(),
                            vanityURLContentletEnglish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletSpanish.getLanguageId(),
                            vanityURLContentletSpanish.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());


        }finally{

            try {
                contentletAPI.destroy(vanityURLContentletEnglish, user, false);
                contentletAPI.destroy(vanityURLContentletSpanish, user, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Testing how the cache is working when publishing and unpublishing a VanityURL, we are
     * simulating URL requests that makes exact match with the URI of the VanityURL
     */
    @Test
    public void publishUnpublishVanityURLExact() throws DotDataException, DotSecurityException {

        long currentTime = System.currentTimeMillis();
        publishUnpublishVanityURL("/testing" + currentTime, "/testing" + currentTime,
                defaultHost.getIdentifier());
    }

    /**
     * Testing how the cache is working when publishing and unpublishing a VanityURL, on this test
     * we created a VanityURL with a regex
     */
    @Test
    public void publishUnpublishVanityURLRegex() throws DotDataException, DotSecurityException {

        long currentTime = System.currentTimeMillis();
        publishUnpublishVanityURL("/testing" + currentTime + "(.*)",
                "/testing" + currentTime + "/testing/index", defaultHost.getIdentifier());
    }

    /**
     * Testing how the cache is working when publishing and unpublishing a VanityURL, on this test
     * we created a VanityURL with a regex
     */
    @Test
    public void publishUnpublishVanityURLRegexSystemHost()
            throws DotDataException, DotSecurityException {

        long currentTime = System.currentTimeMillis();
        publishUnpublishVanityURL("/testing" + currentTime + "(.*)",
                "/testing" + currentTime + "/testing/index", Host.SYSTEM_HOST);
    }

    /**
     * Testing how the cache is working with multiple vanities using the same URI and the
     * combinations of existing vanities in other different Sites
     */
    @Test
    public void sameURLSystemHost()
            throws DotDataException, DotSecurityException {

        Contentlet vanityURL = null;
        Contentlet vanityURL1 = null;
        Contentlet vanityURL2 = null;

        try {
            long currentTime = System.currentTimeMillis();
            String uri = "/" + currentTime + "_testing" + "(.*)";
            String requestedURL = "/" + currentTime + "_testing";
            final String nonExistingURL = "/nonexisting/should404/index" + currentTime;

            //------------------------------------
            //Create a VanityURL for the default host
            //------------------------------------
            vanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            defaultHost.getIdentifier(),
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURL);

            //Should not exist in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(
                        new CacheVanityKey(
                                vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                vanityURL.getLanguageId(),
                                vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                        ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with no matches
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(nonExistingURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Create the first VanityURL for the SYSTEM_HOST
            //------------------------------------

            currentTime = System.currentTimeMillis();
            uri = "/testing" + currentTime + "(.*)";
            requestedURL = "/testing" + currentTime;

            vanityURL1 = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            Host.SYSTEM_HOST,
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURL1);

            //Should not exist in cache
            vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    vanityURL1.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    vanityURL1.getLanguageId(),
                                    vanityURL1.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));

            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL1.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL1.getLanguageId(),
                            vanityURL1.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Create the second VanityURL  for the SYSTEM_HOST and with the same URI
            //------------------------------------
            vanityURL2 = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            Host.SYSTEM_HOST,
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURL2);

            //Should not exist in cache
            vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    vanityURL2.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    vanityURL2.getLanguageId(),
                                    vanityURL2.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL2.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL2.getLanguageId(),
                            vanityURL2.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Unpublish the VanityURL
            //------------------------------------
            vanityURL2.setIndexPolicy(IndexPolicy.FORCE);
            vanityURL2.setIndexPolicyDependencies(IndexPolicy.FORCE);
            filtersUtil.unpublishVanityURL(vanityURL2);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL2.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL2.getLanguageId(),
                            vanityURL2.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Unpublish the VanityURL
            //------------------------------------
            vanityURL1.setIndexPolicy(IndexPolicy.FORCE);
            vanityURL1.setIndexPolicyDependencies(IndexPolicy.FORCE);
            filtersUtil.unpublishVanityURL(vanityURL1);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL1.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL1.getLanguageId(),
                            vanityURL1.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Unpublish the VanityURL
            //------------------------------------
            vanityURL2.setIndexPolicy(IndexPolicy.FORCE);
            vanityURL2.setIndexPolicyDependencies(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(vanityURL2);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL2.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL2.getLanguageId(),
                            vanityURL2.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());


        } finally {

            if (null != vanityURL) {
                contentletAPI.destroy(vanityURL, user, false);
            }

            if (null != vanityURL1) {
                contentletAPI.destroy(vanityURL1, user, false);
            }

            if (null != vanityURL2) {
                contentletAPI.destroy(vanityURL2, user, false);
            }
        }
    }

    /**
     * Testing how the cache is working with multiple vanities using on different Sites
     */
    @Test
    public void test_same_vanity_on_system()
            throws DotDataException, DotSecurityException {

        Contentlet systemVanityURL = null;

        try {

            final long currentTime = System.currentTimeMillis();
            final String uri = "/testing" + currentTime + "(.*)";
            final String requestedURL = "/testing" + currentTime;

            //------------------------------------
            //Create a VanityURL for the System Host
            //------------------------------------
            systemVanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            Host.SYSTEM_HOST,
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            systemVanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(systemVanityURL);

            //Should not exist in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    systemVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    systemVanityURL.getLanguageId(),
                                    systemVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match on system and default host
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemVanityURL.getLanguageId(),
                            systemVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));

            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            ///////
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemVanityURL.getLanguageId(),
                            systemVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));

            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            /////// UNPUBLISH

            filtersUtil.unpublishVanityURL(systemVanityURL);

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemVanityURL.getLanguageId(),
                            systemVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));

            Assert.assertNull(vanityURLCached);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemVanityURL.getLanguageId(),
                            requestedURL
                    ));

            Assert.assertNull(vanityURLCached);

            /////////

            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

        } finally {

            contentletAPI.destroy(systemVanityURL, user, false);
        }
    }


    /**
     * Testing how the cache is working with multiple vanities using on different Sites
     */
    @Test
    public void differentSitesTest()
            throws DotDataException, DotSecurityException {

        Contentlet systemHostVanityURL  = null;
        Contentlet defaultHostVanityURL = null;

        try {
            long currentTime = System.currentTimeMillis();
            String uri = "/testing" + currentTime + "(.*)";
            final String requestedURL = "/testing" + currentTime;

            //------------------------------------
            //Create a VanityURL for the System Host
            //------------------------------------
            systemHostVanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            Host.SYSTEM_HOST,
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            systemHostVanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(systemHostVanityURL);

            //Should not exist in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    systemHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    systemHostVanityURL.getLanguageId(),
                                    systemHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemHostVanityURL.getLanguageId(),
                            systemHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Create the first VanityURL for the Default Host
            //------------------------------------

            currentTime = System.currentTimeMillis();
            uri = "/testing_1_" + currentTime + "(.*)";
            final String requestedURL1 = "/testing_1_" + currentTime;

            defaultHostVanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            defaultHost.getIdentifier(),
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            defaultHostVanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

            //Should not exist in cache
            vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    defaultHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    defaultHostVanityURL.getLanguageId(),
                                    defaultHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL1
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            defaultHostVanityURL.getLanguageId(),
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Unpublish/Pubish the VanityURL that lives in the SYSTEM_HOST todo: something between here and the previous remove the secondary cache
            //------------------------------------
            filtersUtil.unpublishVanityURL(systemHostVanityURL);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemHostVanityURL.getLanguageId(),
                            systemHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            systemHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            systemHostVanityURL.getLanguageId(),
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);


            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, // todo: failing
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(systemHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //------------------------------------
            //Unpublish the VanityURL that lives in the default site
            //------------------------------------
            filtersUtil.unpublishVanityURL(defaultHostVanityURL);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            defaultHostVanityURL.getLanguageId(),
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            // -- Unpublish
            filtersUtil.unpublishVanityURL(defaultHostVanityURL);

            //Should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            defaultHostVanityURL.getLanguageId(),
                            defaultHostVanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

        } finally {

            contentletAPI.destroy(systemHostVanityURL, user, false);
            contentletAPI.destroy(defaultHostVanityURL, user, false);
        }
    }

    /**
     * Testing how the cache is working when an URL with no matches at all is used
     */
    @Test
    public void cache_404_Test() throws DotDataException, DotSecurityException {

        Contentlet vanityURL = null;

        try {
            long currentTime = System.currentTimeMillis();
            String uri = "/testing" + currentTime;
            String requestedURL = "/nonexisting/should404/index";

            //------------------------------------
            //Create the VanityURL
            //------------------------------------
            vanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            uri, "https://www.google.com", 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURL);

            //Should not exist in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    vanityURL.getLanguageId(),
                                    vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with no matches
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Check the cache, probably the Vanity we created was added in cache by the getLiveCachedVanityUrl
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            //Now, for the requested url we should have a 404_CACHE
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            defaultHost.getIdentifier(),
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
        } finally {

            contentletAPI.destroy(vanityURL, user, false);
        }
    }

    private void publishUnpublishVanityURL(String vanityURI, String requestedURL,
            String vanityHostId)
            throws DotDataException, DotSecurityException {

        Contentlet vanityURL = null;
        long currentTime = System.currentTimeMillis();

        try {

            //------------------------------------
            //Create the VanityURL
            //------------------------------------
            vanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, vanityHostId,
                            vanityURI, "https://www.google.com", 200, 1, defaultLanguageId);
            vanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(vanityURL);

            //Should not exist yet in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(
                            new CacheVanityKey(
                                    vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                    vanityURL.getLanguageId(),
                                    vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ));
            Assert.assertNull(vanityURLCached);

            //Request a matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Validate the cache for this published content
            checkPublished(vanityURL, vanityURI, requestedURL, vanityHostId);

            //------------------------------------
            //Now we need to unpublish out vanity
            //------------------------------------
            vanityURL.setIndexPolicy(IndexPolicy.FORCE);
            vanityURL.setBoolProperty(Contentlet.IS_TEST_MODE, Boolean.TRUE);
            filtersUtil.unpublishVanityURL(vanityURL);
            //And should NOT be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);
            if (!requestedURL.equals(vanityURI)) {
                //Now, for the requested url we should have a 404_CACHE
                vanityURLCached = vanityUrlCache.get(
                        new CacheVanityKey(
                                defaultHost.getIdentifier(),
                                defaultLanguageId,
                                requestedURL
                        ));
                Assert.assertNull(vanityURLCached);
            }

            //Search for the same matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            if (!requestedURL.equals(vanityURI)) {
                //Check the cache, should NOT be in cache as it was unpublised and we are using regex and we have a different request URL
                vanityURLCached = vanityUrlCache.get(
                        new CacheVanityKey(
                                vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                vanityURL.getLanguageId(),
                                vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                        ));
                Assert.assertNull(vanityURLCached);
                //Now, for the requested url we should have a 404_CACHE
                vanityURLCached = vanityUrlCache.get(
                        new CacheVanityKey(
                                defaultHost.getIdentifier(),
                                defaultLanguageId,
                                requestedURL
                        ));
                Assert.assertNotNull(vanityURLCached);
                Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                        vanityURLCached.getVanityUrlId());
            } else {
                //Check the cache, should be in cache but as a 404_CACHE as it was unpublised
                vanityURLCached = vanityUrlCache.get(
                        new CacheVanityKey(
                                vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                vanityURL.getLanguageId(),
                                vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                        ));
                Assert.assertNotNull(vanityURLCached);
                Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                        vanityURLCached.getVanityUrlId());
            }

            //------------------------------------
            //Now publish the Vanity
            //------------------------------------
            vanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(vanityURL);

            //Should NOT exist yet in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);
            if (!requestedURL.equals(vanityURI)) {
                //Now, for the requested url we should have find something in cache
                vanityURLCached = vanityUrlCache.get(
                        new CacheVanityKey(
                                defaultHost.getIdentifier(),
                                defaultLanguageId,
                                requestedURL
                        ));
                Assert.assertNull(vanityURLCached);
            }

            //Request a matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Validate the cache for this published content
            checkPublished(vanityURL, vanityURI, requestedURL, vanityHostId);

        } finally {

            contentletAPI.destroy(vanityURL, user, false);
        }
    }

    private void checkPublished(Contentlet vanityURL, String vanityURI, String requestedURL,
            String vanityHostId)
            throws DotSecurityException, DotDataException {

        CachedVanityUrl vanityURLCached;

        if (!requestedURL.equals(vanityURI)) {
            //Check the cache, should be in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Now, for the requested url we should have find something in cache
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityHostId,
                            defaultLanguageId,
                            requestedURL
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

        } else {
            //Check the cache, we should have this URL in cache now
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURL.getLanguageId(),
                            vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
        }
    }

    /**
     * Creates two vanity url's one with All Sites(SystemHost) and another with defaultHost(demo.dotcms.com), gets both so are cached.
     * Remove the one in the defaultHost, so it's removed from cache but the one in All Sites is still in cache.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void removeVanityURLDefaultHostfromCacheTest() throws DotDataException, DotSecurityException {
        long currentTime = System.currentTimeMillis();
        Contentlet vanityURLContentletDefaultHost = null;
        Contentlet vanityURLContentletAllSites = null;
        try{
            //Creates vanity with default host
            vanityURLContentletDefaultHost = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            "/testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentletDefaultHost);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletDefaultHost.getLanguageId(),
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletDefaultHost.getLanguageId(),
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());

            //Creates vanity with system host
            vanityURLContentletAllSites = filtersUtil
                    .createVanityUrl("test Vanity Url " + currentTime, Host.SYSTEM_HOST,
                            "testing" + currentTime, "https://www.google.com", 200, 1,
                            defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentletAllSites);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletAllSites.getLanguageId(),
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("testing"+currentTime, hostAPI.findSystemHost(), defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletAllSites.getLanguageId(),
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());

            //Unpublish the vanity with default host
            filtersUtil.unpublishVanityURL(vanityURLContentletDefaultHost);
            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletDefaultHost.getLanguageId(),
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletAllSites.getLanguageId(),
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());


        }finally{

            try {
                contentletAPI.destroy(vanityURLContentletDefaultHost, user, false);
                contentletAPI.destroy(vanityURLContentletAllSites, user, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}