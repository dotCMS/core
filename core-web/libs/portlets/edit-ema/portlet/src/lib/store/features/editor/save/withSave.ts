import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotCMSPageAsset } from '@dotcms/types';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer } from '../../../../shared/models';
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
                )
            };
        })
    );
}
