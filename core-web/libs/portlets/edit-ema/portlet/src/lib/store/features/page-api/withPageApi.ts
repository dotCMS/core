import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, Observable, of, pipe, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Signal } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotPageLayoutService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, EXPERIMENT_RETURN_PARAM } from '@dotcms/dotcms-models';
import { DotCMSPageAsset, DotPageAssetLayoutRow } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';

import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { UVE_STATUS } from '../../../shared/enums';
import {
    DotPageAssetParams,
    PageContainer,
    SaveStylePropertiesPayload
} from '../../../shared/models';
import { compareUrlPaths, getIframeAccessMode, isForwardOrPage } from '../../../utils';
import { PageType, UVEState } from '../../models';
import { PageAssetSource, PageSnapshot } from '../page/withPage';

/**
 * Shared shape for `pageReload`'s REST/GraphQL branches. Named and explicit so both
 * branches of the ternary resolve to the same type — an inferred union here (each branch's
 * `map()` producing a structurally different literal type) breaks overload resolution on the
 * subsequent `.pipe(switchMap(...))`, collapsing `pageResult` to `unknown` at compile time
 * (a real `tsc`/esbuild error that Jest's `isolatedModules` config does not catch).
 */
type PageReloadPayload = {
    pageAsset: DotCMSPageAsset;
    content?: Record<string, unknown>;
    source: PageAssetSource;
};

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
    saveStyleEditor: (
        payload: SaveStylePropertiesPayload
    ) => ReturnType<DotPageApiService['saveStyleProperties']>;
    saveQuickEditFields: (
        fieldValues: Record<string, string>
    ) => ReturnType<DotWorkflowActionsFireService['saveContentlet']>;
}

/**
 * Dependencies interface for withPageApi
 * These are methods/computeds from other features that withPageApi needs
 */
export interface WithPageApiDeps {
    // Client configuration
    resetClientConfiguration: () => void;
    /** Reset readiness + history but keep current pageAssetResponse. */
    markPageLoading: () => void;
    /** Drop the stored client GraphQL request (belongs to the page being left). */
    resetRequestMetadata: () => void;

    // Request metadata
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;

