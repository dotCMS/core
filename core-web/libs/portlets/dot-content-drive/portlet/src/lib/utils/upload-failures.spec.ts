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

    /**
     * Every line, with the severity grouping flattened away.
     *
     * Most of what matters about these lines is what they say, not which message they end up in, so
     * the grouping is asserted on its own below rather than in every case.
     */
    const linesOf = (results: DotBatchItemResult<DotBulkUploadFailureReason>[] | undefined) =>
        describeUploadFailures(results, get).flatMap((group) => group.lines);

    const failed = (key: string, reason: DotBulkUploadFailureReason): DotBatchItemResult => ({
        key,
        status: 'FAILED',
        reason
    });

    it('should say nothing when every file succeeded', () => {
        expect(
            linesOf([
                { key: 'a.png', status: 'SUCCESS' },
                { key: 'b.png', status: 'SUCCESS' }
            ])
        ).toEqual([]);
    });

    it('should say nothing when there are no results at all', () => {
        // A run that recorded no per-file detail is reported by its counts alone; inventing lines
        // here would put words in the server's mouth.
        expect(linesOf(undefined)).toEqual([]);
    });

    it('should name the file that failed, with its reason', () => {
        expect(linesOf([failed('huge.mov', 'OVER_SIZE_LIMIT')])).toEqual([
            'content-drive.upload.failure.over-size-limit|huge.mov'
        ]);
    });

    it('should group files that failed for the same reason into one line', () => {
        // The point of grouping: three lines saying "larger than the limit" tell the author nothing
        // three times.
        expect(
            linesOf([
                failed('a.mov', 'OVER_SIZE_LIMIT'),
                failed('b.mov', 'OVER_SIZE_LIMIT'),
                failed('c.mov', 'OVER_SIZE_LIMIT')
            ])
        ).toEqual(['content-drive.upload.failure.over-size-limit|a.mov, b.mov, c.mov']);
    });

    it('should keep each reason on its own line', () => {
        const lines = linesOf([
            failed('huge.mov', 'OVER_SIZE_LIMIT'),
            failed('notes.exe', 'DISALLOWED_FILE_TYPE'),
            failed('report.pdf', 'NAME_COLLISION')
        ]);

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
            linesOf([failed('z.mov', 'OVER_SIZE_LIMIT'), failed('a.mov', 'OVER_SIZE_LIMIT')])
        ).toEqual(['content-drive.upload.failure.over-size-limit|z.mov, a.mov']);
    });

    it('should fall back to the unclassified reason when a failure carries none', () => {
        // Counted rather than named: an unrecognised reason ranks as an error, and errors are
        // counted, since nothing the author does to the file is known to help.
        expect(linesOf([{ key: 'mystery.png', status: 'FAILED' }])).toEqual([
            'content-drive.upload.failure.unclassified|content-drive.upload.failure.n-files|1'
        ]);
    });

    it('should ignore skipped files, which are not failures', () => {
        // Never attempted is not the same as failed, and the counts report them separately.
        expect(
            linesOf([
                { key: 'later.png', status: 'SKIPPED' },
                failed('huge.mov', 'OVER_SIZE_LIMIT')
            ])
        ).toEqual(['content-drive.upload.failure.over-size-limit|huge.mov']);
    });

    it('should escape a file name so a crafted one cannot alter the message', () => {
        // These lines reach the toast as HTML, and a file name is content the author supplied.
        expect(linesOf([failed('<img src=x onerror=alert(1)>', 'NAME_COLLISION')])).toEqual([
            'content-drive.upload.failure.name-collision|&lt;img src=x onerror=alert(1)&gt;'
        ]);
    });

    it('should split warnings from errors, so each lands in its own message', () => {
        // The developer's call: one message per severity. A name the folder refuses and a
        // permission the author does not hold are different kinds of news, and one notification
        // carrying both makes the reader work out which half they can act on.
        const groups = describeUploadFailures(
            [
                failed('taken.png', 'NAME_COLLISION'),
                failed('locked.png', 'PERMISSION_DENIED'),
                failed('notes.txt', 'FOLDER_FILTER_MISMATCH')
            ],
            get
        );

        expect(groups).toEqual([
            {
                severity: 'error',
                lines: [
                    // Counted, not named: see the error/warning split below.
                    'content-drive.upload.failure.permission-denied|content-drive.upload.failure.n-files|1'
                ]
            },
            {
                severity: 'warn',
                lines: [
                    'content-drive.upload.failure.name-collision|taken.png',
                    'content-drive.upload.failure.folder-filter-mismatch|notes.txt'
                ]
            }
        ]);
    });

    it('should put the errors first, whatever order the files arrived in', () => {
        // Severity decides the order, not the batch: the message the author cannot act on has to
        // be the one they read first, and file order is an accident of how they were selected.
        const groups = describeUploadFailures(
            [failed('taken.png', 'NAME_COLLISION'), failed('locked.png', 'PERMISSION_DENIED')],
            get
        );

        expect(groups.map((group) => group.severity)).toEqual(['error', 'warn']);
    });

    it('should return one group when every failure is the same kind of news', () => {
        const groups = describeUploadFailures(
            [failed('a.png', 'NAME_COLLISION'), failed('b.png', 'OVER_SIZE_LIMIT')],
            get
        );

        expect(groups.map((group) => group.severity)).toEqual(['warn']);
    });

    it('should count a large group instead of naming a few of it', () => {
        // A batch caps at 100 files, so this is the real worst case. Naming the first eight of 94
        // would read as "eight files failed", which is worse than saying nothing — so past the
        // threshold the line leads with the number and names none. The names stay recoverable from
        // the outcome, which the durable notification keeps.
        const many = Array.from({ length: 94 }, (_, i) =>
            failed(`file-${i}.png`, 'NAME_COLLISION')
        );

        expect(linesOf(many)).toEqual([
            'content-drive.upload.failure.name-collision|content-drive.upload.failure.n-files|94'
        ]);
    });

    it('should count an error group rather than naming it, however small', () => {
        // Developer's call, and the argument for it: no per-file action gets the author past a
        // permission they do not hold, so the names are a list they can do nothing with. A count
        // says the same thing without the noise.
        expect(linesOf([failed('locked.png', 'PERMISSION_DENIED')])).toEqual([
            'content-drive.upload.failure.permission-denied|content-drive.upload.failure.n-files|1'
        ]);
    });

    it('should still name a warning group, because that list is a to-do', () => {
        // The other half of the same decision. These names are what the author acts on: rename
        // this, move that, convert the other.
        expect(linesOf([failed('taken.png', 'NAME_COLLISION')])).toEqual([
            'content-drive.upload.failure.name-collision|taken.png'
        ]);
    });

    it('should name a group of exactly eight, and count one of nine', () => {
        // The boundary itself, because "past eight" is a judgement that will get revisited and the
        // two sides of it are different sentences. Eight names is still a list an author can scan;
        // nine reads as a wall of text where the count is the only part they take in.
        const collisions = (count: number) =>
            Array.from({ length: count }, (_, i) => failed(`file-${i}.png`, 'NAME_COLLISION'));

        expect(linesOf(collisions(8))).toEqual([
            'content-drive.upload.failure.name-collision|' +
                Array.from({ length: 8 }, (_, i) => `file-${i}.png`).join(', ')
        ]);

        expect(linesOf(collisions(9))).toEqual([
            'content-drive.upload.failure.name-collision|content-drive.upload.failure.n-files|9'
        ]);
    });

    it('should still name a group small enough to read', () => {
        const few = Array.from({ length: 3 }, (_, i) => failed(`file-${i}.png`, 'NAME_COLLISION'));

        expect(linesOf(few)).toEqual([
            'content-drive.upload.failure.name-collision|file-0.png, file-1.png, file-2.png'
        ]);
    });
});
