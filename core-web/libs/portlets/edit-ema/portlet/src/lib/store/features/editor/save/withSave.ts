import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe, throwError } from 'rxjs';

import { inject, Signal } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotPageLayoutService } from '@dotcms/data-access';
import { DotCMSPageAsset, DotPageAssetLayoutRow } from '@dotcms/types';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { UveIframeMessengerService } from '../../../../services/iframe-messenger/uve-iframe-messenger.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer, SaveStylePropertiesPayload } from '../../../../shared/models';
import { UVEState } from '../../../models';

/**
 * Dependencies interface for withSave
 * These are methods/computeds from other features that withSave needs
 */
export interface WithSaveDeps {
    requestMetadata: () => { query: string; variables: Record<string, string> } | null;
    $requestWithParams: Signal<{ query: string; variables: Record<string, string> } | null>;
    setPageAssetResponse: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    rollbackPageAssetResponse: () => boolean;
    clearHistory: () => void;
    addHistory: (response: { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> }) => void;
    pageAssetResponse: () => { pageAsset: DotCMSPageAsset; content?: Record<string, unknown> } | null;
    pageClientResponse: Signal<DotCMSPageAsset | { pageAsset: DotCMSPageAsset; content?: Record<string, unknown>; requestMetadata: { query: string; variables: Record<string, string> } } | null>;
    pageData: () => any;  // Page data accessor
    pageTemplate: () => any;  // Page template accessor
}

/**
 * Add methods to save the page
 *
 * Dependencies: Requires methods from withClient
 * Pass these via the deps parameter when wrapping with withFeature
 *
 * @export
 * @param deps - Dependencies from other features (provided by withFeature wrapper)
 * @return {*}
 */
export function withSave(deps: WithSaveDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withMethods((store) => {
            const dotPageApiService = inject(DotPageApiService);
            const dotPageLayoutService = inject(DotPageLayoutService);
            const iframeMessenger = inject(UveIframeMessengerService);

            return {
                editorSave: rxMethod<PageContainer[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((pageContainers) => {
                            const payload = {
                                pageContainers,
                                pageId: deps.pageData()?.identifier,
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
                                                    status: UVE_STATUS.LOADED
                                                });
                                            },
                                            (e) => {
                                                console.error(e);
                                                patchState(store, {
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        )
                                    );
                                }),
                                catchError((e) => {
                                    console.error(e);
                                    patchState(store, {
                                        status: UVE_STATUS.ERROR
                                    });

                                    return EMPTY;
                                })
                            );
                        })
                    )
                ),
                updateRows: rxMethod<DotPageAssetLayoutRow[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((sortedRows) => {
                            const page = deps.pageData();
                            const layoutData = deps.pageAssetResponse()?.pageAsset?.layout;
                            const template = deps.pageTemplate();
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
                                            status: UVE_STATUS.LOADED
                                        });
                                    },
                                    (e) => {
                                        console.error(e);
                                        patchState(store, {
                                            status: UVE_STATUS.ERROR
                                        });
                                    }
                                )
                            );
                        }),
                        catchError((e) => {
                            console.error(e);
                            patchState(store, {
                                status: UVE_STATUS.ERROR
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
                                const currentResponse = deps.pageAssetResponse();
                                if (currentResponse) {
                                    deps.clearHistory();
                                    deps.addHistory(currentResponse);
                                }
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
                                const rolledBackResponse = deps.pageClientResponse();
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
