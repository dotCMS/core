import { DotBatchItemResult, DotFolderBulkDuplicateReason } from '@dotcms/dotcms-models';

import { describeFolderOutcome } from './folder-outcome';

import { DotFolderOutcomeVocabulary, ResolveMessage } from '../shared/models';

/**
 * Reason → copy for bulk folder duplication (#37062).
 *
 * Reads exactly like bulk folder delete's report: the line builder is delete's, shared through
 * {@link describeFolderOutcome}, so the grouping, the name threshold and the count-only overflow are
 * the same. Only the words are duplication's, because delete's copy says a folder was removed and a
 * duplicated folder is untouched.
 */

/** A folder its selected parent covered: the parent's duplicate already carries a copy of it. */
export const SKIPPED_BY_PARENT_KEY = 'content-drive.duplicate.skipped.covered-by-parent';

/** A folder the run never reached, because it was cancelled first. Nothing was copied for it. */
export const SKIPPED_CANCELLED_KEY = 'content-drive.duplicate.skipped.cancelled';

/**
 * Every reason the contract can return, each with its own sentence.
 *
 * A `Record` so the compiler refuses a new member of the union that nobody wrote copy for.
 */
const MESSAGE_KEY_BY_REASON: Record<DotFolderBulkDuplicateReason, string> = {
    PERMISSION_DENIED: 'content-drive.duplicate.failure.permission-denied',
    // Its own copy: the fix is on the folder it sits in, not on the folder itself.
    PARENT_PERMISSION_DENIED: 'content-drive.duplicate.failure.parent-permission-denied',
    PATH_NOT_FOUND: 'content-drive.duplicate.failure.path-not-found',
    PROTECTED_FOLDER: 'content-drive.duplicate.failure.protected-folder',
    // Reached only if the server ever reports it as a failure; as a skip it reads through
    // {@link SKIPPED_BY_PARENT_KEY}, which does not read as a problem.
    COVERED_BY_PARENT: 'content-drive.duplicate.failure.covered-by-parent',
    UNCLASSIFIED: 'content-drive.duplicate.failure.unclassified'
};

// Own keys only: `in` walks the prototype chain, so `constructor` would pass as a reason.
const isKnownReason = (reason: string): reason is DotFolderBulkDuplicateReason =>
    Object.hasOwn(MESSAGE_KEY_BY_REASON, reason);

/**
 * Resolves a duplication failure reason to the message key that explains it.
 *
 * Anything unrecognised, or no reason at all, falls back to the unclassified copy, so the folder is
 * still named and still reported as failed (FR-025).
 */
export function messageKeyForFolderDuplicateReason(reason: string | undefined): string {
    return reason && isKnownReason(reason)
        ? MESSAGE_KEY_BY_REASON[reason]
        : MESSAGE_KEY_BY_REASON.UNCLASSIFIED;
}

const FOLDER_DUPLICATE_VOCABULARY: DotFolderOutcomeVocabulary = {
    keyForFailure: messageKeyForFolderDuplicateReason,
    skippedByParentKey: SKIPPED_BY_PARENT_KEY,
    skippedCancelledKey: SKIPPED_CANCELLED_KEY
};

/**
 * Describes a bulk duplication's shortfall, one line per reason. See {@link describeFolderOutcome}.
 *
 * @param results the run's per-folder records, keyed by the submitted folder path
 * @param resolve resolves a message key with its arguments
 * @returns one line per reason, in the order the reasons were first met
 */
export function describeFolderDuplicateOutcome(
    results: DotBatchItemResult<string>[],
    resolve: ResolveMessage
): string[] {
    return describeFolderOutcome(results, resolve, FOLDER_DUPLICATE_VOCABULARY);
}
