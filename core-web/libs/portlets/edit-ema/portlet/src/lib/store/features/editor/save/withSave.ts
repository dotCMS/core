import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe, throwError } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotCMSPageAsset } from '@dotcms/types';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { UveIframeMessengerService } from '../../../../services/iframe-messenger/uve-iframe-messenger.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer, SaveStylePropertiesPayload } from '../../../../shared/models';
import { UVEState } from '../../../models';
import { withLoad } from '../../load/withLoad';

/**
 * Add methods to save the page
 *
 * @export
 * @return {*}
 */
export function withSave() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withLoad(),
        withMethods((store) => {
            const dotPageApiService = inject(DotPageApiService);
            const iframeMessenger = inject(UveIframeMessengerService);

            return {
                savePage: rxMethod<PageContainer[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((pageContainers) => {
                            const payload = {
                                pageContainers,
                                pageId: store.pageAPIResponse().page.identifier,
                                params: store.pageParams()
                            };

                            return dotPageApiService.save(payload).pipe(
                                switchMap(() => {
                                    const pageRequest = !store.graphql()
                                        ? dotPageApiService.get(store.pageParams())
                                        : dotPageApiService
                                              .getGraphQLPage(store.$graphqlWithParams())
                                              .pipe(
                                                  tap((response) =>
                                                      store.setGraphqlResponse(response)
                                                  ),
                                                  map((response) => response.pageAsset)
                                              );

                                    return pageRequest.pipe(
                                        tapResponse({
                                            next: (pageAPIResponse: DotCMSPageAsset) => {
                                                patchState(store, {
                                                    status: UVE_STATUS.LOADED,
                                                    pageAPIResponse: pageAPIResponse
                                                });
                                            },
                                            error: (e) => {
                                                console.error(e);
                                                patchState(store, {
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        })
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
                                const currentResponse = store.graphqlResponse();
                                if (currentResponse) {
                                    store.clearHistory();
                                    store.addHistory(currentResponse);
                                }
                            },
                            error: (error) => {
                                console.error('Error saving style properties:', error);

                                // Rollback the optimistic update to the last saved state
                                const rolledBack = store.rollbackGraphqlResponse();

                                if (!rolledBack) {
                                    console.error(
                                        'Failed to rollback optimistic update - no history available'
                                    );

                                    return;
                                }

                                // Update iframe with rolled back state
                                const rolledBackResponse = store.$customGraphqlResponse();
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
