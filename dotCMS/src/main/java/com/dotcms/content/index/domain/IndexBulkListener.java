package com.dotcms.content.index.domain;

import java.util.List;

/**
 * Vendor-neutral listener for async bulk-processor callbacks.
 *
 * <p>Replaces direct extension of
 * {@code org.elasticsearch.action.bulk.BulkProcessor.Listener} so that the
 * business logic in {@code BulkProcessorListener} is decoupled from any
 * vendor library.  Each {@link com.dotcms.content.index.ContentletIndexOperations}
 * implementation adapts its vendor-specific callback types to this interface
 * before forwarding to the application listener.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@link #beforeBulk} — called once per flush, before the batch is sent.</li>
 *   <li>{@link #afterBulk(long, List)} — called on success with per-item results.</li>
 *   <li>{@link #afterBulk(long, Throwable)} — called when the entire batch fails.</li>
 * </ol>
 *
 * @author Fabrizzio Araya
 */
public interface IndexBulkListener {

    /**
     * Called before a batch is submitted to the index.
     *
     * @param executionId unique ID assigned by the bulk processor
     * @param actionCount number of operations in the batch
     */
    void beforeBulk(long executionId, int actionCount);

    /**
     * Called after a batch completes successfully (even if some individual items failed).
     *
     * @param executionId unique ID assigned by the bulk processor
     * @param results     one {@link IndexBulkItemResult} per item in the batch
     */
    void afterBulk(long executionId, List<IndexBulkItemResult> results);

    /**
     * Called when the entire batch fails with an unrecoverable exception.
     *
     * @param executionId unique ID assigned by the bulk processor
     * @param failure     the exception that caused the batch to fail
     */
    void afterBulk(long executionId, Throwable failure);

    /**
     * Tells the listener that one document of this identifier was withheld from the batch and its
     * reindex entry already recorded the failure (#37269).
     *
     * <p>From this call on, the listener must not delete the entry when a sibling document of the
     * same identifier (another language, or the other of working/live) is indexed successfully,
     * and must not mark it failed again if the whole request fails, so the recorded reason is
     * kept. Callers invoke it before queueing any sibling document, because the processor may
     * flush while documents are still being added.</p>
     *
     * <p>The default does nothing, which suits listeners that never touch the reindex journal.</p>
     *
     * @param identifier the contentlet identifier whose entry already holds a failure
     */
    default void withhold(final String identifier) {
        // nothing to track by default
    }
}
