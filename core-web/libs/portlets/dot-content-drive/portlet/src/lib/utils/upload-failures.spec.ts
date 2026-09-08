import { describe, expect, it } from '@jest/globals';

import { DotBatchItemResult, DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

import { describeUploadFailures } from './upload-failures';

/**
 * Turning a batch's per-file results into something an author can act on.
 *
 * The requirement is that a partial outcome names the files that failed and gives a reason for each
 * — and that reasons are **grouped**, not repeated once per file: "3 files were larger than the
 * limit" rather than three lines saying the same thing, and a wholly failed batch of fifty reading
 * as a statement rather than fifty rows.
 */
describe('describeUploadFailures', () => {
    // Returns the key so assertions read against the reason rather than translated prose.
    const get = (key: string, ...args: string[]) => [key, ...args].join('|');

    const failed = (key: string, reason: DotBulkUploadFailureReason): DotBatchItemResult => ({
        key,
        status: 'FAILED',
        reason
    });

    it('should say nothing when every file succeeded', () => {
        expect(
            describeUploadFailures(
                [
                    { key: 'a.png', status: 'SUCCESS' },
                    { key: 'b.png', status: 'SUCCESS' }
                ],
                get
            )
        ).toEqual([]);
    });

    it('should say nothing when there are no results at all', () => {
        // A run that recorded no per-file detail is reported by its counts alone; inventing lines
        // here would put words in the server's mouth.
        expect(describeUploadFailures(undefined, get)).toEqual([]);
    });

    it('should name the file that failed, with its reason', () => {
        expect(describeUploadFailures([failed('huge.mov', 'OVER_SIZE_LIMIT')], get)).toEqual([
            'content-drive.upload.failure.over-size-limit|huge.mov'
        ]);
    });

    it('should group files that failed for the same reason into one line', () => {
        // The point of grouping: three lines saying "larger than the limit" tell the author nothing
        // three times.
        expect(
            describeUploadFailures(
                [
                    failed('a.mov', 'OVER_SIZE_LIMIT'),
                    failed('b.mov', 'OVER_SIZE_LIMIT'),
                    failed('c.mov', 'OVER_SIZE_LIMIT')
                ],
                get
            )
        ).toEqual(['content-drive.upload.failure.over-size-limit|a.mov, b.mov, c.mov']);
    });

    it('should keep each reason on its own line', () => {
        const lines = describeUploadFailures(
            [
                failed('huge.mov', 'OVER_SIZE_LIMIT'),
                failed('notes.exe', 'DISALLOWED_FILE_TYPE'),
                failed('report.pdf', 'NAME_COLLISION')
            ],
            get
        );

        expect(lines).toHaveLength(3);
        expect(lines).toEqual(
            expect.arrayContaining([
                'content-drive.upload.failure.over-size-limit|huge.mov',
                'content-drive.upload.failure.disallowed-file-type|notes.exe',
                'content-drive.upload.failure.name-collision|report.pdf'
            ])
        );
    });

    it('should preserve submission order within a reason', () => {
        // The outcome reports results in submission order, which is the order the author chose the
        // files in — so a name is findable in the list they still have in their head.
        expect(
            describeUploadFailures(
                [failed('z.mov', 'OVER_SIZE_LIMIT'), failed('a.mov', 'OVER_SIZE_LIMIT')],
                get
            )
        ).toEqual(['content-drive.upload.failure.over-size-limit|z.mov, a.mov']);
    });

    it('should fall back to the unclassified reason when a failure carries none', () => {
        expect(describeUploadFailures([{ key: 'mystery.png', status: 'FAILED' }], get)).toEqual([
            'content-drive.upload.failure.unclassified|mystery.png'
        ]);
    });

    it('should ignore skipped files, which are not failures', () => {
        // Never attempted is not the same as failed, and the counts report them separately.
        expect(
            describeUploadFailures(
                [{ key: 'later.png', status: 'SKIPPED' }, failed('huge.mov', 'OVER_SIZE_LIMIT')],
                get
            )
        ).toEqual(['content-drive.upload.failure.over-size-limit|huge.mov']);
    });

    it('should escape a file name so a crafted one cannot alter the message', () => {
        // These lines reach the toast as HTML, and a file name is content the author supplied.
        expect(
            describeUploadFailures([failed('<img src=x onerror=alert(1)>', 'NAME_COLLISION')], get)
        ).toEqual([
            'content-drive.upload.failure.name-collision|&lt;img src=x onerror=alert(1)&gt;'
        ]);
    });

    it('should count a large group instead of naming a few of it', () => {
        // A batch caps at 100 files, so this is the real worst case. Naming the first eight of 94
        // would read as "eight files failed", which is worse than saying nothing — so past the
        // threshold the line leads with the number and names none. The names stay recoverable from
        // the outcome, which the durable notification keeps.
        const many = Array.from({ length: 94 }, (_, i) =>
            failed(`file-${i}.png`, 'NAME_COLLISION')
        );

        expect(describeUploadFailures(many, get)).toEqual([
            'content-drive.upload.failure.name-collision|content-drive.upload.failure.n-files|94'
        ]);
    });

    it('should still name a group small enough to read', () => {
        const few = Array.from({ length: 3 }, (_, i) => failed(`file-${i}.png`, 'NAME_COLLISION'));

        expect(describeUploadFailures(few, get)).toEqual([
            'content-drive.upload.failure.name-collision|file-0.png, file-1.png, file-2.png'
        ]);
    });
});