    // Page asset management
    setPageAsset: (payload: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
        source?: PageAssetSource;
    }) => void;
    rollbackPageAssetResponse: () => boolean;

    // History management
    addHistory: (response: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
        source?: PageAssetSource;
    }) => void;
    resetHistoryToCurrent: () => void;

    // Page access (single accessor)
    pageAsset: () => PageSnapshot;
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
            const dotWindow = inject(WINDOW);

            return {
                /**
                 * Update page parameters (language, variant, etc.)
                 * Does not trigger a page load - call pageLoad() or pageReload() after this
                 */
                pageUpdateParams: (params: Partial<DotPageAssetParams>) => {
                    const nextPageParams = {
                        ...store.pageParams(),
                        ...params
                    };

                    patchState(store, {
                        pageParams: nextPageParams,
                        iframeAccessMode: getIframeAccessMode(
                            nextPageParams.clientHost,
                            dotWindow.location.origin
                        )
                    });
                }
            };
        }),
        withMethods((store) => {
            const router = inject(Router);
            const dotWindow = inject(WINDOW);
            const dotPageApiService = inject(DotPageApiService);
            const dotLanguagesService = inject(DotLanguagesService);
            const dotExperimentsService = inject(DotExperimentsService);
            const dotPageLayoutService = inject(DotPageLayoutService);
            const iframeMessenger = inject(UveIframeMessengerService);
            const dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

            /**
             * Sends the current page asset to the headless client only when it is
             * GraphQL-sourced (or the page is traditional) — never REST-shaped. When it
             * can't push, tells the client to reload itself instead, so it re-syncs rather
             * than being left showing a stale optimistic edit. See dotCMS/core#37097: this
             * mirrors the primary reload effect's gate for the senders that bypass it
             * (rollback-after-failed-save paths).
             */
            const sendPageDataIfGraphQLSourced = () => {
                const asset = deps.pageAsset();
                const canPush =
                    store.pageType() === PageType.TRADITIONAL || asset?.source === 'graphql';

                if (canPush && asset?.clientResponse) {
                    iframeMessenger.sendPageData(asset.clientResponse);
                } else {
                    iframeMessenger.reloadPage();
                }
            };

            return {
                /**
                 * Load page with all related data
                 * Sets uveStatus to LOADING, fetches page/languages/experiment,
                 * updates all state, sets uveStatus to LOADED
                 */
                pageLoad: rxMethod<Partial<DotPageAssetParams>>(
                    pipe(
                        map((params) => {
                            const current = store.pageParams();

                            if (!current) {
                                return params as DotPageAssetParams;
                            }

                            const merged = { ...current, ...params };

                            /**
                             * A different page drops the params that only meant something on the
                             * last one.
                             *
                             * The merge is what makes this editor feel like one screen — language,
                             * persona and mode follow the editor from page to page. But the
                             * experiment ones are not the editor's, they are the page's: a variant
                             * belongs to one page, and carrying `variantName` into another asks the
                             * Page API for a variant that page does not have. The visible half was
                             * the banner announcing a variant over a page the editor had navigated
                             * away from, with a way back to an experiment that was never about it.
                             *
                             * Dropped here rather than at the navigation call sites — there are
                             * four of them and the merge is the one place they all pass through.
                             */
                            if (params.url && params.url !== current.url) {
                                delete merged.experimentId;
                                delete merged[EXPERIMENT_RETURN_PARAM];

                                // Only a real one. `DEFAULT` is not a variant, it is the absence
                                // of one, and dropping it would rewrite the address on every
                                // ordinary navigation for nothing.
                                if (merged.variantName !== DEFAULT_VARIANT_ID) {
                                    delete merged.variantName;
                                }
                            }

                            return merged;
                        }),
                        tap((pageParams) => {
                            // The stored client GraphQL request (sent by the headless
                            // app's CLIENT_READY) is page-scoped: its variables carry
                            // the URL it was captured for. Keep it only when it belongs
                            // to the page being loaded — same-page param changes
                            // (language/persona/mode) or a client-side navigation whose
                            // CLIENT_READY already refreshed it. Otherwise drop it so
                            // this page loads through the standard Page API (same as a
                            // first load) until its own CLIENT_READY installs fresh
                            // metadata. Reusing another page's request (stale variables
                            // like publishDate or custom vars) can resolve to NOT_FOUND
                            // and strand the editor on the previous page.
                            // Requests without a url variable are assumed to belong to
                            // the page currently loaded in the editor.
                            const metadataUrl =
                                deps.requestMetadata()?.variables?.['url'] ??
                                store.pageParams()?.url;
                            const belongsToTargetPage =
                                !!metadataUrl &&
                                !!pageParams.url &&
                                compareUrlPaths(metadataUrl, pageParams.url);

                            if (!belongsToTargetPage) {
                                deps.resetRequestMetadata();
                            }

                            // Don't fully reset — that would null
                            // `pageAssetResponse` and unmount the editor
                            // chrome (toolbars, sidebars, navigation,
                            // overlays) for the duration of the fetch.
                            // Keep the previous asset visible while the
                            // new one loads; setPageAsset replaces it
                            // when the fetch resolves.
                            deps.markPageLoading();
                            patchState(store, {
                                uveStatus: UVE_STATUS.LOADING,
                                pageParams,
                                // Selection belongs to the page being left.
                                // Clear both the active contentlet (drives
                                // the quick-edit panel) and the selected
                                // overlay (drives the floating border) so
                                // we don't carry stale selection into the
                                // new page's contentlet tree.
                                editorSelected: null,
                                editorContentArea: null
                            });
                        }),
                        switchMap((pageParams) => {
                            // Capture content from a GraphQL response so it can be stored
                            // alongside the pageAsset in the final tap below.
                            let graphQLContent: Record<string, unknown> | undefined;

                            const pageAsset$ = deps.requestMetadata()
                                ? dotPageApiService.getGraphQLPage(deps.$requestWithParams()).pipe(
                                      tap((response) => {
                                          graphQLContent = response.content;
                                      }),
                                      map((response) => response.pageAsset)
                                  )
                                : dotPageApiService.get(pageParams);

                            return pageAsset$.pipe(
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
                                switchMap((pageAsset) => {
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
                                            const payload =
                                                graphQLContent !== undefined
                                                    ? {
                                                          pageAsset,
                                                          content: graphQLContent,
                                                          source: 'graphql' as const
                                                      }
                                                    : { pageAsset, source: 'rest' as const };

                                            // Both writes land in the same synchronous tap.
                                            // Angular batches them before flushing effects, so
                                            // $translatePageEffect always sees a consistent state.
                                            // uveCurrentUser is synced reactively from GlobalStore in withUve onInit effect
                                            patchState(store, {
                                                pageExperiment: experiment,
                                                pageLanguages: languages,
                                                pageType: pageParams.clientHost
                                                    ? PageType.HEADLESS
                                                    : PageType.TRADITIONAL,
                                                iframeAccessMode: getIframeAccessMode(
                                                    pageParams.clientHost,
                                                    dotWindow.location.origin
                                                ),
                                                uveStatus: UVE_STATUS.LOADED
                                            });
                                            deps.setPageAsset(payload);
                                            deps.addHistory(payload);
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
                            const pageParams = store.pageParams();
                            const requestWithParams = deps.$requestWithParams();

                            if (!pageParams) {
                                return EMPTY;
                            }

                            // Thread content through the stream value so the payload shape
                            // naturally encodes whether this is a GraphQL reload:
                            // - non-GraphQL emits { pageAsset }         → 'content' NOT in payload
                            // - GraphQL emits     { pageAsset, content } → 'content' IN payload
                            // setPageAsset in withPage.ts uses 'content' in payload to decide
                            // whether to clear the existing content, so this preserves original semantics.
                            // `source` tags provenance explicitly for the headless push gate — see #37097.
                            const pageRequest: Observable<PageReloadPayload> =
                                !deps.requestMetadata() || !requestWithParams
                                    ? dotPageApiService.get(pageParams).pipe(
                                          map(
                                              (pageAsset): PageReloadPayload => ({
                                                  pageAsset,
                                                  source: 'rest'
                                              })
                                          )
                                      )
                                    : dotPageApiService.getGraphQLPage(requestWithParams).pipe(
                                          map(
                                              ({ pageAsset, content }): PageReloadPayload => ({
                                                  pageAsset,
                                                  content,
                                                  source: 'graphql'
                                              })
                                          )
                                      );

                            return pageRequest.pipe(
                                switchMap((pageResult) => {
                                    return dotLanguagesService
                                        .getLanguagesUsedPage(pageResult.pageAsset.page.identifier)
                                        .pipe(
                                            tap((languages) => {
                                                // Both writes land in the same synchronous tap.
                                                // Angular batches them before flushing effects, so
                                                // $translatePageEffect always sees a consistent state.
                                                patchState(store, {
                                                    pageLanguages: languages,
                                                    uveStatus: UVE_STATUS.LOADED
                                                });
                                                deps.setPageAsset(pageResult);
                                            }),
                                            catchError(() => {
                                                // Languages fetch failed: still apply the fresh
                                                // page asset with the current (stale) languages so
                                                // the user sees the page rather than an error screen.
                                                patchState(store, {
                                                    uveStatus: UVE_STATUS.LOADED
                                                });
                                                deps.setPageAsset(pageResult);

                                                return EMPTY;
                                            })
                                        );
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
                                pageId: deps.pageAsset()?.page?.identifier,
                                params: store.pageParams()
                            };

                            return dotPageApiService.save(payload).pipe(
                                switchMap(() => {
                                    const pageParams = store.pageParams();

                                    if (!pageParams) {
                                        return EMPTY;
                                    }

                                    const pageRequest = !deps.requestMetadata()
                                        ? dotPageApiService.get(pageParams).pipe(
                                              tap((pageAsset) =>
                                                  deps.setPageAsset({
                                                      pageAsset,
                                                      source: 'rest'
                                                  })
                                              )
                                          )
                                        : dotPageApiService
                                              .getGraphQLPage(deps.$requestWithParams())
                                              .pipe(
                                                  tap((response) =>
                                                      deps.setPageAsset({
                                                          pageAsset: response.pageAsset,
                                                          content: response.content,
                                                          source: 'graphql'
                                                      })
                                                  ),
                                                  map((response) => response.pageAsset)
                                              );

                                    return pageRequest.pipe(
                                        catchError((e) => {
                                            console.error(e);
                                            patchState(store, {
                                                uveStatus: UVE_STATUS.ERROR
                                            });

                                            return EMPTY;
                                        }),
                                        tap(() => {
                                            patchState(store, {
                                                uveStatus: UVE_STATUS.LOADED
                                            });
                                        })
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
                            const page = deps.pageAsset()?.page;
                            const layoutData = deps.pageAsset()?.layout;
                            const template = deps.pageAsset()?.template;
                            if (!layoutData) {
                                return EMPTY;
                            }

                            return dotPageLayoutService
                                .save(page.identifier, {
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
                                                            containers: column.containers,
                                                            metadata: column.metadata
                                                        };
                                                    })
                                                };
                                            })
                                        }
                                    },
                                    themeId: template?.theme,
                                    title: null
                                })
                                .pipe(
                                    /**********************************************************************
                                     * IMPORTANT: After saving the layout, we must re-fetch the page here  *
                                     * to obtain the new rendered content WITH all `data-*` attributes.    *
                                     * This is required because saveLayout API DOES NOT return the updated *
                                     * rendered page HTML.                                                 *
                                     **********************************************************************/
                                    switchMap(() => {
                                        const pageParams = store.pageParams();

                                        if (!pageParams) {
                                            return EMPTY;
                                        }

                                        return !deps.requestMetadata()
                                            ? dotPageApiService.get(pageParams).pipe(
                                                  tap((pageAsset) =>
                                                      deps.setPageAsset({
                                                          pageAsset,
                                                          source: 'rest'
                                                      })
                                                  )
                                              )
                                            : dotPageApiService
                                                  .getGraphQLPage(deps.$requestWithParams())
                                                  .pipe(
                                                      tap((response) =>
                                                          deps.setPageAsset({
                                                              pageAsset: response.pageAsset,
                                                              content: response.content,
                                                              source: 'graphql'
                                                          })
                                                      ),
                                                      map((response) => response.pageAsset)
                                                  );
                                    }),
                                    tap(
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
                        tap(() => {
                            deps.resetHistoryToCurrent();
                        }),
                        catchError((error) => {
                            const rolledBack = deps.rollbackPageAssetResponse();

                            if (rolledBack) {
                                sendPageDataIfGraphQLSourced();
                            }

                            return throwError(() => error);
                        })
                    );
                },

                /**
                 * Saves quick-edit contentlet field values via the EDIT workflow action.
                 * Resets history to current on success; rolls back the optimistic page asset
                 * update and re-sends the previous state to the iframe on failure.
                 *
                 * @param fieldValues - Flat record of field variable → value, must include inode
                 * @returns Observable that emits on success or errors on failure
                 */
                saveQuickEditFields: (fieldValues: Record<string, string>) => {
                    return dotWorkflowActionsFireService
                        .saveContentlet({
                            ...fieldValues,
                            variantName: store.pageParams()?.variantName ?? DEFAULT_VARIANT_ID
                        })
                        .pipe(
                            tap(() => {
                                deps.resetHistoryToCurrent();
                            }),
                            catchError((error) => {
                                const rolledBack = deps.rollbackPageAssetResponse();

                                if (rolledBack) {
                                    sendPageDataIfGraphQLSourced();
                                }

                                return throwError(() => error);
                            })
                        );
                }
            };
        })
    );
}
