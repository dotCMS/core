package com.dotcms.vanityurl.cache;


import java.util.List;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * This cache is used to map the Vanity URLs path to the Vanity Url
 * content.
 *
 * @author oswaldogallango
 */
public abstract class VanityUrlCache implements Cachable {


    /**
     * Removes all entries from cache
     */
    public abstract void clearCache();

    /**
     * Removes from cache in all the registered regions a given VanityURL
     * @param vanityURL
     */
    public abstract void remove(final Contentlet vanityURL);



    /**
     * Get the associated list of CachedVanityUrl to current host Id and language Id key
     * @param key SecondaryCacheVanityKey The current key composed of the host Id and languageId
     * @return a list of CachedVanityUrl
     */
    public abstract List<CachedVanityUrl> getCachedVanityUrls(final Host host, final Language lang) ;
    /**
     * Checks if there is a 404 response for the given url
     * @param host
     * @param url
     * @return
     */
    public abstract boolean is404(final Host host, final Language lang, final String url) ;

    

    /**
     * puts a url into the 404 cache
     * @param host
     * @param url
     */
    public abstract void put404(final Host host, final Language lang, final String url);

    public abstract void put(Host host, Language lang, List<CachedVanityUrl> vanityURLs) ;
    
    
    
}