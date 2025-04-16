import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer } from '../../../../shared/models';
import { UVEState } from '../../../models';
import { withClient } from '../../client/withClient';

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
        withClient(),
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
                                switchMap(() =>
                                    dotPageApiService
                                        .get(store.pageAPIResponse().page.pageURI, {
                                            ...store.pageParams(),
                                            ...store.clientRequestProps()
                                        })
                                        .pipe(
                                            tapResponse(
                                                ({ pageAPIResponse }) => {
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
