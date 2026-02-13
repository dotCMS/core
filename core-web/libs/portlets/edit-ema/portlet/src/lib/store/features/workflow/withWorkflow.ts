import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withComputed, withMethods, withState } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';


import { DotContentletLockerService, DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { UVE_STATUS } from '../../../shared/enums';
import { computeIsPageLocked } from '../../../utils';
import { UVEState } from '../../models';

import type { PageAssetComputed } from '../page/withPage';

interface WithWorkflowState {
    workflowActions: DotCMSWorkflowAction[];
    workflowIsLoading: boolean;
    workflowLockIsLoading: boolean;
}

/**
 * Lock-related UI options and types
 */
export interface UnlockOptions {
    inode: string;
    loading: boolean;
}

export interface ToggleLockOptions {
    inode: string;
    isLocked: boolean;
    isLockedByCurrentUser: boolean;
    canLock: boolean;
    lockedBy: string | null;
}

export interface WorkflowComputed {
    workflowIsPageLocked: Signal<boolean>;
    systemIsLockFeatureEnabled: Signal<boolean>;

    $unlockButton: Signal<UnlockOptions | null>;
    $workflowLockOptions: Signal<ToggleLockOptions | null>;
}

/**
 * Interface defining the methods provided by withWorkflow
 * Use this as props type in dependent features
 *
 * @export
 * @interface WithWorkflowMethods
 */
export interface WithWorkflowMethods extends WorkflowComputed {
    // Methods
    workflowFetch: RxMethod<string>;
    setWorkflowActionLoading: (workflowIsLoading: boolean) => void;
    workflowToggleLock: (inode: string, isLocked: boolean, isLockedByCurrentUser: boolean, lockedBy?: string) => void;
}

/**
 * Workflow dependencies from other features
 * No dependencies needed - pageReload accessed directly from store at runtime
 */
export interface WithWorkflowDeps {
    // No dependencies - avoiding circular dependency issues
}

/**
 * Workflow and lock management.
 * @export
 * @return {*}
 */
export function withWorkflow(_deps?: WithWorkflowDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageAssetComputed>()
        },
        withState<WithWorkflowState>({
            workflowActions: [],
            workflowIsLoading: true,
            workflowLockIsLoading: false
        }),
        withComputed((store) => {
            const workflowIsPageLocked = computed(() => {
                return computeIsPageLocked(store.pageData(), store.uveCurrentUser());
            });

            const systemIsLockFeatureEnabled = computed(() =>
                store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK
            );

            const $unlockButton = computed<UnlockOptions | null>(() => {
                const page = store.pageData();
                const isLocked = workflowIsPageLocked();

                if (!page || !isLocked) {
                    return null;
                }

                return {
                    inode: page.inode,
                    loading: store.workflowLockIsLoading()
                };
            });

            const $workflowLockOptions = computed<ToggleLockOptions | null>(() => {
                const page = store.pageData();
                const user = store.uveCurrentUser();

                if (!page) {
                    return null;
                }

                const isLocked = page.locked;
                const isLockedByCurrentUser = page.lockedBy === user?.userId;

                return {
                    inode: page.inode,
                    isLocked,
                    isLockedByCurrentUser,
                    canLock: page.canLock ?? false,
                    lockedBy: page.lockedByName
                };
            });

            return {
                workflowIsPageLocked,
                systemIsLockFeatureEnabled,
                $unlockButton,
                $workflowLockOptions
            } satisfies WorkflowComputed;
        }),
        withMethods((store) => {
            const dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
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
                        (store as any).pageReload?.();
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
                        (store as any).pageReload?.();
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
                /**
                 * Load workflow actions
                 */
                workflowFetch: rxMethod<string>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                workflowIsLoading: true
                            });
                        }),
                        switchMap((pageInode) => {
                            return dotWorkflowsActionsService.getByInode(pageInode).pipe(
                                tapResponse({
                                    next: (workflowActions = []) => {
                                        patchState(store, {
                                            workflowActions,
                                            workflowIsLoading: false
                                        });
                                    },
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            pageErrorCode: errorStatus,
                                            uveStatus: UVE_STATUS.ERROR
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),
                setWorkflowActionLoading: (workflowIsLoading: boolean) => {
                    patchState(store, { workflowIsLoading });
                },
                /**
                 * Toggle page lock/unlock with confirmation dialog
                 */
                workflowToggleLock(
                    inode: string,
                    isLocked: boolean,
                    isLockedByCurrentUser: boolean,
                    lockedBy?: string
                ) {
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
