import { JsonObject } from '@angular-devkit/core';
import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { AnalyticsQueryType, ComponentStatus, HealthStatusTypes } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

import { AnalyticsQueryExamples, isValidJson } from '../utils';

interface RouteData {
    isEnterprise: boolean;
    healthCheck: HealthStatusTypes;
}

export type AnalyticsQueryExample = {
    title: string;
    query: string;
};

/**
 * Type definition for the state of the DotContentAnalytics.
 */
export type DotContentAnalyticsState = {
    isEnterprise: boolean;
    results: string;
    query: {
        value: string;
        type: AnalyticsQueryType;
        isValidJson: boolean;
    };
    state: ComponentStatus;
    healthCheck: HealthStatusTypes;
    wallEmptyConfig: PrincipalConfiguration | null;
    emptyResultsConfig: PrincipalConfiguration | null;
    queryExamples: AnalyticsQueryExample[];
};

/**
 * Initial state for the DotContentAnalytics.
 */
export const initialState: DotContentAnalyticsState = {
    isEnterprise: false,
    results: '',
    query: {
        value: '',
        type: AnalyticsQueryType.CUBE,
        isValidJson: false
    },
    state: ComponentStatus.INIT,
    healthCheck: HealthStatusTypes.NOT_CONFIGURED,
    wallEmptyConfig: null,
    emptyResultsConfig: null,
    queryExamples: AnalyticsQueryExamples
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
             * @param query - The query to fetch results for.
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

                        return analyticsSearchService.get(query, store.query.type()).pipe(
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
         * Sets the initial state based on the route data and messages.
         * @param store - The store instance.
         */
        onInit: (store) => {
            const activatedRoute = inject(ActivatedRoute);
            const dotMessageService = inject(DotMessageService);

            const { isEnterprise, healthCheck } = activatedRoute.snapshot.data as RouteData;

            const configurationMap = {
                [HealthStatusTypes.NOT_CONFIGURED]: {
                    title: dotMessageService.get('analytics.search.no.configured'),
                    icon: 'pi-search',
                    subtitle: dotMessageService.get('analytics.search.no.configured.subtitle')
                },
                [HealthStatusTypes.CONFIGURATION_ERROR]: {
                    title: dotMessageService.get('analytics.search.config.error'),
                    icon: 'pi-search',
                    subtitle: dotMessageService.get('analytics.search.config.error.subtitle')
                },
                [HealthStatusTypes.OK]: null,
                ['noLicense']: {
                    title: dotMessageService.get('analytics.search.no.license'),
                    icon: 'pi-search',
                    subtitle: dotMessageService.get('analytics.search.no.license.subtitle')
                }
            };

            const emptyResultsConfig = {
                title: dotMessageService.get('analytics.search.no.results'),
                icon: 'pi-search',
                subtitle: dotMessageService.get('analytics.search.execute.results')
            };

            const wallEmptyConfig = isEnterprise
                ? configurationMap[healthCheck]
                : configurationMap['noLicense'];

            patchState(store, {
                isEnterprise,
                healthCheck,
                wallEmptyConfig,
                emptyResultsConfig
            });
        }
    })
);
