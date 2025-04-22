import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { DotPageAssetParams } from '../../../shared/models';
import { computeCanEditPage, computePageIsLocked, isForwardOrPage } from '../../../utils';
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
                loadPageAsset: rxMethod<Partial<DotPageAssetParams>>(
                    pipe(
                        map((params) => {
                            if (!store.pageParams()) {
                                return params as DotPageAssetParams;
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
                            return dotPageApiService.get(pageParams).pipe(
                                tap((pageAPIResponse) => {
                                    const isTraditionalPage = !pageParams.clientHost;
                                    const isClientReady = isTraditionalPage;

                                    patchState(store, {
                                        pageAPIResponse,
                                        isTraditionalPage,
                                        isClientReady
                                    });
                                }),
                                switchMap(({ vanityUrl, page, runningExperimentId }) => {
                                    if (!isForwardOrPage(vanityUrl)) {
                                        router.navigate([], {
                                            queryParamsHandling: 'merge',
                                            queryParams: { url: vanityUrl.forwardTo }
                                        });

                                        return EMPTY;
                                    }

                                    const identifier = page.identifier;
                                    const experimentId =
                                        pageParams?.experimentId ?? runningExperimentId;

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
                                catchError((err: HttpErrorResponse) => {
                                    const errorStatus = err.status;
                                    console.error('Error UVEStore', err);

                                    patchState(store, {
                                        errorCode: errorStatus,
                                        status: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
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
                reloadCurrentPage: rxMethod<Partial<DotPageAssetParams> | void>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        map((params) => {
                            if (!store.pageParams()) {
                                return params as DotPageAssetParams;
                            }

                            return {
                                ...store.pageParams(),
                                ...params
                            };
                        }),
                        switchMap((params: DotPageAssetParams) => {
                            const pageRequest = !store.graphql()
                                ? dotPageApiService.get(params)
                                : dotPageApiService.getGraphQLPage(store.$graphql()).pipe(
                                      map((response) => {
                                          store.setGraphqlResponse(response);

                                          return response.page;
                                      })
                                  );

                            return pageRequest.pipe(
                                tap((pageAPIResponse) => {
                                    patchState(store, { pageAPIResponse });
                                }),
                                switchMap((pageAPIResponse) => {
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
                                catchError((err: HttpErrorResponse) => {
                                    const errorStatus = err.status;
                                    console.error('Error UVEStore', err);

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
                        .pipe(take(1), shareReplay())
                        .subscribe((isEnterprise) => {
                            patchState(store, { isEnterprise });
                        });

                    loginService
                        .getCurrentUser()
                        .pipe(take(1), shareReplay())
                        .subscribe((currentUser) => {
                            patchState(store, { currentUser });
                        });

                    // These should be computeds
                    // if you see this comments, remind me to move this to a computed
                    effect(
                        () => {
                            const canEditPage = computeCanEditPage(
                                store.pageAPIResponse()?.page,
                                store.currentUser(),
                                store.experiment()
                            );

                            patchState(store, { canEditPage });

                            const pageIsLocked = computePageIsLocked(
                                store.pageAPIResponse()?.page,
                                store.currentUser()
                            );

                            patchState(store, { pageIsLocked });
                        },
                        { allowSignalWrites: true }
                    );
                }
            };
        })
    );
}
