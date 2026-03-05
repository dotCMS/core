package com.dotcms.content.index;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.IndexStats;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vendor-neutral interface for search-engine index management operations.
 *
 * <p>This interface provides a unified contract for managing search indices,
 * including creation, deletion, optimization, and monitoring operations.
 * All public method signatures use domain DTOs from
 * {@code com.dotcms.content.index.domain} instead of vendor-specific types,
 * allowing transparent swapping between Elasticsearch and OpenSearch.</p>
 *
 * <p><strong>Key Operations:</strong></p>
 * <ul>
 *   <li><strong>Index Management:</strong> Create, delete, clear, open, close indices</li>
 *   <li><strong>Performance:</strong> Optimize indices, flush caches, update replicas</li>
 *   <li><strong>Monitoring:</strong> Get index statistics, cluster health, status</li>
 *   <li><strong>Alias Management:</strong> Create and manage index aliases</li>
 * </ul>
 *
 * @author Fabrizio Araya
 * @see ESIndexAPI
 * @see com.dotcms.content.index.domain.ClusterStats
 * @see com.dotcms.content.index.domain.IndexStats
 */
public interface IndexAPI {

    /**
     * Status enumeration for index states.
     */
    enum Status {

        ACTIVE("active"),
        INACTIVE("inactive"),
        PROCESSING("processing");

        private final String status;

