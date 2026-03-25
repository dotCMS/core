package com.dotcms.content.index.domain;

/**
 * Vendor-neutral handle for a batch of document index and delete operations.
 *
 * <p>Callers receive an {@code IndexBulkRequest} from
 * {@link com.dotcms.content.elasticsearch.business.ContentletIndexAPI#createBulkRequest()}
 * and pass it back to
 * {@link com.dotcms.content.elasticsearch.business.ContentletIndexAPI#appendBulkRequest} /
 * {@link com.dotcms.content.elasticsearch.business.ContentletIndexAPI#putToIndex(IndexBulkRequest)}.
 * No Elasticsearch or OpenSearch types leak through this interface.</p>
 *
 * @author Fabrizio Araya
 */
public interface IndexBulkRequest {

    /**
     * Vendor-neutral refresh policy applied to a bulk batch at submit time.
     *
     * <p>Maps to {@code WriteRequest.RefreshPolicy} on Elasticsearch and
     * {@code org.opensearch.client.opensearch._types.Refresh} on OpenSearch.</p>
     */
    enum RefreshPolicy {
        /** Do not refresh (server default). ES: {@code NONE}, OS: {@code false}. */
        NONE,
        /** Refresh affected shards immediately. ES: {@code IMMEDIATE}, OS: {@code true}. */
        IMMEDIATE,
        /** Wait for the next scheduled refresh. ES: {@code WAIT_UNTIL}, OS: {@code wait_for}. */
        WAIT_FOR
    }

    /** Returns the number of pending operations accumulated in this batch. */
    int size();

    /** Returns {@code true} when no operations have been added yet. */
    default boolean isEmpty() {
        return size() == 0;
    }
}