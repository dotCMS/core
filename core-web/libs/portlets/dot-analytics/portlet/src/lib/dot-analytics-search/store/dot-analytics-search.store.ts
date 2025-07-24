import { JsonObject } from '@angular-devkit/core';
import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { AnalyticsQueryType, ComponentStatus } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

import { AnalyticsQueryExamples, isValidJson } from '../utils';

export type AnalyticsQueryExample = {
    title: string;
    query: string;
};

/**
 * Type definition for the state of the DotContentAnalytics.
 */
export type DotContentAnalyticsState = {
    results: string;
    query: {
        value: string;
        type: AnalyticsQueryType;
        isValidJson: boolean;
    };
    state: ComponentStatus;
    emptyResultsConfig: PrincipalConfiguration | null;
    queryExamples: AnalyticsQueryExample[];
};

/**
 * Initial state for the DotContentAnalytics.
 */
export const initialState: DotContentAnalyticsState = {
    results: '',
    query: {
        value: '',
        type: AnalyticsQueryType.CUBE,
        isValidJson: false
    },
    state: ComponentStatus.INIT,
    emptyResultsConfig: null,
    queryExamples: AnalyticsQueryExamples
};

/**
 * Store for managing the state and actions related to DotAnalyticsSearch.
 * Note: Route protection is now handled by analyticsHealthGuard.
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
             * Sets the query in the store state.
             * @param query - The query string to be set.
             */
            setQuery: (query: string) => {
                patchState(store, {
                    query: { ...store.query(), value: query, isValidJson: !!isValidJson(query) }
                });
            },

            /**
             * Fetches the results based on the current query.
             */
            getResults: rxMethod<void>(
                pipe(
                    tap(() => {
                        patchState(store, {
                            state: ComponentStatus.LOADING
                        });
                    }),
                    switchMap(() => {
                        const query = isValidJson(store.query().value) as JsonObject;

                        return analyticsSearchService.get(query, store.query().type).pipe(
                            tapResponse({
                                next: (results: JsonObject[]) => {
                                    patchState(store, {
                                        results: JSON.stringify(results, null, 2),
                                        state: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.ERROR
                                    });

                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            )
        })
    ),
    withHooks({
        /**
         * Hook that runs on initialization of the store.
         * Sets up the empty results configuration.
         */
        onInit: (store) => {
            const dotMessageService = inject(DotMessageService);

            const emptyResultsConfig = {
                title: dotMessageService.get('analytics.search.no.results'),
                icon: 'pi-search',
                subtitle: dotMessageService.get('analytics.search.execute.results')
            };

            patchState(store, {
                emptyResultsConfig
            });
        }
    })
);
