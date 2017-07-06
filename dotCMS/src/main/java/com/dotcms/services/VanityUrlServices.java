package com.dotcms.services;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Set;

/**
 * This service allows to invalidate the Vanity URL Cache
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    private static VanityUrlServices vanituUrlService;
    private final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final VanityUrlAPI vanityUrlAPI = APILocator.getVanityUrlAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    /**
     * Get the VanityUrlService singleton
     *
     * @return a VanityUrlServices
     */
    public static VanityUrlServices getInstance() {
        if (vanituUrlService == null) {

            synchronized (VanityUrlServices.class) {
                vanituUrlService = new VanityUrlServices();
            }

        }
        return vanituUrlService;
    }

    private VanityUrlServices() {

    }

    /**
     * Remove the vanity URL from the vanityURLCache
     *
     * @param vanityUrl The vanity URL object
     */
    public void invalidateVanityUrl(VanityUrl vanityUrl) {
        invalidateVanityUrl((Contentlet) vanityUrl);
    }

    /**
     * Remove the vanity URL Contentlet from the vanityURLCache
     *
     * @param vanityUrl The vanity URL contentlet object
     */
    public void invalidateVanityUrl(Contentlet vanityUrl) {
        invalidateAllVanityUrlVersions(vanityUrl);
    }

    /**
     * Remove all the vanity Url contentlet versions from the vanity URL cache
     *
     * @param vanityUrl The vanity URL contentlet object
     */
    public void invalidateAllVanityUrlVersions(Contentlet vanityUrl) {
        try {
            Identifier identifier = identifierAPI.find(vanityUrl.getIdentifier());
            List<Contentlet> contentletVersions = contentletAPI
                    .findAllVersions(identifier, APILocator.systemUser(), false);
            contentletVersions.stream().forEach((Contentlet con) -> removeFromCache(con));

            removeFromCache(vanityUrl);
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL identifier:" + vanityUrl.getIdentifier(),
                    e);
        }
    }

    /**
     * Remove the vanity Url contentlet version from cache
     */
    private void removeFromCache(Contentlet contentlet) {
        try {
            vanityURLCache.remove(VanityUrlUtil.sanitizeKey(contentlet));
            vanityURLCache.removeCachedVanityUrls(VanityUrlUtil.sanitizeSecondCachedKey(contentlet));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL identifier:" + contentlet
                            .getIdentifier(), e);
        }
    }

    /**
     * Load in cache the active vanities Urls
     */
    public void initializeVanityUrlCache() {
        List<CachedVanityUrl> activeVanityUrls = APILocator.getVanityUrlAPI()
                .getActiveCachedVanityUrls(APILocator.systemUser());
        activeVanityUrls.stream().forEach((CachedVanityUrl vanity) -> {
            Set<CachedVanityUrl> currentCachedVanities = CacheLocator.getVanityURLCache()
                    .getCachedVanityUrls(vanity.getSiteId());
            currentCachedVanities.add(vanity);
            CacheLocator.getVanityURLCache().setCachedVanityUrls(VanityUrlUtil
                            .sanitizeSecondCacheKey(vanity.getSiteId(), vanity.getLanguageId()),
                    currentCachedVanities);
        });
    }

    /**
     * Load in cache the active vanities Urls
     * searching by host and languageId
     *
     * @param hostId The current host Id
     * @param languageId The current language Id
     */
    public void initializeVanityUrlCache(String hostId, long languageId) {
        List<VanityUrl> activeVanityUrls = APILocator.getVanityUrlAPI()
                .getActiveVanityUrls(APILocator.systemUser());
        activeVanityUrls.stream().forEach((VanityUrl vanity) -> updateCache(vanity));
    }

    /**
     * Add the vanity URL to the caches
     *
     * @param vanity The vanity URL to add
     */
    public void updateCache(VanityUrl vanity) {
        try {
            //Update primary Cache
            vanityURLCache
                    .add(VanityUrlUtil
                                    .sanitizeKey(vanity.getSite(), vanity.getURI(), vanity.getLanguageId()),
                            vanity);

            //update Secondary cache
            Set<CachedVanityUrl> hostCachedVanityUrl = vanityURLCache
                    .getCachedVanityUrls(VanityUrlUtil
                            .sanitizeSecondCacheKey(vanity.getSite(), vanity.getLanguageId()));
            hostCachedVanityUrl.add(new CachedVanityUrl(vanity));

            vanityURLCache.setCachedVanityUrls(
                    VanityUrlUtil.sanitizeSecondCacheKey(vanity.getSite(), vanity.getLanguageId()),
                    hostCachedVanityUrl);
        } catch (DotRuntimeException e) {
            Logger.error(this, "Error trying to add Vanity URL to cache", e);
        }
    }

    /**
     * Get the cached vanity Url from the primary cache
     *
     * @param uri The current uri
     * @param hostId The current host Id
     * @param languageId The current language Id
     * @return CachedVanityUrl object
     */
    public CachedVanityUrl getCachedVanityUrlByUri(String uri, String hostId, long languageId) {
        return vanityURLCache.get(VanityUrlUtil.sanitizeKey(hostId, uri, languageId));
    }

    /**
     * Get the list of cached Vanity Url associated to a host
     *
     * @param hostId The current host Id
     * @param languageId The current language Id
     * @return A set of CachedVanityUrl
     */
    public Set<CachedVanityUrl> getVanityUrlByHostAndLanguage(String hostId, long languageId) {
        return CacheLocator.getVanityURLCache()
                .getCachedVanityUrls(VanityUrlUtil.sanitizeSecondCacheKey(hostId, languageId));
    }

}
