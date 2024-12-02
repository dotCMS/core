import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
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
                        tap(() => store.resetClientConfiguration()),
                        tap(() => {
                            patchState(store, { status: UVE_STATUS.LOADING, isClientReady: false });
                        }),
                        switchMap((params) => {
                            const pageParams = {
                                ...(store.pageParams() ?? {}),
                                ...params
                            } as DotPageApiParams;

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
                                switchMap(({ pageAsset, isEnterprise, currentUser }) =>
                                    forkJoin({
                                        experiment: dotExperimentsService.getById(
                                            pageParams?.experimentId ??
                                                pageAsset?.runningExperimentId ??
                                                DEFAULT_VARIANT_ID
                                        ),
                                        languages: dotLanguagesService.getLanguagesUsedPage(
                                            pageAsset.page.identifier
                                        )
                                    }).pipe(
                                        tap({
                                            next: ({ experiment, languages }) => {
                                                const canEditPage = computeCanEditPage(
                                                    pageAsset?.page,
                                                    currentUser,
                                                    experiment
                                                );

                                                const pageIsLocked = computePageIsLocked(
                                                    pageAsset?.page,
                                                    currentUser
                                                );

                                                const isTraditionalPage = !pageParams.clientHost; // If we don't send the clientHost we are using as VTL page

                                                patchState(store, {
                                                    pageParams,
                                                    pageAPIResponse: pageAsset,
                                                    isEnterprise,
                                                    currentUser,
                                                    experiment,
                                                    languages,
                                                    canEditPage,
                                                    pageIsLocked,
                                                    isTraditionalPage,
                                                    isClientReady: isTraditionalPage, // If is a traditional page we are ready
                                                    status: UVE_STATUS.LOADED
                                                });
                                            },
                                            error: ({ status: errorStatus }: HttpErrorResponse) => {
                                                patchState(store, {
                                                    errorCode: errorStatus,
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        })
                                    )
                                )
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
                                    switchMap((pageAPIResponse) =>
                                        dotLanguagesService
                                            .getLanguagesUsedPage(pageAPIResponse.page.identifier)
                                            .pipe(
                                                map((languages) => ({
                                                    pageAPIResponse,
                                                    languages
                                                }))
                                            )
                                    ),
                                    tapResponse({
                                        next: ({ pageAPIResponse, languages }) => {
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
                                                isClientReady: partialState?.isClientReady ?? true
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
