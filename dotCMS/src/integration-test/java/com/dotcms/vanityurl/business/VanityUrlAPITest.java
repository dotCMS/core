package com.dotcms.vanityurl.business;


import com.dotcms.cache.VanityUrlCache;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
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
    private static LanguageAPI languageAPI;
    private static long defaultLanguageId;
    private static ContentTypeAPI contentTypeAPI;
    private static PermissionAPI permissionAPI;
    private static ContentType contentType;
    private static VanityUrlCache vanityUrlCache;

    private static final String VANITY_URL_CONTENT_TYPE_NAME = "Vanity URL";
    private static final String VANITY_URL_CONTENT_TYPE_VARNAME = "Vanityurl";

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
        languageAPI = APILocator.getLanguageAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        permissionAPI = APILocator.getPermissionAPI();

        /* Load Cache */
        vanityUrlCache = CacheLocator.getVanityURLCache();

        /* Default variables */
        defaultHost = hostAPI.findDefaultHost(user, false);
        defaultLanguageId = languageAPI.getDefaultLanguage().getId();
        getContentType();
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

            contentlet1 = createVanityUrl(title, site, uri,
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
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
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

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test2_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet1));
            Assert.assertNull(vanityURLCached);

            CachedVanityUrl vanity = vanityUrlAPI.getLiveCachedVanityUrl(uri, defaultHost, defaultLanguageId, user);

            Assert.assertNotNull(vanity);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity.getVanityUrlId());
            Assert.assertEquals(site, vanity.getSiteId());
            Assert.assertEquals(uri, vanity.getUrl());
            Assert.assertEquals(forwardTo, vanity.getForwardTo());
            Assert.assertEquals(action, vanity.getResponse());

            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet1));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanityURLCached.getVanityUrlId());

            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet2));
            Assert.assertNull(vanityURLCached);

            vanity = vanityUrlAPI.getLiveCachedVanityUrl(uri2, defaultHost, defaultLanguageId, user);

            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanity.getVanityUrlId());

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }

                if (contentlet2 != null) {
                    contentletAPI.delete(contentlet2, user, false);
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

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

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
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getActiveVanityUrls(com.liferay.portal.model.User)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getActiveVanityUrls() {

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        Contentlet contentlet3 = null;
        Contentlet contentlet4 = null;
        try {

            List<VanityUrl> currentActiveVanityUrls = vanityUrlAPI.getActiveVanityUrls(user);
            int initialActiveVanityUrlsCount = currentActiveVanityUrls.size();

            long i = System.currentTimeMillis();

            //Live
            String title = "VanityURL_1_" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test4_1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test4_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            //Live
            String title3 = "VanityURL_3_" + i;
            String site3 = defaultHost.getIdentifier();
            String uri3 = "/test4_3_" + i;
            String forwardTo3 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action3 = 500;
            int order3 = 1;

            contentlet3 = createVanityUrl(title3, site3, uri3,
                    forwardTo3, action3, order3, defaultLanguageId);

            publishVanityUrl(contentlet3);

            //not Live
            String title4 = "VanityURL_4_" + i;
            String site4 = defaultHost.getIdentifier();
            String uri4 = "/test4_4_" + i;
            String forwardTo4 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action4 = 200;
            int order4 = 1;

            contentlet4 = createVanityUrl(title4, site4, uri4,
                    forwardTo4, action4, order4, defaultLanguageId);

            List<VanityUrl> vanityURLs = vanityUrlAPI.getActiveVanityUrls(user);
            
            Assert.assertTrue(vanityURLs.size() > initialActiveVanityUrlsCount);
            Assert.assertTrue(vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet1)));
            Assert.assertTrue(!vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet2)));
            Assert.assertTrue(vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet3)));
            Assert.assertTrue(!vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet4)));

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
                if (contentlet2 != null) {
                    contentletAPI.delete(contentlet2, user, false);
                }
                if (contentlet3 != null) {
                    contentletAPI.delete(contentlet3, user, false);
                }
                if (contentlet4 != null) {
                    contentletAPI.delete(contentlet4, user, false);
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
    		vanityURLContentlet = this.createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier() , "/testing"+currentTime , "https://www.google.com", 200, 1, defaultLanguageId);
    		publishVanityUrl(vanityURLContentlet);
    		contentletAPI.isInodeIndexed(vanityURLContentlet.getInode(), true);
    		
    		CachedVanityUrl vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentlet));
    		Assert.assertNull(vanityURLCached);
            
            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentlet));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals("/testing"+currentTime, vanityURLCached.getUrl());
            
            currentTime = System.currentTimeMillis();
            Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setStringProperty("uri", "/testing"+currentTime);
            vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            publishVanityUrl(vanityURLContentletUpdated);
            contentletAPI.isInodeIndexed(vanityURLContentletUpdated.getInode(), true);
            
            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentlet));
            Assert.assertNull(vanityURLCached);
            
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletUpdated));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals("/testing"+currentTime, vanityURLCached.getUrl());
            
    	}finally{
            contentletAPI.delete(vanityURLContentlet, user, false);
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
    		vanityURLContentlet = this.createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier() , "/testing"+currentTime , "https://www.google.com", 200, 1, defaultLanguageId);
    		publishVanityUrl(vanityURLContentlet);
    		contentletAPI.isInodeIndexed(vanityURLContentlet.getInode(), true);
    		
    		CachedVanityUrl vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentlet));
    		Assert.assertNull(vanityURLCached);
            
            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentlet));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals(200, vanityURLCached.getResponse());
            
            Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
            vanityURLContentletUpdated.setLongProperty("action", 403);
            vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            publishVanityUrl(vanityURLContentletUpdated);
            contentletAPI.isInodeIndexed(vanityURLContentletUpdated.getInode(), true);
            
            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletUpdated));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            Assert.assertEquals(403, vanityURLCached.getResponse());
            
    	}finally{
            contentletAPI.delete(vanityURLContentlet, user, false);
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
    public void removeVanityURLDefaultLanguaguefromCacheTest() throws DotDataException, DotSecurityException {
    	long currentTime = System.currentTimeMillis();
    	Contentlet vanityURLContentletEnglish = null;
    	Contentlet vanityURLContentletSpanish = null;
    	int spanishLang = 2;
    	try{
    		vanityURLContentletEnglish = this.createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier() , "/testing"+currentTime , "https://www.google.com", 200, 1, defaultLanguageId);
    		publishVanityUrl(vanityURLContentletEnglish);
    		contentletAPI.isInodeIndexed(vanityURLContentletEnglish.getInode(), true);
    		
    		CachedVanityUrl vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletEnglish));
    		Assert.assertNull(vanityURLCached);
            
    		vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletEnglish));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            vanityURLContentletSpanish = contentletAPI.find(vanityURLContentletEnglish.getInode(), user, false);
            vanityURLContentletSpanish.setInode("");
            vanityURLContentletSpanish.setLanguageId(spanishLang);
            vanityURLContentletSpanish = contentletAPI.checkin(vanityURLContentletSpanish, user, false);
            
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, spanishLang, user).getVanityUrlId());
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletSpanish));
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            publishVanityUrl(vanityURLContentletSpanish);
            contentletAPI.isInodeIndexed(vanityURLContentletSpanish.getInode(), true);
    		
            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, spanishLang, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletSpanish));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            contentletAPI.unpublish(vanityURLContentletEnglish, user, false);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletEnglish));
    		Assert.assertNull(vanityURLCached);
    		
    		vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletSpanish));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
    		
            
    	}finally{
    		contentletAPI.archive(vanityURLContentletSpanish, user, false);
    		contentletAPI.archive(vanityURLContentletEnglish, user, false);
    		contentletAPI.delete(vanityURLContentletSpanish, user, false);
    		contentletAPI.delete(vanityURLContentletEnglish, user, false);
    	}
    }

    /**
     * Testing how the cache is working when publishing and unpublishing a VanityURL, we are
     * simulating URL requests that makes exact match with the URI of the VanityURL
     */
    @Test
    public void publishUnpublishVanityURLExact() throws DotDataException, DotSecurityException {

        long currentTime = System.currentTimeMillis();
        publishUnpublishVanityURL("/testing" + currentTime, "/testing" + currentTime);
    }

    /**
     * Testing how the cache is working when publishing and unpublishing a VanityURL, on this test
     * we created a VanityURL with a regex
     */
    @Test
    public void publishUnpublishVanityURLRegex() throws DotDataException, DotSecurityException {

        long currentTime = System.currentTimeMillis();
        publishUnpublishVanityURL("/testing" + currentTime + "(.*)",
                "/testing" + currentTime + "/testing/index");
    }

    private void publishUnpublishVanityURL(String vanityURI, String requestedURL)
            throws DotDataException, DotSecurityException {

        Contentlet vanityURL = null;
        long currentTime = System.currentTimeMillis();

        try {

            //------------------------------------
            //Create the VanityURL
            //------------------------------------
            vanityURL = this
                    .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                            vanityURI, "https://www.google.com", 200, 1, defaultLanguageId);
            publishVanityUrl(vanityURL);
            contentletAPI.isInodeIndexed(vanityURL.getInode(), true);

            //Should not exist yet in cache
            CachedVanityUrl vanityURLCached = vanityUrlCache
                    .get(VanityUrlUtil.sanitizeKey(vanityURL));
            Assert.assertNull(vanityURLCached);

            //Request a matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Validate the cache for this published content
            checkPublished(vanityURL, vanityURI, requestedURL);

            //------------------------------------
            //Now we need to unpublish out vanity
            //------------------------------------
            contentletAPI.unpublish(vanityURL, user, false);
            //And should NOT be in cache
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
            Assert.assertNull(vanityURLCached);
            if (!requestedURL.equals(vanityURI)) {
                //Now, for the requested url we should have a 404_CACHE
                vanityURLCached = vanityUrlCache.get(VanityUrlUtil
                        .sanitizeKey(defaultHost.getIdentifier(), requestedURL, defaultLanguageId));
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
                vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
                Assert.assertNull(vanityURLCached);
                //Now, for the requested url we should have a 404_CACHE
                vanityURLCached = vanityUrlCache.get(VanityUrlUtil
                        .sanitizeKey(defaultHost.getIdentifier(), requestedURL, defaultLanguageId));
                Assert.assertNotNull(vanityURLCached);
                Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                        vanityURLCached.getVanityUrlId());
            } else {
                //Check the cache, should be in cache but as a 404_CACHE as it was unpublised
                vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
                Assert.assertNotNull(vanityURLCached);
                Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                        vanityURLCached.getVanityUrlId());
            }

            //------------------------------------
            //Now publish the Vanity
            //------------------------------------
            publishVanityUrl(vanityURL);
            contentletAPI.isInodeIndexed(vanityURL.getInode(), true);

            //Should NOT exist yet in cache
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
            Assert.assertNull(vanityURLCached);
            if (!requestedURL.equals(vanityURI)) {
                //Now, for the requested url we should have find something in cache
                vanityURLCached = vanityUrlCache.get(VanityUrlUtil
                        .sanitizeKey(defaultHost.getIdentifier(), requestedURL, defaultLanguageId));
                Assert.assertNull(vanityURLCached);
            }

            //Request a matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguageId, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Validate the cache for this published content
            checkPublished(vanityURL, vanityURI, requestedURL);

        } finally {
            contentletAPI.archive(vanityURL, user, false);
            contentletAPI.delete(vanityURL, user, false);
        }
    }

    private void checkPublished (Contentlet vanityURL, String vanityURI, String requestedURL)
            throws DotSecurityException, DotDataException {

        CachedVanityUrl vanityURLCached;

        if (!requestedURL.equals(vanityURI)) {
            //Check the cache, should be in cache
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());
            //Now, for the requested url we should have find something in cache
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil
                    .sanitizeKey(defaultHost.getIdentifier(), requestedURL, defaultLanguageId));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

        } else {
            //Check the cache, we should have this URL in cache now
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURL));
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
    		vanityURLContentletDefaultHost = this.createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier() , "/testing"+currentTime , "https://www.google.com", 200, 1, defaultLanguageId);
    		publishVanityUrl(vanityURLContentletDefaultHost);
    		contentletAPI.isInodeIndexed(vanityURLContentletDefaultHost.getInode(), true);
    		
    		CachedVanityUrl vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletDefaultHost));
    		Assert.assertNull(vanityURLCached);
            
    		vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletDefaultHost));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            //Creates vanity with system host
            vanityURLContentletAllSites = this.createVanityUrl("test Vanity Url " + currentTime, Host.SYSTEM_HOST , "testing"+currentTime , "https://www.google.com", 200, 1, defaultLanguageId);
    		publishVanityUrl(vanityURLContentletAllSites);
    		contentletAPI.isInodeIndexed(vanityURLContentletAllSites.getInode(), true);
    		
    		vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletAllSites));
    		Assert.assertNull(vanityURLCached);
            
    		vanityUrlAPI.getLiveCachedVanityUrl("testing"+currentTime, hostAPI.findSystemHost(), defaultLanguageId, user);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletAllSites));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            //Unpublish the vanity with default host
            contentletAPI.unpublish(vanityURLContentletDefaultHost, user, false);
            vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletDefaultHost));
    		Assert.assertNull(vanityURLCached);
    		
    		vanityURLCached = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(vanityURLContentletAllSites));
            Assert.assertNotNull(vanityURLCached);
            Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL, vanityURLCached.getVanityUrlId());
            
            
    	}finally{
            contentletAPI.delete(vanityURLContentletDefaultHost, user, false);
            contentletAPI.delete(vanityURLContentletAllSites, user, false);
    	}
    }

    /**
     * Get the Vanity Url Content Type. If the content Type doesn't
     * exist then it will be created
     *
     * @return a Vanity Url Content Type
     */
    private static ContentType getContentType() throws DotDataException, DotSecurityException {
        String query = " velocity_var_name = '" + VANITY_URL_CONTENT_TYPE_VARNAME + "'";
        List<ContentType> contentTypes = contentTypeAPI.search(query);

        if (contentTypes.size() == 0) {
            contentType = createVanityUrl();
        } else {
            contentType = contentTypes.get(0);
        }

        return contentType;
    }

    /**
     * Create a VanityUrl content type
     *
     * @return A new vanity Url content Type
     */
    private static ContentType createVanityUrl() throws DotDataException, DotSecurityException {
        BaseContentType base = BaseContentType
                .getBaseContentType(BaseContentType.VANITY_URL.getType());

        final ContentType type = new ContentType() {
            @Override
            public String name() {
                return VANITY_URL_CONTENT_TYPE_NAME;
            }

            @Override
            public String id() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String variable() {
                return VANITY_URL_CONTENT_TYPE_VARNAME;
            }

            @Override
            public BaseContentType baseType() {
                return base;
            }
        };

        return contentTypeAPI.save(new ImplClassContentTypeTransformer(type).from());
    }

    /**
     * Creates a new Vanity URL contentlet
     */
    private Contentlet createVanityUrl(String title, String site, String uri,
            String forwardTo, int action, int order, long languageId)
            throws DotDataException, DotSecurityException {
        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(site);
        contentlet.setLanguageId(languageId);

        contentlet.setStringProperty(VanityUrlContentType.TITLE_FIELD_VAR, title);
        contentlet.setStringProperty(VanityUrlContentType.SITE_FIELD_VAR, site);
        contentlet.setStringProperty(VanityUrlContentType.URI_FIELD_VAR, uri);
        contentlet.setStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, forwardTo);
        contentlet.setLongProperty(VanityUrlContentType.ACTION_FIELD_VAR, action);
        contentlet.setLongProperty(VanityUrlContentType.ORDER_FIELD_VAR, order);

        //Get The permissions of the Content Type
        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);

        //Validate if the contenlet is OK
        contentletAPI.validateContentlet(contentlet, new ArrayList());

        //Save the contentlet
        contentlet = contentletAPI.checkin(contentlet, contentTypePermissions, user, true);
        contentletAPI.isInodeIndexed(contentlet.getInode());

        return contentlet;
    }

    /**
     * Publish the Vanity URL contentlet
     */
    private void publishVanityUrl(Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        //Publish Vanity Url
        contentletAPI.publish(contentlet, user, false);
        contentletAPI.isInodeIndexed(contentlet.getInode(),true);
    }
}
