import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotPageApiService, UVEPageParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { isForwardOrPage } from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';
import { withWorkflow } from '../workflow/withWorkflow';

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
        withWorkflow(),
        withMethods((store) => {
            const router = inject(Router);
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotExperimentsService = inject(DotExperimentsService);

            return {
                /**
                 * Fetches the page asset based on the provided page parameters.
                 * This method updates the user permissions, pageAsset, and pageParams.
                 *
                 * @param {DotPageApiParams} pageParams - The parameters used to fetch the page asset.
                 * @memberof DotEmaShellComponent
                 */
                loadPageAsset: rxMethod<{ url: string; params?: UVEPageParams }>(
                    pipe(
                        map(({ url, params }) => {
                            return {
                                url,
                                pageParams: {
                                    ...(store.pageParams() || {}),
                                    ...params
                                }
                            };
                        }),
                        tap(({ pageParams }) => {
                            store.resetClientConfiguration();
                            patchState(store, {
                                status: UVE_STATUS.LOADING,
                                isClientReady: false,
                                pageParams
                            });
                        }),
                        switchMap(({ url, pageParams }) =>
                            dotPageApiService.get(url, pageParams).pipe(
                                tap(({ pageAPIResponse }) => {
                                    patchState(store, {
                                        pageAPIResponse,
                                        isClientReady: store.isTraditionalPage()
                                    });
                                }),
                                switchMap(({ pageAPIResponse }) => {
                                    const { vanityUrl, page, runningExperimentId } =
                                        pageAPIResponse;
                                    if (!isForwardOrPage(vanityUrl)) {
                                        router.navigate([], {
                                            queryParamsHandling: 'merge',
                                            queryParams: { url: vanityUrl.forwardTo }
                                        });

                                        return EMPTY;
                                    }

                                    const identifier = page.identifier;
                                    const experimentId =
                                        store.viewParams()?.experimentId ?? runningExperimentId;

                                    return forkJoin({
                                        languages:
                                            dotLanguagesService.getLanguagesUsedPage(identifier),
                                        experiment: dotExperimentsService.getById(
                                            experimentId ?? DEFAULT_VARIANT_ID
                                        )
                                    });
                                }),
                                tap(({ experiment, languages }) => {
                                    patchState(store, {
                                        experiment,
                                        languages,
                                        status: UVE_STATUS.LOADED
                                    });
                                }),
                                // Centralized error handling
                                catchError(({ status: errorStatus }: HttpErrorResponse) => {
                                    patchState(store, {
                                        errorCode: errorStatus,
                                        status: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
                                })
                            )
                        )
                    )
                ),
                /**
                 * Reloads the current page to bring the latest content.
                 * Called this method when after changes, updates, or saves.
                 *
                 * @param {Partial<DotPageApiParams>} params - The parameters used to fetch the page asset.
                 * @memberof DotEmaShellComponent
                 */
                reloadCurrentPage: rxMethod<{
                    params?: Partial<UVEPageParams>;
                    isClientReady?: boolean;
                } | void>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((arg) => {
                            const { params = {}, isClientReady = true } = arg || {};

                            return dotPageApiService
                                .get('/', { ...store.pageParams(), ...params })
                                .pipe(
                                    tap(({ pageAPIResponse }) => {
                                        patchState(store, { pageAPIResponse, isClientReady });
                                    }),
                                    switchMap(({ pageAPIResponse }) => {
                                        return dotLanguagesService.getLanguagesUsedPage(
                                            pageAPIResponse.page.identifier
                                        );
                                    }),
                                    tap((languages) => {
                                        patchState(store, {
                                            languages,
                                            status: UVE_STATUS.LOADED
                                        });
                                    }),
                                    catchError(({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            errorCode: errorStatus,
                                            status: UVE_STATUS.ERROR
                                        });

                                        return EMPTY;
                                    })
                                );
                        })
                    )
                )
            };
        }),

        withHooks((store) => {
            const dotLicenseService = inject(DotLicenseService);
            const loginService = inject(LoginService);

            return {
                onInit: () => {
                    dotLicenseService
                        .isEnterprise()
                        .pipe(take(1))
                        .subscribe((isEnterprise) => {
                            patchState(store, { isEnterprise });
                        });

                    loginService
                        .getCurrentUser()
                        .pipe(take(1))
                        .subscribe((currentUser) => {
                            patchState(store, { currentUser });
                        });

                    effect(
                        () => {
                            const page = store.pageAPIResponse()?.page;
                            if (page) {
                                store.getWorkflowActions(page.inode);
                            }
                        },
                        { allowSignalWrites: true }
                    );
                }
            };
        })
    );
}
