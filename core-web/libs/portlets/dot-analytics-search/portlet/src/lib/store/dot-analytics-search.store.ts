import { JsonObject } from '@angular-devkit/core';
import { tapResponse } from '@ngrx/component-store';
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

interface RouteData {
    isEnterprise: boolean;
    healthCheck: HealthStatusTypes;
}

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
    healthCheck: HealthStatusTypes;
    wallEmptyConfig: PrincipalConfiguration | null;
    emptyResultsConfig: PrincipalConfiguration | null;
};

/**
 * Initial state for the DotContentAnalytics.
 */
export const initialState: DotContentAnalyticsState = {
    isEnterprise: false,
    results: null,
    query: {
        value: null,
        type: AnalyticsQueryType.CUBE
    },
    state: ComponentStatus.INIT,
    healthCheck: HealthStatusTypes.NOT_CONFIGURED,
    wallEmptyConfig: null,
    emptyResultsConfig: null
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
