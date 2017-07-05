package com.dotcms.services;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.util.VanityUrlUtil;
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
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    private static final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    private VanityUrlServices(){

    }
    /**
     * Remove the vanity URL from the vanityURLCache
     * @param vanityUrl The vanity URL object
     */
    public static void invalidateVanityUrl(VanityUrl vanityUrl){
        invalidateVanityUrl((Contentlet)vanityUrl);
    }

    /**
     * Remove the vanity URL Contentlet from the vanityURLCache
     * @param vanityUrl The vanity URL contentlet object
     */
    public static void invalidateVanityUrl(Contentlet vanityUrl){
        invalidateAllVanityUrlVersions(vanityUrl);
    }

    /**
     * Remove all the vanity Url contentlet versions from the vanity URL cache
     * @param vanityUrl The vanity URL contentlet object
     */
    public static void invalidateAllVanityUrlVersions(Contentlet vanityUrl){
        try {
            Identifier identifier = identifierAPI.find(vanityUrl.getIdentifier());
            List<Contentlet> contentletVersions = contentletAPI.findAllVersions(identifier,APILocator.systemUser(),false);
            contentletVersions.stream().forEach( (Contentlet con) -> removeFromCache(con) );
            vanityURLCache.remove(VanityUrlUtil.sanitizeKey(vanityUrl));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class, "Error trying to invalidate Vanity URL identifier:"+vanityUrl.getIdentifier(),e);
        }
    }

    /**
     * Remove the vanity Url contentlet from cache
     * @param contentlet
     */
    private static void removeFromCache(Contentlet contentlet){
        try {
            vanityURLCache.remove(VanityUrlUtil.sanitizeKey(contentlet));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class, "Error trying to invalidate Vanity URL identifier:"+contentlet.getIdentifier(),e);
        }
    }

    /**
     * Load in cache the active vanities Urls
     */
    public static void initializeVanityUrlCache(){
        List<CachedVanityUrl> activeVanityUrls = APILocator.getVanityUrlAPI().getActiveCachedVanityUrls(APILocator.systemUser());
        activeVanityUrls.stream().forEach((CachedVanityUrl vanity) ->{
            Set<CachedVanityUrl> currentCachedVanities = CacheLocator.getVanityURLCache().getCachedVanityUrls(vanity.getSiteId());
            currentCachedVanities.add(vanity);
            CacheLocator.getVanityURLCache().setCachedVanityUrls(vanity.getSiteId(),currentCachedVanities);
        });
    }
}
