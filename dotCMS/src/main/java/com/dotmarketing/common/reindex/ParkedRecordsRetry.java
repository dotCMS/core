package com.dotmarketing.common.reindex;

/**
 * What a retry of the parked reindex records did (#37270).
 *
 * @param requeued         how many parked rows were sent back to the queue at their original
 *                         priority with a fresh retry budget
 * @param requeuedAsDelete how many of those had no content left, and so were requeued as a
 *                         removal of their documents from the index instead of a reindex
 */
public record ParkedRecordsRetry(int requeued, int requeuedAsDelete) {
}
