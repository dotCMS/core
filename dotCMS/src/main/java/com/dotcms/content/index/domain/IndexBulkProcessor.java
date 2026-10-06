package com.dotcms.content.index.domain;

/**
 * Vendor-neutral handle for an asynchronous, self-flushing bulk processor.
 *
 * <p>Used by high-throughput reindex pipelines (e.g. {@code ReindexThread}) that
 * add operations one by one and let the processor decide when to flush based on
 * configured thresholds (batch size, byte size, flush interval).</p>
 *
 * <p>Implementations hold the vendor-specific processor instance and expose it only
 * through this interface. Callers must always close the processor when done —
 * either explicitly or via try-with-resources.</p>
 *
 * @author Fabrizio Araya
 */
public interface IndexBulkProcessor extends AutoCloseable {

    /**
     * Flushes any remaining operations and releases underlying resources.
     * Blocks until all pending operations are acknowledged by the cluster.
     */
    @Override
    void close() throws Exception;
}