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
     * Provides the supplier to stores the metadata generated, if store is true
     */
    private final Supplier<Optional<File>>    metaDataFileSupplier;

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
        this.metaDataFileSupplier = builder.metaDataFileSupplier;
    }

    public Supplier<Optional<File>> getMetaDataFileSupplier() {
        return metaDataFileSupplier;
    }

    public boolean isCache() {
        return cache;
    }

    public Supplier<String> getCacheKeySupplier() {
        return cacheKeySupplier;
    }

    public static final class Builder {

        /**
         * Provides the supplier to stores the metadata generated, if store is true
         */
        private Supplier<Optional<File>>    metaDataFileSupplier = ()-> Optional.empty();

        /**
         * If true, means the medatada output will be stores in the memory cache.
         */
        private boolean           cache = false;

        /**
         * Cache key supplier, if cache is true
         */
        private Supplier<String>  cacheKeySupplier = null;


        public Builder cache(final Supplier<String>  cacheKeySupplier) {

            this.cache            = true;
            this.cacheKeySupplier = cacheKeySupplier;
            return this;
        }

        public Builder metaDataFileSupplier(final Supplier<Optional<File>> metaDataFileSupplier) {

            this.metaDataFileSupplier = metaDataFileSupplier;
            return this;
        }


        public RequestMetaData build() {
            return new RequestMetaData(this);
        }
    }
}
