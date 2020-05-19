package com.dotcms.storage;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Encapsulates the parameters to request the metadata
 * it could be by cache or file system
 * @author jsanca
 */
public class RequestMetaData {

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

    private RequestMetaData(final Builder builder) {

        this.cache                = builder.cache;
        this.cacheKeySupplier     = builder.cacheKeySupplier;
        this.storageKey           = builder.storageKey;
    }

    public StorageKey getStorageKey() {
        return storageKey;
    }

    public boolean isCache() {
        return cache;
    }

    public Supplier<String> getCacheKeySupplier() {
        return cacheKeySupplier;
    }

    public static final class Builder {

        /**
         * Provides the key for the storage
         */
        private  StorageKey    storageKey;

        /**
         * If true, means the medatada output will be stores in the memory cache.
         */
        private boolean           cache = false;

        /**
         * Cache key supplier, if cache is true
         */
        private Supplier<String>  cacheKeySupplier = null;

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


        public RequestMetaData build() {
            return new RequestMetaData(this);
        }
    }
}
