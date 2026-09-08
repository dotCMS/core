package com.dotcms.rest.api.v1.asset.bulkupload;

import java.io.IOException;
import java.io.InputStream;

/**
 * The staging layer, as this feature needs it — the seam between reading a request and writing
 * bytes.
 * <p>
 * <b>Why an interface rather than calling {@code TempFileAPI} directly.</b> The bounding and
 * reclaim logic of {@link BoundedMultipartReader} is where this feature can lose an author's files
 * or leak bytes permanently, and it has to be testable without a filesystem, an HTTP request, or a
 * running server. The production implementation is a thin adapter over
 * {@code TempFileAPI.createTempFile}; nothing else lives here.
 *
 * @author dotCMS
 */
public interface BatchStaging {

    /**
     * Writes one part to the staging layer and reports what it measured.
     *
     * @throws IOException if the content cannot be written — including a read that dies underneath
     *                     because the client went away
     */
    StagedPart stage(String fileName, InputStream content) throws IOException;

    /**
     * Removes previously staged content. Called for every part staged so far when a submission
     * ends without becoming a run (spec FR-013d) — a refusal, or a read that died. Best-effort:
     * a failure to reclaim is logged, never propagated, because the caller is already unwinding.
     */
    void reclaim(String tempFileId);
}
