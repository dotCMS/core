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

/**
 * Type definition for the state of the DotContentAnalytics.
 */
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

/**
 * Initial state for the DotContentAnalytics.
 */
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

/**
 * Store for managing the state and actions related to DotAnalyticsSearch.
 */
export const DotAnalyticsSearchStore = signalStore(
    withState(initialState),
    withMethods(
        (
            store,
            analyticsSearchService = inject(DotAnalyticsSearchService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
        ) => ({
            /**
             * Initializes the state with the given enterprise status.
             * @param isEnterprise - Boolean indicating if the user is an enterprise user.
             */
            initLoad: (isEnterprise: boolean) => {
                patchState(store, {
                    ...initialState,
                    isEnterprise
                });
            },

            /**
             * Updates the query type and resets the results.
             * @param type - The new query type.
             */
            updateQueryType: (type: AnalyticsQueryType): void => {
                patchState(store, {
                    results: null,
                    query: {
                        ...store.query(),
                        type
                    }
                });
            },

            /**
             * Fetches the results based on the current query.
             * @param query - The query to fetch results for.
             */
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
