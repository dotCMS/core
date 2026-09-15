package com.dotcms.rest.api.v1.asset.bulkupload;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * What {@code POST /v1/assets/_bulkupload} answers: a handle, not a result (contracts §1).
 * <p>
 * <b>{@code statusUrl} as well as {@code jobId}</b> — a deliberate divergence from bulk refresh,
 * which returns the id alone and leaves every client to assemble the URL by hand. The job
 * framework's own status response and the content import endpoint both include it, and they are
 * the nearer precedents for an endpoint that answers before doing the work.
 * <p>
 * {@code submitted} is the count the <b>server</b> read, and it is the one a client should display:
 * it equals the {@code total} the outcome later reports, so the first screen and the last agree by
 * construction. A part the per-file ceiling refused still counts, because it is carried into the
 * batch as that file's own failure rather than dropped (FR-011) — so this normally matches what the
 * author selected, and where it does not, parts were lost in transit and the client's own count is
 * a number no later screen will confirm.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = BulkUploadSubmitResponse.class)
@JsonDeserialize(as = BulkUploadSubmitResponse.class)
public interface AbstractBulkUploadSubmitResponse {

    /** The run's handle. Everything the client can do afterwards is addressed by this. */
    String jobId();

    /** Where to follow, cancel and read the outcome, ready to use. */
    String statusUrl();

    /** How many files the server accepted into the batch. */
    int submitted();
}
