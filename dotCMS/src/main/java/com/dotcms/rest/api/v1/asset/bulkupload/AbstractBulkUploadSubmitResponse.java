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
 * {@code submitted} is the count the server accepted, which is not necessarily what the client
 * believes it sent — a client rendering its own file count instead of this one would misreport a
 * batch whose parts were partly rejected by the reader.
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
