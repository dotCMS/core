package com.dotcms.content.index;

import com.dotmarketing.exception.DotDataException;
import java.time.Instant;
import java.util.List;

/**
 * Modern API to store and retrieve information about search engine indices with MANDATORY version support.
 *
 * IMPORTANT: This API ONLY handles versioned indices and will REJECT any index without a version.
 *
 * Key constraints:
 * - All save operations require a valid version in the ModernIndicesInfo object
 * - All load operations only return indices that have versions (legacy null-version indices are ignored)
 * - Attempting to save indices without version will throw DotDataException
 *
 * This API exclusively uses ModernIndicesInfo for all operations, providing a clean interface
 * for managing versioned index information in the indicies table.
 *
 * @author Fabrizzio
 */
public interface VersionedIndicesAPI {

    /**
     * Returns ModernIndicesInfo with index names for the given version.
     * Only returns indices that have the specified version (ignores legacy null-version indices).
     *
     * @param version the version to load indices for (must not be null or empty)
     * @return ModernIndicesInfo instance with version information, or empty if not found
     * @throws DotDataException if there's an error loading the indices or if version is null/empty
     */
    VersionedIndices loadIndices(String version) throws DotDataException;

    /**
     * Returns all ModernIndicesInfo instances for all versions stored in the system.
     * Only includes indices that have versions (filters out legacy null-version indices).
     *
     * @return List of ModernIndicesInfo instances, empty list if no versioned indices exist
     * @throws DotDataException if there's an error loading the indices
     */
    List<VersionedIndices> loadAllIndices() throws DotDataException;

    /**
     * Updates the information about search engine indices. The version is taken from
     * the ModernIndicesInfo object itself and MUST be present.
     *
     * @param indicesInfo the indices information to save, MUST include a valid version
     * @throws DotDataException if version is missing/null/empty or if there's an error saving
     */
    void saveIndices(VersionedIndices indicesInfo) throws DotDataException;

    /**
     * Removes indices information for a specific version.
     *
     * @param version the version to remove
     * @throws DotDataException if there's an error removing the version
     */
    void removeVersion(String version) throws DotDataException;

    /**
     * Checks if a version exists.
     * @param version
     * @return
     * @throws DotDataException
     */
    boolean versionExists(String version) throws DotDataException;

    /**
     * Gets the count of indices for a specific version.
     *
     * @param version the version to count indices for
     * @return number of indices for the given version
     * @throws DotDataException if there's an error counting the indices
     */
    int getIndicesCount(String version) throws DotDataException;

    /**
     * Extracts the timestamp from an index name.
     * @param indexName
     * @return
     * @throws DotDataException
     */
     Instant extractTimestamp(String indexName) throws DotDataException;

    /**
     * Loads legacy non-versioned indices (those with index_version = NULL).
     * These are indices from the old system that don't have version information.
     *
     * NOTE: This method is provided for migration/compatibility purposes only.
     * The main API focuses on versioned indices, but this allows access to legacy data.
     *
     * @return VersionedIndicesInfo containing legacy indices without version information
     * @throws DotDataException if there's an error loading the legacy indices
     */
    VersionedIndices loadNonVersionedIndices() throws DotDataException;

    /**
     * Clears all cached indices data.
     * This should be called when indices are modified outside of this API
     * to ensure cache consistency.
     */
    void clearCache();
}
