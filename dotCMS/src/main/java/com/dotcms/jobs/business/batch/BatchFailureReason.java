package com.dotcms.jobs.business.batch;

/**
 * Machine-readable cause of a {@link BatchItemStatus#FAILED} item.
 * <p>
 * <b>This is what the client presents</b>, mapped to resolved product copy; the accompanying
 * message is diagnostic and is never displayed. Adding a value here is a change to
 * both halves of the feature, because every value needs client copy.
 * <p>
 * Reasons are derived from facts the staging layer reported — the measured size and the resolved
 * media type — never from the text of a validation exception, whose wording differs only by a
 * translated string between the size and type cases.
 *
 * @author dotCMS
 */
public enum BatchFailureReason {

    /** Larger than the ceiling that applies: the content type's own, or the configured fallback. */
    OVER_SIZE_LIMIT,

    /**
     * The resolved media type is not in the content type's allow list. A <b>media-type</b> rule,
     * not a file-extension one.
     */
    DISALLOWED_FILE_TYPE,

    /** An item of that name already exists in the target. Case-insensitive. */
    NAME_COLLISION,

    /**
     * The target folder's own filename filter ({@code filesMasks}, e.g. {@code *.jpg}) does not
     * admit this name.
     * <p>
     * <b>Distinct from {@link #DISALLOWED_FILE_TYPE}</b>, which is the content type's media-type
     * allow list and is decided by sniffing content. This one is a glob on the <i>file name</i>,
     * configured per folder, so the same file is accepted in one folder and refused in the next —
     * which is exactly why the author has to be told which of the two stopped them. Telling them
     * "type not allowed" when the folder is the constraint sends them to change the wrong thing.
     */
    FOLDER_FILTER_MISMATCH,

    /** A per-item permission check narrower than the submission-time one that already passed. */
    PERMISSION_DENIED,

    /**
     * The staged content could not be retrieved when the run reached it. Not the
     * author's fault — copy must not suggest they supplied a bad file.
     */
    STAGED_CONTENT_UNAVAILABLE,

    /**
     * The path no longer resolves to a folder — it is gone, it is a file, or it is malformed.
     * Added by bulk folder delete (#37063, spec FR-010, FR-019). Named to match the frontend
     * half's independently-fixed vocabulary
     * ({@code DOT_FOLDER_DELETE_FAILURE_REASONS}, PR dotCMS/core#37612) rather than the name
     * originally drafted here ({@code NOT_FOUND}) — adopted 2026-09-19 so the client copy already
     * written for this reason is not silently lost to its {@code UNCLASSIFIED} fallback.
     */
    PATH_NOT_FOUND,

    /**
     * The path names a folder the system protects — the system folder, or a site root — and never
     * deletes. Added by bulk folder delete (#37063, spec FR-011, FR-019).
     */
    PROTECTED_FOLDER,

    /**
     * Content in the subtree was locked by another author and blocked the operation. Added by bulk
     * folder delete (#37063, spec D-010, FR-019). Named {@code IN_USE} rather than {@code LOCKED}
     * to match the frontend half's fixed vocabulary — same reconciliation as
     * {@link #PATH_NOT_FOUND}.
     */
    IN_USE,

    /**
     * An ancestor in the same submission removed this path first, so it was never attempted as its
     * own unit — pairs with {@link BatchItemStatus#SKIPPED}, never {@link BatchItemStatus#FAILED}.
     * Added by bulk folder delete (#37063, spec FR-013, FR-019). Named {@code COVERED_BY_PARENT}
     * rather than {@code ANCESTOR_REMOVED} to match the frontend half's fixed vocabulary — same
     * reconciliation as {@link #PATH_NOT_FOUND}.
     */
    COVERED_BY_PARENT,

    /** Anything else. The message carries the detail, for logs only. */
    UNCLASSIFIED
}
