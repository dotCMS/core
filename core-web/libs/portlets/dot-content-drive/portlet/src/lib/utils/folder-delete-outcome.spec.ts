import { describe, expect, it } from 'vitest';

import { DotBatchItemResult, DotFolderDeleteFailureReason } from '@dotcms/dotcms-models';

import {
    describeFolderDeleteOutcome,
    MAX_FOLDER_NAMES,
    messageKeyForFolderDeleteReason,
    SKIPPED_BY_PARENT_KEY,
    SKIPPED_CANCELLED_KEY
} from './folder-delete-outcome';

/**
 * Reason → copy for bulk folder delete (#37063 US4).
 *
 * Mirrors `failure-reasons.ts`, which does the same job for upload, and for the same reason: a
 * reason with no copy is a hole the author sees. Written once here because folder copy (#37062) and
 * move (#37165) consume the same outcome shape and the same vocabulary.
 */
describe('folder-delete-outcome', () => {
    const resolve = (key: string, ...args: string[]) =>
        args.length ? `${key}(${args.join('|')})` : key;

    const failure = (
        key: string,
        reason: DotFolderDeleteFailureReason
    ): DotBatchItemResult<DotFolderDeleteFailureReason> =>
        ({ key, status: 'FAILED', reason }) as DotBatchItemResult<DotFolderDeleteFailureReason>;

    const skipped = (
        key: string,
        reason?: DotFolderDeleteFailureReason
    ): DotBatchItemResult<DotFolderDeleteFailureReason> =>
        ({ key, status: 'SKIPPED', reason }) as DotBatchItemResult<DotFolderDeleteFailureReason>;

    describe('messageKeyForFolderDeleteReason', () => {
        it('should give every reason the contract can return its own copy', () => {
            const reasons: DotFolderDeleteFailureReason[] = [
                'PERMISSION_DENIED',
                'PATH_NOT_FOUND',
                'PROTECTED_FOLDER',
                'IN_USE',
                'COVERED_BY_PARENT',
                'UNCLASSIFIED'
            ];

            const keys = reasons.map(messageKeyForFolderDeleteReason);

            // Distinct: two reasons sharing a sentence sends half the authors to fix the wrong
            // thing. `PATH_NOT_FOUND` and `PERMISSION_DENIED` are the pair most tempting to merge,
            // and the most misleading if merged.
            expect(new Set(keys).size).toBe(reasons.length);
        });

        it('should fall back to unclassified for a reason it does not recognise', () => {
            // A reason the server added before the client learned about it. A raw code or a blank
            // would tell the author nothing; the folder is still named and still reported as failed
            // (FR-030).
            expect(messageKeyForFolderDeleteReason('SOMETHING_NEW')).toBe(
                messageKeyForFolderDeleteReason('UNCLASSIFIED')
            );
        });

        it('should fall back to unclassified when there is no reason at all', () => {
            expect(messageKeyForFolderDeleteReason(undefined)).toBe(
                messageKeyForFolderDeleteReason('UNCLASSIFIED')
            );
        });

        it('should not resolve an inherited property as a reason', () => {
            // `in` walks the prototype chain, so a reason of `constructor` would pass a naive guard
            // and hand an inherited *function* straight to the message service.
            expect(messageKeyForFolderDeleteReason('constructor')).toBe(
                messageKeyForFolderDeleteReason('UNCLASSIFIED')
            );
        });
    });

    describe('describeFolderDeleteOutcome', () => {
        it('should name the folders that failed, with their reason', () => {
            const lines = describeFolderDeleteOutcome(
                [failure('//d/a/', 'PERMISSION_DENIED'), failure('//d/b/', 'IN_USE')],
                resolve
            );

            expect(lines.join(' ')).toContain('//d/a/');
            expect(lines.join(' ')).toContain('//d/b/');
        });

        it('should group folders that failed for the same reason onto one line', () => {
            // Three lines saying "no permission" tell an author nothing three times.
            const lines = describeFolderDeleteOutcome(
                [
                    failure('//d/a/', 'PERMISSION_DENIED'),
                    failure('//d/b/', 'PERMISSION_DENIED'),
                    failure('//d/c/', 'IN_USE')
                ],
                resolve
            );

            expect(lines.length).toBe(2);
        });

        it('should count rather than name once there are too many to read', () => {
            // Naming the first eight of fifty reads as "eight folders failed", which is worse than
            // saying nothing. The full list lives in the durable record (FR-027a).
            const many = Array.from({ length: MAX_FOLDER_NAMES + 3 }, (_, i) =>
                failure(`//d/f-${i}/`, 'PERMISSION_DENIED')
            );

            const lines = describeFolderDeleteOutcome(many, resolve);

            expect(lines.join(' ')).toContain(String(MAX_FOLDER_NAMES + 3));
            expect(lines.join(' ')).not.toContain('//d/f-0/');
        });

        it('should never render the server’s diagnostic message', () => {
            // It is written for a log (CR-04). Everything the author reads comes from the reason.
            const withMessage = {
                key: '//d/a/',
                status: 'FAILED',
                reason: 'IN_USE',
                message: 'java.lang.IllegalStateException: locked by user 42'
            } as DotBatchItemResult<DotFolderDeleteFailureReason>;

            const lines = describeFolderDeleteOutcome([withMessage], resolve);

            expect(lines.join(' ')).not.toContain('IllegalStateException');
            expect(lines.join(' ')).not.toContain('user 42');
        });

        it('should tell the two kinds of skip apart', () => {
            // Both are "not attempted", for entirely different reasons. One sentence cannot serve
            // both: a folder an ancestor already removed is not a problem, and a folder the run
            // never reached is (FR-031).
            const lines = describeFolderDeleteOutcome(
                [skipped('//d/a/', 'COVERED_BY_PARENT'), skipped('//d/b/')],
                resolve
            );

            const text = lines.join(' ');

            expect(text).toContain(SKIPPED_BY_PARENT_KEY);
            expect(text).toContain(SKIPPED_CANCELLED_KEY);
        });

        it('should not read a folder removed by its parent as a failure', () => {
            const lines = describeFolderDeleteOutcome(
                [skipped('//d/a/', 'COVERED_BY_PARENT')],
                resolve
            );

            expect(lines.join(' ')).not.toContain(
                messageKeyForFolderDeleteReason('COVERED_BY_PARENT')
            );
        });

        it('should say nothing at all for a clean run', () => {
            expect(describeFolderDeleteOutcome([], resolve)).toEqual([]);
            expect(
                describeFolderDeleteOutcome(
                    [
                        {
                            key: '//d/a/',
                            status: 'SUCCESS'
                        } as DotBatchItemResult<DotFolderDeleteFailureReason>
                    ],
                    resolve
                )
            ).toEqual([]);
        });
    });
});