        Status(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * Gets statistics for all indices managed by this cluster.
     *
     * @return a map of index names to their statistics
     */
    Map<String, IndexStats> getIndicesStats();

    /**
     * Flushes field and filter caches for the specified indices.
     * This operation can take up to a minute to complete.
     *
     * @param indexNames list of index names to flush caches for
     * @return map containing failed and successful shard counts
     */
    Map<String, Integer> flushCaches(List<String> indexNames);

    /**
     * Optimizes the specified indices by force merging segments.
     *
     * @param indexNames list of index names to optimize
     * @return true if optimization was successful
     */
    boolean optimize(List<String> indexNames);

    /**
     * Deletes the index with the specified name.
     *
     * @param indexName the name of the index to delete
     * @return true if the deletion was acknowledged
     */
    boolean delete(String indexName);

    /**
     * Deletes multiple indices with the specified names.
     *
     * @param indexNames vararg with the names of the indices to delete
     * @return true if the deletion was acknowledged
     */
    boolean deleteMultiple(String... indexNames);

    /**
     * Deletes inactive live/working indices older than the specified number of sets to keep.
     *
     * @param inactiveLiveWorkingSetsToKeep number of live/working sets to keep
     */
    void deleteInactiveLiveWorkingIndices(int inactiveLiveWorkingSetsToKeep);

    /**
     * Returns a set of all managed indices.
     *
     * @return set containing all index names
     */
    Set<String> listIndices();

    /**
     * Checks if the specified index is closed.
     *
     * @param index the index name to check
     * @return true if the index is closed
     */
    boolean isIndexClosed(String index);

    /**
     * Checks if the specified index exists.
     *
     * @param indexName the index name to check
     * @return true if the index exists
     */
    boolean indexExists(String indexName);

    /**
     * Creates an index with default settings.
     *
     * @param indexName the name of the index to create
     * @throws DotStateException if index creation fails
     * @throws IOException if I/O operations fail
     */
    void createIndex(String indexName) throws DotStateException, IOException;

    /**
     * Creates an index with default settings and specified number of shards.
     *
     * @param indexName the name of the index to create
     * @param shards number of shards (if less than 1, default will be used)
     * @return the create index status
     * @throws DotStateException if index creation fails
     * @throws IOException if I/O operations fail
     */
    CreateIndexStatus createIndex(String indexName, int shards) throws DotStateException, IOException;

    /**
     * Deletes and recreates an index (clears all data).
     *
     * @param indexName the name of the index to clear
     * @throws DotStateException if the index doesn't exist or clearing fails
     * @throws IOException if I/O operations fail
     * @throws DotDataException if data operations fail
     */
    void clearIndex(String indexName) throws DotStateException, IOException, DotDataException;

    /**
     * Creates an index with custom settings and specified number of shards.
     *
     * @param indexName the name of the index to create
     * @param settings JSON string with custom settings (null for defaults)
     * @param shards number of shards (if less than 1, default will be used)
     * @return the create index status
     * @throws IOException if I/O operations fail
     */
    CreateIndexStatus createIndex(String indexName, String settings, int shards)
            throws IOException;

    /**
     * Gets the default index settings as JSON string.
     *
     * @return JSON string containing default index settings
     */
    String getDefaultIndexSettings();

    /**
     * Returns cluster health information for all indices.
     *
     * @return map of index names to their cluster health information
     */
    Map<String, ClusterIndexHealth> getClusterHealth();

    /**
     * Updates the number of replicas for the specified index.
     *
     * @param indexName the name of the index
     * @param replicas the number of replicas to set
     * @throws DotDataException if replica update fails or enterprise license is required
     */
    void updateReplicas(String indexName, int replicas) throws DotDataException;

    /**
     * Creates an alias for the specified index.
     *
     * @param indexName the name of the index
     * @param alias the alias name to create
     */
    void createAlias(String indexName, String alias);

    /**
     * Gets aliases for the specified list of index names.
     *
     * @param indexNames list of index names
     * @return map of index names to their aliases
     */
    Map<String, String> getIndexAlias(List<String> indexNames);

    /**
     * Gets aliases for the specified array of index names.
     *
     * @param indexNames array of index names
     * @return map of index names to their aliases
     */
    Map<String, String> getIndexAlias(String[] indexNames);

    /**
     * Gets the alias for a single index.
     *
     * @param indexName the index name
     * @return the alias name, or null if no alias exists
     */
    String getIndexAlias(String indexName);

    /**
     * Gets a reverse mapping of aliases to index names.
     *
     * @param indices list of indices to check
     * @return map of alias names to index names
     */
    Map<String, String> getAliasToIndexMap(List<String> indices);

    /**
     * Closes the specified index.
     *
     * @param indexName the name of the index to close
     */
    void closeIndex(String indexName);

    /**
     * Opens the specified index.
     *
     * @param indexName the name of the index to open
     */
    void openIndex(String indexName);

    /**
     * Gets a list of indices with specified expansion options.
     *
     * @param expandToOpenIndices whether to include open indices
     * @param expandToClosedIndices whether to include closed indices
     * @return list of index names sorted by creation date
     */
    List<String> getIndices(boolean expandToOpenIndices, boolean expandToClosedIndices);

    /**
     * Gets a list of live/working indices sorted by creation date in descending order.
     *
     * @return list of index names sorted by creation date descending
     */
    List<String> getLiveWorkingIndicesSortedByCreationDateDesc();

    /**
     * Removes the cluster ID prefix from an index or alias name.
     *
     * @param name index name or alias with potential cluster prefix
     * @return name without cluster prefix
     */
    String removeClusterIdFromName(String name);

    /**
     * Gets a list of closed indices.
     *
     * @return list of closed index names
     */
    List<String> getClosedIndexes();

    /**
     * Gets the status of the specified index.
     *
     * @param indexName the name of the index
     * @return the index status (ACTIVE, INACTIVE, or PROCESSING)
     * @throws DotDataException if status cannot be determined
     */
    Status getIndexStatus(String indexName) throws DotDataException;

    /**
     * Adds cluster ID prefix to an index or alias name.
     *
     * @param name index name or alias
     * @return name with cluster ID prefix
     */
    String getNameWithClusterIDPrefix(String name);

    /**
     * Waits until the index/cluster is ready for operations.
     * This method will wait for a configured number of attempts before shutting down dotCMS.
     *
     * @return true when the index is ready
     */
    boolean waitUtilIndexReady();

    /**
     * Gets cluster statistics including node information and document counts.
     *
     * @return cluster statistics object
     */
    ClusterStats getClusterStats();
}
