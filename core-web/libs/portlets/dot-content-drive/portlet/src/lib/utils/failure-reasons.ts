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
const MESSAGE_KEY_BY_REASON: Record<DotBulkUploadFailureReason, string> = {
    OVER_SIZE_LIMIT: 'content-drive.upload.failure.over-size-limit',
    // Named for the *type*, not the extension: the server resolves the media type by detection
    // rather than by trusting the file name (FR-039), so copy about extensions would describe a
    // check the product does not make.
    DISALLOWED_FILE_TYPE: 'content-drive.upload.failure.disallowed-file-type',
    NAME_COLLISION: 'content-drive.upload.failure.name-collision',
    PERMISSION_DENIED: 'content-drive.upload.failure.permission-denied',
    STAGED_CONTENT_UNAVAILABLE: 'content-drive.upload.failure.staged-content-unavailable',
    UNCLASSIFIED: 'content-drive.upload.failure.unclassified'
};

const isKnownReason = (reason: string): reason is DotBulkUploadFailureReason =>
    reason in MESSAGE_KEY_BY_REASON;

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
