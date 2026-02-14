import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withComputed, withMethods, withState } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { map, switchMap, tap } from 'rxjs/operators';

import { DotContentletLockerService, DotLanguagesService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotCMSPageAsset } from '@dotcms/types';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { ToggleLockOptions } from '../../../shared/models';
import { computeIsPageLocked } from '../../../utils';
import { UVEState } from '../../models';

import type { PageComputed } from '../page/withPage';

interface WithWorkflowState {
    workflowActions: DotCMSWorkflowAction[];
    workflowIsLoading: boolean;
    workflowLockIsLoading: boolean;
}

export interface WorkflowComputed {
    workflowIsPageLocked: Signal<boolean>;
    systemIsLockFeatureEnabled: Signal<boolean>;
    $workflowLockOptions: Signal<ToggleLockOptions | null>;
}

/**
 * Interface defining the methods provided by withWorkflow
 * Use this as props type in dependent features
 */
export interface WithWorkflowMethods extends WorkflowComputed {
    workflowFetch: RxMethod<string>;
    setWorkflowActionLoading: (workflowIsLoading: boolean) => void;
    workflowToggleLock: (inode: string, isLocked: boolean, isLockedByCurrentUser: boolean, lockedBy?: string) => void;
}

/**
 * Workflow and lock management feature
 *
 * Responsibilities:
 * - Fetch workflow actions for the current page
 * - Manage page lock/unlock operations
 * - Provide lock state computeds (isPageLocked, lock options)
 * - Support toggle lock feature flag
 *
 * Note: pageReload() is accessed via type assertion because withWorkflow is composed
 * BEFORE withPageApi in the store, so TypeScript cannot guarantee the method exists
 * at composition time. The runtime access is safe because methods are called after
 * full store initialization.
 */
export function withWorkflow() {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageComputed>()
        },
        withState<WithWorkflowState>({
            workflowActions: [],
            workflowIsLoading: true,
            workflowLockIsLoading: false
        }),
        withComputed((store) => {
            const workflowIsPageLocked = computed(() => {
                return computeIsPageLocked(store.pageAsset()?.page ?? null, store.uveCurrentUser());
            });

            const systemIsLockFeatureEnabled = computed(() =>
                store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK
            );

            const $workflowLockOptions = computed<ToggleLockOptions | null>(() => {
                const page = store.pageAsset()?.page;
                const user = store.uveCurrentUser();

                if (!page) {
                    return null;
                }

                const isLocked = Boolean(page.locked);
                const lockedByUserId = page.lockedBy ?? '';
                const isLockedByCurrentUser = isLocked && lockedByUserId === user?.userId;
                const lockedByName = page.lockedByName ?? '';
                // Some page responses omit `canLock` entirely; allow attempting lock/unlock and
                // let backend authorization be the final source of truth.
                const canLock = page.canLock ?? true;

                return {
                    inode: page.inode,
                    isLocked,
                    isLockedByCurrentUser,
                    canLock,
                    lockedBy: lockedByName
                };
            });

            return {
                workflowIsPageLocked,
                systemIsLockFeatureEnabled,
                $workflowLockOptions
            } satisfies WorkflowComputed;
        }),
        withMethods((store) => {
            const dotWorkflowsActionsService = inject(DotWorkflowsActionsService);
            const dotContentletLockerService = inject(DotContentletLockerService);
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const pageStore = store as typeof store & {
                requestMetadata: () => { query: string; variables: Record<string, string> } | null;
                $requestWithParams: () => { query: string; variables: Record<string, string> } | null;
                setPageAssetResponse: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
            };

            const reloadPageAfterLockChange = () => {
                const params = store.pageParams();

                if (!params) {
                    patchState(store, { workflowLockIsLoading: false });
                    return;
                }

                patchState(store, { uveStatus: UVE_STATUS.LOADING });

                const requestWithParams = pageStore.$requestWithParams?.();
                const requestMetadata = pageStore.requestMetadata?.();
                const pageRequest = !requestMetadata || !requestWithParams
                    ? dotPageApiService.get(params).pipe(
                        map((pageAsset) => ({ pageAsset }))
                    )
                    : dotPageApiService.getGraphQLPage(requestWithParams);

                pageRequest.subscribe({
                    next: (response) => {
                        const pageResponse = 'pageAsset' in response ? response : { pageAsset: response };

                        pageStore.setPageAssetResponse(pageResponse);

                        dotLanguagesService.getLanguagesUsedPage(pageResponse.pageAsset.page.identifier).subscribe({
                            next: (languages) => {
                                patchState(store, {
                                    pageLanguages: languages,
                                    uveStatus: UVE_STATUS.LOADED,
                                    workflowLockIsLoading: false
                                });
                            },
                            error: ({ status: errorStatus }: HttpErrorResponse) => {
                                patchState(store, {
                                    pageErrorCode: errorStatus,
                                    uveStatus: UVE_STATUS.ERROR,
                                    workflowLockIsLoading: false
                                });
                            }
                        });
                    },
                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                        patchState(store, {
                            pageErrorCode: errorStatus,
                            uveStatus: UVE_STATUS.ERROR,
                            workflowLockIsLoading: false
                        });
                    }
                });
            };

            const lockPage = (inode: string) => {
                patchState(store, { workflowLockIsLoading: true });

                dotContentletLockerService.lock(inode).subscribe({
                    next: () => {
                        patchState(store, { editorActiveContentlet: null });
                        reloadPageAfterLockChange();
                    },
                    error: () => {
                        patchState(store, { workflowLockIsLoading: false });
                    }
                });
            };

            const unlockPage = (inode: string) => {
                patchState(store, { workflowLockIsLoading: true });

                dotContentletLockerService.unlock(inode).subscribe({
                    next: () => {
                        patchState(store, { editorActiveContentlet: null });
                        reloadPageAfterLockChange();
                    },
                    error: () => {
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
                 * Toggle page lock/unlock
                 */
                workflowToggleLock(
                    inode: string,
                    isLocked: boolean,
                    isLockedByCurrentUser: boolean,
                    lockedBy?: string
                ) {
                    if (isLocked && !isLockedByCurrentUser) {
                        void lockedBy; // kept for method signature compatibility
                        unlockPage(inode);
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
