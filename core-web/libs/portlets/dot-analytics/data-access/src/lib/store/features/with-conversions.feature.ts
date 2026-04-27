import { mapResponse, tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    ContentAttributionEntity,
    ConversionsOverviewEntity,
    ConvertingVisitorsEntity,
    DEFAULT_GRANULARITY,
    RequestState,
    TimeRangeInput,
    TotalConversionsEntity
} from '../../types';
import { createCubeQuery } from '../../utils/cube/cube-query-builder.util';
import {
    aggregateTotalConversions,
    ConversionTrendEntity,
    createEmptyAnalyticsEntity,
    createEmptyTrafficVsConversionsEntity,
    createInitialRequestState,
    fillMissingDates,
    toTimeRangeCubeJS,
    TrafficVsConversionsEntity
} from '../../utils/data/analytics-data.utils';
import { conversionsApiEvents } from '../events';

/**
 * State interface for the Conversions feature.
 * Contains all conversion-related data states.
 */
export interface ConversionsState {
    /** Total conversions metric */
    totalConversions: RequestState<TotalConversionsEntity>;
    /** Converting visitors metric (includes uniqueVisitors and uniqueConvertingVisitors) */
    convertingVisitors: RequestState<ConvertingVisitorsEntity>;
    /** Conversion trend timeline data */
    conversionTrend: RequestState<ConversionTrendEntity[]>;
    /** Traffic vs conversions comparison data (per day) */
    trafficVsConversions: RequestState<TrafficVsConversionsEntity[]>;
    /** Content attribution table data */
    contentConversions: RequestState<ContentAttributionEntity[]>;
    /** Conversions overview table data */
    conversionsOverview: RequestState<ConversionsOverviewEntity[]>;
}

/**
 * Initial state for the Conversions feature.
 */
const initialConversionsState: ConversionsState = {
    totalConversions: createInitialRequestState(),
    convertingVisitors: createInitialRequestState(),
    conversionTrend: createInitialRequestState(),
    trafficVsConversions: createInitialRequestState(),
    contentConversions: createInitialRequestState(),
    conversionsOverview: createInitialRequestState()
};

