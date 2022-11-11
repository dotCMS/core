package com.dotcms.rendering.velocity.viewtools.dotcache;

import java.io.Serializable;
import java.util.Optional;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

public class DotCacheTool implements ViewTool {

    final Lazy<DotVelocityCache> cache = Lazy.of(DotVelocityCache::new);

    
    
    public DotCacheTool() {
        Logger.debug(getClass(), "starting dotvelocitycache");

    }



    @Override
    public void init(Object initData) {
      // We do not use this method
    }


    public boolean exists(final String key) {
        return cache.get().getEvenIfStale(key).isPresent();
    }

    public boolean isStale(final String key) {
        Optional<ExpirableCacheEntry> entry = cache.get().getEvenIfStale(key);
        return entry.isPresent() || entry.get().isExpired();
    }



    public Serializable get(final String key) {
        Optional<ExpirableCacheEntry> entry = cache.get().get(key);
        return entry.isPresent() ? entry.get().getResults() : null;
    }

    public Optional<ExpirableCacheEntry> getRaw(final String key) {
        
        return cache.get().getEvenIfStale(key);
    }
    
    
    
    /**
     * Stale cache will always return what is in the cache. Cached values will be refreshed every 5
     * minutes in a seprate thread
     * 
     * @param key
     * @param valueSupplier
     * @param ttlInSeconds
     * @return
     */
    public Serializable getEvenIfStale(final String key) {
        Optional<ExpirableCacheEntry> entry = cache.get().getEvenIfStale(key);
        return entry.orElse(null);
    }


    public void put(final String key, final Serializable value) {
        cache.get().put(key, value);
    }

    public void put(final String key, final Serializable value, int ttl) {
        if(ttl==0) {
            cache.get().remove(key);
            return;
        }
        cache.get().put(key, value, ttl);
    }


    public void remove(final String key) {
        cache.get().remove(key);
    }


    public String getKey(Object... objects) {
        return cache.get().generateKey(objects);

    }



}
