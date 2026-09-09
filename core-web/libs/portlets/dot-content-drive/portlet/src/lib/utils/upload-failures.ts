import { DotBatchItemResult, DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

import {
    DotUploadFailureSeverity,
    messageKeyForFailureReason,
    severityForFailureReason
} from './failure-reasons';

/**
 * How many names one line will print before it counts them instead.
 *
 * Past this, the line leads with the number and names none. Naming the first eight of fifty would
 * read as "eight files failed", which is worse than saying nothing — and a batch caps at 100 files,
 * so a wholly failed one is well past being a sentence.
 *
 * **What is lost, stated honestly.** The names are not recoverable from anywhere the author can
 * reach: the durable notification carries counts only, so past this threshold those file names are
 * gone from the interface. They survive in the job's own record, which the status endpoint returns
 * and this client already knows how to read, so showing them is a *feature nobody has built* rather
 * than information the server threw away — which is the difference between this being a considered
 * trade and a quiet loss.
 */
const MAX_NAMES_PER_LINE = 8;

/** Names a group by its size when it is too large to read as a list. */
const N_FILES_KEY = 'content-drive.upload.failure.n-files';

/** Resolves a message key with arguments. Narrower than `DotMessageService` on purpose. */
type ResolveMessage = (key: string, ...args: string[]) => string;

/** Lines that belong in one message, because they are the same kind of news. */
export interface DotUploadFailureGroup {
    severity: DotUploadFailureSeverity;
    /** One line per reason, each naming the files it applied to. */
    lines: string[];
}

/**
 * Describes a batch's failures as one message per severity, each holding one line per reason.
 *
 * **Two levels of grouping, for two different reasons.** Lines are grouped by reason because three
 * lines saying "larger than the limit" tell an author nothing three times. Lines are then grouped
 * by *severity* because a name the folder refuses and a permission the author does not hold are
 * different kinds of news: one is a thing to go and fix, the other is a wall. A single notification
 * carrying both leaves the reader to work out which half is theirs to act on, and the half they can
 * act on is the whole point of naming the files at all.
 *
 * Errors come first regardless of the order the files arrived in. File order is an accident of how
 * they were selected; which message the author reads first is not.
 *
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
): DotUploadFailureGroup[] {
    if (!results?.length) {
        return [];
    }

    // A Map, so reasons come out in the order they were first met and the names inside each keep
    // submission order — which is the order the author chose them in, and therefore the order they
    // can still find a name in.
    const namesByReason = new Map<
        string,
        { severity: DotUploadFailureSeverity; names: string[] }
    >();

    results
        .filter((result) => result.status === 'FAILED')
        .forEach((result) => {
            // A failure with no reason is the unclassified case, which has real copy of its own
            // rather than a blank or a raw code — and is ranked an error, since an unrecognised
            // reason is no evidence the author can do anything about it.
            const key = messageKeyForFailureReason(result.reason);
            const group = namesByReason.get(key) ?? {
                severity: severityForFailureReason(result.reason),
                names: []
            };

            group.names.push(escapeFileName(result.key));
            namesByReason.set(key, group);
        });

    return (
        (['error', 'warn'] as const)
            .map((severity) => ({
                severity,
                lines: [...namesByReason]
                    .filter(([, group]) => group.severity === severity)
                    .map(([key, group]) => resolve(key, subjectFor(group.names, resolve)))
            }))
            // A severity nothing failed under gets no message at all, rather than an empty one.
            .filter((group) => group.lines.length > 0)
    );
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
