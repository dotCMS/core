import { DotBatchItemResult, DotFolderDeleteFailureReason } from '@dotcms/dotcms-models';

import {
    describeFolderOutcome,
    DotFolderOutcomeVocabulary,
    ResolveMessage
} from './folder-outcome';

/**
 * Reason → copy for bulk folder delete (#37063).
 *
 * Mirrors `failure-reasons.ts`, which does this job for upload, and exists for the same reason: a
 * reason with no copy is a hole the author sees. Only delete's words live here; the line builder
 * every folder operation shares is in `folder-outcome.ts`.
 */

/** A folder its ancestor removed first. Not a problem, and must not read as one. */
export const SKIPPED_BY_PARENT_KEY = 'content-drive.delete.skipped.covered-by-parent';

/** A folder the run never reached, because it was cancelled first. */
export const SKIPPED_CANCELLED_KEY = 'content-drive.delete.skipped.cancelled';

/**
 * Every reason the contract can return, each with its own sentence.
 *
 * A `Record` rather than a `switch` so the compiler refuses a new member of the union that nobody
 * wrote copy for: adding a reason to the contract fails the build here until someone writes it,
 * which is the hole this mapping exists to prevent.
 */
const MESSAGE_KEY_BY_REASON: Record<DotFolderDeleteFailureReason, string> = {
    PERMISSION_DENIED: 'content-drive.delete.failure.permission-denied',
    // Its own copy, not the permission one. "It is not there" and "you may not touch it" send the
    // author to look in completely different places.
    PATH_NOT_FOUND: 'content-drive.delete.failure.path-not-found',
    PROTECTED_FOLDER: 'content-drive.delete.failure.protected-folder',
    IN_USE: 'content-drive.delete.failure.in-use',
    // Reached only through the failure path; as a *skip* it is described by
    // {@link SKIPPED_BY_PARENT_KEY}, which does not read as a problem.
    COVERED_BY_PARENT: 'content-drive.delete.failure.covered-by-parent',
    UNCLASSIFIED: 'content-drive.delete.failure.unclassified'
};

// `hasOwnProperty`, not `in`: `in` walks the prototype chain, so a reason of `constructor` or
// `toString` would pass the guard and hand an inherited *function* to `DotMessageService.get()`.
const isKnownReason = (reason: string): reason is DotFolderDeleteFailureReason =>
    Object.prototype.hasOwnProperty.call(MESSAGE_KEY_BY_REASON, reason);

/**
 * Resolves a failure reason to the message key that explains it to the author.
 *
 * Anything unrecognised — a reason the server added before the client learned about it, or none at
 * all — falls back to the unclassified copy. The folder is still named and still reported as
 * failed; a raw code or a blank would tell the author nothing (FR-030).
 */
export function messageKeyForFolderDeleteReason(reason: string | undefined): string {
    return reason && isKnownReason(reason)
        ? MESSAGE_KEY_BY_REASON[reason]
        : MESSAGE_KEY_BY_REASON.UNCLASSIFIED;
}

/** Bulk delete's words. See {@link DotFolderOutcomeVocabulary}. */
const FOLDER_DELETE_VOCABULARY: DotFolderOutcomeVocabulary = {
    keyForFailure: messageKeyForFolderDeleteReason,
    skippedByParentKey: SKIPPED_BY_PARENT_KEY,
    skippedCancelledKey: SKIPPED_CANCELLED_KEY
};

/**
 * Describes a bulk delete's shortfall. See {@link describeFolderOutcome}.
 *
 * @param results the run's per-folder records
 * @param resolve resolves a message key with its arguments
 * @returns one line per reason, in the order the reasons were first met
 */
export function describeFolderDeleteOutcome(
    results: DotBatchItemResult<string>[],
    resolve: ResolveMessage
): string[] {
    return describeFolderOutcome(results, resolve, FOLDER_DELETE_VOCABULARY);
}
