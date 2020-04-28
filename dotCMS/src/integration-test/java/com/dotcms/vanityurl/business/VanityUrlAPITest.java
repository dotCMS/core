package com.dotcms.vanityurl.business;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Optional;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.cache.VanityUrlCacheImpl;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
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
    private static Language defaultLanguage;
    private static VanityUrlCache cache;
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

        cache=new VanityUrlCacheImpl();

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
    public void testing_the_from_contentlet_method() throws Exception {


        long i = System.currentTimeMillis();
        String title = "VanityURL" + i;
        String site = defaultHost.getIdentifier();
        String uri = "/test1_" + i;
        String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
        int action = 200;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());

        VanityUrl vanity1 = vanityUrlAPI.fromContentlet(contentletAPI.find(contentlet1.getInode(), user, false));

        Assert.assertNotNull(vanity1);
        Assert.assertEquals(title, vanity1.getTitle());
        Assert.assertEquals(site, vanity1.getSite());
        Assert.assertEquals(uri, vanity1.getURI());
        Assert.assertEquals(forwardTo, vanity1.getForwardTo());
        Assert.assertEquals(action, vanity1.getAction());
        Assert.assertEquals(order, vanity1.getOrder());

    }
    
    
    
    
    /**
     * Creates a vanity url but no publish it, gets it and checks there is not in cache.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void working_vanities_do_not_resolve() throws Exception {

        Contentlet contentlet1 = null;


        long i = System.currentTimeMillis();
        String title = "VanityURL" + i;
        String site = defaultHost.getIdentifier();
        String uri = "/test1_" + i;
        String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
        int action = 200;
        int order = 1;

        contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());

        
        
        Optional<CachedVanityUrl> optionalCache = cache.getDirectMapping(uri, defaultHost, defaultLanguage);
        assert(optionalCache==null );
        

        Optional<CachedVanityUrl> vanityCached = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);

        // it is not here because it is not published
        assert(!vanityCached.isPresent());

            
        // it is stored in the cache as an Optional.empty();
        optionalCache = cache.getDirectMapping(uri, defaultHost, defaultLanguage);
        
        assert(optionalCache!=null && !optionalCache.isPresent());
            
    }

    /**
     * Creates a couple of vanity urls, publish only the first one.
     * Checks that the first one is in cache and the second one is not.
     *
     * @see VanityUrlAPI
     */
    @Test
    public void vanities_resolve_once_they_are_published() throws Exception {


 
        long i = System.currentTimeMillis();
        String title = "VanityURL" + i;
        String site = defaultHost.getIdentifier();
        String uri = "/test2_1_" + i;
        String forwardTo = "/products/";
        int action = 200;
        int order = 1;

        Contentlet contentlet1 = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());

        
        Optional<CachedVanityUrl> vanityResolved = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);
        
        // there is no vanity as it is not published
        assert(!vanityResolved.isPresent());
        
        
        // publish the vanity
        filtersUtil.publishVanityUrl(contentlet1);

        // resolve again
        vanityResolved = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);
        
        // we hava a vanity
        assert(vanityResolved.isPresent());

        CachedVanityUrl vanity = vanityResolved.get();
        assertEquals(vanity.vanityUrlId, contentlet1.getIdentifier());

        // if we request directly, the vanity resolves as well
        Optional<CachedVanityUrl> optionalCache = cache.getDirectMapping(uri, defaultHost, defaultLanguage);

        assert(optionalCache.get() == vanity);


    }


    /**
     * This test creates a vanity url, gets it so it's cached.
     * Then modify the uri, and checks if the old one is removed from cache.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void vanities_resolve_correctly_after_url_has_been_updated() throws Exception {

        Contentlet vanityURLContentlet = null;
        long i = System.currentTimeMillis();
        String title = "VanityURL" + i;
        String site = defaultHost.getIdentifier();
        String uri = "/test2_1_" + i;
        String forwardTo = "/products/";
        int action = 200;
        int order = 1;

        vanityURLContentlet = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());
            
        filtersUtil.publishVanityUrl(vanityURLContentlet);

        Optional<CachedVanityUrl> vanityResolved = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);

        // we have vanity
        assert(vanityResolved.isPresent());


        // update the vanity
        String newUri = "/testing"+System.currentTimeMillis();
        Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
        vanityURLContentletUpdated.setStringProperty("uri", newUri);
        vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
        filtersUtil.publishVanityUrl(vanityURLContentletUpdated);

        // old vanity is removed
        Optional<CachedVanityUrl> oldVanity= vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);
        assert(!oldVanity.isPresent());
        
        // updated vanity present with the new URI
        Optional<CachedVanityUrl> updatedVanity= vanityUrlAPI.resolveVanityUrl(newUri, defaultHost, defaultLanguage);
        assert(updatedVanity.isPresent());
        assertEquals(updatedVanity.get().url,newUri);
        
        
        

    }

    /**
     * This test creates a vanity url, gets it so it's cached.
     * Then modify the action, and checks if the old one is removed from cache.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void vanities_resolve_correctly_after_action_has_been_updated() throws DotDataException, DotSecurityException {
        Contentlet vanityURLContentlet = null;
        long i = System.currentTimeMillis();
        String title = "VanityURL" + i;
        String site = defaultHost.getIdentifier();
        String uri = "/test2_1_" + i;
        String forwardTo = "/products/";
        int action = 200;
        int order = 1;

        vanityURLContentlet = filtersUtil.createVanityUrl(title, site, uri,
                forwardTo, action, order, defaultLanguage.getId());
            
        filtersUtil.publishVanityUrl(vanityURLContentlet);

        Optional<CachedVanityUrl> vanityResolved = vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);

        // we have vanity
        assert(vanityResolved.isPresent());


        // update the vanity
        Contentlet vanityURLContentletUpdated = contentletAPI.checkout(vanityURLContentlet.getInode(), user, false);
        vanityURLContentletUpdated.setLongProperty("action", 301);
        vanityURLContentletUpdated = contentletAPI.checkin(vanityURLContentletUpdated, user, false);
        filtersUtil.publishVanityUrl(vanityURLContentletUpdated);

        // updated vanity present with the new URI
        Optional<CachedVanityUrl> updatedVanity= vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);
        assert(updatedVanity.isPresent());
        assertEquals(updatedVanity.get().response,301);
    }

    /**
     * Checks the proper validation of not allowed Action codes
     */
    @Test
    public void insure_vanitys_have_proper_response_actions_set()
            throws DotDataException, DotSecurityException, LanguageException {

        long currentTime = System.currentTimeMillis();
        final String expectedInvalidCodeMessage = LanguageUtil
                .get("message.vanity.url.error.invalidAction");
        Contentlet vanityURLContentlet = null;

        vanityURLContentlet = filtersUtil
                .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                        "/testing" + currentTime, "https://www.google.com", 200, 1,
                        defaultLanguage.getId());

        vanityURLContentlet.setIndexPolicy(IndexPolicy.FORCE);
        filtersUtil.publishVanityUrl(vanityURLContentlet);

        Optional<CachedVanityUrl> vanityResolved = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, defaultLanguage);

        // we have vanity
        assert(vanityResolved.isPresent());
        assert(vanityResolved.get().response==200);


        //Now lets try to add an invalid action code, this should throw a DotContentletValidationException
        Contentlet vanityURLContentletUpdated = contentletAPI
                .checkout(vanityURLContentlet.getInode(), user, false);
        vanityURLContentletUpdated.setLongProperty("action", 600);
        vanityURLContentletUpdated.setIndexPolicy(IndexPolicy.FORCE);
        try {
            contentletAPI.checkin(vanityURLContentletUpdated, user, false);
            fail("Using an invalid 600 action code, the checking method should fail...");
        } catch (Exception e) {
            assertEquals(e.getMessage(),"The action code of the Vanity URL is not valid");
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
    public void resolving_multilingual_vanities_with_defualt_language_unpublished() throws DotDataException, DotSecurityException {
        long currentTime = System.currentTimeMillis();
        Contentlet vanityURLContentletEnglish = null;
        Contentlet vanityURLContentletSpanish = null;
        final Language spanish = TestDataUtils.getSpanishLanguage();

        vanityURLContentletEnglish = filtersUtil
                .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                        "/testing" + currentTime, "https://www.google.com", 200, 1,
                        defaultLanguage.getId());
        filtersUtil.publishVanityUrl(vanityURLContentletEnglish);

        Optional<CachedVanityUrl> englishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, defaultLanguage);

        // we have vanity in english
        assert(englishVanity.isPresent());
        assert(englishVanity.get().response==200);

        Optional<CachedVanityUrl> vanityFromCache = cache.getDirectMapping("/testing" + currentTime, defaultHost, defaultLanguage);

        assertEquals(englishVanity.get(),vanityFromCache.get());
        
        vanityURLContentletSpanish = contentletAPI.find(vanityURLContentletEnglish.getInode(), user, false);
        vanityURLContentletSpanish.setInode("");
        vanityURLContentletSpanish.setLanguageId(spanish.getId());
        vanityURLContentletSpanish = contentletAPI.checkin(vanityURLContentletSpanish, user, false);

        Optional<CachedVanityUrl> spanishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, spanish);

        // we do not have a spanish vanity because it is not published
        assert(!spanishVanity.isPresent());
        
        filtersUtil.publishVanityUrl(vanityURLContentletSpanish);

        spanishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, spanish);

        // we have a spanish vanity because it is published
        assert(spanishVanity.isPresent());
        assertEquals(spanishVanity.get().vanityUrlId, englishVanity.get().vanityUrlId);

        // unpublish the english one
        filtersUtil.unpublishVanityURL(vanityURLContentletEnglish);
        
        
        englishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, defaultLanguage);
        assert(!englishVanity.isPresent());

        
        spanishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, spanish);
        assert(spanishVanity.isPresent());


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
                            uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
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
                            defaultLanguage,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with no matches
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(nonExistingURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage);
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
                            defaultLanguage,
                            requestedURL
                    ));

            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage);
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
                            defaultLanguage,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
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
                            defaultLanguage,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match on system and default host
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
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
                            defaultLanguage,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage);
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
                            defaultLanguage,
                            requestedURL1
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL, // todo: failing
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(systemHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguage, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguage, user);
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
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguage, user);
            Assert.assertNotNull(vanityURLCached);
            Assert.assertEquals(VanityUrlAPI.CACHE_404_VANITY_URL,
                    vanityURLCached.getVanityUrlId());

            // -- Publish
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL1, defaultHost, defaultLanguage, user);
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
                            uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
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
                            defaultLanguage,
                            requestedURL
                    ));
            Assert.assertNull(vanityURLCached);

            //Request a vanity with a URL with no matches
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            defaultLanguage,
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
                            vanityURI, "https://www.google.com", 200, 1, defaultLanguage.getId());
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
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                                defaultLanguage,
                                requestedURL
                        ));
                Assert.assertNull(vanityURLCached);
            }

            //Search for the same matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                                defaultLanguage,
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
                                defaultLanguage,
                                requestedURL
                        ));
                Assert.assertNull(vanityURLCached);
            }

            //Request a matching URL
            vanityURLCached = vanityUrlAPI
                    .getLiveCachedVanityUrl(requestedURL, defaultHost, defaultLanguage, user);
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
                            defaultLanguage,
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
                            defaultLanguage.getId());
            filtersUtil.publishVanityUrl(vanityURLContentletDefaultHost);

            CachedVanityUrl vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletDefaultHost.getLanguageId(),
                            vanityURLContentletDefaultHost.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("/testing"+currentTime, defaultHost, defaultLanguage, user);
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
                            defaultLanguage);
            filtersUtil.publishVanityUrl(vanityURLContentletAllSites);

            vanityURLCached = vanityUrlCache.get(
                    new CacheVanityKey(
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                            vanityURLContentletAllSites.getLanguageId(),
                            vanityURLContentletAllSites.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                    ));
            Assert.assertNull(vanityURLCached);

            vanityUrlAPI.getLiveCachedVanityUrl("testing"+currentTime, hostAPI.findSystemHost(), defaultLanguage, user);
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

        final CachedVanityUrl v1 = vanityUrlAPI.getLiveCachedVanityUrl(testUri1, host, defaultLanguage, APILocator.systemUser());
        Assert.assertNotNull(v1);
        Assert.assertNotNull(v1.getVanityUrlId());
        Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,v1.getVanityUrlId());
        Assert.assertEquals(v1.getForwardTo(),forwardTo);

        final CachedVanityUrl v2 = vanityUrlAPI.getLiveCachedVanityUrl(testUri2, host, defaultLanguage, APILocator.systemUser());
        Assert.assertNotNull(v2);
        Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,v2.getVanityUrlId());
        Assert.assertEquals(v2.getForwardTo(),forwardTo);

        final CachedVanityUrl vanityURLCached = vanityUrlCache.get(new CacheVanityKey(
                vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                vanityURL.getLanguageId(),
                vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
        ));
        Assert.assertNotNull(vanityURLCached);
        Assert.assertNotEquals(VanityUrlAPI.CACHE_404_VANITY_URL,vanityURLCached.getVanityUrlId());
    }

}