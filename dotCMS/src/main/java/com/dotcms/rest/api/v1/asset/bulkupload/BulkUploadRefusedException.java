package com.dotcms.rest.api.v1.asset.bulkupload;

/**
 * A submission refused while its body was being read, because a ceiling was crossed
 * (spec FR-010a, FR-013c.2).
 * <p>
 * Carries <b>which</b> ceiling, because "too many files" and "too much data" are different problems
 * with different fixes and the author has to be told which one they hit.
 *
 * @author dotCMS
 */
public class BulkUploadRefusedException extends RuntimeException {

    /** Which ceiling the submission crossed. */
    public enum Ceiling {
        /** More parts than {@code CONTENT_BULK_UPLOAD_MAX_FILES} — answered {@code 400}. */
        FILE_COUNT,
        /** Accumulated size over {@code CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES} — answered {@code 413}. */
        TOTAL_SIZE
    }

    private final Ceiling ceiling;

    public BulkUploadRefusedException(final Ceiling ceiling, final String message) {
        super(message);
        this.ceiling = ceiling;
    }

    public Ceiling ceiling() { return ceiling; }
}
