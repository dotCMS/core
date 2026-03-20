package com.dotcms.content.index;

import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotmarketing.exception.DotDataException;
import java.io.IOException;

/**
 * Vendor-neutral contract for the <em>write side</em> of the contentlet index pipeline.
 *
 * <p>This interface separates the two vendor-specific write implementations
 * ({@code ContentletIndexOperationsES} and {@code ContentletIndexOperationsOS}) from
 * the business logic that lives in the router ({@code ContentletIndexAPIImpl}).
 * Vendor types ({@code BulkRequest}, {@code BulkProcessor}, OpenSearch equivalents)
 * must not appear in any public method signature of either implementation.</p>
 *
 * <p>The design mirrors {@link ContentFactoryIndexOperations} for the read path:
 * the router holds two typed fields, selects the active implementation via
 * {@code getProvider()}, and delegates every call.</p>
 *
 * @author Fabrizio Araya
 * @see com.dotcms.content.elasticsearch.business.ContentletIndexOperationsES
 * @see com.dotcms.content.index.opensearch.ContentletIndexOperationsOS
 */
public interface ContentletIndexOperations {

    // =========================================================================
    // Batch (synchronous) write path
    // =========================================================================

    /**
     * Creates a new empty batch ready to accumulate index and delete operations.
     *
     * @return a fresh, empty {@link IndexBulkRequest}
     */
    IndexBulkRequest createBulkRequest();

    /**
     * Appends an <em>index (upsert)</em> operation to {@code req}.
     *
     * @param req         the batch to append to
     * @param indexName   the target index name
     * @param docId       document id — {@code identifier_languageId_variantId}
     * @param jsonMapping the JSON string produced by the content mapping API
     */
    void addIndexOp(IndexBulkRequest req, String indexName, String docId, String jsonMapping);

    /**
     * Appends a <em>delete</em> operation to {@code req}.
     *
     * @param req       the batch to append to
     * @param indexName the target index name
     * @param docId     document id to delete
     */
    void addDeleteOp(IndexBulkRequest req, String indexName, String docId);

    /**
     * Sets the refresh policy on the batch before submission.
     *
     * @param req    the batch to configure
     * @param policy one of {@code "NONE"}, {@code "WAIT_FOR"}, or {@code "IMMEDIATE"}
     */
    void setRefreshPolicy(IndexBulkRequest req, IndexBulkRequest.RefreshPolicy policy);

    /**
     * Submits the accumulated batch synchronously and blocks until the cluster
     * acknowledges all operations.
     *
     * @param req the batch to submit — no-op if the batch is empty
     */
    void putToIndex(IndexBulkRequest req);

    // =========================================================================
    // Async bulk-processor write path (used by ReindexThread)
    // =========================================================================

    /**
     * Creates a self-flushing asynchronous bulk processor backed by the
     * vendor-specific client.
     *
     * @param listener receives completion and failure callbacks for each flush
     * @return a new {@link IndexBulkProcessor}
     */
    IndexBulkProcessor createBulkProcessor(IndexBulkListener listener);

    /**
     * Adds an <em>index (upsert)</em> operation directly to the async processor,
     * which will flush it according to its internal thresholds.
     *
     * @param proc        the target processor
     * @param indexName   the target index name
     * @param docId       document id
     * @param jsonMapping the JSON string produced by the content mapping API
     */
    void addIndexOpToProcessor(IndexBulkProcessor proc, String indexName, String docId,
            String jsonMapping);

    /**
     * Adds a <em>delete</em> operation directly to the async processor.
     *
     * @param proc      the target processor
     * @param indexName the target index name
     * @param docId     document id to delete
     */
    void addDeleteOpToProcessor(IndexBulkProcessor proc, String indexName, String docId);

    // =========================================================================
    // Index lifecycle
    // =========================================================================

    /**
     * Creates a search index with the provider-specific default settings and content mapping.
     *
     * <p>Each implementation loads its own settings file ({@code es-content-settings.json} or
     * {@code os-content-settings.json}) and applies the corresponding content mapping, so the
     * router does not need to know which backend is being targeted.</p>
     *
     * @param indexName the fully-qualified index name (with cluster prefix)
     * @param shards    number of primary shards; {@code 0} means "use provider default"
     * @return {@code true} if the index was created and the mapping applied successfully
     * @throws IOException if index creation or mapping application fails
     */
    boolean createContentIndex(String indexName, int shards) throws IOException;

    // =========================================================================
    // Other write operations
    // =========================================================================

    /**
     * Removes all documents belonging to the given content type from every
     * active index in the cluster.
     *
     * @param contentType the content type whose documents should be removed
     * @throws DotDataException if the operation fails
     */
    void removeContentFromIndexByContentType(ContentType contentType) throws DotDataException;

    /**
     * Returns the number of documents currently stored in the given index.
     *
     * @param indexName the fully-qualified index name (with cluster prefix)
     * @return document count
     */
    long getIndexDocumentCount(String indexName);
}