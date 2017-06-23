package com.dotcms.services;

import com.dotcms.cache.VanityUrlCache;
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

/**
 * This service allows to invalidate the Vanity URL Cache
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    final static VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    final static ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final static IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
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
            contentletVersions.stream().forEach( (Contentlet con) -> {
                try {
                    vanityURLCache.remove(VanityUrlUtil.sanitizeKey(con));
                } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
                    Logger.error(VanityUrlServices.class, "Error trying to invalidate Vanity URL identifier:"+vanityUrl.getIdentifier(),e);
                }
            });
            vanityURLCache.remove(VanityUrlUtil.sanitizeKey(vanityUrl));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class, "Error trying to invalidate Vanity URL identifier:"+vanityUrl.getIdentifier(),e);
        }
    }
}
