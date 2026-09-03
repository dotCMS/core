/**
 * Why a single file in a batch did not make it, and the product copy that explains it.
 *
 * FR-036: every reason the server can return must have its own copy. A reason with no copy is a
 * hole the author sees, so the mapping is exhaustive over a closed set and the fallback is real
 * copy rather than a placeholder.
 *
 * **This union is the client's, not the wire format.** The server's reason codes belong to the
 * submission contract, which is not settled (see the feature's `contracts/client-requirements.md`).
 * Translating whatever strings arrive into this union is a separate step and belongs with the
 * upload work; this file needs no contract at all, which is why the copy can land first.
 */
export const CONTENT_DRIVE_FAILURE_REASONS = [
    'OVER_SIZE_LIMIT',
    'DISALLOWED_TYPE',
    'NAME_COLLISION',
    'PERMISSION_DENIED',
    'STAGED_CONTENT_UNAVAILABLE',
    'UNCLASSIFIED'
] as const;

export type DotContentDriveFailureReason = (typeof CONTENT_DRIVE_FAILURE_REASONS)[number];

/**
 * `Record` rather than a `switch`: the compiler then refuses a new member of the union that nobody
 * wrote copy for, which is the failure this whole mapping exists to prevent.
 */
const MESSAGE_KEY_BY_REASON: Record<DotContentDriveFailureReason, string> = {
    OVER_SIZE_LIMIT: 'content-drive.upload.failure.over-size-limit',
    // Named for the *type*, not the extension: the server resolves the media type by detection
    // rather than by trusting the file name (FR-039), so copy about extensions would describe a
    // check the product does not make.
    DISALLOWED_TYPE: 'content-drive.upload.failure.disallowed-type',
    NAME_COLLISION: 'content-drive.upload.failure.name-collision',
    PERMISSION_DENIED: 'content-drive.upload.failure.permission-denied',
    STAGED_CONTENT_UNAVAILABLE: 'content-drive.upload.failure.staged-content-unavailable',
    UNCLASSIFIED: 'content-drive.upload.failure.unclassified'
};

const isKnownReason = (reason: string): reason is DotContentDriveFailureReason =>
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
