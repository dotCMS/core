package com.dotcms.rest.api.v1.content.bulkrefresh;

/**
 * Outcome of a single item in a bulk refresh (reindex) run.
 * <p>
 * One status-discriminated enum rather than separate success / failure / skip collections, so every
 * outcome is described uniformly and a fourth outcome later needs no new field.
 *
 * @author dotCMS
 */
public enum BulkRefreshItemStatus {

    /** The identifier's versions were reindexed. */
    SUCCESS,

    /** The item could not be reindexed. {@code errorMessage} carries the reason. */
    FAILED,

    /**
     * The item was never attempted — the run was cancelled before reaching it. De-duplication of
     * several inodes onto one identifier is <b>not</b> a skip.
     */
    SKIPPED
}
