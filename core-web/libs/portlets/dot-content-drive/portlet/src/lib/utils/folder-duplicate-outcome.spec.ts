import { describe, expect, it } from 'vitest';

import {
    DOT_FOLDER_BULK_DUPLICATE_REASONS,
    DotBatchItemResult,
    DotFolderBulkDuplicateReason
} from '@dotcms/dotcms-models';

import {
    describeFolderDuplicateOutcome,
    messageKeyForFolderDuplicateReason,
    SKIPPED_BY_PARENT_KEY,
    SKIPPED_CANCELLED_KEY
} from './folder-duplicate-outcome';

import { MAX_FOLDER_NAMES } from '../shared/constants';

/**
 * Reason → copy for bulk folder duplication (#37062 US2).
 *
 * Reads exactly like bulk folder delete's report (developer decision, 2026-09-28): the same line
 * builder, the same name threshold and the same count-only overflow. Only the reasons and the words
 * are duplication's own, because delete's copy says a folder was removed and a duplicated folder is
 * untouched.
 */
describe('folder-duplicate-outcome', () => {
    const resolve = (key: string, ...args: string[]) =>
        args.length ? `${key}(${args.join('|')})` : key;

    const failure = (
        key: string,
        reason?: string
    ): DotBatchItemResult<DotFolderBulkDuplicateReason> =>
        ({ key, status: 'FAILED', reason }) as DotBatchItemResult<DotFolderBulkDuplicateReason>;

    const skipped = (
        key: string,
        reason?: DotFolderBulkDuplicateReason
    ): DotBatchItemResult<DotFolderBulkDuplicateReason> =>
        ({ key, status: 'SKIPPED', reason }) as DotBatchItemResult<DotFolderBulkDuplicateReason>;

    describe('messageKeyForFolderDuplicateReason', () => {
        it('should give every reason the contract can return its own duplication copy', () => {
            const keys = DOT_FOLDER_BULK_DUPLICATE_REASONS.map(messageKeyForFolderDuplicateReason);

            expect(new Set(keys).size).toBe(DOT_FOLDER_BULK_DUPLICATE_REASONS.length);
            expect(keys.every((key) => key.startsWith('content-drive.duplicate.failure.'))).toBe(
                true
            );
        });

        it('should tell no rights on the folder apart from no rights to add to its parent', () => {
            // The two send the author to different places: the folder's own permissions, or the
            // folder it sits in.
            expect(messageKeyForFolderDuplicateReason('PERMISSION_DENIED')).not.toBe(
                messageKeyForFolderDuplicateReason('PARENT_PERMISSION_DENIED')
            );
        });

        it('should fall back to unclassified for a reason it does not recognise', () => {
            expect(messageKeyForFolderDuplicateReason('SOMETHING_NEW')).toBe(
                messageKeyForFolderDuplicateReason('UNCLASSIFIED')
            );
        });

        it('should fall back to unclassified when there is no reason at all', () => {
            expect(messageKeyForFolderDuplicateReason(undefined)).toBe(
                messageKeyForFolderDuplicateReason('UNCLASSIFIED')
            );
        });

        it('should not resolve an inherited property as a reason', () => {
            expect(messageKeyForFolderDuplicateReason('constructor')).toBe(
                messageKeyForFolderDuplicateReason('UNCLASSIFIED')
            );
        });

        it("should not borrow delete's copy for any reason", () => {
            expect(
                DOT_FOLDER_BULK_DUPLICATE_REASONS.map(messageKeyForFolderDuplicateReason).some(
                    (key) => key.includes('.delete.')
                )
            ).toBe(false);
        });
    });

    describe('describeFolderDuplicateOutcome', () => {
        it('should name the folders that failed, with their reason', () => {
            const lines = describeFolderDuplicateOutcome(
                [
                    failure('//d/a/', 'PERMISSION_DENIED'),
                    failure('//d/b/', 'PARENT_PERMISSION_DENIED')
                ],
                resolve
            );

            expect(lines).toEqual([
                `${messageKeyForFolderDuplicateReason('PERMISSION_DENIED')}(//d/a/)`,
                `${messageKeyForFolderDuplicateReason('PARENT_PERMISSION_DENIED')}(//d/b/)`
            ]);
        });

        it('should group folders that failed for the same reason onto one line', () => {
            const lines = describeFolderDuplicateOutcome(
                [
                    failure('//d/a/', 'PATH_NOT_FOUND'),
                    failure('//d/b/', 'PATH_NOT_FOUND'),
                    failure('//d/c/', 'PROTECTED_FOLDER')
                ],
                resolve
            );

            expect(lines.length).toBe(2);
            expect(lines[0]).toContain('//d/a/, //d/b/');
        });

        it('should still name a folder whose reason it does not recognise', () => {
            const lines = describeFolderDuplicateOutcome(
                [failure('//d/a/', 'SOMETHING_NEW')],
                resolve
            );

            expect(lines).toEqual([
                `${messageKeyForFolderDuplicateReason('UNCLASSIFIED')}(//d/a/)`
            ]);
        });

        it('should count rather than name once there are too many to read, as delete does', () => {
            const many = Array.from({ length: MAX_FOLDER_NAMES + 3 }, (_, i) =>
                failure(`//d/f-${i}/`, 'PERMISSION_DENIED')
            );

            const lines = describeFolderDuplicateOutcome(many, resolve);

            expect(lines).toEqual([
                `${messageKeyForFolderDuplicateReason('PERMISSION_DENIED')}-many(${MAX_FOLDER_NAMES + 3})`
            ]);
        });

        it('should never render the server’s diagnostic message', () => {
            const withMessage = {
                key: '//d/a/',
                status: 'FAILED',
                reason: 'UNCLASSIFIED',
                message: 'java.lang.IllegalStateException: for the log only'
            } as DotBatchItemResult<DotFolderBulkDuplicateReason>;

            const lines = describeFolderDuplicateOutcome([withMessage], resolve);

            expect(lines.join(' ')).not.toContain('IllegalStateException');
        });

        it('should tell a folder its selected parent covered from one the run never reached', () => {
            const lines = describeFolderDuplicateOutcome(
                [skipped('//d/a/child/', 'COVERED_BY_PARENT'), skipped('//d/b/')],
                resolve
            );

            expect(lines).toEqual([
                `${SKIPPED_BY_PARENT_KEY}(//d/a/child/)`,
                `${SKIPPED_CANCELLED_KEY}(//d/b/)`
            ]);
        });

        it('should use duplication’s own skip copy, not delete’s', () => {
            // Delete says an ancestor removed the folder. Here the folder is untouched: the parent's
            // duplicate already carries a copy of it.
            expect(SKIPPED_BY_PARENT_KEY).toContain('.duplicate.');
            expect(SKIPPED_CANCELLED_KEY).toContain('.duplicate.');
        });

        it('should not read a folder its parent covered as a failure', () => {
            const lines = describeFolderDuplicateOutcome(
                [skipped('//d/a/', 'COVERED_BY_PARENT')],
                resolve
            );

            expect(lines.join(' ')).not.toContain('.failure.');
        });

        it('should say nothing at all for a clean run', () => {
            expect(describeFolderDuplicateOutcome([], resolve)).toEqual([]);
            expect(
                describeFolderDuplicateOutcome(
                    [
                        {
                            key: '//d/a/',
                            status: 'SUCCESS'
                        } as DotBatchItemResult<DotFolderBulkDuplicateReason>
                    ],
                    resolve
                )
            ).toEqual([]);
        });
    });
});
