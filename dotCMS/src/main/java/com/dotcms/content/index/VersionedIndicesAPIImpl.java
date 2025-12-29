package com.dotcms.content.index;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of IndicesAPI that uses IndicesFactory by composition.
 * This class provides the main API interface while delegating actual data operations
 * to the injected IndicesFactory. Exclusively works with ModernIndicesInfo.
 *
 * @author Fabrizzio
 */
@ApplicationScoped
public class VersionedIndicesAPIImpl implements VersionedIndicesAPI {

    private static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss");

    private final IndicesFactory indicesFactory;
    private static final Cache cache = new Cache();

    @Inject
    public VersionedIndicesAPIImpl(IndicesFactory indicesFactory) {
        this.indicesFactory = indicesFactory;
    }

    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadIndices(String version) throws DotDataException {
        Logger.debug(this, "Loading indices for version: " + version);

        // Try cache first
        VersionedIndices cached = cache.get(version);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Load from database
        Optional<VersionedIndices> loaded = indicesFactory.loadIndices(version);

        // Cache the result if present
        loaded.ifPresent(cache::put);

        return loaded;
    }

    @CloseDBIfOpened
    @Override
    public List<VersionedIndices> loadAllIndices() throws DotDataException {
        Logger.debug(this, "Loading all indices");

        // Try cache first
        List<VersionedIndices> cached = cache.getAllVersions();
        if (cached != null) {
            return cached;
        }

        // Load from database
        List<VersionedIndices> loaded = indicesFactory.loadAllIndices();

        // Cache the result
        cache.putAllVersions(loaded);

        return loaded;
    }

    @WrapInTransaction
    @Override
    public void saveIndices(VersionedIndices indicesInfo) throws DotDataException {
        Logger.debug(this, "Saving indices with embedded version: " + indicesInfo.version());

        // Save to database
        indicesFactory.saveIndices(indicesInfo);

        // Update cache
        cache.put(indicesInfo);

        // Invalidate all versions cache since it's now stale
        cache.invalidateAllVersionsCache();
    }

    @WrapInTransaction
    @Override
    public void removeVersion(String version) throws DotDataException {
        Logger.debug(this, "Removing version: " + version);

        // Remove from database
        indicesFactory.removeVersion(version);

        // Remove from cache
        cache.remove(version);
    }

    @CloseDBIfOpened
    @Override
    public boolean versionExists(String version) throws DotDataException {
        Logger.debug(this, "Checking if version exists: " + version);
        return indicesFactory.versionExists(version);
    }

    @CloseDBIfOpened
    @Override
    public int getIndicesCount(String version) throws DotDataException {
        Logger.debug(this, "Getting indices count for version: " + version);
        return indicesFactory.getIndicesCount(version);
    }

    public Instant extractTimestamp(String indexName) throws DotDataException {
        Logger.debug(this, "Extracting timestamp from index name: " + indexName);

        if (!UtilMethods.isSet(indexName)) {
            throw new DotDataException("Index name cannot be null or empty");
        }

        try {
            // Extract timestamp from pattern: cluster_<CLUSTER_ID>.<INDEX_TYPE_PREFIX>_<TIMESTAMP>
            final int lastUnderscoreIndex = indexName.lastIndexOf("_");
            if (lastUnderscoreIndex == -1 || lastUnderscoreIndex == indexName.length() - 1) {
                throw new DotDataException("Index name does not follow expected pattern: " + indexName);
            }

            final String timestampStr = indexName.substring(lastUnderscoreIndex + 1);
            final Date parsedDate = TIMESTAMP_FORMATTER.parse(timestampStr);
            return parsedDate.toInstant();
        } catch (ParseException e) {
            throw new DotDataException("Failed to extract timestamp from index name: " + indexName, e);
        }
    }

    @CloseDBIfOpened
    public String[] getAvailableVersions() throws DotDataException {
        Logger.debug(this, "Getting available versions");
        return indicesFactory.getAvailableVersions();
    }

    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadNonVersionedIndices() throws DotDataException {
        Logger.debug(this, "Loading legacy non-versioned indices (migration/compatibility purpose)");

        // Try cache first
        VersionedIndices cached = cache.getLegacyIndices();
        if (cached != null) {
            return Optional.of(cached);
        }

        // Load from database
        Optional<VersionedIndices> loaded = indicesFactory.loadNonVersionedIndices();

        // Cache the result if present
        loaded.ifPresent(cache::putLegacyIndices);

        return loaded;
    }

    @CloseDBIfOpened
    @Override
    public Optional<VersionedIndices> loadDefaultVersionedIndices() throws DotDataException {
        Logger.debug(this, "Loading default versioned indices for version: " + VersionedIndices.OPENSEARCH_3X);
        return loadIndices(VersionedIndices.OPENSEARCH_3X);
    }

    /**
     * Clears all cached indices data.
     * This should be called when indices are modified to ensure cache consistency.
     */
    public void clearCache() {
        cache.clearCache();
        Logger.info(this, "VersionedIndicesAPI cache cleared");
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

        public Cache() {
            this.cacheAdmin = CacheLocator.getCacheAdministrator();
        }

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

        @SuppressWarnings("unchecked")
        public List<VersionedIndices> getAllVersions() {
            try {
                return (List<VersionedIndices>) cacheAdmin.get(ALL_VERSIONS_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error retrieving cached all versions", ex);
                return null;
            }
        }

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

        public VersionedIndices getLegacyIndices() {
            try {
                return (VersionedIndices) cacheAdmin.get(LEGACY_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error retrieving cached legacy indices", ex);
                return null;
            }
        }

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

        public void clearCache() {
            try {
                cacheAdmin.flushGroup(primaryGroup);
            } catch (Exception ex) {
                Logger.warn(this, "Error clearing cache", ex);
            }
        }

        public void invalidateAllVersionsCache() {
            try {
                cacheAdmin.remove(ALL_VERSIONS_KEY, primaryGroup);
            } catch (Exception ex) {
                Logger.debug(this, "Error invalidating all versions cache", ex);
            }
        }
    }
}