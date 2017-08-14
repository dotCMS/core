package com.dotcms.services;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotcms.system.event.local.type.content.CommitListenerEvent;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

import java.util.List;

/**
 * This service allows to invalidate the Vanity URL Cache
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    private static VanityUrlServices vanityURLServices;
    private final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    /**
     * Get the VanityUrlService singleton
     *
     * @return a VanityUrlServices
     */
    public static VanityUrlServices getInstance() {
        if (vanityURLServices == null) {

            synchronized (VanityUrlServices.class) {
                vanityURLServices = new VanityUrlServices();
            }

        }
        return vanityURLServices;
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

            contentletVersions.stream()
                    .filter(con -> vanityUrl.getLanguageId() == con.getLanguageId())
                    .forEach(vanityURLCache::remove);

            vanityURLCache.remove(vanityUrl);
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL identifier:" + vanityUrl.getIdentifier(),
                    e);
        }
    }

    /**
     * Load in cache the active vanities Urls
     */
    public void initializeVanityUrlCache() {
        APILocator.getVanityUrlAPI()
                .initializeVanityURLsCache(APILocator.systemUser());
    }

    /**
     * Add the vanity URL to the caches
     *
     * @param vanity The vanity URL to add
     */
    public void updateCache(VanityUrl vanity) {
        vanityURLCache.update(vanity);
    }

    /**
     * Add the vanity URL to the caches
     *
     * @param vanity The vanity URL to add
     */
    public void updateCache(CachedVanityUrl vanity) {
        vanityURLCache.update(vanity);
    }

    /**
     * Add single {@link CachedVanityUrl} to the cache not affecting secondaries caches.
     * @param vanityUrl {@link CachedVanityUrl}
     */
    public void addSingleCache(final VanityUrl vanityUrl) {

        this.vanityURLCache.addSingle(vanityUrl);
    }

    /**
     * Get the cached vanity Url from the primary cache for a given site and SYSTEM_HOST
     *
     * @param uri The current uri
     * @param siteId The current site Id
     * @param languageId The current language Id
     * @return CachedVanityUrl object
     */
    public CachedVanityUrl getCachedVanityUrlByUri(String uri, String siteId, long languageId) {

        CachedVanityUrl foundVanity;
        if (null != siteId && !siteId.equals(Host.SYSTEM_HOST)) {

            //First search in cache with the given site
            foundVanity = vanityURLCache.get(VanityUrlUtil.sanitizeKey(siteId, uri, languageId));

            //If nothing found lets try with the SYSTEM_HOST
            if (null == foundVanity) {
                foundVanity = vanityURLCache
                        .get(VanityUrlUtil.sanitizeKey(Host.SYSTEM_HOST, uri, languageId));
            }
        } else {
            foundVanity = vanityURLCache
                    .get(VanityUrlUtil.sanitizeKey(Host.SYSTEM_HOST, uri, languageId));
        }

        return foundVanity;
    }

    /**
     * Set the list of cached vanity urls list
     * @param siteId The current site Id
     * @param languageId The current language Id
     * @param vanityUrlList List of {@link CachedVanityUrl}
     */
    public void setCachedVanityUrlList(final String siteId,
                                       final long   languageId,
                                       final List<CachedVanityUrl>   vanityUrlList) {

        this.vanityURLCache.setCachedVanityUrls(siteId, languageId, vanityUrlList);
    } // setCachedVanityUrlList.

    /**
     * Set the list of cached vanity urls list
     * @param siteId The current site Id
     * @param languageId The current language Id
     * @return List of {@link CachedVanityUrl}
     */
    public List<CachedVanityUrl> getCachedVanityUrlList(final String siteId,
                                       final long   languageId) {

        return this.vanityURLCache
                .getCachedVanityUrls(VanityUrlUtil
                        .sanitizeSecondCacheKey(siteId,
                                languageId));
    } // setCachedVanityUrlList.

    /**
     * Subscriber that listen to events of type CommitListenerEvent, this event will be trigger when
     * the commit listener related to this event is executed.
     */
    @Subscriber
    public void onCommitListener(CommitListenerEvent commitListenerEvent) {

        Contentlet contentlet = commitListenerEvent.getContentlet();

        try {
            if (contentlet.isVanityUrl()) {

                LocalTransaction.wrap(() -> {

                    //When the contentlet finished to index we need to invalidate it on cache
                    boolean indexed = false;
                    try {
                        indexed = APILocator.getContentletAPI()
                                .isInodeIndexed(contentlet.getInode(),
                                        contentlet.isLive(), contentlet.isWorking());
                    } catch (DotSecurityException e) {
                        Logger.error(this,
                                String.format("Unable to invalidate VanityURL in cache [%s]",
                                        contentlet.getIdentifier()), e);
                    }

                    if (indexed) {
                        //Invalidate this VanityURL
                        VanityUrlServices.getInstance().invalidateVanityUrl(contentlet);
                    }
                });

            }
        } catch (Exception e) {
            Logger.error(this,
                    String.format("Unable to invalidate VanityURL in cache [%s]",
                            contentlet.getIdentifier()), e);
        }
    }


}