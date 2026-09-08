import { DotBatchItemResult, DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

import { messageKeyForFailureReason } from './failure-reasons';

/**
 * How many names one line will print before it counts them instead.
 *
 * Past this, the line leads with the number and names none. Naming the first eight of fifty would
 * read as "eight files failed", which is worse than saying nothing — and a batch caps at 100 files,
 * so a wholly failed one is well past being a sentence. The names stay recoverable from the outcome,
 * which the durable notification keeps, so nothing is lost by not reciting them here.
 */
const MAX_NAMES_PER_LINE = 8;

/** Names a group by its size when it is too large to read as a list. */
const N_FILES_KEY = 'content-drive.upload.failure.n-files';

/** Resolves a message key with arguments. Narrower than `DotMessageService` on purpose. */
type ResolveMessage = (key: string, ...args: string[]) => string;

/**
 * Describes a batch's failures as one line per reason, each naming the files it applied to.
 *
 * Grouped rather than one line per file: three lines saying "larger than the limit" tell an author
 * nothing three times, and the same reason repeated is the noise this feature set out to remove.
 * Skipped files are left out entirely — never attempted is not the same as failed, and the counts
 * report them separately.
 *
 * Returns resolved copy, not keys, so the caller has nothing left to decide. The strings reach the
 * toast as HTML, so every file name is escaped here rather than at the call site: a name is content
 * the author supplied, and escaping it is not optional.
 */
export function describeUploadFailures(
    results: DotBatchItemResult<DotBulkUploadFailureReason>[] | undefined,
    resolve: ResolveMessage
): string[] {
    if (!results?.length) {
        return [];
    }

    // A Map, so reasons come out in the order they were first met and the names inside each keep
    // submission order — which is the order the author chose them in, and therefore the order they
    // can still find a name in.
    const namesByReason = new Map<string, string[]>();

    results
        .filter((result) => result.status === 'FAILED')
        .forEach((result) => {
            // A failure with no reason is the unclassified case, which has real copy of its own
            // rather than a blank or a raw code.
            const key = messageKeyForFailureReason(result.reason);
            const names = namesByReason.get(key) ?? [];

            names.push(escapeFileName(result.key));
            namesByReason.set(key, names);
        });

    return [...namesByReason].map(([key, names]) => resolve(key, subjectFor(names, resolve)));
}

/**
 * What the line is about: the names when they read as a list, the count when they do not.
 *
 * Either way it is one `{0}`, so each reason keeps a single piece of copy rather than needing a
 * singular and a plural variant.
 */
function subjectFor(names: string[], resolve: ResolveMessage): string {
    return names.length > MAX_NAMES_PER_LINE
        ? resolve(N_FILES_KEY, String(names.length))
        : names.join(', ');
}

/**
 * Escapes a file name for the HTML the toast renders.
 *
 * Only the four characters that can end an attribute or open a tag. A file name is not markup, and
 * escaping more would print entity codes into a name the author has to recognise.
 */
function escapeFileName(name: string): string {
    return name
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
