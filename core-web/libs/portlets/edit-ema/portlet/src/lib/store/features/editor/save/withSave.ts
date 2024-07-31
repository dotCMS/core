import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { graphqlToPageEntity } from '@dotcms/client';

import { DotPageApiResponse, DotPageApiService } from '../../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer } from '../../../../shared/models';
import { UVEState } from '../../../models';

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
        withMethods((store) => {
            const dotPageApiService = inject(DotPageApiService);

            const getPage = () => {
                if (!store.graphQL()) {
                    return dotPageApiService.get(store.params());
                }

                return dotPageApiService.getPageAssetFromGraphql(store.graphQL()).pipe(
                    map((data) => {
                        const page = graphqlToPageEntity(data) as unknown;

                        return page as DotPageApiResponse;
                    })
                );
            };

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
                                params: store.params()
                            };

                            return dotPageApiService.save(payload).pipe(
                                switchMap(() =>
                                    getPage().pipe(
                                        tapResponse(
                                            (pageAPIResponse: DotPageApiResponse) => {
                                                patchState(store, {
                                                    status: UVE_STATUS.LOADED,
                                                    pageAPIResponse: pageAPIResponse
                                                });
                                            },
                                            (e) => {
                                                console.error(e);

                                                patchState(store, {
                                                    status: UVE_STATUS.ERROR
                                                });
                                            }
                                        )
                                    )
                                ),
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
