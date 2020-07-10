package com.dotcms.vanityurl.business;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.toImmutableList;
import static com.dotcms.util.VanityUrlUtil.isValidRegex;
import static com.dotcms.util.VanityUrlUtil.processExpressions;
import static java.util.stream.IntStream.rangeClosed;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.vanityurl.model.CacheVanityKey;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.commons.collections.keyvalue.MultiKey;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {
    
    private final Set<Integer> allowedActions = new ImmutableSet.Builder<Integer>().add(200).add(301).add(302).build();

    public static final String URL_SUFFIX = "/";
    public static final String LEGACY_CMS_HOME_PAGE = "/cmsHomePage";
    private final ContentletAPI     contentletAPI;
    private final VanityUrlServices vanityUrlServices;
    private final LanguageAPI       languageAPI;
    private final UserAPI           userAPI;
    private final long              defaultLanguageId;
    private final User              systemUser;

    private static final int CODE_404_VALUE = 404;
    private static final VanityMatches VANITY_MATCHES_FALSE =
            new VanityMatches(Boolean.FALSE, null);
    private static final VanityMatches VANITY_MATCHES_TRUE =
            new VanityMatches(Boolean.TRUE, null);


    public VanityUrlAPIImpl() throws DotDataException {
        this(APILocator.getContentletAPI(),
                VanityUrlServices.getInstance(),
                APILocator.getLanguageAPI(),
                APILocator.getUserAPI());
    }

    @VisibleForTesting
    public VanityUrlAPIImpl(final ContentletAPI contentletAPI,
            final VanityUrlServices vanityUrlServices,
            final LanguageAPI       languageAPI,
            final UserAPI           userAPI) throws DotDataException {

        this.contentletAPI     = contentletAPI;
        this.vanityUrlServices = vanityUrlServices;
        this.languageAPI       = languageAPI;
        this.userAPI           = userAPI;
        this.defaultLanguageId = languageAPI.getDefaultLanguage()
                .getId();
        this.systemUser        = userAPI.getSystemUser();

    }

    @CloseDBIfOpened
    @Override
    public void initializeVanityURLsCache(final User user) {
        initializeActiveVanityURLsCacheBySiteAndLanguage(null, null);
    }

    /**
     * Searches for all Vanity URLs for a given Site in the system. It goes directly to the database in order to
     * avoid retrieving data that has not been updated in the ES index yet. This initialization routine will also
     * add Vanity URLs located under System Host.
     *
     * @param siteId     The ID of the Site whose Vanity URLs will be retrieved.
     * @param languageId The ID of the language for the Vanity URLs.
     * @return A list of VanityURLs
     */
    private void initializeActiveVanityURLsCacheBySiteAndLanguage(final String siteId, final Long languageId) {
        final boolean includeSystemHost = Boolean.TRUE;
        try {
            final List<Contentlet> contentResults = searchAndPopulate(siteId, languageId, includeSystemHost);
            if (null == contentResults || contentResults.isEmpty()) {
                // Empty is a valid cache value
                this.setEmptyCaches(siteId, languageId, includeSystemHost);
                return;
            }
            final List<VanityUrl> vanityUrls = contentResults.stream()
                    .map(this::getVanityUrlFromContentlet)
                    .sorted(Comparator.comparing(VanityUrl::getOrder))
                    .collect(toImmutableList());
            // Add them to caches
            this.addSecondaryVanityURLCacheCollection (vanityUrls);
            vanityUrls.forEach(this::addToSingleVanityURLCache);
             // If a site was sent we need to make sure it was initialized in the cache
            if (null != siteId && null != languageId) {
                this.checkSiteLanguageVanities
                        (siteId, languageId, includeSystemHost);
            }
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format("Error searching and populating the Vanity URL from DB for " +
                    "Site ID '%s', language ID = '%s': %s", siteId, languageId, e.getMessage()), e);
        }
    } // initializeActiveVanityURLsCacheBySiteAndLanguage.

    /**
     * Searches for all Vanity URLs for a given Site in the system <b>without loading data into any of the Vanity URL
     * Cache Regions</b>. This initialization routine will also add Vanity URLs located under System Host.
     *
     * @param siteId     The ID of the Site whose Vanity URLs will be retrieved.
     * @param languageId The ID of the language for the Vanity URLs.
     *
     * @return A list of Vanity URLs read from the data source.
     */
    private List<CachedVanityUrl> getActiveVanityURLsNoCacheBySiteAndLanguage(final String siteId, final Long languageId) {
        final boolean includeSystemHost = Boolean.TRUE;
        try {
            final List<Contentlet> contentResults = searchAndPopulate(siteId, languageId, includeSystemHost);
            final List<VanityUrl> vanityUrls = contentResults.stream()
                    .map(this::getVanityUrlFromContentlet)
                    .sorted(Comparator.comparing(VanityUrl::getOrder))
                    .collect(toImmutableList());
            final List<CachedVanityUrl> cachedVanityUrls = new ArrayList<>();
            if (UtilMethods.isSet(vanityUrls)) {
                // Simulate Vanity URLs coming from cache
                for (final VanityUrl vanityUrl : vanityUrls) {
                    cachedVanityUrls.add(new CachedVanityUrl(vanityUrl));
                }
            }
            return cachedVanityUrls;
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format("Error searching and populating the Vanity URL from DB for " +
                    "Site ID '%s', language ID = '%s': %s", siteId, languageId, e.getMessage()), e);
        }
    }

    /**
     * Executes a SQL query that will return all the Vanity URLs that belong to a specific Site. This method moved from
     * using the ES index to using a SQL query in order to avoid situations where the index was not fully updated when
     * reading new data.
     *
     * @param siteId            The Identifier of the Site whose Vanity URLs will be retrieved.
     * @param languageId        The language ID used to created the Vanity URLs.
     * @param includeSystemHost If set to {@code true}, the results will include Vanity URLs that were created under
     *                          System Host. Otherwise, set to {@code false}.
     *
     * @return The list of Vanity URLs.
     */
    @CloseDBIfOpened
    private List<Contentlet> searchAndPopulate(final String siteId, final Long languageId, final boolean
            includeSystemHost) {
        List<Contentlet> contentlets = new ArrayList<>();
        final StringBuilder query = new StringBuilder();
        query.append("SELECT cvi.live_inode FROM contentlet c ");
        //c.text2 is the Site Field for Vanity URL Content Type
        query.append("INNER JOIN identifier i ON c.identifier = i.id AND c.text2 ");
        if (includeSystemHost) {
            query.append("IN ('" + Host.SYSTEM_HOST + "', ?) ");
        } else {
            query.append("= ? ");
        }
        query.append("INNER JOIN contentlet_version_info cvi ON c.identifier = cvi.identifier AND c.inode = cvi" +
                ".live_inode ");
        query.append("WHERE c.language_id = ? AND c.structure_inode IN (SELECT s.inode FROM structure s WHERE s" +
                ".structuretype = ?)");
        try {
            final List<Map<String, Object>> vanityUrls = new DotConnect().setSQL(query.toString())
                    .addParam(siteId)
                    .addParam(languageId)
                    .addParam(BaseContentType.VANITY_URL.getType())
                    .loadObjectResults();
            final List<String> vanityUrlInodes = vanityUrls.stream().map(vanity -> vanity.get("live_inode").toString
                    ()).collect(Collectors.toList());
            contentlets = this.contentletAPI.findContentlets(vanityUrlInodes);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when retrieving Vanity URLs: siteId=[%s], " +
                    "languageId=[%s], includeSystemHost=[%s]", siteId, languageId, includeSystemHost), e);
        } catch (final DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when retrieving Vanity URLs: siteId=[%s], " +
                    "languageId=[%s], includeSystemHost=[%s]", siteId, languageId, includeSystemHost), e);
        }
        return contentlets;
    }

    private boolean is404 (final CachedVanityUrl cachedVanityUrl) {

        return UtilMethods.isSet(cachedVanityUrl) && VanityUrlAPI.CACHE_404_VANITY_URL
                .equals(cachedVanityUrl.getVanityUrlId());
    }

    @CloseDBIfOpened
    @Override
    public boolean isVanityUrl(final String url, final Host host, final long languageId) {

        final String uri    = url; // not process now for it.
        final String siteId = this.getSiteId(host);

        // tries first cache.
        final CachedVanityUrl cachedVanityUrl =
                this.vanityUrlServices
                        .getCachedVanityUrlByUri(uri, siteId, languageId);
        boolean isVanityURL = !is404(cachedVanityUrl)
                && this.patternMatches(cachedVanityUrl, uri).isPatternMatches();

        if (!isVanityURL) {

            CachedVanityUrl liveCachedVanityUrl =
                    this.searchLiveCachedVanityUrlBySiteAndLanguage
                            (uri, siteId, languageId);

            if (null == liveCachedVanityUrl) {

                liveCachedVanityUrl =
                        this.getFallback(uri, siteId, languageId, this.systemUser);
            }

            isVanityURL =
                    !is404(liveCachedVanityUrl);

            // Still support legacy cmsHomePage
            if (URL_SUFFIX.equals(uri) && !isVanityURL) {

                liveCachedVanityUrl =
                        this.getLiveCachedVanityUrl
                                (LEGACY_CMS_HOME_PAGE, host, languageId, this.systemUser);
                isVanityURL = UtilMethods.isSet(liveCachedVanityUrl)
                        && !VanityUrlAPI.CACHE_404_VANITY_URL
                            .equals(liveCachedVanityUrl.getVanityUrlId());
            }
        }

        return isVanityURL;
    } // isVanityUrl

    @CloseDBIfOpened
    @Override
    public VanityUrl getVanityUrlFromContentlet(final Contentlet contentlet) {

        if (contentlet != null) {
            try {
                if (!contentlet.isVanityUrl()) {
                    throw new DotStateException(
                            "Contentlet : " + contentlet.getInode() + " is not a Vanity Url");
                }
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException(
                        "Contentlet : " + contentlet.getInode() + " is not a Vanity Url", e);
            }
        } else {
            throw new DotStateException("Contentlet is null");
        }

        final DefaultVanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setContentTypeId(contentlet.getContentTypeId());
        try {
            this.contentletAPI.copyProperties(vanityUrl, contentlet.getMap());
        } catch (Exception e) {
            throw new DotStateException("Vanity Url Copy Failed", e);
        }
        vanityUrl.setHost(contentlet.getHost());
        vanityUrl.setFolder(contentlet.getFolder());

        return vanityUrl;
    } // getVanityUrlFromContentlet.

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
    } // addToSingleVanityURLCache.

    /**
     * Add a 404 Vanity URL to the vanityURLCache
     *
     * @param siteId The current site Id
     * @param uri The current URI
     * @param languageId The current language Id
     */
    private void add404URIToCache(final String siteId,
                                  final String uri,
                                  final long languageId) {

        final VanityUrl cache404VanityUrl = new DefaultVanityUrl();
        cache404VanityUrl.setInode(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setIdentifier(VanityUrlAPI.CACHE_404_VANITY_URL);
        cache404VanityUrl.setAction(CODE_404_VALUE);
        cache404VanityUrl.setLanguageId(languageId);
        cache404VanityUrl.setURI(uri);
        cache404VanityUrl.setSite(siteId);
        cache404VanityUrl.setOrder(0);

        this.vanityUrlServices.updateCache(cache404VanityUrl);
    }

    private String getSiteId (final Host site) {

        return
            (null != site && !site.isSystemHost())?
                        site.getIdentifier():Host.SYSTEM_HOST;

    } // getSiteId.

    @CloseDBIfOpened
    @Override
    public CachedVanityUrl getLiveCachedVanityUrl(final String uri, final Host site,
            final long languageId, final User user) {

        final String siteId = this.getSiteId(site);

        //First lets try with the cache
        CachedVanityUrl result = vanityUrlServices
                .getCachedVanityUrlByUri(uri, siteId, languageId);

        final VanityMatches matches =
                this.patternMatches(result, uri);
        if (matches.isPatternMatches()) {

            return processExpressions(result, matches.getGroups());
        } else {
            //Search for the URI in the vanityURL cached by site and language Ids
            result =
                    this.searchLiveCachedVanityUrlBySiteAndLanguage
                            (uri, siteId, languageId);
        }

        return (null == result)?
                this.getFallback(uri, siteId, languageId, this.systemUser):result;
    } // getLiveCachedVanityUrl.

    private CachedVanityUrl getFallback (final String uri, final String siteId,
                                         final long languageId, final User user) {

        CachedVanityUrl result = null;

        try {

            //Search for the list of ContentTypes of type VanityURL
            final List<ContentType> vanityUrlContentTypes = APILocator.getContentTypeAPI(user)
                    .findByType(BaseContentType.VANITY_URL);

            //Verify if this Content Type has a Multilinguable fallback
            if (!vanityUrlContentTypes.isEmpty()
                    && (this.defaultLanguageId != languageId)
                    && ((VanityUrlContentType) vanityUrlContentTypes.get(0)).fallback()) {

                //if the fallback is set then is going to try to get it by the default language
                result = this.vanityUrlServices
                        .getCachedVanityUrlByUri(uri, siteId, this.defaultLanguageId);

                final VanityMatches matches = this.patternMatches(result, uri);
                if (matches.isPatternMatches()) {

                    return processExpressions(result, matches.getGroups());
                } else {
                    //Search for the URI in the vanityURL cached by site and default language Ids
                    result = searchLiveCachedVanityUrlBySiteAndLanguage(uri, siteId,
                            this.defaultLanguageId);
                }
            }

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching for Vanity URL by URI", e);
        }

        if (result == null) {
            //Add 404 to cache
            this.add404URIToCache(siteId, uri, languageId);
            result = this.vanityUrlServices
                    .getCachedVanityUrlByUri(uri, siteId, languageId);
        }

        return result;
    } // getFallback.

    /**
     * Validate if the CachedVanityUrl pattern matches the uri
     *
     * @param cachedVanityUrl The CachedVanityUrl
     * @param uri The current uri
     * @return true if the CachedVanityUrl match, false if not
     */
    private VanityMatches patternMatches(final CachedVanityUrl cachedVanityUrl,
                                         final String uri) {
        if (cachedVanityUrl != null) {

            final Matcher matcher = cachedVanityUrl.getPattern().matcher(uri);
            if (matcher.matches()) {
                if (matcher.groupCount() > 0) {

                    final String[] matches = new String[matcher.groupCount()];
                    rangeClosed(1, matcher.groupCount())
                            .forEach(i -> matches[i - 1] = matcher.group(i));
                    return new VanityMatches(Boolean.TRUE, matches);
                }

                return VANITY_MATCHES_TRUE;
            }
        }

        return VANITY_MATCHES_FALSE;
    } // patternMatches.

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
        List<CachedVanityUrl> cachedVanityUrls =
                this.getVanityUrlBySiteAndLanguageFromCache
                        (siteId, languageId,true);

        if (null == cachedVanityUrls) {

            synchronized (VanityUrlAPIImpl.class) {

                cachedVanityUrls =
                        this.getVanityUrlBySiteAndLanguageFromCache
                            (siteId, languageId, true);

                if (null == cachedVanityUrls) {

                    //Initialize the Cached Vanity URL cache if is null
                    this.initializeActiveVanityURLsCacheBySiteAndLanguage
                            (siteId, languageId);

                    //Get the list of site cached Vanities URLs
                    cachedVanityUrls =
                            this.getVanityUrlBySiteAndLanguageFromCache
                                    (siteId, languageId,true);
                    if (!UtilMethods.isSet(cachedVanityUrls)) {
                        // If this point is reached, there's probably an issue with the Vanity URL Caches. So, just read
                        // directly from the data source WITHOUT reading or loading from any cache
                        cachedVanityUrls = this.getActiveVanityURLsNoCacheBySiteAndLanguage(siteId, languageId);
                    }
                }
            }
        }

        VanityMatches matches = null;
        if (null != cachedVanityUrls) {
            //Validates if onw of the site cachedVanityUrls matches the uri
            for (CachedVanityUrl vanity : cachedVanityUrls) {

                matches = this.patternMatches(vanity, uri);
                if (matches.isPatternMatches()) {

                    result = processExpressions(vanity, matches.getGroups());
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
            this.vanityUrlServices.updateCache(new CachedVanityUrl(result, uri));
        }

        return result;
    } // searchLiveCachedVanityUrlBySiteAndLanguage.

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
        SiteLanguageKey key;

        for (final VanityUrl vanityUrl : vanityUrls) {

            key = new SiteLanguageKey(vanityUrl.getSite(), vanityUrl.getLanguageId());
            if (!vanityPerSiteLanguageMap.containsKey(key)) {

                vanityPerSiteLanguageMap.put(key, new ImmutableList.Builder());
            }

            vanityPerSiteLanguageMap.get(key).add(new CachedVanityUrl(vanityUrl));
        }

        // when the site + lang list is gonna be override, the main cache items should be clean too
        vanityPerSiteLanguageMap.keySet().forEach(this::cleanCurrentVanityUrlsPerSite);

        vanityPerSiteLanguageMap.forEach( (k, vanityUrlBuilder) ->
                    this.vanityUrlServices.setCachedVanityUrlList (k.hostId(), k.languageId(), vanityUrlBuilder.build()));
    } // addSecondaryVanityURLCacheCollection.

    private void cleanCurrentVanityUrlsPerSite(final SiteLanguageKey siteLanguageKey) {

        final List<CachedVanityUrl> cachedVanityUrls = this.vanityUrlServices.getCachedVanityUrlList
                (siteLanguageKey.hostId(), siteLanguageKey.languageId());

        if (null != cachedVanityUrls) {

            for (final CachedVanityUrl cachedVanityUrl : cachedVanityUrls) {

                CacheLocator.getVanityURLCache().remove(new CacheVanityKey(
                        cachedVanityUrl.getSiteId(),
                        cachedVanityUrl.getLanguageId(),
                        cachedVanityUrl.getUrl()
                ).toString());
            }
        }
    }

    @CloseDBIfOpened
    @Override
    public void validateVanityUrl(final Contentlet contentlet) {

        final User user = getUser(contentlet);
        Language language =
                APILocator.getLanguageAPI().getLanguage(user.getLanguageId());

        language = (null == language ? APILocator.getLanguageAPI().getDefaultLanguage() : language);

        // check fields
        checkMissingField(contentlet, language, VanityUrlContentType.ACTION_FIELD_VAR);
        checkMissingField(contentlet, language, VanityUrlContentType.URI_FIELD_VAR);

        final Integer action     =
                (int)contentlet.getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR);
        final String uri         =
                contentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR);

        if(!this.allowedActions.contains(action)){
            throwContentletValidationError(language, "message.vanity.url.error.invalidAction");
        }

        if (!isValidRegex(uri)) {
            throwContentletValidationError(language, "message.vanity.url.error.invalidURIPattern");
        }
    } // validateVanityUrl.

    private void throwContentletValidationError(final Language language, final String key) {
        final String message = this.languageAPI
                .getStringKey(language, key);

        throw new DotContentletValidationException(message);
    }

    private void checkMissingField(final Contentlet contentlet,
                                   final Language language,
                                   final String fieldName) {

        final String identifier = contentlet.getIdentifier();

        if (!contentlet.getMap().containsKey(fieldName)) {

            throw new DotContentletValidationException(
                    MessageFormat.format(
                            this.languageAPI.getStringKey(language, "missing.field"),
                            fieldName, identifier)
                    );
        }
    }

    private User getUser(final Contentlet contentlet) {
        User user;
        try {
            user = this.userAPI.loadUserById(contentlet.getModUser());
        } catch (Exception e) {
            Logger.debug(this,e.getMessage(),e);
            user = this.systemUser;
        }
        return user;
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

    private static class VanityMatches {

        private final boolean patternMatches;
        private final String  [] groups;

        public VanityMatches(final boolean patternMatches,
                             final String[] groups) {
            this.patternMatches = patternMatches;
            this.groups = groups;
        }

        boolean isPatternMatches() {
            return patternMatches;
        }

        String[] getGroups() {
            return groups;
        }
    }
}