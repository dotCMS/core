package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a bulk-upload submission part by part, staging as it goes and stopping the moment a
 * ceiling is crossed.
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
    private final long perFileCeiling;

    public BoundedMultipartReader(final BatchStaging staging,
                                  final int maxFiles,
                                  final long maxTotalBytes) {
        this(staging, maxFiles, maxTotalBytes, -1L);
    }

    /**
     * @param perFileCeiling the staging layer's own per-file ceiling, or {@code -1} for none. When
     *                       set, this reader enforces it itself so that crossing it is a fact
     *                       rather than a guess — see {@link PerFileCeilingExceededException}.
     */
    public BoundedMultipartReader(final BatchStaging staging,
                                  final int maxFiles,
                                  final long maxTotalBytes,
                                  final long perFileCeiling) {
        this.staging = staging;
        this.maxFiles = maxFiles;
        this.maxTotalBytes = maxTotalBytes;
        this.perFileCeiling = perFileCeiling;
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
                final StagedPart stagedPart = stageWithinCeiling(part);
                staged.add(stagedPart);

                // A refused part contributes nothing to the batch total, because nothing of it was
                // measured. That under-counts the batch by whatever the author actually sent for
                // it, which is the right way to be wrong here: the total exists to bound what
                // reaches DISK, and a refused part reaches none.
                if (stagedPart.isStaged()) {
                    totalBytes += stagedPart.sizeBytes();
                }

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
     * Stages one part, turning a crossing of the per-file ceiling into <b>that part's</b> refusal
     * rather than the whole submission's.
     * <p>
     * FR-011 requires a size rejection to be the file's own failure and to leave the batch running,
     * and that must hold whether the ceiling is the content type's (decided later, by the run, from
     * the measured size) or the staging layer's (decided here, because the file never finishes
     * being written). Before this, the second case took the entire submission down with it.
     */
    private StagedPart stageWithinCeiling(final UploadPart part) throws IOException {

        if (perFileCeiling <= 0) {
            // Unbounded, which is how the staging layer ships. Nothing is wrapped, so the ordinary
            // path is exactly what it was.
            return staging.stage(part.fileName(), part.content());
        }

        try {
            return staging.stage(part.fileName(),
                    new CeilingBoundedInputStream(part.content(), part.fileName(),
                            perFileCeiling));
        } catch (final PerFileCeilingExceededException refused) {
            Logger.info(this, refused.getMessage() + "; recorded as that file's own failure");
            return StagedPart.refused(part.fileName(), BatchFailureReason.OVER_SIZE_LIMIT);
        }
    }

    /**
     * Hands every part staged so far back for cleanup. Best-effort per part: one failure must not
     * stop the rest being reclaimed, and none of it may mask the exception already unwinding.
     * <p>
     * <b>Parts that were never staged are skipped, not reclaimed.</b> A part refused by the
     * per-file ceiling has no {@code tempFileId} by construction — nothing of it reached the
     * staging layer — so asking for it back would log the warning below about content that will
     * never be collected, for content that was never written. That warning has to stay credible:
     * it is the only notice anyone gets that a leak happened, and nothing purges staged content on
     * a schedule to correct a false one.
     */
    private void reclaimAll(final List<StagedPart> staged) {
        for (final StagedPart part : staged) {
            if (!part.isStaged()) {
                continue;
            }
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
