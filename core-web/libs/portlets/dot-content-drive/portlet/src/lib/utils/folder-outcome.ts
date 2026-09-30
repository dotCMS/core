import { DotBatchItemResult } from '@dotcms/dotcms-models';

/**
 * What every folder operation's report shares, whatever its words (#37062, #37063).
 *
 * Bulk delete and bulk duplicate read the same way, the same grouping, name threshold and
 * count-only overflow, so the line builder lives here once. Each operation keeps only its own
 * vocabulary, in its own file.
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

/** Resolves a message key with arguments. Narrower than `DotMessageService` on purpose. */
export type ResolveMessage = (key: string, ...args: string[]) => string;

/**
 * The words a folder operation's report is written in: one sentence per failure reason, and one
 * for each of the two kinds of skip.
 *
 * The line builder below is shared, so bulk delete and bulk duplication (#37062) read the same way:
 * the same grouping, the same name threshold, the same count-only overflow. What differs is only
 * the copy, because an ancestor that *removed* a folder and one whose duplicate *carries* it are
 * different facts.
 */
export interface DotFolderOutcomeVocabulary {
    /** The message key explaining a failure reason, with its own unclassified fallback. */
    keyForFailure: (reason: string | undefined) => string;
    /** A folder an ancestor in the same submission already covered. Not a problem. */
    skippedByParentKey: string;
    /** A folder the run never reached, because it was cancelled first. */
    skippedCancelledKey: string;
}

/**
 * Describes a folder operation's shortfall as one line per reason, each naming the folders it
 * applied to, in the given vocabulary.
 *
 * Grouped by reason because three lines saying "no permission" tell an author nothing three times.
 * Skips are described separately from failures, and the **two kinds of skip** get different
 * sentences: a folder an ancestor already covered is not a problem, and a folder the run never
 * reached is (FR-031).
 *
 * The server's per-folder `message` is never read here. It is diagnostic, written for a log (CR-04).
 */
export function describeFolderOutcome(
    // Deliberately the wide type. The outcome's `failures` carries whichever vocabulary its
    // producer speaks, and this function's whole contract is that an unrecognised reason still names
    // its folder and still reports as failed — narrowing would only move the problem to a cast at
    // the call site, where the fallback stops being visible.
    results: DotBatchItemResult<string>[],
    resolve: ResolveMessage,
    vocabulary: DotFolderOutcomeVocabulary
): string[] {
    const byKey = new Map<string, string[]>();

    const add = (messageKey: string, folder: string): void => {
        byKey.set(messageKey, [...(byKey.get(messageKey) ?? []), folder]);
    };

    for (const item of results) {
        if (item.status === 'FAILED') {
            add(vocabulary.keyForFailure(item.reason), item.key);
        } else if (item.status === 'SKIPPED') {
            // The two senses of skipped. Both are "not attempted", for entirely different reasons:
            // an ancestor already covered this one, or the run never reached it. One sentence
            // cannot serve both, and the first is not a problem at all (FR-031).
            add(
                item.reason === 'COVERED_BY_PARENT'
                    ? vocabulary.skippedByParentKey
                    : vocabulary.skippedCancelledKey,
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
