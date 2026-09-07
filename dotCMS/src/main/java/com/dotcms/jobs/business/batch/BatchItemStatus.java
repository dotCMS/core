package com.dotcms.jobs.business.batch;

/**
 * Outcome of a single item in any batch job run — upload, folder copy, bulk delete, reindex.
 * <p>
 * Generalized from {@code BulkRefreshItemStatus}, whose three values this reproduces exactly so
 * the shipped consumer can be expressed in terms of this type without changing behaviour
 * (spec FR-018, SC-006).
 *
 * @author dotCMS
 */
public enum BatchItemStatus {

    /** The item was processed successfully. */
    SUCCESS,

    /** The item could not be processed. The result's reason and message carry why. */
    FAILED,

    /** The item was never attempted — the run was cancelled before reaching it (FR-028). */
    SKIPPED
}
