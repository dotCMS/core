import { describe, expect, it } from '@jest/globals';

import {
    refuseOverCeiling,
    TOO_LARGE_NAMED_KEY,
    TOO_MANY_FILES_NAMED_KEY
} from './upload-ceilings';

/**
 * Refusing a batch the server would refuse anyway, before a byte of it is uploaded.
 *
 * The ceilings are the server's, read from the configuration it advertises. Two things follow from
 * a number the client can read: the refusal can name the limit instead of saying "fewer", and it
 * can arrive in the file chooser rather than after an author has waited out the upload of a batch
 * that was never going to be accepted.
 */
describe('refuseOverCeiling', () => {
    const MB = 1024 * 1024;

    const filesOf = (count: number, bytesEach = 1) =>
        Array.from(
            { length: count },
            (_, i) => ({ name: `file-${i}.png`, size: bytesEach }) as File
        );

    it('should allow a batch inside both ceilings', () => {
        expect(
            refuseOverCeiling(filesOf(3), { maxFiles: 100, maxTotalBytes: 1024 * MB })
        ).toBeUndefined();
    });

    it('should refuse more files than one upload allows, naming both numbers', () => {
        // Naming them is the whole point: "fewer" makes an author with 140 files retry with 120,
        // then 110, uploading the entire batch each time to be refused again.
        expect(
            refuseOverCeiling(filesOf(101), { maxFiles: 100, maxTotalBytes: 1024 * MB })
        ).toEqual({ key: TOO_MANY_FILES_NAMED_KEY, args: ['101', '100'] });
    });

    it('should refuse a batch larger than one upload allows, in megabytes', () => {
        expect(
            refuseOverCeiling(filesOf(2, 700 * MB), { maxFiles: 100, maxTotalBytes: 1024 * MB })
        ).toEqual({ key: TOO_LARGE_NAMED_KEY, args: ['1400', '1024'] });
    });

    it('should report the size ceiling first when a batch crosses both', () => {
        // The order the server checks in: it refuses a declared over-size batch before it reads a
        // part, so a client that answered "too many files" would name a different ceiling than the
        // one the server would have named for the very same batch.
        expect(
            refuseOverCeiling(filesOf(200, 20 * MB), { maxFiles: 100, maxTotalBytes: 1024 * MB })
        ).toEqual({ key: TOO_LARGE_NAMED_KEY, args: ['4000', '1024'] });
    });

    it('should round the batch up and the allowance down, so the two never read as equal', () => {
        // A batch 400 KB over a 1024 MB ceiling rounds to "1024 MB, and one upload allows 1024 MB"
        // if both are rounded the same way, which reads as a refusal for no reason.
        expect(
            refuseOverCeiling(filesOf(1, 1024 * MB + 400 * 1024), {
                maxFiles: 100,
                maxTotalBytes: 1024 * MB
            })
        ).toEqual({ key: TOO_LARGE_NAMED_KEY, args: ['1025', '1024'] });
    });

    it('should allow everything when the server advertises no ceilings', () => {
        // An instance older than the configuration field, or a request that never landed. The
        // server still enforces its own limits and answers with the generic copy; refusing here on
        // a guessed default would refuse batches the server accepts.
        expect(refuseOverCeiling(filesOf(500, 50 * MB), null)).toBeUndefined();
        expect(refuseOverCeiling(filesOf(500, 50 * MB), {})).toBeUndefined();
    });

    it('should treat a ceiling of zero or less as no ceiling at all', () => {
        expect(
            refuseOverCeiling(filesOf(500, 50 * MB), { maxFiles: 0, maxTotalBytes: -1 })
        ).toBeUndefined();
    });

    it('should say nothing about an empty selection', () => {
        // Not this function's refusal to make: choosing nothing is not a batch over a ceiling, and
        // the flows above it already do nothing with an empty selection.
        expect(refuseOverCeiling([], { maxFiles: 100, maxTotalBytes: 1024 * MB })).toBeUndefined();
    });
});
