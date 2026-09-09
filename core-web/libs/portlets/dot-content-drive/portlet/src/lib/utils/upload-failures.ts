import { DotBatchItemResult, DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

import {
    DotUploadFailureSeverity,
    FOLDER_FILTER_MISMATCH_KEY,
    FOLDER_FILTER_MISMATCH_NAMED_KEY,
    messageKeyForFailureReason,
    severityForFailureReason
} from './failure-reasons';

/**
 * How many names a **warning** line will print before it counts them instead.
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

/**
 * What the caller knows that the wire does not.
 *
 * Only the folder's own filter so far, and it is optional because it is knowable only sometimes:
 * see {@link FOLDER_FILTER_MISMATCH_KEY}.
 */
export interface DotUploadFailureContext {
    /**
     * The target folder's `filesMasks` as stored, e.g. `*.jpg,*.png`. Read into a readable list
     * before it reaches the copy, so how the field happens to be punctuated is not the author's
     * problem.
     */
    folderFilter?: string;
}

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
 * **Errors are counted, warnings are named.** A warning's file names are the author's to-do list —
 * rename this, move that, convert the other — so they are printed. An error's are not: no per-file
 * action gets them past a permission they do not hold or content that expired, so the names would
 * be a list they can act on no part of, and the count says as much with none of the noise.
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
    resolve: ResolveMessage,
    context: DotUploadFailureContext = {}
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
                    .map(([key, group]) =>
                        lineFor(key, subjectFor(group.names, severity, resolve), context, resolve)
                    )
            }))
            // A severity nothing failed under gets no message at all, rather than an empty one.
            .filter((group) => group.lines.length > 0)
    );
}

/**
 * Resolves one line, taking the fuller copy where the caller supplied what it needs.
 *
 * Matched on the message key rather than special-cased at the mapping, so a reason with no extra
 * copy needs nothing said about it here and the reason-to-copy `Record` stays the one place a new
 * reason has to be answered.
 */
function lineFor(
    key: string,
    subject: string,
    context: DotUploadFailureContext,
    resolve: ResolveMessage
): string {
    const folderFilter = readableFilter(context.folderFilter);

    return FOLDER_FILTER_MISMATCH_KEY === key && folderFilter
        ? resolve(FOLDER_FILTER_MISMATCH_NAMED_KEY, subject, folderFilter)
        : resolve(key, subject);
}

/**
 * A folder's stored filter as a list a sentence can end with.
 *
 * `filesMasks` is one comma-separated string with no promised spacing, so it is re-punctuated here
 * rather than dropped into the copy as typed. Empty means the folder names no rule, which is not
 * the same as naming an empty one: the caller falls back to the generic line.
 */
function readableFilter(filesMasks: string | undefined): string {
    return (filesMasks ?? '')
        .split(',')
        .map((mask) => mask.trim())
        .filter((mask) => mask.length > 0)
        .join(', ');
}

/**
 * What the line is about: the names when they read as a list, the count when they do not.
 *
 * Either way it is one `{0}`, so each reason keeps a single piece of copy rather than needing a
 * singular and a plural variant.
 */
function subjectFor(
    names: string[],
    severity: DotUploadFailureSeverity,
    resolve: ResolveMessage
): string {
    // Errors are counted however few there are (developer's call). Nothing the author does to one
    // of these files gets them past it — no permission appears because they renamed something — so
    // the names are a list they can act on no part of, and a count says the same thing without
    // making them read it. Warnings are the opposite: those names *are* the work.
    return 'error' === severity || names.length > MAX_NAMES_PER_LINE
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
