package com.dotcms.content.index;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory implementation for managing modern indices with version support.
 * This factory handles storage and retrieval of index information in the indicies table
 * with proper version management and CDI integration.
 * Used by IndicesAPIImpl through composition pattern.
 * Exclusively works with ModernIndicesInfo.
 *
 * @author Fabrizzio
 */
@ApplicationScoped
public class IndicesFactory {

    // SQL queries for ModernIndicesInfo operations - ONLY with version support
    private static final String LOAD_INDICES_BY_VERSION_SQL =
        "SELECT index_name, index_type, index_version FROM indicies WHERE index_version = ? AND index_version IS NOT NULL";
    private static final String LOAD_LATEST_INDICES_SQL =
        "SELECT index_name, index_type, index_version FROM indicies WHERE index_version = (SELECT MAX(index_version) FROM indicies WHERE index_version IS NOT NULL) AND index_version IS NOT NULL";
    private static final String LOAD_ALL_INDICES_SQL =
        "SELECT index_name, index_type, index_version FROM indicies WHERE index_version IS NOT NULL ORDER BY index_version DESC";
    private static final String LOAD_NON_VERSIONED_INDICES_SQL =
        "SELECT index_name, index_type FROM indicies WHERE index_version IS NULL";
    private static final String INSERT_INDEX_SQL =
        "INSERT INTO indicies (index_name, index_type, index_version) VALUES (?, ?, ?)";
    private static final String DELETE_INDICES_BY_VERSION_SQL =
        "DELETE FROM indicies WHERE index_version = ? AND index_version IS NOT NULL";
    private static final String GET_VERSIONS_SQL =
        "SELECT DISTINCT index_version FROM indicies WHERE index_version IS NOT NULL ORDER BY index_version DESC";
    private static final String VERSION_EXISTS_SQL =
        "SELECT COUNT(*) as count FROM indicies WHERE index_version = ? AND index_version IS NOT NULL";
    private static final String COUNT_INDICES_BY_VERSION_SQL =
        "SELECT COUNT(*) as count FROM indicies WHERE index_version = ? AND index_version IS NOT NULL";

