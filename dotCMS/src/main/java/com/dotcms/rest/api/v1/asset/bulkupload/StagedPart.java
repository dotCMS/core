package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.jobs.business.batch.BatchFailureReason;

/**
 * One file the submission placed on the staging layer, and what staging reported about it.
 * <p>
 * {@code sizeBytes} and {@code mimeType} are <b>measured and resolved facts</b>, not values a
 * caller declared (spec FR-013). That is what lets the batch total be accumulated during the read
 * and lets a type rejection later name a media type rather than infer one from a validation
 * exception whose wording differs only by a translated string (research R4).
 *
 * @author dotCMS
 */
public class StagedPart {

    private final String tempFileId;
    private final String fileName;
    private final long sizeBytes;
    private final String mimeType;
    private final BatchFailureReason refusedReason;

    public StagedPart(final String tempFileId, final String fileName,
                      final long sizeBytes, final String mimeType) {
        this(tempFileId, fileName, sizeBytes, mimeType, null);
    }

    private StagedPart(final String tempFileId, final String fileName, final long sizeBytes,
                       final String mimeType, final BatchFailureReason refusedReason) {
        this.tempFileId = tempFileId;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.refusedReason = refusedReason;
    }

    /**
     * A part the submission decided against before it could be staged.
     * <p>
     * <b>It stays in the batch rather than taking the batch down.</b> FR-011 is explicit that a
     * size rejection is that file's own failure and must not fail the run, and that has to hold
     * whether the ceiling was crossed at creation time or while the body was still being read.
     * Carrying the refusal forward is what lets the run report it by name alongside the files that
     * did land, instead of the author getting one opaque refusal for the whole submission.
     * <p>
     * Has no {@code tempFileId}: nothing of it is on the staging layer for the run to fetch or for
     * the reclaim to release.
     */
    static StagedPart refused(final String fileName, final BatchFailureReason reason) {
        // -1 rather than 0 for the size: the part was cut off mid-read, so no measurement was
        // completed, and a 0 here would travel into the outcome as though it had been measured.
        return new StagedPart(null, fileName, -1L, null, reason);
    }

    /** Whether this part was refused before staging, and why. Null when it staged normally. */
    public BatchFailureReason refusedReason() {
        return refusedReason;
    }

    /** Whether anything of this part actually reached the staging layer. */
    public boolean isStaged() {
        return refusedReason == null;
    }

    public String tempFileId() { return tempFileId; }

    public String fileName() { return fileName; }

    public long sizeBytes() { return sizeBytes; }

    public String mimeType() { return mimeType; }
}
