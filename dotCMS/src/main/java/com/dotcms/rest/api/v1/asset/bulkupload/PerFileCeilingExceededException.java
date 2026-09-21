package com.dotcms.rest.api.v1.asset.bulkupload;

/**
 * One part crossed the staging layer's per-file ceiling while it was being read.
 * <p>
 * <b>Raised by this feature rather than recovered from the staging layer's own refusal</b>, and
 * that is the whole point of it existing. {@code TempFileAPI} enforces the same ceiling, but
 * collapses the result into a {@code DotStateException} carrying a size-flavoured translated
 * message — <b>and it raises the same class, with the same wording, when the read simply dies
 * underneath it</b> ({@code TempFileAPI:191-193}, wrapping a plain {@code IOException} from
 * {@code BoundedOutputStream}). So "this file is too big" and "the connection dropped" are
 * genuinely indistinguishable from the exception, and telling them apart by message text is what
 * research R4 rejected.
 * <p>
 * They must be told apart, because they deserve opposite handling: an over-size file is
 * <b>that file's own failure</b> and the batch continues (FR-011), while a dead read means no
 * batch at all. Counting the bytes here makes the difference a fact instead of a guess.
 *
 * @author dotCMS
 */
public class PerFileCeilingExceededException extends RuntimeException {

    private final String fileName;

    public PerFileCeilingExceededException(final String fileName, final long ceilingBytes) {
        super(String.format("'%s' exceeds the staging layer's per-file ceiling of %d bytes",
                fileName, ceilingBytes));
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
