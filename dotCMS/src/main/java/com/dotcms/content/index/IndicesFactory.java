package com.dotcms.content.index;

import com.dotmarketing.exception.DotDataException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This interface defines methods for managing and interacting with versioned and non-versioned indices.
 * It provides functionality for loading, saving, removing, and inspecting indices and their versions.
 * It also includes utilities for constructing indices objects from database query results.
 *
 * <p><strong>DB tagging invariant:</strong> the {@code indicies} table stores OS index names with
 * the {@code .os} suffix ({@link IndexTag#OS}) to prevent primary-key collisions with ES rows.
 * Every {@link #saveIndices} implementation MUST call {@link #requireOSTagged(VersionedIndices)}
 * before writing, so untagged names are rejected fast rather than silently corrupting the DB.</p>
 */
public interface IndicesFactory {

    String CLUSTER_PREFIX = "cluster_";

    /**
     * Loads the indices information associated with the specified version.
     *
     * @param version the version identifier for which the indices need to be loaded
     * @return an {@link Optional} containing the {@link VersionedIndices} if available,
     *         or an empty {@link Optional} if no indices are found for the specified version
     * @throws DotDataException if an error occurs while accessing the data source
     */
    Optional<VersionedIndices> loadIndices(String version) throws DotDataException;

    /**
     * Loads all versioned indices available in the system.
     *
     * @return a list of {@link VersionedIndices} representing all loaded indices
     * @throws DotDataException if an error occurs while accessing the data source
     */
    List<VersionedIndices> loadAllIndices() throws DotDataException;

    /**
     * Loads the indices information that are not associated with any specific version.
     *
     * @return an {@link Optional} containing the {@link VersionedIndices} if non-versioned indices
     *         are available, or an empty {@link Optional} if no non-versioned indices are found
     * @throws DotDataException if an error occurs while accessing the data source
     */
    Optional<VersionedIndices> loadNonVersionedIndices() throws DotDataException;

    /**
     * Saves the provided indices information, including their associated version and index types.
     *
     * @param indicesInfo the {@link VersionedIndices} instance containing the index names and version
     *                    details to be saved
     * @throws DotDataException if an error occurs while saving the indices information to the data source
     */
    void saveIndices(VersionedIndices indicesInfo) throws DotDataException;

    /**
     * Retrieves a list of all available version identifiers for indices.
     *
     * @return an array of strings representing the available versions of indices.
     *         Returns an empty array if no versions are available.
     * @throws DotDataException if an error occurs while accessing the data source.
     */
    String[] getAvailableVersions() throws DotDataException;

    /**
     * Removes all indices associated with the specified version.
     *
     * @param version the version identifier for which the indices need to be removed.
     *                This should correspond to the version of indices currently managed
     *                in the system.
     * @throws DotDataException if an error occurs while removing the data or if the
     *         specified version does not exist in the system.
     */
    void removeVersion(String version) throws DotDataException;

    /**
     * Checks whether a specific version of indices exists in the system.
     *
     * @param version the version identifier to check for existence
     * @return true if the specified version exists, false otherwise
     * @throws DotDataException if an error occurs while accessing the data source
     */
    boolean versionExists(String version) throws DotDataException;

    /**
     * Retrieves the count of indices associated with the specified version.
     *
     * @param version the version identifier for which the indices count is requested
     * @return the number of indices associated with the given version
     * @throws DotDataException if an error occurs while accessing the data source
     */
    int getIndicesCount(String version) throws DotDataException;

    /**
     * Removes all existing indices associated with the specified version from the system.
     * This operation ensures that any indices tied to the provided version are deleted.
     *
     * @param version the version identifier for which the indices need to be removed
     *                from the existing data. The version must correspond to indices
     *                managed in the system.
     * @throws DotDataException if an error occurs while removing the indices, or if
     *         there are any issues related to accessing the data source.
     */
    void removeExistingIndicesForVersion(String version) throws DotDataException;

    /**
     * Inserts the indices information for modern indices based on the provided {@link VersionedIndices}.
     * The method processes the given indices information and ensures it is appropriately added
     * to the system. All necessary validations or operations required for insertion are performed.
     *
     * @param indicesInfo the {@link VersionedIndices} instance containing the index names and version
     *                    details that need to be inserted into the system
     * @throws DotDataException if an error occurs during the insertion process or while accessing
     *                          the underlying data source
     */
    void insertIndicesForModernIndicesInfo(VersionedIndices indicesInfo) throws DotDataException;

    /**
     * Inserts the specified index into the system if it exists. This method checks
     * whether the provided index information (name, type, and version) is valid and
     * can be processed, and adds it to the system if appropriate. The operation ensures
     * that necessary validations are performed.
     *
     * @param indexName the name of the index to be inserted
     * @param indexType the type of the index to be inserted, such as live, working, etc.
     * @param version   the version associated with the index being inserted
     * @throws DotDataException if an error occurs while attempting to insert the index,
     *                          or if the data source cannot be accessed
     */
    void insertIndexIfPresent(String indexName, String indexType,
            String version) throws DotDataException;

    /**
     * Removes the legacy ElasticSearch content-index pointers (NULL {@code index_version})
     * from the {@code indicies} table: the {@code live} / {@code working} pair and the transient
     * {@code reindex_live} / {@code reindex_working} pair.
     *
     * <p>Intended for Phase 3 (OpenSearch-only) cleanup, where ES is decommissioned and these
     * NULL-version rows are pure orphans. The {@code site_search} pointer — also stored
     * NULL-versioned but NOT part of the content-index migration — is deliberately preserved.
     * OS rows (which carry a non-NULL {@code index_version}) are never touched.</p>
     *
     * @return the number of rows removed
     * @throws DotDataException if a SQL error occurs
     */
    int removeLegacyIndices() throws DotDataException;

    /**
     * Validates that every present index name in {@code indicesInfo} carries the
     * {@link IndexTag#OS} marker ({@code .os} suffix) required for DB storage.
     *
     * <p>Call this at the top of every {@link #saveIndices} implementation.  The check is
     * skipped for legacy (non-versioned) indices because those predate the tagging scheme.</p>
     *
     * <p>Rationale: the {@code .os} suffix is the only thing that keeps OS and ES rows from
     * colliding on the same primary key in the shared {@code indicies} table.  If an untagged
     * name reaches the DB the row silently overwrites the ES entry — stripping a tag before
     * calling {@code saveIndices} is the one regression we must catch before it hits disk.</p>
     *
     * @param indicesInfo the object about to be persisted
     * @throws DotDataException if any present name is missing the {@code .os} tag
     */
    static void requireOSTagged(final VersionedIndices indicesInfo) throws DotDataException {
        if (indicesInfo.isLegacy()) {
            return; // non-versioned / legacy ES rows: no tag required
        }
        for (final String name : indicesInfo.allIndices()) {
            if (!IndexTag.OS.isTagged(name)) {
                throw new DotDataException(
                        "IndicesFactory invariant violated: index name '" + name +
                        "' (version=" + indicesInfo.version() + ") is missing the .os tag. " +
                        "Tag it with IndexTag.OS.tag(name) before persisting, or call " +
                        "VersionedIndicesAPIImpl.saveIndices() instead of the factory directly.");
            }
        }
    }

}
