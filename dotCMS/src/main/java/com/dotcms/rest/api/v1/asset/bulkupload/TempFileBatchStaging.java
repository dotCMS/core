package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 * The production {@link BatchStaging} — a thin adapter over the product's temp file API.
 * <p>
 * <b>This is the whole of the staging implementation.</b> Every byte is written by
 * {@code TempFileAPI.createTempFile}, exactly as content import does it; nothing here reimplements
 * or wraps that behaviour. The interface exists so the bounding and reclaim logic in
 * {@link BoundedMultipartReader} — the code that can leak bytes permanently or lose an author's
 * files — can be covered by unit tests instead of requiring a running server.
 * <p>
 * <b>Why a run on another node can still read what this staged (FR-034).</b> The guarantee is that
 * the node accepting the submission and the node executing the run need not be the same one — the
 * job queue is shared, and nothing pins a run to its submitter. That holds because
 * {@code TempFileAPI} writes through {@code ConfigUtils.getAssetTempPath()}, which is
 * {@code <assets root>/tmp_upload} — <b>the same volume that holds every file asset's binary</b>,
 * resolved from {@code ASSET_REAL_PATH} / {@code ASSET_PATH}. So this rests on no assumption of its
 * own: a cluster whose assets volume is not shared has already lost ordinary file assets across
 * nodes, long before a bulk upload is attempted. Recorded here because it was previously left
 * unstated, and an unstated assumption is one a later change can break without noticing.
 * <p>
 * The corollary is worth keeping in view: staged content is <b>not</b> node-local scratch. It costs
 * shared storage until {@link #reclaim(String)} takes it back, which is why the run reclaims on
 * every terminal state rather than leaving it to expire — see that method.
 *
 * @author dotCMS
 */
public class TempFileBatchStaging implements BatchStaging {

    private final HttpServletRequest request;

    public TempFileBatchStaging(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public StagedPart stage(final String fileName, final InputStream content) throws IOException {
        try {
            final DotTempFile tempFile = APILocator.getTempFileAPI()
                    .createTempFile(fileName, request, content);

            return new StagedPart(tempFile.id, tempFile.fileName, tempFile.length(),
                    tempFile.mimeType);
        } catch (final DotSecurityException e) {
            throw new IOException("Not allowed to stage '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Deletes the staged file outright rather than leaving it to age out, because the two clocks
     * that would otherwise collect it are both slower than they look:
     * <ul>
     *   <li>{@code TEMP_RESOURCE_MAX_AGE_SECONDS} (30 min) removes <b>nothing</b> — it is a TTL
     *       enforced on retrieval, so past it {@code getTempFile} answers empty while the bytes
     *       stay on disk;</li>
     *   <li>{@code BinaryCleanupJob} does delete from {@code tmp_upload}, but only files older
     *       than {@code CLEANUP_TMP_FILES_OLDER_THAN_HOURS} (3h), and only while its cron is
     *       firing — which by default is every three minutes of the <b>midnight hour alone</b>
     *       ({@code BINARY_CLEANUP_JOB_CRON_EXPRESSION}), not around the clock.</li>
     * </ul>
     * So content left here is collected, eventually — after up to about a day on the shared assets
     * volume, in bytes the author cannot see and no run will ever use. Reclaiming now is what keeps
     * a batch's debris from outliving the batch by a day.
     */
    @Override
    public void reclaim(final String tempFileId) {
        try {
            final Optional<DotTempFile> tempFile =
                    APILocator.getTempFileAPI().getTempFile(request, tempFileId);
            // delete() first and exists() only if it failed: delete() already answers false for a
            // file that is not there, so the old order paid a stat call on the shared assets volume
            // for every staged file to learn what the delete reports anyway. The exists() survives
            // on the failure side so the warning still means "it is still there", not "it was
            // already gone" — a warning nobody can act on is one nobody reads.
            if (tempFile.isPresent() && !tempFile.get().file.delete()
                    && tempFile.get().file.exists()) {
                Logger.warn(this, "Could not delete staged content: " + tempFileId);
            }
        } catch (final Exception e) {
            Logger.warn(this, String.format("Could not reclaim staged content '%s': %s",
                    tempFileId, e.getMessage()), e);
        }
    }
}
