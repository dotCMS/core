package com.dotcms.storage;

import com.dotmarketing.util.Config;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Configuration to generate the meta data.
 * @author jsanca
 */
public class GenerateMetaDataConfiguration {

    /**
     * Provides the supplier to stores the metadata generated, if store is true
     */
    private final Supplier<Optional<File>>    metaDataFileSupplier;

    /**
     * If true, means the metadata output will be stores in the metaDataFileSupplier file
     */
    private final boolean           store;

    /**
     * if store is true, and you want to force always the file generation set this to true (keep in mind it could be expensive)
     */
    private final boolean           override;

    /**
     * On huge file, we probably do not want to parse all the content, so this max will limited how much do we want to read
     */
    private final int               maxLength;

    /**
     * {@link Predicate} filter the meta data key for the map result generation
     */
    private final Predicate<String> metaDataKeyFilter;

    /**
     * If true, means the medatada output will be stores in the memory cache.
     */
    private final boolean           cache;

    /**
     * Cache key supplier, if cache is true
     */
    private final Supplier<String>  cacheKeySupplier;

    /**
     * Cache group, if cache is true
     */
    private final Supplier<String>  cacheGroupSupplier;

    /**
     * If true, means the metadata generated will be full except if there is any metaDataKeyFilter, only these fields will be accepted.
     */
    private final boolean           full;

    private GenerateMetaDataConfiguration(final Builder builder) {

        this.cache                = builder.cache;
        this.cacheGroupSupplier   = builder.cacheGroupSupplier;
        this.cacheKeySupplier     = builder.cacheKeySupplier;
        this.maxLength            = builder.maxLength;
        this.metaDataFileSupplier = builder.metaDataFileSupplier;
        this.metaDataKeyFilter    = builder.metaDataKeyFilter;
        this.override             = builder.override;
        this.store                = builder.store;
        this.full                 = builder.full;
    }

    public Supplier<Optional<File>> getMetaDataFileSupplier() {
        return metaDataFileSupplier;
    }

    public boolean isStore() {
        return store;
    }

    public boolean isOverride() {
        return override;
    }

    public int getMaxLength() {
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

    public Supplier<String> getCacheGroupSupplier() {
        return cacheGroupSupplier;
    }

    public boolean isFull() {
        return full;
    }

    public static final class Builder {

        /**
         * If true, means the metadata generated will be full except if there is any metaDataKeyFilter, only these fields will be accepted.
         */
        private boolean           full = false;

        /**
         * Provides the supplier to stores the metadata generated, if store is true
         */
        private Supplier<Optional<File>>    metaDataFileSupplier = ()-> Optional.empty();

        /**
         * If true, means the metadata output will be stores in the metaDataFileSupplier file
         */
        private boolean           store = false;

        /**
         * if store is true, and you want to force always the file generation set this to true (keep in mind it could be expensive)
         */
        private boolean           override = false;

        /**
         * On huge file, we probably do not want to parse all the content, so this max will limited how much do we want to read
         */
        private int               maxLength = FileStorageAPI.configuredMaxLength(); // todo: convert to long

        /**
         * {@link Predicate} filter the meta data key for the map result generation
         */
        private Predicate<String> metaDataKeyFilter = s -> true; // no filter by default

        /**
         * If true, means the medatada output will be stores in the memory cache.
         */
        private boolean           cache = false;

        /**
         * Cache key supplier, if cache is true
         */
        private Supplier<String>  cacheKeySupplier = null;

        /**
         * Cache group, if cache is true, by default goes to FileAssetMetadataCache
         */
        private Supplier<String>  cacheGroupSupplier = ()-> "Contentlet"; // todo: this must be not editable

        public Builder metaDataFileSupplier(final Supplier<Optional<File>> metaDataFileSupplier) {

            this.store                = true;
            this.metaDataFileSupplier = metaDataFileSupplier;
            return this;
        }

        public Builder full(final boolean full) {

            this.full = full;
            return this;
        }

        public Builder override(final boolean override) {

            this.override = override;
            return this;
        }

        public Builder maxLength(final int maxLength) {

            this.maxLength = maxLength;
            return this;
        }

        public Builder metaDataKeyFilter(final Predicate<String> metaDataKeyFilter) {

            this.metaDataKeyFilter = metaDataKeyFilter;
            return this;
        }

        public Builder cache(final Supplier<String>  cacheKeySupplier) {

            this.cache            = true;
            this.cacheKeySupplier = cacheKeySupplier;
            return this;
        }

        public Builder cache(final Supplier<String>  cacheKeySupplier, final Supplier<String>  cacheGroupSupplier) {

            this.cache              = true;
            this.cacheKeySupplier   = cacheKeySupplier;
            this.cacheGroupSupplier = cacheGroupSupplier;
            return this;
        }



        public GenerateMetaDataConfiguration build() {
            return new GenerateMetaDataConfiguration(this);
        }
    }
}
