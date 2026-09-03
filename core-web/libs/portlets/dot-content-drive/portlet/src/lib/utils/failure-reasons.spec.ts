import { describe, expect, it } from '@jest/globals';

import {
    CONTENT_DRIVE_FAILURE_REASONS,
    DotContentDriveFailureReason,
    messageKeyForFailureReason
} from './failure-reasons';

/**
 * FR-036: every failure reason the server can return must have its own product copy. A reason with
 * no copy is a hole the author sees, so this file's job is to make an unmapped reason impossible to
 * ship rather than to check a handful of examples.
 *
 * **Why the reason set is ours and not the server's.** The wire codes are part of the submission
 * contract, which is not settled (see `contracts/client-requirements.md`, open item 2). This maps a
 * *client-side* union to message keys, which needs no contract at all — which is why the copy work
 * can proceed while that conversation is still open. Translating whatever strings the server
 * actually sends into this union is a separate, tiny step that belongs in Phase 7, once the
 * contract exists.
 */
describe('messageKeyForFailureReason', () => {
    it('should cover every reason in the closed set, with no gaps', () => {
        // The point of the whole file. Adding a seventh reason without copy fails here rather than
        // rendering a blank to an author.
        const unmapped = CONTENT_DRIVE_FAILURE_REASONS.filter(
            (reason) => !messageKeyForFailureReason(reason)
        );

        expect(unmapped).toEqual([]);
    });

    it('should give each reason its own distinct copy', () => {
        // Two reasons sharing a key means one of them is being explained by the wrong sentence.
        const keys = CONTENT_DRIVE_FAILURE_REASONS.map(messageKeyForFailureReason);

        expect(new Set(keys).size).toBe(CONTENT_DRIVE_FAILURE_REASONS.length);
    });

    it.each([
        ['OVER_SIZE_LIMIT'],
        ['DISALLOWED_TYPE'],
        ['NAME_COLLISION'],
        ['PERMISSION_DENIED'],
        ['STAGED_CONTENT_UNAVAILABLE'],
        ['UNCLASSIFIED']
    ] as [DotContentDriveFailureReason][])('should resolve a key for %s', (reason) => {
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
            expect(messageKeyForFailureReason('DISALLOWED_TYPE')).not.toContain('extension');
        });
    });
});
