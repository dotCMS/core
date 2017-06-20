package com.dotcms.services;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.content.model.VanityUrl;
import com.dotcms.util.VanityUrlUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

/**
 * This service allows to invalidate the Vanity URL Cache
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 20, 2017.
 */
public class VanityUrlServices {

    final static VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
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
        try {
            vanityURLCache.remove(VanityUrlUtil.sanitizeKey(vanityUrl));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(VanityUrlServices.class, "Error trying to invalidate Vanity URL identifier:"+vanityUrl.getIdentifier(),e);
        }
    }
}
