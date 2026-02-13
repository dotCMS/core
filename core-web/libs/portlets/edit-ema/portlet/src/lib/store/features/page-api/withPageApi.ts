import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Signal } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotExperimentsService, DotLanguagesService, DotLicenseService, DotPageLayoutService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { DotCMSPageAsset, DotPageAssetLayoutRow } from '@dotcms/types';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { UVE_STATUS } from '../../../shared/enums';
import { DotPageAssetParams, PageContainer, SaveStylePropertiesPayload } from '../../../shared/models';
import { isForwardOrPage } from '../../../utils';
import { PageType, UVEState } from '../../models';
import { PageSnapshot } from '../page/withPage';

/**
 * Interface defining the methods provided by withPageApi
 * Use this as props type in dependent features
 */
export interface WithPageApiMethods {
    // Load methods
    pageUpdateParams: (params: Partial<DotPageAssetParams>) => void;
    pageLoad: RxMethod<Partial<DotPageAssetParams>>;
    pageReload: RxMethod<Partial<DotPageAssetParams> | void>;

    // Save methods
    editorSave: RxMethod<PageContainer[]>;
    updateRows: RxMethod<DotPageAssetLayoutRow[]>;
    saveStyleEditor: (payload: SaveStylePropertiesPayload) => ReturnType<DotPageApiService['saveStyleProperties']>;
}

/**
 * Dependencies interface for withPageApi
 * These are methods/computeds from other features that withPageApi needs
 */
export interface WithPageApiDeps {
    // Client configuration
    resetClientConfiguration: () => void;

    // Workflow
    workflowFetch: (inode: string) => void;

    // Request metadata
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;

    // Page asset management
    setPageAssetResponse: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    rollbackPageAssetResponse: () => boolean;

    // History management
    addHistory: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    resetHistoryToCurrent: () => void;

    // Page access (single accessor)
    page: () => PageSnapshot;
}

/**
 * Page API feature - Handles all backend interactions for page operations
 *
 * Responsibilities:
 * - Loading pages (initial load, reload, param updates)
 * - Saving page content (containers, layout, styles)
 * - Orchestrating uveStatus updates during async operations
 * - Fetching related data (languages, experiments, license, user)
 *
 * This feature consolidates withLoad and withSave for better organization
 * and clearer ownership of backend communication concerns.
 */
