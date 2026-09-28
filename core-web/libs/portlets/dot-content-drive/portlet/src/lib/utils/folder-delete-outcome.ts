import { DotBatchItemResult, DotFolderDeleteFailureReason } from '@dotcms/dotcms-models';

/**
 * Reason → copy for bulk folder delete (#37063).
 *
 * Mirrors `failure-reasons.ts`, which does this job for upload, and exists for the same reason: a
 * reason with no copy is a hole the author sees. Written once here because folder copy (#37062) and
 * move (#37165) consume the same outcome shape and the same vocabulary — three half-overlapping
 * mappings is exactly what the shared contract was meant to prevent.
 */

/**
 * How many folder names a line prints before it counts them instead.
 *
 * Past this the line leads with the number and names none. Naming the first eight of fifty reads as
 * "eight folders failed", which is worse than saying nothing.
 *
 * **What is lost, stated honestly:** the names past this point are not in the notification. They are
 * in the run's durable record, which the author reaches by following the count (FR-027a) — so this
 * is a readability trade, not information the client threw away.
 */
export const MAX_FOLDER_NAMES = 8;

/** A folder its ancestor removed first. Not a problem, and must not read as one. */
export const SKIPPED_BY_PARENT_KEY = 'content-drive.delete.skipped.covered-by-parent';

/** A folder the run never reached, because it was cancelled first. */
export const SKIPPED_CANCELLED_KEY = 'content-drive.delete.skipped.cancelled';

/** Resolves a message key with arguments. Narrower than `DotMessageService` on purpose. */
type ResolveMessage = (key: string, ...args: string[]) => string;

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

/**
 * Describes a run's shortfall as one line per reason, each naming the folders it applied to.
 *
 * Grouped by reason because three lines saying "no permission" tell an author nothing three times.
 * Skips are described separately from failures, and the **two kinds of skip** get different
 * sentences: a folder an ancestor already removed is not a problem, and a folder the run never
 * reached is (FR-031).
 *
 * The server's per-folder `message` is never read here. It is diagnostic, written for a log (CR-04).
 */
export function describeFolderDeleteOutcome(
    // Deliberately the wide type. The outcome's `failures` carries whichever vocabulary its
    // producer speaks, and this function's whole contract is that an unrecognised reason still names
    // its folder and still reports as failed — narrowing would only move the problem to a cast at
    // the call site, where the fallback stops being visible.
    results: DotBatchItemResult<string>[],
    resolve: ResolveMessage
): string[] {
    const byKey = new Map<string, string[]>();

    const add = (messageKey: string, folder: string): void => {
        byKey.set(messageKey, [...(byKey.get(messageKey) ?? []), folder]);
    };

    for (const item of results) {
        if (item.status === 'FAILED') {
            add(messageKeyForFolderDeleteReason(item.reason), item.key);
        } else if (item.status === 'SKIPPED') {
            // The two senses of skipped. Both are "not attempted", for entirely different reasons:
            // an ancestor already removed this one, or the run never reached it. One sentence
            // cannot serve both, and the first is not a problem at all (FR-031).
            add(
                item.reason === 'COVERED_BY_PARENT' ? SKIPPED_BY_PARENT_KEY : SKIPPED_CANCELLED_KEY,
                item.key
            );
        }
    }

    return [...byKey.entries()].map(([messageKey, folders]) =>
        folders.length > MAX_FOLDER_NAMES
            ? // Past the threshold the line leads with the number and names none. The full list is
              // in the run's durable record, which the author reaches by following the count.
              resolve(`${messageKey}-many`, String(folders.length))
            : resolve(messageKey, folders.join(', '))
    );
}
