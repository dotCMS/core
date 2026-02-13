import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotContentletLockerService, DotMessageService } from '@dotcms/data-access';

import { UVEState } from '../../models';

interface WithLockState {
    workflowLockIsLoading: boolean;
}

export interface WithLockDeps {
    pageReload: () => void;
}

export function withLock(deps: WithLockDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<WithLockState>({
            workflowLockIsLoading: false
        }),
        withMethods((store) => {
            const messageService = inject(MessageService);
            const dotMessageService = inject(DotMessageService);
            const dotContentletLockerService = inject(DotContentletLockerService);
            const confirmationService = inject(ConfirmationService);

            const lockPage = (inode: string) => {
                patchState(store, { workflowLockIsLoading: true });

                dotContentletLockerService.lock(inode).subscribe({
                    next: () => {
                        messageService.add({
                            severity: 'success',
                            summary: dotMessageService.get('edit.ema.page.lock'),
                            detail: dotMessageService.get('edit.ema.page.lock.success')
                        });
                        patchState(store, {
                            editorActiveContentlet: null,
                            workflowLockIsLoading: false
                        });
                        deps.pageReload();
                    },
                    error: () => {
                        messageService.add({
                            severity: 'error',
                            summary: dotMessageService.get('edit.ema.page.lock'),
                            detail: dotMessageService.get('edit.ema.page.lock.error')
                        });
                        patchState(store, { workflowLockIsLoading: false });
                    }
                });
            };

            /**
             * Internal method to unlock a page
             */
            const unlockPage = (inode: string) => {
                patchState(store, { workflowLockIsLoading: true });

                dotContentletLockerService.unlock(inode).subscribe({
                    next: () => {
                        messageService.add({
                            severity: 'success',
                            summary: dotMessageService.get('edit.ema.page.unlock'),
                            detail: dotMessageService.get('edit.ema.page.unlock.success')
                        });
                        patchState(store, {
                            editorActiveContentlet: null,
                            workflowLockIsLoading: false
                        });
                        deps.pageReload();
                    },
                    error: () => {
                        messageService.add({
                            severity: 'error',
                            summary: dotMessageService.get('edit.ema.page.unlock'),
                            detail: dotMessageService.get('edit.ema.page.unlock.error')
                        });
                        patchState(store, { workflowLockIsLoading: false });
                    }
                });
            };

            return {
                workflowToggleLock(inode: string, isLocked: boolean, isLockedByCurrentUser: boolean, lockedBy?: string) {
                    if (store.workflowLockIsLoading()) {
                        return;
                    }

                    if (isLocked && !isLockedByCurrentUser) {

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