/**
 * Signal Store Feature for managing conversions analytics data.
 *
 * This feature provides:
 * - State management for all conversion-related metrics
 * - Methods to load individual conversion metrics
 * - Coordinated method to load all conversions data
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
        withReducer(
            // totalConversions
            on<ConversionsState>(conversionsApiEvents.totalConversionsRequested, () => ({
                totalConversions: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(conversionsApiEvents.totalConversionsLoaded, ({ payload }) => ({
                totalConversions: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<ConversionsState>(conversionsApiEvents.totalConversionsFailed, ({ payload }) => ({
                totalConversions: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // conversionTrend
            on<ConversionsState>(conversionsApiEvents.conversionTrendRequested, () => ({
                conversionTrend: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(conversionsApiEvents.conversionTrendLoaded, ({ payload }) => ({
                conversionTrend: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<ConversionsState>(conversionsApiEvents.conversionTrendFailed, ({ payload }) => ({
                conversionTrend: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // convertingVisitors
            on<ConversionsState>(conversionsApiEvents.convertingVisitorsRequested, () => ({
                convertingVisitors: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(
                conversionsApiEvents.convertingVisitorsLoaded,
                ({ payload }) => ({
                    convertingVisitors: {
                        status: ComponentStatus.LOADED,
                        data: payload.data,
                        error: null
                    }
                })
            ),
            on<ConversionsState>(
                conversionsApiEvents.convertingVisitorsFailed,
                ({ payload }) => ({
                    convertingVisitors: {
                        status: ComponentStatus.ERROR,
                        data: null,
                        error: payload.error
                    }
                })
            ),

            // trafficVsConversions
            on<ConversionsState>(conversionsApiEvents.trafficVsConversionsRequested, () => ({
                trafficVsConversions: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(
                conversionsApiEvents.trafficVsConversionsLoaded,
                ({ payload }) => ({
                    trafficVsConversions: {
                        status: ComponentStatus.LOADED,
                        data: payload.data,
                        error: null
                    }
                })
            ),
            on<ConversionsState>(
                conversionsApiEvents.trafficVsConversionsFailed,
                ({ payload }) => ({
                    trafficVsConversions: {
                        status: ComponentStatus.ERROR,
                        data: null,
                        error: payload.error
                    }
                })
            ),

            // contentConversions
            on<ConversionsState>(conversionsApiEvents.contentConversionsRequested, () => ({
                contentConversions: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(
                conversionsApiEvents.contentConversionsLoaded,
                ({ payload }) => ({
                    contentConversions: {
                        status: ComponentStatus.LOADED,
                        data: payload.data,
                        error: null
                    }
                })
            ),
            on<ConversionsState>(
                conversionsApiEvents.contentConversionsFailed,
                ({ payload }) => ({
                    contentConversions: {
                        status: ComponentStatus.ERROR,
                        data: null,
                        error: payload.error
                    }
                })
            ),

            // conversionsOverview
            on<ConversionsState>(conversionsApiEvents.conversionsOverviewRequested, () => ({
                conversionsOverview: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(
                conversionsApiEvents.conversionsOverviewLoaded,
                ({ payload }) => ({
                    conversionsOverview: {
                        status: ComponentStatus.LOADED,
                        data: payload.data,
                        error: null
                    }
                })
            ),
            on<ConversionsState>(
                conversionsApiEvents.conversionsOverviewFailed,
                ({ payload }) => ({
                    conversionsOverview: {
                        status: ComponentStatus.ERROR,
                        data: null,
                        error: payload.error
                    }
                })
            )
        ),
        // HTTP event handlers — listen to per-metric *Requested events and
        // dispatch *Loaded / *Failed. switchMap cancels stale requests; the
        // reducer above transitions state. Keep the legacy rxMethod loaders
        // until Step 10's component cutover.
        withEventHandlers(
            (
                _store,
                events = inject(Events),
                analyticsService = inject(DotAnalyticsService),
                dotMessageService = inject(DotMessageService)
            ) => ({
                loadTotalConversions$: events
                    .on(conversionsApiEvents.totalConversionsRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .conversions()
                                .measures(['totalEvents'])
                                .siteId(payload.currentSiteId)
                                .timeRange(
                                    'day',
                                    toTimeRangeCubeJS(payload.timeRange),
                                    DEFAULT_GRANULARITY
                                )
                                .build();

                            return analyticsService
                                .cubeQuery<TotalConversionsEntity>(query)
                                .pipe(
                                    map((entities) => aggregateTotalConversions(entities)),
                                    mapResponse({
                                        next: (data) =>
                                            conversionsApiEvents.totalConversionsLoaded({ data }),
                                        error: (error: HttpErrorResponse) =>
                                            conversionsApiEvents.totalConversionsFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.total-conversions'
                                                    )
                                            })
                                    })
                                );
                        })
                    ),

                loadConversionTrend$: events
                    .on(conversionsApiEvents.conversionTrendRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .conversions()
                                .measures(['totalEvents'])
                                .siteId(payload.currentSiteId)
                                .timeRange(
                                    'day',
                                    toTimeRangeCubeJS(payload.timeRange),
                                    DEFAULT_GRANULARITY
                                )
                                .build();

                            return analyticsService.cubeQuery<ConversionTrendEntity>(query).pipe(
                                map((entities) =>
                                    fillMissingDates<ConversionTrendEntity>(
                                        entities,
                                        payload.timeRange,
                                        DEFAULT_GRANULARITY,
                                        createEmptyAnalyticsEntity
                                    )
                                ),
                                mapResponse({
                                    next: (data) =>
                                        conversionsApiEvents.conversionTrendLoaded({ data }),
                                    error: (error: HttpErrorResponse) =>
                                        conversionsApiEvents.conversionTrendFailed({
                                            error:
                                                error.message ||
                                                dotMessageService.get(
                                                    'analytics.error.loading.conversion-trend'
                                                )
                                        })
                                })
                            );
                        })
                    ),

                loadConvertingVisitors$: events
                    .on(conversionsApiEvents.convertingVisitorsRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .measures(['uniqueVisitors', 'uniqueConvertingVisitors'])
                                .siteId(payload.currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                                .build();

                            return analyticsService
                                .cubeQuery<ConvertingVisitorsEntity>(query)
                                .pipe(
                                    map((entities) => entities[0] ?? null),
                                    mapResponse({
                                        next: (data) =>
                                            conversionsApiEvents.convertingVisitorsLoaded({
                                                data: data as ConvertingVisitorsEntity
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            conversionsApiEvents.convertingVisitorsFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.converting-visitors'
                                                    )
                                            })
                                    })
                                );
                        })
                    ),

                loadTrafficVsConversions$: events
                    .on(conversionsApiEvents.trafficVsConversionsRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .measures(['uniqueVisitors', 'uniqueConvertingVisitors'])
                                .siteId(payload.currentSiteId)
                                .timeRange(
                                    'day',
                                    toTimeRangeCubeJS(payload.timeRange),
                                    DEFAULT_GRANULARITY
                                )
                                .build();

                            return analyticsService
                                .cubeQuery<TrafficVsConversionsEntity>(query)
                                .pipe(
                                    map((entities) =>
                                        fillMissingDates(
                                            entities,
                                            payload.timeRange,
                                            DEFAULT_GRANULARITY,
                                            createEmptyTrafficVsConversionsEntity
                                        )
                                    ),
                                    mapResponse({
                                        next: (data) =>
                                            conversionsApiEvents.trafficVsConversionsLoaded({
                                                data
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            conversionsApiEvents.trafficVsConversionsFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.traffic-vs-conversions'
                                                    )
                                            })
                                    })
                                );
                        })
                    ),

                loadContentConversions$: events
                    .on(conversionsApiEvents.contentConversionsRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('ContentAttribution')
                                .dimensions(['eventType', 'identifier', 'title'])
                                .measures(['sumConversions', 'sumEvents'])
                                .siteId(payload.currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                                .build();

                            return analyticsService
                                .cubeQuery<ContentAttributionEntity>(query)
                                .pipe(
                                    mapResponse({
                                        next: (data) =>
                                            conversionsApiEvents.contentConversionsLoaded({
                                                data
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            conversionsApiEvents.contentConversionsFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.content-conversions'
                                                    )
                                            })
                                    })
                                );
                        })
                    ),

                loadConversionsOverview$: events
                    .on(conversionsApiEvents.conversionsOverviewRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('Conversion')
                                .dimensions([
                                    'conversionName',
                                    'totalConversion',
                                    'convRate',
                                    'topAttributedContent'
                                ])
                                .siteId(payload.currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                                .build();

                            return analyticsService
                                .cubeQuery<ConversionsOverviewEntity>(query)
                                .pipe(
                                    mapResponse({
                                        next: (data) =>
                                            conversionsApiEvents.conversionsOverviewLoaded({
                                                data
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            conversionsApiEvents.conversionsOverviewFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.conversions-overview'
                                                    )
                                            })
                                    })
                                );
                        })
                    )
            })
        ),
        withMethods(
            (
                store,
                globalStore = inject(GlobalStore),
                analyticsService = inject(DotAnalyticsService),
                dotMessageService = inject(DotMessageService)
            ) => ({
                /**
                 * Loads total conversions metric.
                 */
                _loadTotalConversions: rxMethod<{
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
                                .timeRange('day', toTimeRangeCubeJS(timeRange), DEFAULT_GRANULARITY)
                                .build();

                            return analyticsService.cubeQuery<TotalConversionsEntity>(query).pipe(
                                tapResponse({
                                    next: (entities) => {
                                        const totalConversionsEntity =
                                            aggregateTotalConversions(entities);
                                        patchState(store, {
                                            totalConversions: {
                                                status: ComponentStatus.LOADED,
                                                data: totalConversionsEntity,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        const errorMessage =
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.total-conversions'
                                            );
                                        patchState(store, {
                                            totalConversions: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: errorMessage
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Loads conversion trend timeline data (line chart).
                 */
                _loadConversionTrend: rxMethod<{
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
                                .timeRange('day', toTimeRangeCubeJS(timeRange), DEFAULT_GRANULARITY)
                                .build();

                            return analyticsService.cubeQuery<ConversionTrendEntity>(query).pipe(
                                map((entities) =>
                                    fillMissingDates<ConversionTrendEntity>(
                                        entities,
                                        timeRange,
                                        DEFAULT_GRANULARITY,
                                        createEmptyAnalyticsEntity
                                    )
                                ),
                                tapResponse({
                                    next: (data) => {
                                        patchState(store, {
                                            conversionTrend: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        const errorMessage =
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.conversion-trend'
                                            );
                                        patchState(store, {
                                            conversionTrend: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: errorMessage
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Loads converting visitors metric (uniqueVisitors and uniqueConvertingVisitors).
                 */
                _loadConvertingVisitors: rxMethod<{
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
                                tapResponse({
                                    next: (entities) => {
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.LOADED,
                                                data: entities[0] ?? null,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        const errorMessage =
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.converting-visitors'
                                            );
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: errorMessage
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Loads traffic vs conversions chart data (per day).
                 * Returns uniqueVisitors (bars) and conversion rate % (line) per day.
                 */
                _loadTrafficVsConversions: rxMethod<{
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
                                .timeRange('day', toTimeRangeCubeJS(timeRange), DEFAULT_GRANULARITY)
                                .build();

                            return analyticsService
                                .cubeQuery<TrafficVsConversionsEntity>(query)
                                .pipe(
                                    map((entities) =>
                                        fillMissingDates(
                                            entities,
                                            timeRange,
                                            DEFAULT_GRANULARITY,
                                            createEmptyTrafficVsConversionsEntity
                                        )
                                    ),
                                    tapResponse({
                                        next: (entities) => {
                                            patchState(store, {
                                                trafficVsConversions: {
                                                    status: ComponentStatus.LOADED,
                                                    data: entities,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            const errorMessage =
                                                error.message ||
                                                dotMessageService.get(
                                                    'analytics.error.loading.traffic-vs-conversions'
                                                );
                                            patchState(store, {
                                                trafficVsConversions: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: errorMessage
                                                }
                                            });
                                        }
                                    })
                                );
                        })
                    )
                ),

                /**
                 * Loads content attribution table data.
                 * Shows content present in conversions with event type, identifier, title, etc.
                 */
                _loadContentConversions: rxMethod<{
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
                                .dimensions(['eventType', 'identifier', 'title'])
                                .measures(['sumConversions', 'sumEvents'])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService.cubeQuery<ContentAttributionEntity>(query).pipe(
                                tapResponse({
                                    next: (entities) => {
                                        patchState(store, {
                                            contentConversions: {
                                                status: ComponentStatus.LOADED,
                                                data: entities,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        const errorMessage =
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.content-conversions'
                                            );
                                        patchState(store, {
                                            contentConversions: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: errorMessage
                                            }
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Loads conversions overview table data.
                 * Shows conversion names with total conversions, conversion rate, and top attributed content.
                 */
                _loadConversionsOverview: rxMethod<{
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
                        switchMap(({ timeRange, currentSiteId }) => {
                            const query = createCubeQuery()
                                .fromCube('Conversion')
                                .dimensions([
                                    'conversionName',
                                    'totalConversion',
                                    'convRate',
                                    'topAttributedContent'
                                ])
                                .siteId(currentSiteId)
                                .timeRange('day', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService
                                .cubeQuery<ConversionsOverviewEntity>(query)
                                .pipe(
                                    tapResponse({
                                        next: (entities) => {
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.LOADED,
                                                    data: entities,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            const errorMessage =
                                                error.message ||
                                                dotMessageService.get(
                                                    'analytics.error.loading.conversions-overview'
                                                );
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: errorMessage
                                                }
                                            });
                                        }
                                    })
                                );
                        })
                    )
                ),

                /**
                 * Loads all conversions data.
                 * This method is called when the conversions tab is activated.
                 */
                loadConversionsData(): void {
                    const currentSiteId = globalStore.currentSiteId();
                    const timeRange = store.timeRange();

                    if (!currentSiteId) {
                        return;
                    }

                    // Load all conversions metrics
                    this._loadTotalConversions({ timeRange, currentSiteId });
                    this._loadConversionTrend({ timeRange, currentSiteId });
                    this._loadConvertingVisitors({ timeRange, currentSiteId });
                    this._loadTrafficVsConversions({ timeRange, currentSiteId });
                    this._loadContentConversions({ timeRange, currentSiteId });
                    this._loadConversionsOverview({ timeRange, currentSiteId });
                }
            })
        )
    );
}
