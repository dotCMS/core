package com.dotcms.vanityurl.cache;


import java.util.List;
import java.util.Optional;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * This cache is used to map the Vanity URLs path to the Vanity Url content.
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
     * 
     * @param vanityURL
     */
    public abstract void remove(final Contentlet vanityURL);



    /**
     * Get the associated list of CachedVanityUrl to current host Id and language Id key
     * 
     * @param key SecondaryCacheVanityKey The current key composed of the host Id and languageId
     * @return a list of CachedVanityUrl
     */
    public abstract List<CachedVanityUrl> getSiteMappings(final Host host, final Language lang);


    /**
     * puts all a hosts vanity urls into cache at once
     * @param host
     * @param lang
     * @param vanityURLs
     */
    public abstract void putSiteMappings(Host host, Language lang, List<CachedVanityUrl> vanityURLs);

    /**
     * removes all a hosts vanity urls into cache at once, in all languages
     * @param host
     * @param lang
     * @param vanityURLs
     */
    public abstract void remove(Host vanityHost);
    
    /**
     * removes all a hosts vanity urls into cache at once, in a specific langauge
     * @param host
     * @param lang
     */
    public abstract void remove(Host vanityHost, Language lang);

    /**
     * stores a direct mapping for a url to its vanity resolver
     * @param host
     * @param lang
     * @param url
     * @param vanityUrl
     */
    public abstract void putDirectMapping(Host host, Language lang, String url, final Optional<CachedVanityUrl> vanityUrl);

    
    /**
     * This method will return 1 of 3 possible values:
     * null - which means the cache does not know anything about the url
     * Optional.empty() - which means there is no matching vanity URL, e.g. 404 cached response
     * Optional<CachedVanityUrl> - the matching vanity url
     * @param url
     * @param host
     * @param lang
     * @return
     */
    public abstract Optional<CachedVanityUrl> getDirectMapping(String url, Host host, Language lang) ;



}