export function withPageApi(deps: WithPageApiDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withMethods((store) => {
            return {
                /**
                 * Update page parameters (language, variant, etc.)
                 * Does not trigger a page load - call pageLoad() or pageReload() after this
                 */
                pageUpdateParams: (params: Partial<DotPageAssetParams>) => {
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
            const dotPageLayoutService = inject(DotPageLayoutService);
            const iframeMessenger = inject(UveIframeMessengerService);

            return {
                /**
                 * Load page with all related data
                 * Sets uveStatus to LOADING, fetches page/languages/experiment,
                 * updates all state, sets uveStatus to LOADED
                 */
                pageLoad: rxMethod<Partial<DotPageAssetParams>>(
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
                                uveStatus: UVE_STATUS.LOADING,
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
                                    deps.workflowFetch(pageAsset?.page?.inode);
                                }),
                                catchError((err: HttpErrorResponse) => {
                                    const errorStatus = err.status;
                                    console.error('Error UVEStore', err);

                                    patchState(store, {
                                        pageErrorCode: errorStatus,
                                        uveStatus: UVE_STATUS.ERROR
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
                                                pageErrorCode: errorStatus,
                                                uveStatus: UVE_STATUS.ERROR
                                            });

                                            return EMPTY;
                                        }),
                                        tap(({ experiment, languages }) => {
                                            deps.setPageAssetResponse({ pageAsset });
                                            deps.addHistory({ pageAsset });

                                            patchState(store, {
                                                uveIsEnterprise: isEnterprise,
                                                uveCurrentUser: currentUser,
                                                pageExperiment: experiment,
                                                pageLanguages: languages,
                                                pageType: pageParams.clientHost
                                                    ? PageType.HEADLESS
                                                    : PageType.TRADITIONAL,
                                                uveStatus: UVE_STATUS.LOADED
                                            });
                                        })
                                    );
                                })
                            );
                        })
                    )
                ),

                /**
                 * Reload current page (refresh)
                 * Optionally update params before reloading
                 */
                pageReload: rxMethod<Partial<DotPageAssetParams> | void>(
                    pipe(
                        tap((params) => {
                            patchState(store, {
                                uveStatus: UVE_STATUS.LOADING
                            });

                            if (params) {
                                store.pageUpdateParams(params);
                            }
                        }),
                        switchMap(() => {
                            const pageRequest = !deps.requestMetadata()
                                ? dotPageApiService.get(store.pageParams())
                                : dotPageApiService.getGraphQLPage(deps.$requestWithParams()).pipe(
                                      tap((response) => deps.setPageAssetResponse(response)),
                                      map((response) => response.pageAsset)
                                  );

                            return pageRequest.pipe(
                                tap((pageAsset) => {
                                    deps.setPageAssetResponse({ pageAsset });
                                    deps.workflowFetch(pageAsset?.page?.inode);
                                }),
                                switchMap((pageAsset) => {
                                    return dotLanguagesService.getLanguagesUsedPage(
                                        pageAsset.page.identifier
                                    );
                                }),
                                tap((languages) => {
                                    patchState(store, {
                                        pageLanguages: languages,
                                        uveStatus: UVE_STATUS.LOADED
                                    });
                                }),
                                catchError((err: HttpErrorResponse) => {
                                    const errorStatus = err.status;
                                    console.error('Error UVEStore', err);

                                    patchState(store, {
                                        pageErrorCode: errorStatus,
                                        uveStatus: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
                                })
                            );
                        })
                    )
                ),

                /**
                 * Save page content (containers)
                 * Sets uveStatus to LOADING, saves containers, refetches page, sets to LOADED
                 */
                editorSave: rxMethod<PageContainer[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                uveStatus: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((pageContainers) => {
                            const payload = {
                                pageContainers,
                                pageId: deps.page()?.page?.identifier,
                                params: store.pageParams()
                            };

                            return dotPageApiService.save(payload).pipe(
                                switchMap(() => {
                                    const pageRequest = !deps.requestMetadata()
                                        ? dotPageApiService.get(store.pageParams()).pipe(
                                                  tap((pageAsset) => deps.setPageAssetResponse({ pageAsset }))
                                              )
                                        : dotPageApiService.getGraphQLPage(deps.$requestWithParams()).pipe(
                                                  tap((response) => deps.setPageAssetResponse(response)),
                                                  map((response) => response.pageAsset)
                                              );

                                    return pageRequest.pipe(
                                        tapResponse(
                                            () => {
                                                patchState(store, {
                                                    uveStatus: UVE_STATUS.LOADED
                                                });
                                            },
                                            (e) => {
                                                console.error(e);
                                                patchState(store, {
                                                    uveStatus: UVE_STATUS.ERROR
                                                });
                                            }
                                        )
                                    );
                                }),
                                catchError((e) => {
                                    console.error(e);
                                    patchState(store, {
                                        uveStatus: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
                                })
                            );
                        })
                    )
                ),

                /**
                 * Save page layout (update rows)
                 * Sets uveStatus to LOADING, saves layout, refetches page, sets to LOADED
                 */
                updateRows: rxMethod<DotPageAssetLayoutRow[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                uveStatus: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((sortedRows) => {
                            const page = deps.page()?.page;
                            const layoutData = deps.page()?.layout;
                            const template = deps.page()?.template;
                            if (!layoutData) {
                                return EMPTY;
                            }

                            return dotPageLayoutService.save(page.identifier, {
                                layout: {
                                    ...layoutData,
                                    body: {
                                        ...layoutData.body,
                                        rows: sortedRows.map((row) => {
                                            return {
                                                ...row,
                                                columns: row.columns.map((column) => {
                                                    return {
                                                        leftOffset: column.leftOffset,
                                                        styleClass: column.styleClass,
                                                        width: column.width,
                                                        containers: column.containers
                                                    };
                                                })
                                            };
                                        })
                                    }
                                },
                                themeId: template?.theme,
                                title: null
                            }).pipe(
                                /**********************************************************************
                                 * IMPORTANT: After saving the layout, we must re-fetch the page here  *
                                 * to obtain the new rendered content WITH all `data-*` attributes.    *
                                 * This is required because saveLayout API DOES NOT return the updated *
                                 * rendered page HTML.                                                 *
                                 **********************************************************************/
                                switchMap(() => {
                                    return dotPageApiService.get(store.pageParams()).pipe(
                                        map((response) => response)
                                    );
                                }),
                                tapResponse(
                                    (pageRender: DotCMSPageAsset) => {
                                        deps.setPageAssetResponse({ pageAsset: pageRender });
                                        patchState(store, {
                                            uveStatus: UVE_STATUS.LOADED
                                        });
                                    },
                                    (e) => {
                                        console.error(e);
                                        patchState(store, {
                                            uveStatus: UVE_STATUS.ERROR
                                        });
                                    }
                                )
                            );
                        }),
                        catchError((e) => {
                            console.error(e);
                            patchState(store, {
                                uveStatus: UVE_STATUS.ERROR
                            });

                            return EMPTY;
                        })
                    )
                ),

                /**
                 * Saves style properties optimistically with automatic rollback on failure.
                 * Returns an observable that can be subscribed to for handling success/error.
                 * The optimistic update should be done before calling this method.
                 * This method handles the API call and rolls back the state if the save fails.
                 *
                 * @param payload - Style properties save payload
                 * @returns Observable that emits on success or error
                 */
                saveStyleEditor: (payload: SaveStylePropertiesPayload) => {
                    return dotPageApiService.saveStyleProperties(payload).pipe(
                        tap({
                            next: () => {
                                // Success - clear history and set current state as the new base (last saved state)
                                // This ensures future rollbacks always go back to this saved state
                                deps.resetHistoryToCurrent();
                            },
                            error: (error) => {
                                console.error('Error saving style properties:', error);

                                // Rollback the optimistic update to the last saved state
                                const rolledBack = deps.rollbackPageAssetResponse();

                                if (!rolledBack) {
                                    console.error(
                                        'Failed to rollback optimistic update - no history available'
                                    );

                                    return;
                                }

                                // Update iframe with rolled back state
                                const rolledBackResponse = deps.page()?.clientResponse;
                                if (rolledBackResponse) {
                                    iframeMessenger.sendPageData(rolledBackResponse);
                                }
                                console.warn(
                                    'Rolled back optimistic style update due to save failure'
                                );
                            }
                        }),
                        catchError((error) => {
                            console.error('Error saving style properties:', error);
                            // Re-throw error so component can handle it (show toast, etc.)
                            // Rollback is already handled in tap error callback
                            return throwError(() => error);
                        })
                    );
                }
            };
        })
    );
}
