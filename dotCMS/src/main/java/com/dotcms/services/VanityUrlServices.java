package com.dotcms.services;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.cache.VanityUrlCache;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotcms.system.event.local.type.content.CommitListenerEvent;
import com.dotcms.util.OptionalBoolean;
import com.dotcms.vanityurl.model.CacheVanityKey;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.SecondaryCacheVanityKey;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
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

import static com.dotcms.util.FunctionUtils.*;

/**
 * This service allows to invalidate the Vanity URL Cache
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    private static VanityUrlServices vanityURLServices;
    private final  VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    private final  ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final  IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

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
    public void invalidateVanityUrl(final VanityUrl vanityUrl) {
        this.invalidateVanityUrl((Contentlet) vanityUrl);
    }

    /**
     * Remove the vanity URL Contentlet from the vanityURLCache
     *
     * @param vanityUrl The vanity URL contentlet object
     */
    public void invalidateVanityUrl(final Contentlet vanityUrl) {
        this.invalidateAllVanityUrlVersions(vanityUrl);
    }

    /**
     * Remove all the vanity Url contentlet versions from the vanity URL cache
     *
     * @param vanityUrl The vanity URL contentlet object
     */
    public void invalidateAllVanityUrlVersions(final Contentlet vanityUrl) {
        try {
            final Identifier identifier = this.identifierAPI.find(vanityUrl.getIdentifier());

            final List<Contentlet> contentletVersions = this.contentletAPI
                    .findAllVersions(identifier, APILocator.systemUser(), false);

            contentletVersions.stream()
                    .filter(con -> vanityUrl.getLanguageId() == con.getLanguageId())
                    .forEach(vanityURLCache::remove);

            this.vanityURLCache.remove(vanityUrl);
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
    public void updateCache(final VanityUrl vanity) {
        this.vanityURLCache.update(vanity);
    }

    /**
     * Add the vanity URL to the caches
     *
     * @param vanity The vanity URL to add
     */
    public void updateCache(final CachedVanityUrl vanity) {
        this.vanityURLCache.update(vanity);
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
    public CachedVanityUrl getCachedVanityUrlByUri(final String uri,
                                                   final String siteId,
                                                   final long languageId) {

        CachedVanityUrl foundVanity = null;

        if (!Host.SYSTEM_HOST.equals(siteId)) {

            //First search in cache with the given site
            foundVanity =
                    this.vanityURLCache
                            .get(new CacheVanityKey(siteId, languageId, uri));
        }

        //If nothing found lets try with the SYSTEM_HOST
        return (null == foundVanity)?
                    this.vanityURLCache
                        .get(new CacheVanityKey(Host.SYSTEM_HOST, languageId, uri)):
                foundVanity;
    } // getCachedVanityUrlByUri.

    /**
     * Set the list of cached vanity urls list
     * @param siteId The current site Id
     * @param languageId The current language Id
     * @param vanityUrlList List of {@link CachedVanityUrl}
     */
    public void setCachedVanityUrlList(final String siteId,
                                       final long   languageId,
                                       final List<CachedVanityUrl>   vanityUrlList) {

        this.vanityURLCache.setCachedVanityUrls(
                new SecondaryCacheVanityKey(siteId, languageId), vanityUrlList);
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
                .getCachedVanityUrls(new SecondaryCacheVanityKey(siteId,
                                languageId));
    } // setCachedVanityUrlList.

    /**
     * Subscriber that listen to events of type CommitListenerEvent, this event will be trigger when
     * the commit listener related to this event is executed.
     */
    @Subscriber
    @WrapInTransaction
    public void onCommitListener(final CommitListenerEvent commitListenerEvent) {

        final Contentlet contentlet =
                commitListenerEvent.getContentlet();

        try {

            Logger.debug(this, "Invalidating the vanity: "
                    + contentlet);

            ifElse( null != contentlet && contentlet.isVanityUrl() &&
                                    this.contentletAPI.isInodeIndexed
                                            (contentlet.getInode(),contentlet.isLive(), contentlet.isWorking()),
                                    () -> this.invalidateVanityUrl(contentlet),
                                    () -> Logger.error(this,
                                            "Unable to invalidate VanityURL in cache:" +
                                                    contentlet) );
        } catch (Exception e) {
            Logger.error(this,
                    String.format("Unable to invalidate VanityURL in cache [%s]",
                            contentlet.getIdentifier()), e);
        }
    } // onCommitListener.

} // E:O:F:VanityUrlServices.