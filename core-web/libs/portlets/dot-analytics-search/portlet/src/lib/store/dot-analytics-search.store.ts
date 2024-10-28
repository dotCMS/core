import { JsonObject } from '@angular-devkit/core';
import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotAnalyticsSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { AnalyticsQueryType, ComponentStatus } from '@dotcms/dotcms-models';

export type DotContentAnalyticsState = {
    isEnterprise: boolean;
    results: JsonObject[] | null;
    query: {
        value: JsonObject[] | null;
        type: AnalyticsQueryType;
    };
    state: ComponentStatus;
    errorMessage: string;
};

export const initialState: DotContentAnalyticsState = {
    isEnterprise: false,
    results: null,
    query: {
        value: null,
        type: AnalyticsQueryType.DEFAULT
    },
    state: ComponentStatus.INIT,
    errorMessage: ''
};

export const DotAnalyticsSearchStore = signalStore(
    withState(initialState),
    withMethods(
        (
            store,
            analyticsSearchService = inject(DotAnalyticsSearchService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
        ) => ({
            /**
             * Set if initial state, including, the user is enterprise or not
             * @param isEnterprise
             */
            initLoad: (isEnterprise: boolean) => {
                patchState(store, {
                    ...initialState,
                    isEnterprise
                });
            },
            getResults: rxMethod<JsonObject>(
                pipe(
                    tap(() => {
                        patchState(store, {
                            state: ComponentStatus.LOADING
                        });
                    }),
                    switchMap((query) => {
                        return analyticsSearchService.get(query, store.query.type()).pipe(
                            tapResponse({
                                next: (results: JsonObject[]) => {
                                    patchState(store, {
                                        results,
                                        state: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.ERROR,
                                        errorMessage: 'Error loading data'
                                    });

                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            )
        })
    )
);
