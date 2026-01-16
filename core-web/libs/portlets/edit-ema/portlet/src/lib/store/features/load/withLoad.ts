import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Signal } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { DotCMSPageAsset } from '@dotcms/types';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { DotPageAssetParams } from '../../../shared/models';
import { isForwardOrPage } from '../../../utils';
import { PageType, UVEState } from '../../models';

/**
 * Interface defining the methods provided by withLoad
 * Use this as props type in dependent features
 *
 * @export
 * @interface WithLoadMethods
 */
export interface WithLoadMethods {
    // Methods
    updatePageParams: (params: Partial<DotPageAssetParams>) => void;
    loadPageAsset: RxMethod<Partial<DotPageAssetParams>>;
    reloadCurrentPage: RxMethod<Partial<DotPageAssetParams> | void>;
}

/**
 * Dependencies interface for withLoad
 * These are methods/computeds from other features that withLoad needs
 */
export interface WithLoadDeps {
    resetClientConfiguration: () => void;
    getWorkflowActions: (inode: string) => void;
    graphqlRequest: () => { query: string; variables: Record<string, string> } | null;
    $graphqlWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;
    setGraphqlResponse: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    addHistory: (state: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
}

/**
 * Add load and reload method to the store
 *
 * Dependencies: Requires methods from withClient and withWorkflow
 * Pass these via the deps parameter when wrapping with withFeature
 *
 * @export
 * @param deps - Dependencies from other features (provided by withFeature wrapper)
 * @return {*}
 */
export function withLoad(deps: WithLoadDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
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
                            deps.resetClientConfiguration();
                            patchState(store, {
                                status: UVE_STATUS.LOADING,
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
                                tap(({ pageAsset }) => {
                                    deps.getWorkflowActions(pageAsset?.page?.inode);
                                }),
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
                                            deps.addHistory({ pageAsset });

                                            patchState(store, {
                                                page: pageAsset?.page,
                                                site: pageAsset?.site,
                                                viewAs: pageAsset?.viewAs,
                                                template: pageAsset?.template,
                                                layout: pageAsset?.layout,
                                                urlContentMap: pageAsset?.urlContentMap,
                                                containers: pageAsset?.containers,
                                                vanityUrl: pageAsset?.vanityUrl,
                                                numberContents: pageAsset?.numberContents,
                                                isEnterprise,
                                                currentUser,
                                                experiment,
                                                languages,
                                                pageType: pageParams.clientHost
                                                    ? PageType.HEADLESS
                                                    : PageType.TRADITIONAL,
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
                            const pageRequest = !deps.graphqlRequest()
                                ? dotPageApiService.get(store.pageParams())
                                : dotPageApiService.getGraphQLPage(deps.$graphqlWithParams()).pipe(
                                      tap((response) => deps.setGraphqlResponse(response)),
                                      map((response) => response.pageAsset)
                                  );

                            return pageRequest.pipe(
                                tap((pageAsset) => {
                                    patchState(store, {
                                        page: pageAsset?.page,
                                        site: pageAsset?.site,
                                        viewAs: pageAsset?.viewAs,
                                        template: pageAsset?.template,
                                        layout: pageAsset?.layout,
                                        urlContentMap: pageAsset?.urlContentMap,
                                        containers: pageAsset?.containers,
                                        vanityUrl: pageAsset?.vanityUrl,
                                        numberContents: pageAsset?.numberContents
                                    });
                                    deps.getWorkflowActions(pageAsset.page.inode);
                                }),
                                switchMap((pageAsset) => {
                                    return dotLanguagesService.getLanguagesUsedPage(
                                        pageAsset.page.identifier
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
