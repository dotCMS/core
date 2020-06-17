package com.dotcms.vanityurl.business;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.cache.VanityUrlCache;
import com.dotcms.vanityurl.cache.VanityUrlCacheImpl;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
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
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
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
        String uri = "/" + UUIDGenerator.shorty() + i;
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

        vanityUrlAPI.resolveVanityUrl(uri, defaultHost, defaultLanguage);
        
        
        // if we request directly, the vanity resolves as well
        Optional<CachedVanityUrl> optionalCache = cache.getDirectMapping(uri, defaultHost, defaultLanguage);

        assertEquals(optionalCache.get() , vanity);


    }


    /**
     * This makes sure that if you have a pattern than matches a regular url, e.g. /saftey
     * that it does act like a regex and catch urls below it, e.g. it should not match /saftey/health/preventing-injuries-illnesses
     * @throws Exception
     */

    @Test
    public void test_that_regex_does_not_catch_regular_strings() throws Exception {


        final String title = "VanityURL" + System.currentTimeMillis();
        final String site = defaultHost.getIdentifier();
        final String uri = "/saftey" + System.currentTimeMillis();
        final String forwardTo = "/products/";
        final int action = 301;
        final int order = 1;
        
        final String requestUir=uri + "/health/preventing-injuries-illnesses/get-started-with-safety-health/chemical-safety-basics";

       
        // save and publish the vanity
        filtersUtil.publishVanityUrl(filtersUtil.createVanityUrl(title, site, uri,
                        forwardTo, action, order, defaultLanguage.getId()));
        

        
        Optional<CachedVanityUrl> vanityResolved = vanityUrlAPI.resolveVanityUrl(requestUir, defaultHost, defaultLanguage);

        // does not match /saftey
        assert(!vanityResolved.isPresent());
        
        
        // add a regex, which should now match
        String newUrl = uri + ".*";
        
        // save and publish the vanity
        filtersUtil.publishVanityUrl(filtersUtil.createVanityUrl(title, site, newUrl,
                        forwardTo, action, order, defaultLanguage.getId()));
        
        
        
        vanityResolved = vanityUrlAPI.resolveVanityUrl(requestUir, defaultHost, defaultLanguage);

        // DOES match /saftey.*
        assert(vanityResolved.isPresent());
        

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
    public void resolving_multilingual_vanities_with_default_language_unpublished() throws Exception {
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
        assertEquals(spanishVanity.get().languageId,spanish.getId());
        
        // unpublish the english one
        filtersUtil.unpublishVanityURL(vanityURLContentletEnglish);
        
        
        englishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, defaultLanguage);
        assert(!englishVanity.isPresent());

        
        spanishVanity = vanityUrlAPI.resolveVanityUrl("/testing" + currentTime, defaultHost, spanish);
        assert(spanishVanity.isPresent());


    }

    

    /**
     * Testing how the cache is working with multiple vanities using the same URI and the
     * combinations of existing vanities in other different Sites
     */
    @Test
    public void does_a_site_specific_vanity_override_system_host_vanity()
            throws Exception {

        DefaultVanityUrl siteVanity = null;
        DefaultVanityUrl systemHostVanity = null;



        final long currentTime = System.currentTimeMillis();
        final String uri = "/" + currentTime + "_testing" + "(.*)";
        final String requestedURL = "/" + currentTime + "_testing" + "RANDOM_CHARACTERS";
        final String nonExistingURL = "/nonexisting/should404/index" + currentTime;

        //------------------------------------
        //Create a VanityURL for the default host
        //------------------------------------
        siteVanity = filtersUtil
                .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                        defaultHost.getIdentifier(),
                        uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(siteVanity);
        
        // nothing in cache
        List<CachedVanityUrl> siteCache= cache.getSiteMappings(defaultHost, defaultLanguage);
        assert( siteCache==null);
        
        //None of these exist in cache
        Optional<CachedVanityUrl> vanityURLCached = cache.getDirectMapping(uri, defaultHost, defaultLanguage);
        assert(vanityURLCached ==null );
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        assert(vanityURLCached ==null );
        vanityURLCached = cache.getDirectMapping(nonExistingURL, defaultHost, defaultLanguage);
        assert(vanityURLCached ==null );

        
        // one hit loads cache
        vanityURLCached= vanityUrlAPI.resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
        assert( vanityURLCached.isPresent());
        
        siteCache= cache.getSiteMappings(defaultHost, defaultLanguage);
        
        // it lives in the host cache
        assert( siteCache!=null);
        assert(siteCache.contains(vanityURLCached.get()));
        Optional<CachedVanityUrl> directCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        // and the direct cache
        assertEquals(vanityURLCached.get(), directCached.get());
        
        

        //------------------------------------
        //Create the VanityURL for the SYSTEM_HOST
        //------------------------------------

        systemHostVanity = filtersUtil.createVanityUrl("test Vanity Url " + System.currentTimeMillis(), Host.SYSTEM_HOST, uri,
                        "https://www.google.com", 200, 1, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(systemHostVanity);

        //Should not exist in direct cache (the save/publish wipes cache) or in the host cache
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        Assert.assertNull(vanityURLCached);

        siteCache= cache.getSiteMappings(APILocator.systemHost(), defaultLanguage);
        assert(siteCache==null);
            
        // defaultHost should stil have its host cache
        siteCache= cache.getSiteMappings(defaultHost, defaultLanguage);
        assert(siteCache!=null && !siteCache.isEmpty());

        
        //Request a vanity with a URL from defaultHost
        vanityURLCached = vanityUrlAPI.resolveVanityUrl(requestedURL , defaultHost, defaultLanguage);
        assert(vanityURLCached.isPresent());

        // this is the defaultHost vanity
        assertEquals(vanityURLCached.get().vanityUrlId, siteVanity.getIdentifier());
        
        
        //Request a vanity with a URL from SYSTEM_HOST
        vanityURLCached = vanityUrlAPI.resolveVanityUrl(requestedURL , APILocator.systemHost(), defaultLanguage);
        assert(vanityURLCached.isPresent());
        
        // this is the SYSTEM_HOST vanity
        assertEquals(vanityURLCached.get().vanityUrlId, systemHostVanity.getIdentifier());
        
        // should be in SYSTEM_HOST site cache
        siteCache= cache.getSiteMappings(APILocator.systemHost(), defaultLanguage);
        assert(siteCache.contains(vanityURLCached.get()));


        //Should still be in direct cache for the defaultHost
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        assert(vanityURLCached.isPresent());
        
        
        //------------------------------------
        //Unpublish the defaultHost VanityURL
        //------------------------------------
        systemHostVanity.setIndexPolicy(IndexPolicy.FORCE);
        systemHostVanity.setIndexPolicyDependencies(IndexPolicy.FORCE);
        filtersUtil.unpublishVanityURL(siteVanity);

        //Should not exist in cache
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        Assert.assertNull(vanityURLCached);

        siteCache= cache.getSiteMappings(defaultHost, defaultLanguage);
        assert(siteCache==null);

        //Request a vanity with a URL with a match, will fall back to system host
        vanityURLCached = vanityUrlAPI.resolveVanityUrl(requestedURL , defaultHost, defaultLanguage);
        assert(vanityURLCached.isPresent());
            
        // should NOT be in defaultHost site cache
        siteCache= cache.getSiteMappings(defaultHost, defaultLanguage);
        assert(siteCache!=null);
        assert(!siteCache.contains(vanityURLCached.get()));

        // should be in SYSTEM_HOST site cache
        siteCache= cache.getSiteMappings(APILocator.systemHost(), defaultLanguage);
        assert(siteCache!=null);
        assert(siteCache.contains(vanityURLCached.get()));

        //Should exist in direct cache
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        assert(vanityURLCached.isPresent());
        assertEquals(vanityURLCached.get().vanityUrlId, systemHostVanity.getIdentifier());



    }

    /**
     * Testing how the cache is working with multiple vanities using on different Sites
     */
    @Test
    public void test_publish_unpublish_on_system_vanity()
            throws DotDataException, DotSecurityException {

        Contentlet systemVanityURL = null;


        final long currentTime = System.currentTimeMillis();
        final String uri = "/testing" + currentTime + "(.*)";
        final String requestedURL = "/testing" + currentTime;

        
        Optional<CachedVanityUrl> vanityURLCached= vanityUrlAPI.resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
        assert(!vanityURLCached.isPresent());
        
        
        //------------------------------------
        //Create a VanityURL for the System Host
        //------------------------------------
        systemVanityURL = filtersUtil
                .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                        Host.SYSTEM_HOST,
                        uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
        systemVanityURL.setIndexPolicy(IndexPolicy.FORCE);
        filtersUtil.publishVanityUrl(systemVanityURL);

        vanityURLCached= vanityUrlAPI.resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);


        Assert.assertNotNull(vanityURLCached);
        assert(vanityURLCached.isPresent());


        /////// UNPUBLISH

        filtersUtil.unpublishVanityURL(systemVanityURL);



        /////////

        vanityURLCached = vanityUrlAPI.resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
        assert(!vanityURLCached.isPresent());


    }


    /**
     * Testing how the cache is working with multiple vanities using on different Sites
     */
    @Test
    public void differentSitesTest()
            throws DotDataException, DotSecurityException {

        Contentlet systemHostVanityURL  = null;
        Contentlet defaultHostVanityURL = null;

            final long currentTime = System.currentTimeMillis();
            final String uri = "/testing" + currentTime + "(.*)";
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

            
            Optional<CachedVanityUrl> vanityURLCached = cache.getDirectMapping(uri, defaultHost, defaultLanguage);
            assert(vanityURLCached ==null );
            
            vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached ==null );
            



            //Request a vanity with a URL with a match
            vanityURLCached = vanityUrlAPI
                    .resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached.isPresent() );



            //------------------------------------
            //Create the first VanityURL for the Default Host
            //------------------------------------

            defaultHostVanityURL = filtersUtil
                    .createVanityUrl("test Vanity Url " + System.currentTimeMillis(),
                            defaultHost.getIdentifier(),
                            uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
            defaultHostVanityURL.setIndexPolicy(IndexPolicy.FORCE);
            filtersUtil.publishVanityUrl(defaultHostVanityURL);



            //match from defaultHost
            vanityURLCached = vanityUrlAPI
                    .resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached.isPresent());



            //------------------------------------
            //Unpublish/Pubish the VanityURL that lives in the SYSTEM_HOST 
            //------------------------------------
            filtersUtil.unpublishVanityURL(systemHostVanityURL);

            //NO match from SYSTEM_HOST
            vanityURLCached = vanityUrlAPI
                    .resolveVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguage);
            assert(!vanityURLCached.isPresent());


            //match from defaultHost
            vanityURLCached = vanityUrlAPI
                    .resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached.isPresent());
            
            
            // -- Publish SYSTEM_HOST
            filtersUtil.publishVanityUrl(systemHostVanityURL);

            //match from SYSTEM_HOST
            vanityURLCached = vanityUrlAPI
                    .resolveVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguage);
            assert(vanityURLCached.isPresent());


            // -- Unpublish defaultHost
            filtersUtil.unpublishVanityURL(defaultHostVanityURL);

            //match from defaultHost (falls back to SYSTEM_HOST)
            vanityURLCached = vanityUrlAPI
                            .resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached.isPresent());
            assertEquals(vanityURLCached.get().siteId, Host.SYSTEM_HOST);

            
            
            // -- Publish defaultHost
            filtersUtil.publishVanityUrl(defaultHostVanityURL);

           //match from defaultHost
            vanityURLCached = vanityUrlAPI
                            .resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
            assert(vanityURLCached.isPresent());
            assertEquals(vanityURLCached.get().siteId, defaultHost.getIdentifier());

            
            // -- Unpublish defaultHost
            filtersUtil.unpublishVanityURL(defaultHostVanityURL);
            
            //match from defaultHost (falls back to SYSTEM_HOST)
            vanityURLCached = vanityUrlAPI
                            .resolveVanityUrl(requestedURL, APILocator.systemHost(), defaultLanguage);
             assert(vanityURLCached.isPresent());
            assertEquals(vanityURLCached.get().siteId, Host.SYSTEM_HOST);




        
    }

    /**
     * Testing how the cache is working when an URL with no matches at all is used
     */
    @Test
    public void cache_404_Test() throws DotDataException, DotSecurityException {

        Contentlet vanityURL = null;


        final long currentTime = System.currentTimeMillis();
        final String uri = "/testing" + currentTime;
        final String requestedURL = "/nonexisting/should404/index";

        //------------------------------------
        //Create the VanityURL
        //------------------------------------
        vanityURL = filtersUtil
                .createVanityUrl("test Vanity Url " + currentTime, defaultHost.getIdentifier(),
                        uri, "https://www.google.com", 200, 1, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(vanityURL);

        //Nothing should exist in cache
        Optional<CachedVanityUrl> vanityURLCached = cache.getDirectMapping(uri, defaultHost, defaultLanguage);
        Assert.assertNull(vanityURLCached);
        
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        Assert.assertNull(vanityURLCached);

        //Request a vanity with a URL with no matches
        vanityURLCached = vanityUrlAPI.resolveVanityUrl(requestedURL, defaultHost, defaultLanguage);
        assert(vanityURLCached!=null);
        assert(!vanityURLCached.isPresent());

        //Check the cache, now we get an Optional.empty, which means 404
        vanityURLCached = cache.getDirectMapping(requestedURL, defaultHost, defaultLanguage);
        assert(vanityURLCached!=null);
        assert(!vanityURLCached.isPresent());



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

        final CachedVanityUrl v1 = vanityUrlAPI.resolveVanityUrl(testUri1, host, defaultLanguage).get();
        Assert.assertNotNull(v1);
        Assert.assertNotNull(v1.vanityUrlId);
        Assert.assertEquals(v1.forwardTo,forwardTo);

        final CachedVanityUrl v2 = vanityUrlAPI.resolveVanityUrl(testUri2, host, defaultLanguage).get();
        Assert.assertNotNull(v2);
        Assert.assertNotNull(v2.vanityUrlId);
        Assert.assertEquals(v2.forwardTo,forwardTo);

    }

}