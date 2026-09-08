package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a bulk-upload submission part by part, staging as it goes and stopping the moment a
 * ceiling is crossed (spec FR-010a, FR-013c.2, FR-013d).
 * <p>
 * <b>This is the only bound in the path.</b> Research R10 established that nothing below it caps a
 * request: {@code TEMP_RESOURCE_MAX_FILE_SIZE} ships as {@code -1} — the config comment says
 * "authenticated users are unlimited" — the staging layer counts neither files nor bytes, and the
 * servlet container has no multipart limit configured.
 * <p>
 * Two behaviours are load-bearing and are why this is a class rather than a loop inside the
 * resource:
 * <ul>
 *   <li>It aborts <b>mid-body</b>, so a submission over a ceiling cannot commit more than the
 *       ceiling to disk. Checking after the body has arrived would let an author write an unbounded
 *       amount and only then be told no.</li>
 *   <li>It reclaims over the <b>whole read</b>, not just its own refusal path. A read that dies
 *       underneath it — the author navigated away, the connection dropped — raises nothing from
 *       this side, and nothing purges staged content on a schedule, so a reclaim scoped to the
 *       refusal would leak permanently (spec FR-013d.2, C-001a1).</li>
 * </ul>
 *
 * @author dotCMS
 */
public class BoundedMultipartReader {

    private final BatchStaging staging;
    private final int maxFiles;
    private final long maxTotalBytes;

    public BoundedMultipartReader(final BatchStaging staging,
                                  final int maxFiles,
                                  final long maxTotalBytes) {
        this.staging = staging;
        this.maxFiles = maxFiles;
        this.maxTotalBytes = maxTotalBytes;
    }

    /**
     * Stages every part in order, refusing as soon as either ceiling is crossed and reclaiming
     * whatever was staged before that point.
     *
     * @return the staged parts, in submission order, when the whole body was read within both
     *         ceilings
     * @throws BulkUploadRefusedException when a ceiling is crossed; everything staged so far has
     *                                    been reclaimed before it is thrown
     */
    public List<StagedPart> read(final Iterable<UploadPart> parts) {

        final List<StagedPart> staged = new ArrayList<>();
        long totalBytes = 0L;
        boolean completed = false;

        // The reclaim is scoped to the whole read, not to the refusal branch. A refusal is raised
        // here, so a narrower scope would still catch it — but a read that dies underneath (the
        // author navigated away, the connection dropped) raises nothing of ours, and that is the
        // likelier of the two because it is the author's own action rather than a limit being hit.
        // Nothing purges staged content on a schedule, so anything missed here is missed for good.
        try {
            for (final UploadPart part : parts) {

                if (staged.size() + 1 > maxFiles) {
                    throw new BulkUploadRefusedException(
                            BulkUploadRefusedException.Ceiling.FILE_COUNT,
                            String.format("Batch exceeds the maximum of %d files", maxFiles));
                }

                // Staged before it is counted, because the size is only a fact once staging has
                // measured it — a declared figure can be under-stated or absent (spec FR-013).
                final StagedPart stagedPart = staging.stage(part.fileName(), part.content());
                staged.add(stagedPart);
                totalBytes += stagedPart.sizeBytes();

                if (totalBytes > maxTotalBytes) {
                    throw new BulkUploadRefusedException(
                            BulkUploadRefusedException.Ceiling.TOTAL_SIZE,
                            String.format("Batch exceeds the maximum total size of %d bytes",
                                    maxTotalBytes));
                }
            }
            completed = true;
            return staged;

        } catch (final IOException e) {
            // The read died underneath us. Wrapped rather than swallowed: the caller has to know
            // the submission never became a run, and the finally below has already cleaned up.
            throw new DotRuntimeException("Bulk upload submission failed while reading: "
                    + e.getMessage(), e);
        } finally {
            if (!completed) {
                reclaimAll(staged);
            }
        }
    }

    /**
     * Hands every part staged so far back for cleanup. Best-effort per part: one failure must not
     * stop the rest being reclaimed, and none of it may mask the exception already unwinding.
     */
    private void reclaimAll(final List<StagedPart> staged) {
        for (final StagedPart part : staged) {
            try {
                staging.reclaim(part.tempFileId());
            } catch (final Exception e) {
                Logger.warn(this, String.format(
                        "Could not reclaim staged content '%s' for an abandoned or refused bulk "
                                + "upload; nothing purges staged content on a schedule, so this "
                                + "will not be cleaned up later: %s",
                        part.tempFileId(), e.getMessage()), e);
            }
        }
    }
}
