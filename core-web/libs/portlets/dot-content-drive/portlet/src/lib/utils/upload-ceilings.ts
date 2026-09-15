import { DotBulkUploadCeilings } from '@dotcms/dotcms-models';

/**
 * Refusing a batch before it is uploaded, in the words of the ceiling it crossed.
 *
 * The server enforces two ceilings and is the authority on both; this is the courtesy check in
 * front of them. It exists because the alternative is an author waiting out the upload of a batch
 * that was never going to be accepted, and then reading a refusal that says only "fewer".
 *
 * Both ceilings are read from the configuration the server advertises rather than assumed here. A
 * guessed default would be worse than no check: the client would refuse batches the instance
 * accepts, and no author can see why.
 */

/** Too many files, naming the batch's count and the ceiling. */
export const TOO_MANY_FILES_NAMED_KEY = 'content-drive.upload.refused.too-many-files-named';

/** Too much data, naming both sizes in megabytes. */
export const TOO_LARGE_NAMED_KEY = 'content-drive.upload.refused.too-large-named';

const BYTES_PER_MB = 1024 * 1024;

/** A refusal, as the key that explains it and the numbers that go in it. */
export interface DotUploadCeilingRefusal {
    key: string;
    args: string[];
}

/**
 * Whether this batch crosses a ceiling the server would refuse it for.
 *
 * **Size before count**, matching the order the server checks in: it refuses a declared over-size
 * batch before it reads a single part. A client that answered "too many files" where the server
 * would have answered "too large" would name a different fix for the very same batch.
 *
 * A ceiling that is absent, zero or negative is treated as no ceiling. Absent means an instance
 * older than the field, or a configuration request that never landed; zero or below is how the
 * platform spells unbounded elsewhere. Either way the server stays the enforcement point and
 * answers with the copy that names no number.
 *
 * @returns the refusal, or `undefined` when the batch may be attempted
 */
export function refuseOverCeiling(
    files: File[],
    ceilings: Partial<DotBulkUploadCeilings> | null | undefined
): DotUploadCeilingRefusal | undefined {
    if (!files.length) {
        return undefined;
    }

    const maxTotalBytes = ceilings?.maxTotalBytes ?? 0;
    const maxFiles = ceilings?.maxFiles ?? 0;

    if (maxTotalBytes > 0) {
        const totalBytes = files.reduce((total, file) => total + file.size, 0);

        if (totalBytes > maxTotalBytes) {
            // The batch rounds up and the allowance rounds down, so a batch a few hundred kilobytes
            // over a ceiling cannot render as "1024 MB, and one upload allows 1024 MB" — a refusal
            // that reads as having no reason.
            return {
                key: TOO_LARGE_NAMED_KEY,
                args: [
                    String(Math.ceil(totalBytes / BYTES_PER_MB)),
                    String(Math.floor(maxTotalBytes / BYTES_PER_MB))
                ]
            };
        }
    }

    if (maxFiles > 0 && files.length > maxFiles) {
        return {
            key: TOO_MANY_FILES_NAMED_KEY,
            args: [String(files.length), String(maxFiles)]
        };
    }

    return undefined;
}
