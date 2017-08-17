package com.dotcms.vanityurl.business;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.toImmutableList;

import com.dotcms.business.CloseDB;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.collections.keyvalue.MultiKey;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.elasticsearch.indices.IndexMissingException;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {
    
    private final Set<Integer> allowedActions = new ImmutableSet.Builder<Integer>().add(200).add(301).add(302).build();

    private final ContentletAPI contentletAPI;
    private final VanityUrlServices vanityUrlServices;

    private static final String GET_VANITY_URL_BASE_TYPE =
            "+baseType:" + BaseContentType.VANITY_URL.getType();
    private static final String GET_ACTIVE_VANITY_URL =
            GET_VANITY_URL_BASE_TYPE + " +live:true +deleted:false";
    private static final String GET_VANITY_URL_LANGUAGE_ID = " +languageId:";
    private static final int CODE_404_VALUE = 404;

    public VanityUrlAPIImpl() {
        this(APILocator.getContentletAPI(),
                VanityUrlServices.getInstance());
    }

    @VisibleForTesting
    public VanityUrlAPIImpl(final ContentletAPI contentletAPI,
            final VanityUrlServices vanityUrlServices) {

        this.contentletAPI = contentletAPI;
        this.vanityUrlServices = vanityUrlServices;
    }

    @CloseDB
    @Override
    public void initializeVanityURLsCache(final User user) {
        searchAndPopulate(GET_ACTIVE_VANITY_URL, user, null, null, true);
    }

    /**
     * Searches and populates the cache for live VanityURLs by Site and Language id, each VanityURL
     * found is added into the cache. <br> Note this method does not uses cache, always does the ES
     * search, the intention of this method is mainly to populate the cache with the found data.
     *
     * @param user The current user
     */
    private void initializeActiveVanityURLsCacheBySiteAndLanguage(String siteId,
            final long languageId, final User user) {

        String HOST_QUERY;
        Boolean includedSystemHost = Boolean.FALSE;

        if (null != siteId && !siteId.equals(Host.SYSTEM_HOST)) {

            //Verify if we already have cache values for the given host and the System Host
            final List<CachedVanityUrl> foundVanities = getVanityUrlBySiteAndLanguageFromCache(siteId,
                    languageId,
                    false);

            final List<CachedVanityUrl> foundSystemHostVanities = getVanityUrlBySiteAndLanguageFromCache(
                    Host.SYSTEM_HOST, languageId, false);

            //No cache was initialized, we need to include both hosts in the search
            if (null == foundVanities && null == foundSystemHostVanities) {
                includedSystemHost = Boolean.TRUE;
                HOST_QUERY = String.format(" +(conhost:%s conhost:%s)", siteId, Host.SYSTEM_HOST);
            } else {
                if (null == foundVanities) {//We just need to initialize the values for the given host
                    HOST_QUERY = String.format(" +(conhost:%s)", siteId);
                } else {
                    siteId = Host.SYSTEM_HOST;//We just need to initialize the values for the given System Host
                    HOST_QUERY = String.format(" +(conhost:%s)", Host.SYSTEM_HOST);
                }
            }
        } else {
            HOST_QUERY = String.format(" +conhost:%s", Host.SYSTEM_HOST);
        }

        final String luceneQuery = GET_ACTIVE_VANITY_URL + HOST_QUERY
                + GET_VANITY_URL_LANGUAGE_ID
                + languageId;
        searchAndPopulate(luceneQuery, user, siteId, languageId, includedSystemHost);
    }

    @CloseDB
    @Override
    public VanityUrl getVanityUrlFromContentlet(final Contentlet con) {
        if (con != null) {
            try {
                if (!con.isVanityUrl()) {
                    throw new DotStateException(
                            "Contentlet : " + con.getInode() + " is not a Vanity Url");
                }
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException(
                        "Contentlet : " + con.getInode() + " is not a Vanity Url", e);
            }
        } else {
            throw new DotStateException("Contentlet is null");
        }

        DefaultVanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setContentTypeId(con.getContentTypeId());
        try {
            contentletAPI.copyProperties(vanityUrl, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Vanity Url Copy Failed", e);
        }
        vanityUrl.setHost(con.getHost());
        vanityUrl.setFolder(con.getFolder());

        return vanityUrl;
    }

    /**
     * Add the Vanity URL to the vanityURLCache
     *
     * @param vanityUrl The vanityurl URL object
     */
    private void addToVanityURLCache(VanityUrl vanityUrl) {
        try {
            if (vanityUrl.isLive()) {
                vanityUrlServices.updateCache(vanityUrl);
            } else {
                vanityUrlServices.invalidateVanityUrl(vanityUrl);
            }
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(this,
                    "Error trying to add Vanity URL identifier:" + vanityUrl.getIdentifier()
                            + " to VanityURLCache", e);
        }
    }

    /**
     * Add the Vanity URL to the vanityURLCache, without affecting any secondary cache.
     *
     * @param vanityUrl The vanityurl URL object
     */
    private void addToSingleVanityURLCache(final VanityUrl vanityUrl) {
        try {
            if (vanityUrl.isLive()) {
                vanityUrlServices.addSingleCache(vanityUrl);
            } else {
                vanityUrlServices.invalidateVanityUrl(vanityUrl);
            }
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(this,
                    "Error trying to add Vanity URL identifier:" + vanityUrl.getIdentifier()
                            + " to VanityURLCache", e);
        }
    }

    /**
     * Add a 404 Vanity URL to the vanityURLCache
     *
     * @param siteId The current site Id
     * @param uri The current URI
     * @param languageId The current language Id
     */
    private void add404URIToCache(String siteId, String uri, long languageId) {

        VanityUrl cache404VanityUrl = new DefaultVanityUrl();
        cache404VanityUrl.setInode(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setIdentifier(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setAction(CODE_404_VALUE);
        cache404VanityUrl.setLanguageId(languageId);
        cache404VanityUrl.setURI(uri);
        cache404VanityUrl.setSite(siteId);
        cache404VanityUrl.setOrder(0);

        vanityUrlServices.updateCache(cache404VanityUrl);
    }

    private String getSiteId (final Host site) {

        return
            (null != site && !site.isSystemHost())?
                        site.getIdentifier():Host.SYSTEM_HOST;

    }

    @CloseDB
    @Override
    public CachedVanityUrl getLiveCachedVanityUrl(final String uri, final Host site,
            final long languageId, final User user) {

        final String siteId = this.getSiteId(site);

        //First lets try with the cache
        CachedVanityUrl result = vanityUrlServices
                .getCachedVanityUrlByUri(uri, siteId, languageId);

        if (patternMatches(result, uri)) {
            return result;
        } else {
            //Search for the URI in the vanityURL cached by site and language Ids
            result = searchLiveCachedVanityUrlBySiteAndLanguage(uri, siteId, languageId);
        }

        if (null == result) {

            try {

                //Search for the list of ContentTypes of type VanityURL
                List<ContentType> vanityUrlContentTypes = APILocator.getContentTypeAPI(user)
                        .findByType(BaseContentType.VANITY_URL);

                //Verify if this Content Type has a Multilinguable fallback
                if (!vanityUrlContentTypes.isEmpty()
                        && ((VanityUrlContentType) vanityUrlContentTypes.get(0)).fallback()) {

                    //if the fallback is set then is going to try to get it by the default language
                    long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage()
                            .getId();

                    result = vanityUrlServices
                            .getCachedVanityUrlByUri(uri, siteId, defaultLanguageId);

                    if (patternMatches(result, uri)) {
                        return result;
                    } else {
                        //Search for the URI in the vanityURL cached by site and default language Ids
                        result = searchLiveCachedVanityUrlBySiteAndLanguage(uri, siteId,
                                defaultLanguageId);
                    }
                }

            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error searching for Vanity URL by URI", e);
            }

            if (result == null) {
                //Add 404 to cache
                add404URIToCache(siteId, uri, languageId);
                result = vanityUrlServices
                        .getCachedVanityUrlByUri(uri, siteId, languageId);
            }
        }

        return result;
    }

    /**
     * Validate if the CachedVanityUrl pattern matches the uri
     *
     * @param cachedVanityUrl The CachedVanityUrl
     * @param uri The current uri
     * @return true if the CachedVanityUrl match, false if not
     */
    private boolean patternMatches(CachedVanityUrl cachedVanityUrl, String uri) {
        boolean patternMatches = false;
        if (cachedVanityUrl != null) {
            Matcher matcher = cachedVanityUrl.getPattern().matcher(uri);
            patternMatches = matcher.matches();
        }
        return patternMatches;
    }

    /**
     * Search CachedVanity Url checking if the Uri is in the Site Id and language Id cache
     *
     * @param uri The current uri
     * @param siteId the current site Id
     * @param languageId the current language Id
     * @return a CachedVanityUrl object
     */
    private CachedVanityUrl searchLiveCachedVanityUrlBySiteAndLanguage(final String uri,
            final String siteId, final long languageId) {

        CachedVanityUrl result = null;

        //Get the list of site cached Vanities URLs
        List<CachedVanityUrl> cachedVanityUrls = getVanityUrlBySiteAndLanguageFromCache(siteId,
                languageId,
                true);

        if (null == cachedVanityUrls) {

            synchronized (VanityUrlAPIImpl.class) {

                cachedVanityUrls = getVanityUrlBySiteAndLanguageFromCache(siteId, languageId, true);

                if (null == cachedVanityUrls) {

                    //Initialize the Cached Vanity URL cache if is null
                    initializeActiveVanityURLsCacheBySiteAndLanguage(siteId, languageId,
                            APILocator.systemUser());

                    //Get the list of site cached Vanities URLs
                    cachedVanityUrls = getVanityUrlBySiteAndLanguageFromCache(siteId, languageId,
                            true);
                }
            }
        }

        if (null != cachedVanityUrls) {
            //Validates if onw of the site cachedVanityUrls matches the uri
            for (CachedVanityUrl vanity : cachedVanityUrls) {
                if (patternMatches(vanity, uri)) {
                    result = vanity;
                    break;
                }
            }
        }

        /*
        At this point we already found and saved in cache the VanityURL that matches with the given
        URL but adding a new VanityURL to the cache for the completed requested URL
        and not just the regex used can save us time.
         */
        if (null != result && !result.getUrl().equals(uri)) {
            CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(result, uri);
            vanityUrlServices.updateCache(cachedVanityUrl);
        }

        return result;
    }

    /**
     * Using a given lucene query this method searches for VanityURLs, each VanityURL found is
     * added into the cache.
     * <br>
     * Note this method does not uses cache, always does the ES search, the intention of this method
     * is mainly to populate the cache with the found data.
     *
     * @param luceneQuery query for the ES search
     * @param user to use in the ES search
     * @param includedSystemHostOnLuceneQuery True is the search will include the System Host
     * @return A list of VanityURLs
     */
    private void searchAndPopulate(final String luceneQuery, final User user,
            String siteId, Long languageId, final Boolean includedSystemHostOnLuceneQuery) {

        try {

            final List<Contentlet> contentResults = contentletAPI
                    .search(luceneQuery, 0, 0, StringPool.BLANK, user, false);

            //Verify if we found something
            if (null == contentResults || contentResults.isEmpty()) {

                //Empty is a valid cache value
                this.setEmptyCaches(siteId, languageId, includedSystemHostOnLuceneQuery);
                return;
            }

            final List<VanityUrl> vanityUrls = contentResults.stream()
                    .map(this::getVanityUrlFromContentlet)
                    .sorted(Comparator.comparing(VanityUrl::getOrder))
                    .collect(toImmutableList());

            // adds to caches.
            vanityUrls.forEach(this::addToSingleVanityURLCache);
            this.addSecondaryVanityURLCacheCollection (vanityUrls);

            /*
             * If a site was sent we need to make sure it was initialized in the cache
             */
            if (null != siteId && null != languageId) {

                this.checkSiteLanguageVanities(siteId, languageId, includedSystemHostOnLuceneQuery);
            }
        } catch (IndexMissingException e) {
            /*
			 * We catch this exception in order to avoid to stop the
			 * initialization of dotCMS if for some reason at this point we
			 * don't have indexes.
			 */
            Logger.error(this, "Error when initializing Vanity URLs, no index found ", e);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching for active Vanity URLs [" + luceneQuery + "]", e);
        } catch (Exception e) {
            if (e.getCause() instanceof IndexMissingException) {
                /*
				 * We catch this exception in order to avoid to stop the
				 * initialization of dotCMS if for some reason at this point we
				 * don't have indexes.
				 */
                Logger.error(this, "Error when initializing Vanity URLs, no index found ", e);
            } else {
                throw new DotRuntimeException("Error searching and populating the Vanity URL Cache",
                        e);
            }
        }
    }

    private void checkSiteLanguageVanities(final String siteId,
                                           final Long languageId,
                                           final Boolean includedSystemHostOnLuceneQuery) {

        if (null == this.vanityUrlServices.getCachedVanityUrlList(siteId, languageId)) {

            this.vanityUrlServices.setCachedVanityUrlList(siteId, languageId, Collections.EMPTY_LIST);
        }

        if (includedSystemHostOnLuceneQuery && !Host.SYSTEM_HOST.equals(siteId) &&
                null == this.vanityUrlServices.getCachedVanityUrlList(Host.SYSTEM_HOST, languageId)) {

            this.vanityUrlServices.setCachedVanityUrlList(Host.SYSTEM_HOST, languageId, Collections.EMPTY_LIST);
        }
    } // checkSiteLanguageVanities.

    private void setEmptyCaches(final String siteId,
                                final Long languageId,
                                final Boolean includedSystemHostOnLuceneQuery) {

        if (null != siteId && null != languageId) {
            this.vanityUrlServices.setCachedVanityUrlList(
                    siteId, languageId,
                    Collections.EMPTY_LIST);

            if (includedSystemHostOnLuceneQuery && !Host.SYSTEM_HOST.equals(siteId)) {
                this.vanityUrlServices.setCachedVanityUrlList(
                        Host.SYSTEM_HOST, languageId,
                        Collections.EMPTY_LIST);
            }
        }
    } // setEmptyCaches.

    private void addSecondaryVanityURLCacheCollection(final List<VanityUrl> vanityUrls) {

        final Map<SiteLanguageKey, ImmutableList.Builder<CachedVanityUrl>> vanityPerSiteLanguageMap
                = map();
        SiteLanguageKey key = null;

        for (final VanityUrl vanityUrl : vanityUrls) {

            key = new SiteLanguageKey(vanityUrl.getSite(), vanityUrl.getLanguageId());
            if (!vanityPerSiteLanguageMap.containsKey(key)) {

                vanityPerSiteLanguageMap.put(key, new ImmutableList.Builder());
            }

            vanityPerSiteLanguageMap.get(key).add(new CachedVanityUrl(vanityUrl));
        }

        vanityPerSiteLanguageMap.forEach( (k, vanityUrlBuilder) ->
                    this.vanityUrlServices.setCachedVanityUrlList (k.hostId(), k.languageId(), vanityUrlBuilder.build()));
    } // addSecondaryVanityURLCacheCollection.

    @CloseDB
    @Override
    public void validateVanityUrl(Contentlet contentlet) {

        VanityUrl vanityUrl = getVanityUrlFromContentlet(contentlet);
        User user;
        try {
            user = APILocator.getUserAPI().loadUserById(contentlet.getModUser());
        } catch (Exception e) {
            Logger.debug(this,e.getMessage(),e);
            try {
                user = APILocator.getUserAPI().getSystemUser();
            } catch (DotDataException e1) {
                Logger.debug(this,e1.getMessage(),e1);
                throw new DotContentletValidationException("User Not Found");
            }
        }
        
        if(!allowedActions.contains(vanityUrl.getAction())){
            Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
            String message = APILocator.getLanguageAPI()
                    .getStringKey(l, "message.vanity.url.error.invalidAction");

            throw new DotContentletValidationException(message);
        }

        if (!VanityUrlUtil.isValidRegex(vanityUrl.getURI())) {
            Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
            String message = APILocator.getLanguageAPI()
                    .getStringKey(l, "message.vanity.url.error.invalidURIPattern");

            throw new DotContentletValidationException(message);
        }

    }

    /**
     * Get the list of cached Vanity URLs associated to a given site and SYSTEM_HOST
     *
     * @param siteId The current site Id
     * @param languageId The current language Id
     * @param includeSystemHost True if we want to include in the result the SYSTEM_HOST cache
     * contents
     * @return A set of CachedVanityUrl, if null is because the cache needs to be initialized
     */
    private List<CachedVanityUrl> getVanityUrlBySiteAndLanguageFromCache(final String siteId,
            final long languageId,
            final Boolean includeSystemHost) {

        //First search in cache with the given site id
        List<CachedVanityUrl> foundVanities =
                this.vanityUrlServices.getCachedVanityUrlList(siteId, languageId);

        //null means we need to initialize the cache for this site
        if (null != foundVanities &&
                includeSystemHost &&
                !siteId.equals(Host.SYSTEM_HOST)) {
            //Now search in cache with the SYSTEM_HOST
            final List<CachedVanityUrl> systemHostFoundVanities =
                    this.vanityUrlServices.getCachedVanityUrlList(Host.SYSTEM_HOST, languageId);

            if (null != systemHostFoundVanities) {
                foundVanities = ImmutableList.<CachedVanityUrl>builder()
                        .addAll(foundVanities)
                        .addAll(systemHostFoundVanities)
                        .build();
            } else {
                //This means we need to initialize the cache for the SYSTEM_HOST
                foundVanities = null;
            }
        }

        return foundVanities;
    } // getVanityUrlBySiteAndLanguageFromCache.

    private static class SiteLanguageKey extends MultiKey {

        protected SiteLanguageKey(final String hostId, final long languageId) {
            super(hostId, languageId);
        }

        String hostId () {
            return (String)this.getKey(0);
        }

        long languageId () {
            return (long)this.getKey(1);
        }
    }
}