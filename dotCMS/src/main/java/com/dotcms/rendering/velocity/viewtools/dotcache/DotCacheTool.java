package com.dotcms.rendering.velocity.viewtools.dotcache;

import com.dotcms.concurrent.Debouncer;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.BlockDirectiveCacheImpl;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Function0;
import io.vavr.Lazy;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This {@link ViewTool} can be used to set and get values that are stored in the dotCMS cache memory for later retrieval.
 *
 * @author Will Ezell
 * @since Nov 14th, 2022
 */
public class DotCacheTool implements ViewTool {

    final Lazy<BlockDirectiveCache> cache;

    final static String DOT_CACHE_PREFIX="DotCachePrefix";
    
    
    public DotCacheTool() {
        cache = Lazy.of(Function0.of(BlockDirectiveCacheImpl::new));
        cache.get();
        Logger.debug(getClass(), "starting DotCacheTool");
    }

    @VisibleForTesting
    public DotCacheTool(final DotCacheAdministrator admin) {
        cache = Lazy.of(()->new BlockDirectiveCacheImpl(admin, true));
        cache.get();
        Logger.debug(getClass(), "starting DotCacheTool");
    }

    @Override
    public void init(Object initData) {
        // We do not use this method
    }

    /**
     * Gets a given Serializable object cached in memory based on the specified key.
     *
     * @param key The key associated to the cached value.
     *
     * @return The cached object.
     */
    public Serializable get(final String key) {
        final Map<String, Serializable> entry = cache.get().get(DOT_CACHE_PREFIX + key);
        return entry.get(key);
    }

    /**
     * Puts a given value in the cache memory based on the specified key.
     *
     * @param key   The key associated to a given object.
     * @param value The object being cached.
     */
    public void put(final String key, final Object value) {
        this.put(key, value, Integer.MAX_VALUE);
    }

    Debouncer debounceAdd = new Debouncer();

    /**
     * Puts a given value in the cache memory based on the specified key for a specific amount of time.
     *
     * @param key   The key associated to a given object.
     * @param value The object being cached.
     * @param ttl   The Time-To-Live for this entry.
     */
    public void put(final String key, final Object value, final int ttl) {
        if (ttl <= 0) {
            cache.get().remove(DOT_CACHE_PREFIX + key);
            return;
        }
        final Serializable correctedValue = value instanceof Serializable ? (Serializable) value : value.toString();
        final Map<String, Serializable> map = Map.of(key, correctedValue);
        cache.get().add(DOT_CACHE_PREFIX + key, map, Duration.ofSeconds(ttl));
    }
    
    /**
     * This puts into the cache once a second
     * @param key
     * @param value
     * @param ttl
     */
    public void putDebounce(final String key, final Object value, final int ttl) {
        debounceAdd.debounce(key, () -> put(key, value, ttl), 1, TimeUnit.SECONDS);
    }
    

    /**
     * Removes an object from the cache memory based on its key.
     *
     * @param key The key matching a specific object.
     */
    public void remove(final String key) {
        cache.get().remove(DOT_CACHE_PREFIX +key);
    }

    /**
     * Clears all objects from the cache memory associated to this ViewTool.
     */
    public void clear() {
        cache.get().clearCache();
    }

}
