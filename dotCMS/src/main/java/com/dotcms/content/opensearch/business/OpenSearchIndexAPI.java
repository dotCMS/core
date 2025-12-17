package com.dotcms.content.opensearch.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import java.io.IOException;
import java.util.Map;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;

/**
 * OpenSearch Index API interface for managing OpenSearch indices operations.
 * This is the OpenSearch equivalent of ESIndexAPI for OpenSearch 3.7.
 *
 * @author fabrizio
 */
public interface OpenSearchIndexAPI {

    /**
     * Checks if an index exists in OpenSearch
     *
     * @param indexName the name of the index to check
     * @return true if the index exists, false otherwise
     */
    boolean indexExists(String indexName);

    /**
     * Creates an index with default settings
     *
     * @param indexName the name of the index to create
     * @throws DotStateException if there's a state error
     * @throws IOException if there's an I/O error
     */
    void createIndex(String indexName) throws DotStateException, IOException;

    /**
     * Creates an index with default settings and specific number of shards
     * If shards < 1 then default shards will be used
     *
     * @param indexName the name of the index to create
     * @param shards number of shards for the index
     * @return CreateIndexResponse with the response details
     * @throws DotStateException if there's a state error
     * @throws IOException if there's an I/O error
     */
    CreateIndexResponse createIndex(String indexName, int shards) throws DotStateException, IOException;

    /**
     * Creates an index with custom settings and specific number of shards
     * If settings is null, default settings will be applied
     * If shards < 1, then default shards will be used
     *
     * @param indexName the name of the index to create
     * @param settings custom JSON settings for the index
     * @param shards number of shards for the index
     * @return CreateIndexResponse with the response details
     * @throws DotStateException if there's a state error
     * @throws IOException if there's an I/O error
     */
    CreateIndexResponse createIndex(String indexName, String settings, int shards) throws DotStateException, IOException;

    /**
     * Gets the default index settings as JSON string
     *
     * @return JSON string with default index settings
     */
    String getDefaultIndexSettings();

    /**
     * Returns indices statistics
     *
     * @return map of index names to their statistics
     */
    Map<String, IndexStats> getIndicesStats();

    /**
     * Deletes an index by name
     *
     * @param indexName the name of the index to delete
     * @return true if the deletion was acknowledged, false otherwise
     */
    boolean delete(String indexName);

    /**
     * Given an index name, returns the full name including the cluster id prefix
     *
     * @param name index name
     * @return index name with cluster id prefix
     */
    String getNameWithClusterIDPrefix(String name);

    /**
     * Given an index name with cluster prefix, returns the name without the prefix
     *
     * @param name index name with cluster prefix
     * @return index name without cluster prefix
     */
    String removeClusterIdFromName(String name);
}