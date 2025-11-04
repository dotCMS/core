import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_FEATURE_FLAGS } from '../../../shared/consts';
import { UVE_STATUS } from '../../../shared/enums';
import { DotPageAssetParams } from '../../../shared/models';
import { computeCanEditPage, computePageIsLocked, isForwardOrPage } from '../../../utils';
import { UVEState } from '../../models';
import { withClient } from '../client/withClient';
import { withFlags } from '../flags/withFlags';
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
        withFlags(UVE_FEATURE_FLAGS),
        withMethods((store) => {
            return {
                updatePageParams: (params: Partial<DotPageAssetParams>) => {
                    patchState(store, {
                        pageParams: {
                            ...store.pageParams(),
                            ...params
                        }
                    });
                }
            };
        }),
        withMethods((store) => {
            const router = inject(Router);
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotExperimentsService = inject(DotExperimentsService);
            const dotLicenseService = inject(DotLicenseService);
            const loginService = inject(LoginService);
            const globalStore = inject(GlobalStore);

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
                            return forkJoin({
                                pageAsset: dotPageApiService.get(pageParams).pipe(
                                    // This logic should be handled in the Shell component using an effect
                                    switchMap((pageAsset) => {
                                        const { vanityUrl } = pageAsset;

                                        // If there is not vanity and is not a redirect we just return the pageAPI response
                                        if (isForwardOrPage(vanityUrl)) {
                                            return of(pageAsset);
                                        }

                                        // Maybe we can use retryWhen() instead of this navigate.
                                        router.navigate([], {
                                            queryParamsHandling: 'merge',
                                            queryParams: { url: vanityUrl.forwardTo }
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
                                tap(({ pageAsset }) =>
                                    store.getWorkflowActions(pageAsset?.page?.inode)
                                ),
                                catchError((err: HttpErrorResponse) => {
                                    const errorStatus = err.status;
                                    console.error('Error UVEStore', err);

                                    patchState(store, {
                                        errorCode: errorStatus,
                                        status: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
                                }),
                                switchMap(({ pageAsset, isEnterprise, currentUser }) => {
                                    const experimentId =
                                        pageParams?.experimentId ?? pageAsset?.runningExperimentId;

                                    return forkJoin({
                                        experiment: dotExperimentsService.getById(
                                            experimentId ?? DEFAULT_VARIANT_ID
                                        ),
                                        languages: dotLanguagesService.getLanguagesUsedPage(
                                            pageAsset?.page?.identifier
                                        )
                                    }).pipe(
                                        catchError((err: HttpErrorResponse) => {
                                            const errorStatus = err.status;
                                            console.error('Error UVEStore', err);

                                            patchState(store, {
                                                errorCode: errorStatus,
                                                status: UVE_STATUS.ERROR
                                            });

                                            return EMPTY;
                                        }),
                                        tap(({ experiment, languages }) => {
                                            const isFeatureFlagEnabled =
                                                store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK;

                                            const canEditPage = computeCanEditPage(
                                                pageAsset?.page,
                                                currentUser,
                                                experiment,
                                                isFeatureFlagEnabled
                                            );

                                            const pageIsLocked = computePageIsLocked(
                                                pageAsset?.page,
                                                currentUser,
                                                isFeatureFlagEnabled
                                            );
                                            const isTraditionalPage = !pageParams.clientHost;

                                            patchState(store, {
                                                pageAPIResponse: pageAsset,
                                                isEnterprise,
                                                currentUser,
                                                experiment,
                                                languages,
                                                canEditPage,
                                                pageIsLocked,
                                                isClientReady: isTraditionalPage,
                                                isTraditionalPage,
                                                status: UVE_STATUS.LOADED
                                            });
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
                reloadCurrentPage: rxMethod<Partial<DotPageAssetParams> | void>(
                    pipe(
                        tap((params) => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });

                            if (params) {
                                store.updatePageParams(params);
                            }
                        }),
                        switchMap(() => {
                            const pageRequest = !store.graphql()
                                ? dotPageApiService.get(store.pageParams())
                                : dotPageApiService.getGraphQLPage(store.$graphqlWithParams()).pipe(
                                      tap((response) => store.setGraphqlResponse(response)),
                                      map((response) => response.pageAsset)
                                  );

                            return pageRequest.pipe(
                                tap((pageAPIResponse) => {
                                    const canEditPage = computeCanEditPage(
                                        pageAPIResponse.page,
                                        store.currentUser(),
                                        store.experiment(),
                                        store.flags().FEATURE_FLAG_UVE_TOGGLE_LOCK
                                    );
                                    patchState(store, { pageAPIResponse, canEditPage });
                                    store.getWorkflowActions(pageAPIResponse.page.inode);

                                    // Add breadcrumb after the state is updated

                                    globalStore.addNewBreadcrumb({
                                        label: pageAPIResponse?.page.title
                                    });
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
        })
    );
}
