import { describe, expect, it } from '@jest/globals';

import { DOT_BULK_UPLOAD_FAILURE_REASONS, DotBulkUploadFailureReason } from '@dotcms/dotcms-models';

import { messageKeyForFailureReason } from './failure-reasons';

/**
 * FR-036: every failure reason the server can return must have its own product copy. A reason with
 * no copy is a hole the author sees, so this file's job is to make an unmapped reason impossible to
 * ship rather than to check a handful of examples.
 *
 * **The reason set is the server's, and this asserts we cover all of it.** The codes are the closed
 * set fixed by the submission contract, so `DOT_BULK_UPLOAD_FAILURE_REASONS` is imported rather
 * than restated here: a set declared twice is a set that can disagree with itself, and the whole
 * risk this file guards is a reason arriving with no copy behind it. Iterating the real vocabulary
 * means adding a member to the contract fails here until someone writes the copy for it.
 */
describe('messageKeyForFailureReason', () => {
    it('should cover every reason in the closed set, with no gaps', () => {
        // The point of the whole file. Adding a seventh reason without copy fails here rather than
        // rendering a blank to an author.
        const unmapped = DOT_BULK_UPLOAD_FAILURE_REASONS.filter(
            (reason) => !messageKeyForFailureReason(reason)
        );

        expect(unmapped).toEqual([]);
    });

    it('should give each reason its own distinct copy', () => {
        // Two reasons sharing a key means one of them is being explained by the wrong sentence.
        const keys = DOT_BULK_UPLOAD_FAILURE_REASONS.map(messageKeyForFailureReason);

        expect(new Set(keys).size).toBe(DOT_BULK_UPLOAD_FAILURE_REASONS.length);
    });

    it.each([
        ['OVER_SIZE_LIMIT'],
        ['DISALLOWED_FILE_TYPE'],
        ['NAME_COLLISION'],
        ['PERMISSION_DENIED'],
        ['STAGED_CONTENT_UNAVAILABLE'],
        ['UNCLASSIFIED']
    ] as [DotBulkUploadFailureReason][])('should resolve a key for %s', (reason) => {
        expect(messageKeyForFailureReason(reason)).toEqual(
            expect.stringContaining('content-drive')
        );
    });

    describe('the unclassified case', () => {
        it('should resolve real copy, not an empty string', () => {
            // FR-036 is explicit that the unclassified branch reads as a real sentence rather than
            // being a dumping ground for reasons nobody wrote copy for.
            expect(messageKeyForFailureReason('UNCLASSIFIED')).toBeTruthy();
        });

        it('should absorb a reason code the client does not know', () => {
            // The server is free to add one before the client learns about it. That must degrade to
            // "something went wrong" rather than to a blank or to the raw code.
            expect(messageKeyForFailureReason('SOMETHING_NEW_FROM_THE_SERVER')).toBe(
                messageKeyForFailureReason('UNCLASSIFIED')
            );
        });

        it('should absorb a missing reason', () => {
            expect(messageKeyForFailureReason(undefined)).toBe(
                messageKeyForFailureReason('UNCLASSIFIED')
            );
        });
    });

    describe('wording constraints from the spec', () => {
        it('should not describe a rejected file by its extension', () => {
            // FR-039: the server resolves the media type by detection and sniffing, not by trusting
            // the file name, so copy that says "extension" describes a check the product does not
            // make. The key itself is named for the concept, so this guards the naming too.
            expect(messageKeyForFailureReason('DISALLOWED_FILE_TYPE')).not.toContain('extension');
        });
    });
});
