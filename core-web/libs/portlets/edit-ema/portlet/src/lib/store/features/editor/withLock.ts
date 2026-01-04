import { signalStoreFeature, type, withMethods, withState, patchState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotContentletLockerService, DotMessageService } from '@dotcms/data-access';

import { UVEState } from '../../models';

interface WithLockState {
    lockLoading: boolean;
}

/**
 * Dependencies interface for withLock
 * These are methods from other features that withLock needs
 */
export interface WithLockDeps {
    reloadCurrentPage: () => void;
}

/**
 * Signal store feature that adds lock functionality to the UVE store.
 * Provides methods to lock/unlock pages and handles loading states and user notifications.
 *
 * Dependencies: Requires methods from withLoad
 * Pass these via the deps parameter when wrapping with withFeature
 *
 * @export
 * @param deps - Dependencies from other features (provided by withFeature wrapper)
 * @return {*}
 */
export function withLock(deps: WithLockDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<WithLockState>({
            lockLoading: false
        }),
        withMethods((store) => {
            const messageService = inject(MessageService);
            const dotMessageService = inject(DotMessageService);
            const dotContentletLockerService = inject(DotContentletLockerService);
            const confirmationService = inject(ConfirmationService);

            /**
             * Internal method to lock a page
             */
            const lockPage = (inode: string) => {
                patchState(store, { lockLoading: true });

                dotContentletLockerService.lock(inode).subscribe({
                    next: () => {
                        messageService.add({
                            severity: 'success',
                            summary: dotMessageService.get('edit.ema.page.lock'),
                            detail: dotMessageService.get('edit.ema.page.lock.success')
                        });
                        deps.reloadCurrentPage();
                        patchState(store, { lockLoading: false });
                    },
                    error: () => {
                        messageService.add({
                            severity: 'error',
                            summary: dotMessageService.get('edit.ema.page.lock'),
                            detail: dotMessageService.get('edit.ema.page.lock.error')
                        });
                        patchState(store, { lockLoading: false });
                    }
                });
            };

            /**
             * Internal method to unlock a page
             */
            const unlockPage = (inode: string) => {
                patchState(store, { lockLoading: true });

                dotContentletLockerService.unlock(inode).subscribe({
                    next: () => {
                        messageService.add({
                            severity: 'success',
                            summary: dotMessageService.get('edit.ema.page.unlock'),
                            detail: dotMessageService.get('edit.ema.page.unlock.success')
                        });
                        deps.reloadCurrentPage();
                        patchState(store, { lockLoading: false });
                    },
                    error: () => {
                        messageService.add({
                            severity: 'error',
                            summary: dotMessageService.get('edit.ema.page.unlock'),
                            detail: dotMessageService.get('edit.ema.page.unlock.error')
                        });
                        patchState(store, { lockLoading: false });
                    }
                });
            };

            return {
                /**
                 * Toggle lock/unlock with conditional confirmation
                 * Shows confirmation dialog if page is locked by another user
                 *
                 * @param {string} inode - Page inode
                 * @param {boolean} isLocked - Current lock state
                 * @param {boolean} isLockedByCurrentUser - Whether current user owns the lock
                 */
                toggleLock(inode: string, isLocked: boolean, isLockedByCurrentUser: boolean) {
                    // Prevent multiple simultaneous lock/unlock operations
                    if (store.lockLoading()) {
                        return;
                    }

                    // If page is locked but NOT by current user, show confirmation
                    if (isLocked && !isLockedByCurrentUser) {
                        const lockedBy = store.page().lockedByName;

                        confirmationService.confirm({
                            header: dotMessageService.get('uve.editor.unlock.confirm.header'),
                            message: dotMessageService.get(
                                'uve.editor.unlock.confirm.message',
                                lockedBy
                            ),
                            acceptLabel: dotMessageService.get('uve.editor.unlock.confirm.accept'),
                            rejectLabel: dotMessageService.get('dot.common.dialog.reject'),
                            accept: () => {
                                unlockPage(inode);
                            }
                        });

                        return;
                    }

                    // Otherwise, directly lock or unlock without confirmation
                    if (isLocked) {
                        unlockPage(inode);
                    } else {
                        lockPage(inode);
                    }
                }
            };
        })
    );
}
