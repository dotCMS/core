package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * The {@code 202 Accepted} body returned when a bulk refresh is submitted.
 * <p>
 * Deliberately not {@link com.dotcms.rest.api.v1.job.JobStatusResponse}: this adds
 * {@link #submitted()} so a client can tell how many inodes it sent apart from the de-duplicated
 * {@code total} the result reports once the run is done. Kept as its own type so the shared job
 * response other endpoints already serialize stays untouched.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BulkRefreshSubmitResponse.class)
@JsonDeserialize(as = BulkRefreshSubmitResponse.class)
public interface AbstractBulkRefreshSubmitResponse {

    /** The job's id — the handle for the status and cancel calls. */
    String jobId();

    /** Absolute URL of the status snapshot, {@code GET /_bulkrefresh/{jobId}}. Poll it for progress. */
    String statusUrl();

    /**
     * The raw count of inodes accepted, before identifier de-duplication. The de-duplicated
     * {@code total} is reported by the job result, and is often smaller.
     */
    int submitted();
}
