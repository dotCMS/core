package com.dotcms.storage;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Configuration to generate the meta data.
 *
 * @author jsanca
 */
public class GenerateMetadataConfig {

    /**
     * Provides the key for the storage
     */
    private final StorageKey storageKey;

    /**
     * If true, means the metadata output will be stores in the metaDataFileSupplier file
     */
    private final boolean store;

    /**
     * if store is true, and you want to force always the file generation set this to true (keep in
     * mind it could be expensive)
     */
    private final boolean override;

    /**
     * On huge file, we probably do not want to parse all the content, so this max will limited how
     * much do we want to read
     */
    private final long maxLength;

    /**
     * {@link Predicate} filter the meta data key for the map result generation
     */
    private final Predicate<String> metaDataKeyFilter;

    /**
     * If true, means the medatada output will be stores in the memory cache.
     */
    private final boolean cache;

    /**
     * Cache key supplier, if cache is true
     */
    private final Supplier<String> cacheKeySupplier;

    /**
     * If true, means the metadata generated will be full except if there is any metaDataKeyFilter,
     * only these fields will be accepted.
     */
    private final boolean full;

    private GenerateMetadataConfig(final Builder builder) {

        this.cache = builder.cache;
        this.cacheKeySupplier = builder.cacheKeySupplier;
        this.maxLength = builder.maxLength;
        this.storageKey = builder.storageKey;
        this.metaDataKeyFilter = builder.metaDataKeyFilter;
        this.override = builder.override;
        this.store = builder.store;
        this.full = builder.full;
    }

    public StorageKey getStorageKey() {
        return storageKey;
    }

    public boolean isStore() {
        return store;
    }

    public boolean isOverride() {
        return override;
    }

    public long getMaxLength() {
        return maxLength;
    }

    public Predicate<String> getMetaDataKeyFilter() {
        return metaDataKeyFilter;
    }

    public boolean isCache() {
        return cache;
    }

    public Supplier<String> getCacheKeySupplier() {
        return cacheKeySupplier;
    }

    public boolean isFull() {
        return full;
    }

    public static final class Builder {

        /**
         * If true, means the metadata generated will be full except if there is any
         * metaDataKeyFilter, only these fields will be accepted.
         */
        private boolean full;

        /**
         * Provides the supplier to stores the metadata generated, if store is true
         */
        private StorageKey storageKey = null;

        /**
         * If true, means the metadata output will be storage
         */
        private boolean store = false;

        /**
         * if store is true, and you want to force always the file generation set this to true (keep
         * in mind it could be expensive)
         */
        private boolean override = false;

        /**
         * On huge file, we probably do not want to parse all the content, so this max will limited
         * how much do we want to read
         */
        private long maxLength = FileStorageAPI.configuredMaxLength();

        /**
         * {@link Predicate} filter the meta data key for the map result generation
         */
        private Predicate<String> metaDataKeyFilter = s -> true; // no filter by default

        /**
         * If true, means the medatada output will be stores in the memory cache.
         */
        private boolean cache = false;

        /**
         * Cache key supplier, if cache is true
         */
        private Supplier<String> cacheKeySupplier = null;


        public Builder storageKey(final StorageKey storageKey) {

            this.storageKey = storageKey;
            return this;
        }

        public Builder store(final boolean store) {

            this.store = store;
            return this;
        }

        public Builder full(final boolean full) {

            this.full = full;
            return this;
        }

        public Builder cache(final boolean cache) {

            this.cache = cache;
            return this;
        }

        public Builder override(final boolean override) {

            this.override = override;
            if (this.override) {
                this.store = true;
            }
            return this;
        }

        public Builder maxLength(final long maxLength) {

            this.maxLength = maxLength;
            return this;
        }

        public Builder metaDataKeyFilter(final Predicate<String> metaDataKeyFilter) {

            this.metaDataKeyFilter = metaDataKeyFilter;
            return this;
        }

        public Builder cache(final Supplier<String> cacheKeySupplier) {

            this.cache = true;
            this.cacheKeySupplier = cacheKeySupplier;
            return this;
        }

        public GenerateMetadataConfig build() {
            return new GenerateMetadataConfig(this);
        }
    }
}
