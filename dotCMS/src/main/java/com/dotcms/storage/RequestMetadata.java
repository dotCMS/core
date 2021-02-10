package com.dotcms.storage;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Encapsulates the parameters to request the metadata
 * it could be by cache or file system
 * @author jsanca
 */
public class RequestMetadata {

    /**
     * Provides the key for the storage
     */
    private final StorageKey    storageKey;

    /**
     * If true, means the medatada output will be stores in the memory cache.
     */
    private final boolean           cache;

    /**
     * Cache key supplier, if cache is true
     */
    private final Supplier<String>  cacheKeySupplier;

    /**
     * In case the metadata is retrieved from the storage instead of the cache,
     * you can wrap the metadata recovered from the storage in order to add, mod or remove values
     */
    private final Function<Map<String, Serializable>, Map<String, Serializable>> projectionMapForCache;

    /**
     * Builder based constructor
     * @param builder
     */
    private RequestMetadata(final Builder builder) {
        this.cache                   = builder.cache;
        this.cacheKeySupplier        = builder.cacheKeySupplier;
        this.storageKey              = builder.storageKey;
        this.projectionMapForCache = builder.projectionMapForCache;
    }

    public StorageKey getStorageKey() {
        return storageKey;
    }

    /**
     * cache setting read
     * @return
     */
    public boolean isCache() {
        return cache;
    }

    public Supplier<String> getCacheKeySupplier() {
        return cacheKeySupplier;
    }

    public Function<Map<String, Serializable>, Map<String, Serializable>> getProjectionMapForCache() {
        return projectionMapForCache;
    }

    /**
     * Convenience Builder
     */
    public static final class Builder {

        /**
         * Provides the key for the storage
         */
        private  StorageKey    storageKey;

        /**
         * If true, means the medatada output will be stores in the memory cache.
         */
        private boolean           cache;

        /**
         * Cache key supplier, if cache is true
         */
        private Supplier<String>  cacheKeySupplier;

        /**
         * In case the metadata is retrieved from the storage instead of the cache,
         * you can wrap the metadata recovery from the storage in order to add, mod or remove values
         */
        private Function<Map<String, Serializable>, Map<String, Serializable>> projectionMapForCache = map-> map;

        public Builder cache(final boolean cache) {

            this.cache            = cache;
            return this;
        }

        public Builder cache(final Supplier<String>  cacheKeySupplier) {

            this.cache            = true;
            this.cacheKeySupplier = cacheKeySupplier;
            return this;
        }

        public Builder storageKey(final StorageKey storageKey) {

            this.storageKey = storageKey;
            return this;
        }

        public Builder projectionMapForCache(final Function<Map<String, Serializable>, Map<String, Serializable>> projectionMapForCache) {

            this.projectionMapForCache = projectionMapForCache;
            return this;
        }

        public RequestMetadata build() {
            return new RequestMetadata(this);
        }
    }
}
