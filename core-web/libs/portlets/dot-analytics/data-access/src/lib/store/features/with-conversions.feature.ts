import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    ContentAttributionEntity,
    ConversionsOverviewEntity,
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
    /** Conversions overview table data */
    conversionsOverview: RequestState<ConversionsOverviewEntity[]>;
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
    conversionsOverview: createInitialRequestState(),
    conversionsDataLoaded: false
};

/**
 * Signal Store Feature for managing conversions analytics data.
 *
 * This feature provides:
 * - State management for all conversion-related metrics
 * - Methods to load individual conversion metrics
 * - Coordinated method to load all conversions data
 * - conversionsDataLoaded flag for lazy loading tracking
 *
 * Note: Data loading is managed by the main store's effect based on active tab.
 * This feature only provides the methods and state management.
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
                 * Loads conversions overview table data.
                 * Shows conversion names with total conversions, conversion rate, and top attributed content.
                 */
                loadConversionsOverview: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                conversionsOverview: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('Conversion')
                                .dimensions([
                                    'conversionName',
                                    'totalConversion',
                                    'convRate',
                                    'topAttributedContent'
                                ])
                                .siteId(currentSiteId)
                                // TODO: Uncomment this when the time range is implemented
                                // .timeRange('day', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService
                                .cubeQuery<ConversionsOverviewEntity>(query)
                                .pipe(
                                    tapResponse(
                                        (entities) => {
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.LOADED,
                                                    data: entities,
                                                    error: null
                                                }
                                            });
                                        },
                                        (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error.message ||
                                                        'Error loading conversions overview'
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
                    this.loadConversionsOverview({ timeRange, currentSiteId });
                },

                /**
                 * Resets the conversions loaded flag.
                 * Useful when time range changes and data needs to be reloaded.
                 */
                resetConversionsLoaded(): void {
                    patchState(store, { conversionsDataLoaded: false });
                }
            })
        )
    );
}
