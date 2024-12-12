import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { computeCanEditPage, computePageIsLocked, isForwardOrPage } from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';

/**
 * Add load and reload method to the store
 *
 * @export
 * @return {*}
 */
export function withLoad() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withClient(),
        withMethods((store) => {
            const router = inject(Router);
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotLicenseService = inject(DotLicenseService);
            const dotExperimentsService = inject(DotExperimentsService);
            const loginService = inject(LoginService);
            const dotWorkflowsActionsService = inject(DotWorkflowsActionsService);

            return {
                /**
                 * Fetches the page asset based on the provided page parameters.
                 * This method updates the user permissions, pageAsset, and pageParams.
                 *
                 * @param {DotPageApiParams} pageParams - The parameters used to fetch the page asset.
                 * @memberof DotEmaShellComponent
                 */
                loadPageAsset: rxMethod<Partial<DotPageApiParams>>(
                    pipe(
                        map((params) => {
                            if (!store.pageParams()) {
                                return params as DotPageApiParams;
                            }

                            return {
                                ...store.pageParams(),
                                ...params
                            };
                        }),
                        tap((pageParams) => {
                            store.resetClientConfiguration();
                            patchState(store, {
                                status: UVE_STATUS.LOADING,
                                isClientReady: false,
                                pageParams
                            });
                        }),
                        switchMap((pageParams) => {
                            return forkJoin({
                                pageAsset: dotPageApiService.get(pageParams).pipe(
                                    // This logic should be handled in the Shell component using an effect
                                    switchMap((pageAsset) => {
                                        const { vanityUrl } = pageAsset;

                                        // If there is no vanity and is not a redirect we just return the pageAPI response
                                        if (isForwardOrPage(vanityUrl)) {
                                            return of(pageAsset);
                                        }

                                        const queryParams = {
                                            ...pageParams,
                                            url: vanityUrl.forwardTo.replace('/', '')
                                        };

                                        // Will trigger full editor page Reload
                                        router.navigate([], {
                                            queryParams,
                                            queryParamsHandling: 'merge'
                                        });

                                        // EMPTY is a simple Observable that only emits the complete notification.
                                        return EMPTY;
                                    })
                                ),
                                // This can be done in the Withhook: onInit if this ticket is done: https://github.com/dotCMS/core/issues/30760
                                // Reference: https://ngrx.io/guide/signals/signal-store/lifecycle-hooks
                                isEnterprise: dotLicenseService
                                    .isEnterprise()
                                    .pipe(take(1), shareReplay()),
                                currentUser: loginService.getCurrentUser()
                            }).pipe(
                                tap({
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            errorCode: errorStatus,
                                            status: UVE_STATUS.ERROR
                                        });
                                    }
                                }),
                                switchMap(({ pageAsset, isEnterprise, currentUser }) => {
                                    const experimentId =
                                        pageParams?.experimentId ?? pageAsset?.runningExperimentId;
                                    const inode = pageAsset?.page?.inode;

                                    return forkJoin({
                                        experiment: dotExperimentsService.getById(
                                            experimentId ?? DEFAULT_VARIANT_ID
                                        ),
                                        languages: dotLanguagesService.getLanguagesUsedPage(
                                            pageAsset.page.identifier
                                        ),
                                        workflowActions:
                                            dotWorkflowsActionsService.getByInode(inode)
                                    }).pipe(
                                        tap({
                                            next: ({
                                                experiment,
                                                languages,
                                                workflowActions = []
                                            }) => {
                                                const canEditPage = computeCanEditPage(
                                                    pageAsset?.page,
                                                    currentUser,
                                                    experiment
                                                );

                                                const pageIsLocked = computePageIsLocked(
                                                    pageAsset?.page,
                                                    currentUser
                                                );

                                                const isPreview = pageParams.preview === 'true';
                                                const isTraditionalPage = !pageParams.clientHost;
                                                const isClientReady =
                                                    isTraditionalPage || isPreview;

                                                patchState(store, {
                                                    pageAPIResponse: pageAsset,
                                                    isEnterprise,
                                                    currentUser,
                                                    experiment,
                                                    languages,
                                                    canEditPage,
                                                    pageIsLocked,
                                                    isClientReady,
                                                    isTraditionalPage,
                                                    status: UVE_STATUS.LOADED,
                                                    workflowActions
                                                });
                                            },
                                            error: ({ status: errorStatus }: HttpErrorResponse) => {
                                                patchState(store, {
                                                    errorCode: errorStatus,
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        })
                                    );
                                })
                            );
                        })
                    )
                ),
                /**
                 * Reloads the current page to bring the latest content.
                 * Called this method when after changes, updates, or saves.
                 *
                 * @param {Partial<DotPageApiParams>} params - The parameters used to fetch the page asset.
                 * @memberof DotEmaShellComponent
                 */
                reloadCurrentPage: rxMethod<Pick<UVEState, 'isClientReady'> | void>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((partialState: Pick<UVEState, 'isClientReady'>) => {
                            return dotPageApiService
                                .getClientPage(store.pageParams(), store.clientRequestProps())
                                .pipe(
                                    switchMap((pageAPIResponse) => {
                                        return forkJoin({
                                            pageAPIResponse: of(pageAPIResponse),
                                            languages: dotLanguagesService.getLanguagesUsedPage(
                                                pageAPIResponse.page.identifier
                                            ),
                                            workflowActions: dotWorkflowsActionsService.getByInode(
                                                pageAPIResponse.page.inode
                                            )
                                        });
                                    }),
                                    tapResponse({
                                        next: ({
                                            pageAPIResponse,
                                            languages,
                                            workflowActions = []
                                        }) => {
                                            const canEditPage = computeCanEditPage(
                                                pageAPIResponse?.page,
                                                store.currentUser(),
                                                store.experiment()
                                            );

                                            const pageIsLocked = computePageIsLocked(
                                                pageAPIResponse?.page,
                                                store.currentUser()
                                            );

                                            patchState(store, {
                                                pageAPIResponse,
                                                languages,
                                                canEditPage,
                                                pageIsLocked,
                                                status: UVE_STATUS.LOADED,
                                                isClientReady: partialState?.isClientReady ?? true,
                                                workflowActions
                                            });
                                        },
                                        error: ({ status: errorStatus }: HttpErrorResponse) => {
                                            patchState(store, {
                                                errorCode: errorStatus,
                                                status: UVE_STATUS.ERROR
                                            });
                                        }
                                    })
                                );
                        })
                    )
                ),
                reloadWorkflowActions: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            const inode = store.pageAPIResponse()?.page.inode;

                            return dotWorkflowsActionsService.getByInode(inode).pipe(
                                tapResponse({
                                    next: (workflowActions = []) => {
                                        patchState(store, {
                                            workflowActions
                                        });
                                    },
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            errorCode: errorStatus,
                                            status: UVE_STATUS.ERROR
                                        });
                                    }
                                })
                            );
                        })
                    )
                )
            };
        })
    );
}
