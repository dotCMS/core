import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    ContentAttributionEntity,
    ConvertingVisitorsEntity,
    createInitialRequestState,
    RequestState,
    TimeRangeInput,
    TotalConversionsEntity
} from '../../types';
import { createCubeQuery } from '../../utils/cube/cube-query-builder.util';
import {
    ConversionTrendEntity,
    toTimeRangeCubeJS,
    TrafficVsConversionsEntity
} from '../../utils/data/analytics-data.utils';

/**
 * State interface for the Conversions feature.
 * Contains all conversion-related data states.
 */
export interface ConversionsState {
    /** Total conversions metric */
    totalConversions: RequestState<TotalConversionsEntity>;
    /** Converting visitors metric (includes uniqueVisitors and uniqueConvertingVisitors) */
    convertingVisitors: RequestState<ConvertingVisitorsEntity>;
    /** Site-wide conversion rate */
    conversionRate: RequestState<number>;
    /** Conversion trend timeline data */
    conversionTrend: RequestState<ConversionTrendEntity[]>;
    /** Traffic vs conversions comparison data (per day) */
    trafficVsConversions: RequestState<TrafficVsConversionsEntity[]>;
    /** Content attribution table data */
    contentConversions: RequestState<ContentAttributionEntity[]>;
    /** Flag to track if data has been loaded at least once */
    conversionsDataLoaded: boolean;
}

/**
 * Initial state for the Conversions feature.
 */
const initialConversionsState: ConversionsState = {
    totalConversions: createInitialRequestState(),
    convertingVisitors: createInitialRequestState(),
    conversionRate: createInitialRequestState(),
    conversionTrend: createInitialRequestState(),
    trafficVsConversions: createInitialRequestState(),
    contentConversions: createInitialRequestState(),
    conversionsDataLoaded: false
};

/**
 * Signal Store Feature for managing conversions analytics data.
 *
 * This feature provides:
 * - State management for all conversion-related metrics
 * - Lazy loading via explicit method call (no auto-load)
 * - Structure ready for future API endpoints
 *
 * Note: Currently the conversions report uses mock data in the component.
 * When API endpoints are available, this feature will handle the data loading.
 *
 * @returns Signal store feature with conversions state and methods
 */
export function withConversions() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialConversionsState),
        withMethods(
            (
                store,
                globalStore = inject(GlobalStore),
                analyticsService = inject(DotAnalyticsService)
            ) => ({
                /**
                 * Loads total conversions metric.
                 */
                loadTotalConversions: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                totalConversions: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .conversions()
                                .measures(['totalEvents'])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange), 'day')
                                .build();

                            return analyticsService.cubeQuery<TotalConversionsEntity>(query).pipe(
                                tapResponse(
                                    (entities) => {
                                        patchState(store, {
                                            totalConversions: {
                                                status: ComponentStatus.LOADED,
                                                data: entities[0] ?? null,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            totalConversions: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error:
                                                    error.message ||
                                                    'Error loading total conversions'
                                            }
                                        });
                                    }
                                )
                            );
                        })
                    )
                ),

                /**
                 * Loads conversion trend timeline data (line chart).
                 */
                loadConversionTrend: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                conversionTrend: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .conversions()
                                .measures(['totalEvents'])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange), 'day')
                                .build();

                            return analyticsService.cubeQuery<ConversionTrendEntity>(query).pipe(
                                tapResponse(
                                    (entities) => {
                                        patchState(store, {
                                            conversionTrend: {
                                                status: ComponentStatus.LOADED,
                                                data: entities,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            conversionTrend: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error:
                                                    error.message ||
                                                    'Error loading conversion trend'
                                            }
                                        });
                                    }
                                )
                            );
                        })
                    )
                ),

                /**
                 * Loads converting visitors metric (uniqueVisitors and uniqueConvertingVisitors).
                 */
                loadConvertingVisitors: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                convertingVisitors: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .measures(['uniqueVisitors', 'uniqueConvertingVisitors'])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService.cubeQuery<ConvertingVisitorsEntity>(query).pipe(
                                tapResponse(
                                    (entities) => {
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.LOADED,
                                                data: entities[0] ?? null,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error:
                                                    error.message ||
                                                    'Error loading converting visitors'
                                            }
                                        });
                                    }
                                )
                            );
                        })
                    )
                ),

                /**
                 * Loads traffic vs conversions chart data (per day).
                 * Returns uniqueVisitors (bars) and conversion rate % (line) per day.
                 */
                loadTrafficVsConversions: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                trafficVsConversions: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .measures(['uniqueVisitors', 'uniqueConvertingVisitors'])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange), 'day')
                                .build();

                            return analyticsService
                                .cubeQuery<TrafficVsConversionsEntity>(query)
                                .pipe(
                                    tapResponse(
                                        (entities) => {
                                            patchState(store, {
                                                trafficVsConversions: {
                                                    status: ComponentStatus.LOADED,
                                                    data: entities,
                                                    error: null
                                                }
                                            });
                                        },
                                        (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                trafficVsConversions: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error.message ||
                                                        'Error loading traffic vs conversions'
                                                }
                                            });
                                        }
                                    )
                                );
                        })
                    )
                ),

                /**
                 * Loads content attribution table data.
                 * Shows content present in conversions with event type, identifier, title, etc.
                 */
                loadContentConversions: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                contentConversions: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('ContentAttribution')
                                .dimensions([
                                    'eventType',
                                    'identifier',
                                    'title',
                                    'conversions',
                                    'events'
                                ])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService.cubeQuery<ContentAttributionEntity>(query).pipe(
                                tapResponse(
                                    (entities) => {
                                        patchState(store, {
                                            contentConversions: {
                                                status: ComponentStatus.LOADED,
                                                data: entities,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            contentConversions: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error:
                                                    error.message ||
                                                    'Error loading content conversions'
                                            }
                                        });
                                    }
                                )
                            );
                        })
                    )
                ),

                /**
                 * Loads all conversions data.
                 * This method is called explicitly (lazy) when the conversions tab is activated.
                 */
                loadConversionsData(): void {
                    const currentSiteId = globalStore.currentSiteId();
                    const timeRange = store.timeRange();

                    if (!currentSiteId) {
                        return;
                    }

                    patchState(store, { conversionsDataLoaded: true });

                    // Load all conversions metrics
                    this.loadTotalConversions({ timeRange, currentSiteId });
                    this.loadConversionTrend({ timeRange, currentSiteId });
                    this.loadConvertingVisitors({ timeRange, currentSiteId });
                    this.loadTrafficVsConversions({ timeRange, currentSiteId });
                    this.loadContentConversions({ timeRange, currentSiteId });
                },

                /**
                 * Resets the conversions loaded flag.
                 * Useful when time range changes and data needs to be reloaded.
                 */
                resetConversionsLoaded(): void {
                    patchState(store, { conversionsDataLoaded: false });
                }
            })
        ),
        withHooks({
            onInit: (store, globalStore = inject(GlobalStore)) => {
                // Auto-reload conversions data when timeRange or currentSiteId changes
                // Only if conversions data was already loaded (lazy loading)
                effect(() => {
                    // Read signals to establish reactivity
                    store.timeRange();
                    globalStore.currentSiteId();
                    const conversionsDataLoaded = store.conversionsDataLoaded();

                    // Only reload if data was already loaded
                    if (conversionsDataLoaded) {
                        store.loadConversionsData();
                    }
                });
            }
        })
    );
}
