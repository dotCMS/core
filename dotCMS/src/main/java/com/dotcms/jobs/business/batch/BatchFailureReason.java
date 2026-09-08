package com.dotcms.jobs.business.batch;

/**
 * Machine-readable cause of a {@link BatchItemStatus#FAILED} item.
 * <p>
 * <b>This is what the client presents</b>, mapped to resolved product copy; the accompanying
 * message is diagnostic and is never displayed (spec FR-016a). Adding a value here is a change to
 * both halves of the feature, because every value needs client copy (spec C-002b).
 * <p>
 * Reasons are derived from facts the staging layer reported — the measured size and the resolved
 * media type — never from the text of a validation exception, whose wording differs only by a
 * translated string between the size and type cases (research R4).
 *
 * @author dotCMS
 */
public enum BatchFailureReason {

    /** Larger than the ceiling that applies: the content type's own, or the configured fallback. */
    OVER_SIZE_LIMIT,

    /**
     * The resolved media type is not in the content type's allow list. A <b>media-type</b> rule,
     * not a file-extension one (spec FR-012a).
     */
    DISALLOWED_FILE_TYPE,

    /** An item of that name already exists in the target. Case-insensitive (spec FR-042a). */
    NAME_COLLISION,

    /** A per-item permission check narrower than the submission-time one that already passed. */
    PERMISSION_DENIED,

    /**
     * The staged content could not be retrieved when the run reached it (spec FR-032). Not the
     * author's fault — copy must not suggest they supplied a bad file.
     */
    STAGED_CONTENT_UNAVAILABLE,

    /** Anything else. The message carries the detail, for logs only. */
    UNCLASSIFIED
}