    public VersionedIndices loadIndices(String version) throws DotDataException {
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version cannot be null or empty");
        }

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(LOAD_INDICES_BY_VERSION_SQL)
                     .addParam(version);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            return buildModernIndicesFromResults(results);
        } catch (Exception e) {
            Logger.error(this, "Failed to load indices for version: " + version, e);
            throw new DotDataException("Failed to load indices for version: " + version, e);
        }
    }

    public VersionedIndices loadLatestIndices() throws DotDataException {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(LOAD_LATEST_INDICES_SQL);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            return buildModernIndicesFromResults(results);
        } catch (Exception e) {
            Logger.error(this, "Failed to load latest indices", e);
            throw new DotDataException("Failed to load latest indices", e);
        }
    }

    public List<VersionedIndices> loadAllIndices() throws DotDataException {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(LOAD_ALL_INDICES_SQL);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            return buildModernIndicesListFromResults(results);
        } catch (Exception e) {
            Logger.error(this, "Failed to load all indices", e);
            throw new DotDataException("Failed to load all indices", e);
        }
    }

    /**
     * Loads legacy non-versioned indices (those with index_version = NULL).
     * These are indices from the old system that don't have version information.
     *
     * @return VersionedIndicesInfo containing legacy indices without version information
     * @throws DotDataException if there's an error loading the legacy indices
     */
    public VersionedIndices loadNonVersionedIndices() throws DotDataException {
        Logger.debug(this, "Loading legacy non-versioned indices (index_version IS NULL)");

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(LOAD_NON_VERSIONED_INDICES_SQL);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            return buildNonVersionedIndicesInfo(results);
        } catch (Exception e) {
            Logger.error(this, "Failed to load legacy non-versioned indices", e);
            throw new DotDataException("Failed to load legacy non-versioned indices", e);
        }
    }

    public void saveIndices(VersionedIndices indicesInfo) throws DotDataException {
        // STRICT VERSION REQUIREMENT - indices without version are rejected
        final String version = indicesInfo.version().orElse(null);
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version is REQUIRED. This API only handles versioned indices. Indices without version are not supported.");
        }

        // Validate that indices have content
        if (!indicesInfo.hasAnyIndex()) {
            throw new DotDataException("At least one index must be specified when saving versioned indices for version: " + version);
        }

        try {
            // First, remove existing indices for this version
            removeExistingIndicesForVersion(version);

            // Insert new indices
            insertIndicesForModernIndicesInfo(indicesInfo);

            Logger.info(this, "Successfully saved versioned indices for version: " + version);
        } catch (Exception e) {
            Logger.error(this, "Failed to save versioned indices for version: " + version, e);
            throw new DotDataException("Failed to save versioned indices for version: " + version, e);
        }
    }


    public String[] getAvailableVersions() throws DotDataException {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(GET_VERSIONS_SQL);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            return results.stream()
                         .map(row -> row.get("index_version"))
                         .filter(Objects::nonNull)
                         .map(Object::toString)
                         .toArray(String[]::new);
        } catch (Exception e) {
            Logger.error(this, "Failed to get available versions", e);
            throw new DotDataException("Failed to get available versions", e);
        }
    }

    public void removeVersion(String version) throws DotDataException {
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version cannot be null or empty");
        }

        try {
            final DotConnect dotConnect = new DotConnect();
            int deletedRows = dotConnect.executeUpdate(DELETE_INDICES_BY_VERSION_SQL, version);
            Logger.info(this, "Removed " + deletedRows + " indices for version: " + version);
        } catch (Exception e) {
            Logger.error(this, "Failed to remove version: " + version, e);
            throw new DotDataException("Failed to remove version: " + version, e);
        }
    }

    public boolean versionExists(String version) throws DotDataException {
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version cannot be null or empty");
        }

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(VERSION_EXISTS_SQL)
                     .addParam(version);

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            if (!results.isEmpty()) {
                final Object count = results.get(0).get("count");
                return count != null && Integer.parseInt(count.toString()) > 0;
            }
            return false;
        } catch (Exception e) {
            Logger.error(this, "Failed to check if version exists: " + version, e);
            throw new DotDataException("Failed to check if version exists: " + version, e);
        }
    }

    public int getIndicesCount(String version) throws DotDataException {
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version cannot be null or empty");
        }

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(COUNT_INDICES_BY_VERSION_SQL)
                     .addParam(version);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> results = dotConnect.loadResults();
            if (!results.isEmpty()) {
                final Object count = results.get(0).get("count");
                return count != null ? Integer.parseInt(count.toString()) : 0;
            }
            return 0;
        } catch (Exception e) {
            Logger.error(this, "Failed to get indices count for version: " + version, e);
            throw new DotDataException("Failed to get indices count for version: " + version, e);
        }
    }

    /**
     * Builds a ModernIndicesInfo from database results.
     * Expects results to have columns: index_name, index_type, index_version
     * STRICT: Only processes results that have a valid version
     */
    private VersionedIndices buildModernIndicesFromResults(List<Map<String, Object>> results) throws DotDataException {
        if (results.isEmpty()) {
            return VersionedIndicesImpl.builder().build();
        }

        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        String version = null;

        for (Map<String, Object> row : results) {
            final String indexName = (String) row.get("index_name");
            final String indexType = (String) row.get("index_type");
            final String indexVersion = (String) row.get("index_version");

            // STRICT VERSION VALIDATION - skip entries without version
            if (!UtilMethods.isSet(indexVersion)) {
                Logger.debug(this, "Skipping index entry without version: " + indexName + " (type: " + indexType + ")");
                continue;
            }

            if (!UtilMethods.isSet(indexName) || !UtilMethods.isSet(indexType)) {
                Logger.debug(this, "Skipping incomplete index entry for version: " + indexVersion);
                continue;
            }

            // Set version from first valid row
            if (version == null) {
                version = indexVersion;
                builder.version(version);
            } else if (!version.equals(indexVersion)) {
                // This should not happen with proper SQL queries, but just in case
                Logger.warn(this, "Found mixed versions in result set: " + version + " vs " + indexVersion);
            }

            switch (indexType.toLowerCase()) {
                case "live":
                    builder.live(indexName);
                    break;
                case "working":
                    builder.working(indexName);
                    break;
                case "reindex_live":
                    builder.reindexLive(indexName);
                    break;
                case "reindex_working":
                    builder.reindexWorking(indexName);
                    break;
                case "site_search":
                    builder.siteSearch(indexName);
                    break;
                default:
                    Logger.warn(this, "Unknown index type: " + indexType + " for version: " + indexVersion);
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Builds a list of ModernIndicesInfo grouped by version from database results.
     */
    private List<VersionedIndices> buildModernIndicesListFromResults(List<Map<String, Object>> results) throws DotDataException {
        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        // Group by version
        final Map<String, List<Map<String, Object>>> versionGroups = results.stream()
                .filter(row -> UtilMethods.isSet(row.get("index_version")))
                .collect(Collectors.groupingBy(row -> (String) row.get("index_version")));

        final List<VersionedIndices> indicesList = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : versionGroups.entrySet()) {
            final VersionedIndices indicesInfo = buildModernIndicesFromResults(entry.getValue());
            indicesList.add(indicesInfo);
        }

        return indicesList;
    }

    /**
     * Removes all existing indices for a given version.
     */
    private void removeExistingIndicesForVersion(String version) throws DotDataException {
        try {
            final DotConnect dotConnect = new DotConnect();
            final int deletedCount = dotConnect.executeUpdate(DELETE_INDICES_BY_VERSION_SQL, version);
        } catch (Exception e) {
            throw new DotDataException("Failed to remove existing indices for version: " + version, e);
        }
    }

    /**
     * Inserts indices from ModernIndicesInfo into the database.
     */
    private void insertIndicesForModernIndicesInfo(VersionedIndices indicesInfo) throws DotDataException {
        final String version = indicesInfo.version().orElse(null);
        if (!UtilMethods.isSet(version)) {
            throw new DotDataException("Version is required for inserting indices");
        }

        try {
            insertIndexIfPresent(indicesInfo.live().orElse(null), "live", version);
            insertIndexIfPresent(indicesInfo.working().orElse(null), "working", version);
            insertIndexIfPresent(indicesInfo.reindexLive().orElse(null), "reindex_live", version);
            insertIndexIfPresent(indicesInfo.reindexWorking().orElse(null), "reindex_working", version);
            insertIndexIfPresent(indicesInfo.siteSearch().orElse(null), "site_search", version);
        } catch (Exception e) {
            throw new DotDataException("Failed to insert indices for version: " + version, e);
        }
    }

    /**
     * Inserts a single index entry if the index name is present.
     */
    private void insertIndexIfPresent(String indexName, String indexType, String version) throws DotDataException {
        if (UtilMethods.isSet(indexName)) {
            try {
                final DotConnect dotConnect = new DotConnect();
                dotConnect.setSQL(INSERT_INDEX_SQL)
                         .addParam(indexName)
                         .addParam(indexType)
                         .addParam(version);
                dotConnect.getResult();
            } catch (Exception e) {
                throw new DotDataException("Failed to insert index: " + indexName, e);
            }
        }
    }

    /**
     * Builds a VersionedIndicesInfo from legacy non-versioned database results.
     * Expects results to have columns: index_name, index_type (no index_version)
     *
     * @param results Database query results for non-versioned indices
     * @return VersionedIndicesInfo containing legacy indices without version
     */
    private VersionedIndices buildNonVersionedIndicesInfo(List<Map<String, Object>> results) {
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        // Note: No version is set for legacy indices

        if (results.isEmpty()) {
            Logger.debug(this, "No legacy non-versioned indices found");
            return builder.build();
        }

        for (Map<String, Object> row : results) {
            final String indexName = (String) row.get("index_name");
            final String indexType = (String) row.get("index_type");

            if (!UtilMethods.isSet(indexName) || !UtilMethods.isSet(indexType)) {
                Logger.debug(this, "Skipping incomplete legacy index entry: name=" + indexName + ", type=" + indexType);
                continue;
            }

            // Map to appropriate index type in builder
            switch (indexType.toLowerCase()) {
                case "live":
                    builder.live(indexName);
                    break;
                case "working":
                    builder.working(indexName);
                    break;
                case "reindex_live":
                    builder.reindexLive(indexName);
                    break;
                case "reindex_working":
                    builder.reindexWorking(indexName);
                    break;
                case "site_search":
                    builder.siteSearch(indexName);
                    break;
                default:
                    Logger.warn(this, "Unknown legacy index type: " + indexType);
                    break;
            }

            Logger.debug(this, "Found legacy non-versioned index: " + indexType + " -> " + indexName);
        }

        final VersionedIndices result = builder.build();
        Logger.info(this, "Loaded legacy non-versioned indices: " + result.toString());
        return result;
    }
}