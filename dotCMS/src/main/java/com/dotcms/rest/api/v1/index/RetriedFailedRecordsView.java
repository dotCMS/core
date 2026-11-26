package com.dotcms.rest.api.v1.index;

import com.dotmarketing.common.reindex.ParkedRecordsRetry;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What {@code POST /api/v1/index/failed/retry} did (#37270).
 *
 * @param requeued         how many parked records went back to the queue at their original
 *                         priority with a fresh retry budget
 * @param requeuedAsDelete how many of those had no content left and will remove their documents
 *                         from the index instead of reindexing them
 */
@Schema(description = "Result of retrying the parked reindex records")
public record RetriedFailedRecordsView(
        @Schema(description = "Parked records sent back to the queue at their original priority"
                + " with a fresh retry budget", example = "380")
        int requeued,
        @Schema(description = "Of those, records whose content no longer exists; they remove the"
                + " stale documents from the index instead of reindexing", example = "12")
        int requeuedAsDelete) {

    /**
     * Builds the view from the journal's result.
     *
     * @param retried what the retry did
     * @return the view
     */
    public static RetriedFailedRecordsView of(final ParkedRecordsRetry retried) {
        return new RetriedFailedRecordsView(retried.requeued(), retried.requeuedAsDelete());
    }
}
