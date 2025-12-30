import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

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
                 * The optimistic update should be done before calling this method using
                 * setGraphqlResponseOptimistic. This method handles the API call and
                 * rolls back the state if the save fails.
                 *
                 * @param payload - Style properties save payload
                 */
                saveStyleEditor: rxMethod<SaveStylePropertiesPayload>(
                    pipe(
                        switchMap((payload) => {
                            return dotPageApiService.saveStyleProperties(payload).pipe(
                                tapResponse({
                                    next: () => {
                                        // Success - optimistic update remains, no rollback needed
                                    },
                                    error: (error) => {
                                        console.error('Error saving style properties:', error);

                                        // Rollback the optimistic update
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
                                            '[TEST] Rolled back optimistic style update due to save failure'
                                        );
                                    }
                                }),
                                catchError((error) => {
                                    // Additional error handling if needed
                                    console.error('Error in saveStyleEditor:', error);
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
