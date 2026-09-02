package com.dotcms.rest.api.v1.content.bulkrefresh;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * The {@code 202 Accepted} body returned when a bulk refresh is submitted.
 * <p>
 * Deliberately not {@link com.dotcms.rest.api.v1.job.JobStatusResponse}: that carries a status URL to
 * poll, and this endpoint has none — completion is pushed over the websocket as a
 * {@code BULK_REFRESH_COMPLETED} system event. {@link #submitted()} lets a caller tell how many inodes
 * it sent apart from the de-duplicated {@code total} the completion event reports.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BulkRefreshSubmitResponse.class)
@JsonDeserialize(as = BulkRefreshSubmitResponse.class)
public interface AbstractBulkRefreshSubmitResponse {

    /** The job's id — the handle for the cancel call. */
    String jobId();

    /**
     * The raw count of inodes accepted, before identifier de-duplication. The de-duplicated
     * {@code total} is reported by the job result, and is often smaller.
     */
    int submitted();
}
