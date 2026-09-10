import { DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

/**
 * The product copy that explains why a single file in a batch did not make it.
 *
 * FR-036: every reason the server can return must have its own copy, because a reason with no copy
 * is a hole the author sees. The reason vocabulary itself is the wire contract's and lives with the
 * other bulk wire types in `@dotcms/dotcms-models`; resolving one to copy is this portlet's
 * business and lives here. A `results[].reason` off the wire therefore resolves directly, with no
 * translation step in between.
 */

/**
 * `Record` rather than a `switch`: the compiler then refuses a new member of the union that nobody
 * wrote copy for, which is the failure this whole mapping exists to prevent. That is what makes
 * adding a reason to the contract fail the build here until someone writes its copy.
 */
/**
 * The folder-filter copy, in its two forms.
 *
 * The generic one names no rule, because the rule is the *folder's* and is not on the wire: a
 * failure carries the file name and the reason, never the mask that refused it. So the better
 * sentence is available only to a caller that already knows the target folder's `filesMasks`, and
 * the generic one stays for every caller that does not — an outcome that arrives after a reload,
 * or for a folder the author has since navigated away from.
 */
export const FOLDER_FILTER_MISMATCH_KEY = 'content-drive.upload.failure.folder-filter-mismatch';

/** Same refusal, naming what the folder does accept. See {@link FOLDER_FILTER_MISMATCH_KEY}. */
export const FOLDER_FILTER_MISMATCH_NAMED_KEY = `${FOLDER_FILTER_MISMATCH_KEY}-named`;

const MESSAGE_KEY_BY_REASON: Record<DotBulkUploadFailureReason, string> = {
    OVER_SIZE_LIMIT: 'content-drive.upload.failure.over-size-limit',
    // Named for the *type*, not the extension: the server resolves the media type by detection
    // rather than by trusting the file name (FR-039), so copy about extensions would describe a
    // check the product does not make.
    DISALLOWED_FILE_TYPE: 'content-drive.upload.failure.disallowed-file-type',
    // Its own copy, not the type one. This is the *folder's* filename glob (`filesMasks`), so the
    // same file is accepted one folder over — the fix is to rename or move it, where a disallowed
    // type means the file cannot be uploaded here at all. One message for both would send the
    // author to change the wrong thing.
    FOLDER_FILTER_MISMATCH: FOLDER_FILTER_MISMATCH_KEY,
    NAME_COLLISION: 'content-drive.upload.failure.name-collision',
    PERMISSION_DENIED: 'content-drive.upload.failure.permission-denied',
    STAGED_CONTENT_UNAVAILABLE: 'content-drive.upload.failure.staged-content-unavailable',
    UNCLASSIFIED: 'content-drive.upload.failure.unclassified'
};

// `hasOwnProperty`, not `in`: `in` walks the prototype chain, so a reason of `constructor` or
// `toString` would pass the guard and the lookup would return an inherited *function* straight into
// `DotMessageService.get()`.
const isKnownReason = (reason: string): reason is DotBulkUploadFailureReason =>
    Object.prototype.hasOwnProperty.call(MESSAGE_KEY_BY_REASON, reason);

/**
 * Resolves a failure reason to the message key that explains it to the author.
 *
 * Anything unrecognised — a reason the server added before the client learned about it, or none at
 * all — falls back to the unclassified copy. That is a deliberate soft landing: the alternative is
 * rendering a raw code or a blank, and neither tells the author anything.
 */
export function messageKeyForFailureReason(reason: string | undefined): string {
    return reason && isKnownReason(reason)
        ? MESSAGE_KEY_BY_REASON[reason]
        : MESSAGE_KEY_BY_REASON.UNCLASSIFIED;
}

/** The two severities an outcome's messages can carry. */
export type DotUploadFailureSeverity = 'warn' | 'error';

/**
 * How serious each reason is, which decides **which message** a failure is reported in.
 *
 * The line is whether changing the file gets the author past it. A name already taken, a name the
 * folder's glob refuses, a type the content type will not admit, a file over the ceiling: all of
 * those are answered by renaming, moving, converting or shrinking, so they are warnings — the
 * author has somewhere to go. A permission they do not hold, content that expired before the run
 * reached it, or a failure nobody anticipated are none of their doing and no edit to the file will
 * change them, so they are errors.
 *
 * A `Record` for the same reason the copy is one: the compiler refuses a new member of the union
 * that nobody has ranked, so adding a reason to the contract fails the build here until someone
 * decides which of the two it is. That decision is not one to leave to a default.
 */
const SEVERITY_BY_REASON: Record<DotBulkUploadFailureReason, DotUploadFailureSeverity> = {
    NAME_COLLISION: 'warn',
    FOLDER_FILTER_MISMATCH: 'warn',
    DISALLOWED_FILE_TYPE: 'warn',
    OVER_SIZE_LIMIT: 'warn',
    PERMISSION_DENIED: 'error',
    STAGED_CONTENT_UNAVAILABLE: 'error',
    UNCLASSIFIED: 'error'
};

/**
 * Ranks a failure reason.
 *
 * Anything unrecognised is an **error**, which is the opposite default to the copy's. There the
 * soft landing is right, because generic copy still tells the author something. Here it would be a
 * claim: ranking an unknown reason as a warning asserts the author can fix it, and a reason the
 * client has never heard of is no evidence of that.
 */
export function severityForFailureReason(reason: string | undefined): DotUploadFailureSeverity {
    return reason && isKnownReason(reason) ? SEVERITY_BY_REASON[reason] : 'error';
}
