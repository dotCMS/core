package com.dotcms.vanityurl.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;

import io.vavr.control.Try;

/**
 * This class implements {@link VanityUrlCache} the cache for Vanity URLs.
 * Is used to map the Vanity URLs path to the Vanity URL content
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 24, 2017
 */
public class VanityUrlCacheImpl extends VanityUrlCache {

    private DotCacheAdministrator cache;

    private static final String PRIMARY_GROUP = "VanityURLCache";
    private static final String VANITY_URL_404_GROUP = "VanityURL404Cache";

    /**
     * when a vanity url invalidation takes place, it wipes out all vanity urls on a host.
     * This tempCache is intended to hold latest entries temporarly until the new
     * List<CachedVanityUrl> can be rebuilt and brought back online
     */
    private Cache<String, List<CachedVanityUrl>> tempCache = Caffeine
        .newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();

    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public String getPrimaryGroup() {

      return PRIMARY_GROUP;
    }

    @Override
    public String[] getGroups() {

      return new String[] {PRIMARY_GROUP,VANITY_URL_404_GROUP};
    }

    @Override
    public void clearCache() {
      cache.flushGroup(PRIMARY_GROUP);
      cache.flushGroup(VANITY_URL_404_GROUP);
      tempCache.invalidateAll();
    }

    @Override
    public void remove(final Contentlet vanityURL) {
      if(vanityURL==null || !vanityURL.isVanityUrl()) {
        return;
      }
      Host host = Try.of(() -> APILocator.getHostAPI().find(vanityURL.getHost(), APILocator.systemUser(), false)).getOrNull();
      Language lang = Try.of(() -> APILocator.getLanguageAPI().getLanguage(vanityURL.getLanguageId())).getOrNull();
      if(host==null || lang==null ) {
        return;
      }
      List<CachedVanityUrl> cached = this.getCachedVanityUrls(host, lang);
      if(cached!=null) {
        tempCache.put(key(host,lang), cached);
      }
      cache.remove(key(host,lang), PRIMARY_GROUP);
      cache.flushGroup(VANITY_URL_404_GROUP);
    }

    @Override
    public void put(final Host host, Language lang, List<CachedVanityUrl> vanityURLs) {
      if(host==null || lang==null || vanityURLs==null) {
        return;
      }
    
      cache.put(key(host,lang), vanityURLs, PRIMARY_GROUP);
      tempCache.invalidate(key(host,lang));
      cache.flushGroup(VANITY_URL_404_GROUP);
    }

    @Override
    public List<CachedVanityUrl> getCachedVanityUrls(final Host host, final Language lang) {
      if(host==null || lang==null ) {
        return null;
      }
      final String key=key(host,lang);

      List<CachedVanityUrl> actual =  (List<CachedVanityUrl>) cache.getNoThrow(key, PRIMARY_GROUP); 
      // do we have cached vanity urls? if so, send them, otherwise send our temps
      return (actual!=null) ? actual : tempCache.getIfPresent(key);
       
    }
    
    

    
    @Override
    public boolean is404(final Host host, final Language lang, final String url) {

      return cache.getNoThrow(key(host,lang, url), VANITY_URL_404_GROUP)!=null;
    }

    @Override
    public void put404(final Host host, final Language lang, final String url) {

      cache.put(key(host,lang, url), Boolean.TRUE, VANITY_URL_404_GROUP);

    }



    String key(final Host host, final Language lang) {
      return key(host, lang, null);
    }



    String key(final Host host, final Language lang, final String url) {

      return (host!=null ? host.getIdentifier() : StringPool.BLANK)
          + StringPool.UNDERLINE 
          + (lang!=null ? String.valueOf(lang.getId()) : StringPool.BLANK) 
          + StringPool.UNDERLINE  
          + (url != null ?  url : StringPool.BLANK );
    }
  

    


}