package com.dotcms.content.index;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.index.IndexTag;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of IndicesAPI that uses IndicesFactory by composition.
 * This class provides the main API interface while delegating actual data operations
 * to the injected IndicesFactory. Exclusively works with ModernIndicesInfo.
 *
 * @author Fabrizzio
 */
@ApplicationScoped
public class VersionedIndicesAPIImpl implements VersionedIndicesAPI {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final IndicesFactory indicesFactory;
    private static final Cache cache = new Cache();

    /**
     * Constructor for CDI injection.
     *
     * @param indicesFactory the factory used for database operations
     */
    @Inject
    public VersionedIndicesAPIImpl(IndicesFactory indicesFactory) {
        this.indicesFactory = indicesFactory;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Names are returned in their canonical physical form — cluster-prefixed and tag-suffixed
     * (e.g. {@code cluster_xxx.working_ts.os}). The {@code .os} tag is part of the canonical OS
     * name at every layer (DB row, cluster index, and any caller that has resolved through
     * {@code toPhysicalName}); it is NOT a DB-only artifact and is NOT stripped here. Callers can
     * pass the returned name directly to any OS-side operation.</p>
     */
    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadIndices(String version) throws DotDataException {
        Logger.debug(this, "Loading indices for version: " + version);

        // Cache holds the canonical tagged form — same as what the DB returns.
        VersionedIndices cached = cache.get(version);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Load from DB and cache as-is — no transformation; the name carries .os end-to-end.
        Optional<VersionedIndices> loaded = indicesFactory.loadIndices(version);
        loaded.ifPresent(cache::put);
        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @CloseDBIfOpened
    @Override
    public List<VersionedIndices> loadAllIndices() throws DotDataException {
        Logger.debug(this, "Loading all indices");

        // Try cache first
        List<VersionedIndices> cached = cache.getAllVersions();
        if (cached != null) {
            return cached;
        }

        // Load from DB and cache as-is — names retain the canonical .os tag end-to-end.
        List<VersionedIndices> loaded = indicesFactory.loadAllIndices();
        cache.putAllVersions(loaded);

        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Callers may pass any accepted form (logical, cluster-prefixed, with or without the
     * {@code .os} tag). This method calls {@link #tagOS} to ensure the tag is present before
     * INSERT — idempotent on already-tagged names — and caches whatever the caller passed in.
     * In the normal production flow the names arrive already tagged via {@code toPhysicalName},
     * so {@code tagOS} is a belt-and-suspenders guard.</p>
     */
    @WrapInTransaction
    @Override
    public void saveIndices(VersionedIndices indicesInfo) throws DotDataException {
        Logger.debug(this, "Saving indices with embedded version: " + indicesInfo.version());

        // Apply the .os tag idempotently before INSERT (DB row uses the canonical tagged form).
        indicesFactory.saveIndices(tagOS(indicesInfo));

        // Cache the caller's payload as-is. In normal production flow this is already tagged,
        // matching what loadIndices will return on the next cache hit.
        cache.put(indicesInfo);

        // Invalidate the all-versions cache since it's now stale.
        cache.invalidateAllVersionsCache();
    }

    /**
     * {@inheritDoc}
     */
    @WrapInTransaction
    @Override
    public void removeVersion(String version) throws DotDataException {
        Logger.debug(this, "Removing version: " + version);

        // Remove from database
        indicesFactory.removeVersion(version);

        // Remove from cache
        cache.remove(version);
    }

    /**
     * {@inheritDoc}
     */
    @CloseDBIfOpened
    @Override
    public boolean versionExists(String version) throws DotDataException {
        Logger.debug(this, "Checking if version exists: " + version);
        return indicesFactory.versionExists(version);
    }

    /**
     * {@inheritDoc}
     */
    @CloseDBIfOpened
    @Override
    public int getIndicesCount(String version) throws DotDataException {
        Logger.debug(this, "Getting indices count for version: " + version);
        return indicesFactory.getIndicesCount(version);
    }

    /**
     * {@inheritDoc}
     */
    public Instant extractTimestamp(String indexName) throws DotDataException {
        Logger.debug(this, "Extracting timestamp from index name: " + indexName);

        if (!UtilMethods.isSet(indexName)) {
            throw new DotDataException("Index name cannot be null or empty");
        }

        try {
            // Extract the timestamp from the pattern: cluster_<CLUSTER_ID>.<INDEX_TYPE_PREFIX>_<TIMESTAMP>
            // The .os tag is part of the name identity but not parseable as a timestamp — strip it
            // locally first (see "deriving the embedded timestamp" in OPENSEARCH_MIGRATION.md).
            final String base = IndexTag.strip(indexName);
            final int lastUnderscoreIndex = base.lastIndexOf("_");
            if (lastUnderscoreIndex == -1 || lastUnderscoreIndex == base.length() - 1) {
                throw new DotDataException("Index name does not follow expected pattern: " + indexName);
            }

            final String timestampStr = base.substring(lastUnderscoreIndex + 1);
            final LocalDateTime ldt = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new DotDataException("Failed to extract timestamp from index name: " + indexName, e);
        }
    }

    /**
     * Retrieves all available version identifiers for indices.
     * This method delegates to the underlying factory to get version information
     * from the database.
     *
     * @return an array of strings representing available versions, empty array if none exist
     * @throws DotDataException if there's an error accessing the data source
     */
    @CloseDBIfOpened
    public String[] getAvailableVersions() throws DotDataException {
        Logger.debug(this, "Getting available versions");
        return indicesFactory.getAvailableVersions();
    }

    /**
     * {@inheritDoc}
     */
    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadNonVersionedIndices() throws DotDataException {
        Logger.debug(this, "Loading legacy non-versioned indices (migration/compatibility purpose)");

        // Try cache first
        VersionedIndices cached = cache.getLegacyIndices();
        if (cached != null) {
            return Optional.of(cached);
        }

        // Load from DB and cache as-is. Legacy rows have no .os tag to begin with — the tag
        // belongs to the new index set only; nothing to strip here.
        Optional<VersionedIndices> loaded = indicesFactory.loadNonVersionedIndices();
        loaded.ifPresent(cache::putLegacyIndices);

        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadDefaultVersionedIndices() throws DotDataException {
        Logger.debug(this, "Loading default versioned indices for version: " + VersionedIndices.OPENSEARCH_3X);
        return loadIndices(VersionedIndices.OPENSEARCH_3X);
    }

    /**
     * {@inheritDoc}
     */
    public void clearCache() {
        cache.clearCache();
        Logger.info(this, "VersionedIndicesAPI cache cleared");
    }

    /**
     * Returns a copy of {@code indices} with all name fields tagged with the {@code .os} suffix
     * (idempotent — already-tagged names are unchanged). This is the canonical OS physical form
     * — written to both the {@code indices} DB table and the OS cluster index itself.
     */
    private static VersionedIndices tagOS(final VersionedIndices indices) {
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        builder.version(indices.version());
        indices.live()          .map(IndexTag.OS::tag).ifPresent(builder::live);
        indices.working()       .map(IndexTag.OS::tag).ifPresent(builder::working);
        indices.reindexLive()   .map(IndexTag.OS::tag).ifPresent(builder::reindexLive);
        indices.reindexWorking().map(IndexTag.OS::tag).ifPresent(builder::reindexWorking);
        indices.siteSearch()    .map(IndexTag.OS::tag).ifPresent(builder::siteSearch);
        return builder.build();
    }

    /**
     * Static inner cache class for VersionedIndicesAPI.
     * Provides caching functionality to reduce database calls.
     */
    private static class Cache {

        private final DotCacheAdministrator cacheAdmin;
        private final String primaryGroup = "VersionedIndicesCache";

        // Cache key constants
        private static final String VERSION_KEY_PREFIX = "version:";
        private static final String ALL_VERSIONS_KEY = "all_versions";
        private static final String LEGACY_KEY = "legacy_indices";

        /**
         * Constructor that initializes the cache with dotCMS cache administrator.
         */
        public Cache() {
            this.cacheAdmin = CacheLocator.getCacheAdministrator();
        }

        /**
         * Retrieves cached indices for the specified version.
         *
         * @param version the version to retrieve from cache
         * @return the cached VersionedIndices or null if not found or version is invalid
         */
        public VersionedIndices get(String version) {
            if (!UtilMethods.isSet(version)) {
                return null;
            }

            try {
                String key = VERSION_KEY_PREFIX + version;
                return (VersionedIndices) cacheAdmin.get(key, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error retrieving cached indices for version: " + version, ex);
                return null;
            }
        }

        /**
         * Stores indices information in cache using the version as key.
         *
         * @param indicesInfo the indices information to cache, must have a valid version
         */
        public void put(VersionedIndices indicesInfo) {
            if (indicesInfo == null || !UtilMethods.isSet(indicesInfo.version())) {
                return;
            }

            String version = indicesInfo.version();
            try {
                String key = VERSION_KEY_PREFIX + version;
                cacheAdmin.put(key, indicesInfo, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error caching indices for version: " + version, ex);
            }
        }

        /**
         * Retrieves all cached versions of indices.
         *
         * @return list of all cached VersionedIndices or null if not found
         */
        @SuppressWarnings("unchecked")
        public List<VersionedIndices> getAllVersions() {
            try {
                return (List<VersionedIndices>) cacheAdmin.get(ALL_VERSIONS_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error retrieving cached all versions", ex);
                return null;
            }
        }

        /**
         * Stores the complete list of all indices versions in cache.
         *
         * @param allIndices the list of all indices to cache
         */
        public void putAllVersions(List<VersionedIndices> allIndices) {
            if (allIndices == null) {
                return;
            }

            try {
                cacheAdmin.put(ALL_VERSIONS_KEY, allIndices, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error caching all versions", ex);
            }
        }

        /**
         * Removes cached indices for the specified version and invalidates related cache.
         *
         * @param version the version to remove from cache
         */
        public void remove(String version) {
            if (!UtilMethods.isSet(version)) {
                return;
            }

            try {
                String key = VERSION_KEY_PREFIX + version;
                cacheAdmin.remove(key, primaryGroup);
                // Also invalidate the all versions cache since it's now stale
                cacheAdmin.remove(ALL_VERSIONS_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error removing cached indices for version: " + version, ex);
            }
        }

        /**
         * Retrieves cached legacy (non-versioned) indices.
         *
         * @return the cached legacy VersionedIndices or null if not found
         */
        public VersionedIndices getLegacyIndices() {
            try {
                return (VersionedIndices) cacheAdmin.get(LEGACY_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error retrieving cached legacy indices", ex);
                return null;
            }
        }

        /**
         * Stores legacy (non-versioned) indices information in cache.
         *
         * @param legacyIndices the legacy indices to cache
         */
        public void putLegacyIndices(VersionedIndices legacyIndices) {
            if (legacyIndices == null) {
                return;
            }

            try {
                cacheAdmin.put(LEGACY_KEY, legacyIndices, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error caching legacy indices", ex);
            }
        }

        /**
         * Clears all cached data by flushing the entire cache group.
         */
        public void clearCache() {
            try {
                cacheAdmin.flushGroup(primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error clearing cache", ex);
            }
        }

        /**
         * Invalidates only the all-versions cache entry while preserving individual version caches.
         */
        public void invalidateAllVersionsCache() {
            try {
                cacheAdmin.remove(ALL_VERSIONS_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.debug(this, "Error invalidating all versions cache", ex);
            }
        }
    }
}