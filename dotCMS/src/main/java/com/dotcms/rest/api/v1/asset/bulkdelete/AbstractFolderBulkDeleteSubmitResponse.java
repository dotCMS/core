package com.dotcms.rest.api.v1.asset.bulkdelete;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * What {@code POST /v1/assets/folders/_bulkdelete} answers: a handle, not a result (#37063,
 * contracts §1).
 * <p>
 * {@code statusUrl} alongside {@code jobId} — ready to use, so the caller never assembles it
 * itself (FR-002), matching bulk upload's own divergence from bulk refresh's bare id.
 * {@code submitted} is the count the <b>server</b> read, after deduplication — it equals the
 * {@code total} the final outcome reports, by construction (FR-003, C-003). Named {@code submitted}
 * rather than {@code acceptedCount}, matching the frontend half's own type
 * (2026-09-19 — PR dotCMS/core#37612's {@code DotFolderBulkDeleteSubmitResponse}), which itself
 * follows bulk upload's {@code AbstractBulkUploadSubmitResponse#submitted()}.
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderBulkDeleteSubmitResponse.class)
@JsonDeserialize(as = FolderBulkDeleteSubmitResponse.class)
public interface AbstractFolderBulkDeleteSubmitResponse {

    /** The run's handle. Everything the client can do afterwards is addressed by this. */
    String jobId();

    /** Where to follow, cancel and read the outcome, ready to use. */
    String statusUrl();

    /** How many distinct paths the server accepted into the run. */
    int submitted();
}
