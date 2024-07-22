import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotPageApiResponse, DotPageApiService } from '../../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { PageContainer } from '../../../../shared/models';
import { UVEState } from '../../../models';

/**
 * Add computed properties to the store to handle the UVE status
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

            return {
                savePage: rxMethod<PageContainer[]>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                $status: UVE_STATUS.LOADING
                            });
                        }),
                        switchMap((pageContainers) => {
                            const payload = {
                                pageContainers,
                                pageId: store.$pageAPIResponse().page.identifier,
                                params: store.$params()
                            };

                            return dotPageApiService.save(payload).pipe(
                                switchMap(() =>
                                    dotPageApiService.get(payload.params).pipe(
                                        tapResponse(
                                            (pageAPIResponse: DotPageApiResponse) => {
                                                patchState(store, {
                                                    $status: UVE_STATUS.LOADED,
                                                    $pageAPIResponse: pageAPIResponse
                                                });
                                            },
                                            (e) => {
                                                console.error(e);

                                                patchState(store, {
                                                    $status: UVE_STATUS.ERROR
                                                });
                                            }
                                        )
                                    )
                                ),
                                catchError((e) => {
                                    console.error(e);
                                    patchState(store, {
                                        $status: UVE_STATUS.ERROR
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
