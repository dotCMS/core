import { mapResponse } from '@ngrx/operators';
import { signalStoreFeature, type, withState } from '@ngrx/signals';
import { Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    ContentAttributionEntity,
    ConversionsOverviewEntity,
    ConvertingVisitorsEntity,
    DEFAULT_GRANULARITY,
    RequestState,
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
 * State and HTTP follow the same shape as `withPageview`: per-metric
 * `*Requested` events trigger the reducer to set LOADING and the HTTP
 * handler to fetch via CubeJS, dispatching `*Loaded` or `*Failed`.
 *
 * @returns Signal store feature for conversions data
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
            on<ConversionsState>(conversionsApiEvents.convertingVisitorsLoaded, ({ payload }) => ({
                convertingVisitors: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<ConversionsState>(conversionsApiEvents.convertingVisitorsFailed, ({ payload }) => ({
                convertingVisitors: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

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
            on<ConversionsState>(conversionsApiEvents.contentConversionsLoaded, ({ payload }) => ({
                contentConversions: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<ConversionsState>(conversionsApiEvents.contentConversionsFailed, ({ payload }) => ({
                contentConversions: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // conversionsOverview
            on<ConversionsState>(conversionsApiEvents.conversionsOverviewRequested, () => ({
                conversionsOverview: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<ConversionsState>(conversionsApiEvents.conversionsOverviewLoaded, ({ payload }) => ({
                conversionsOverview: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<ConversionsState>(conversionsApiEvents.conversionsOverviewFailed, ({ payload }) => ({
                conversionsOverview: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            }))
        ),
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

                            return analyticsService.cubeQuery<TotalConversionsEntity>(query).pipe(
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

                loadConversionTrend$: events.on(conversionsApiEvents.conversionTrendRequested).pipe(
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

                            return analyticsService.cubeQuery<ConvertingVisitorsEntity>(query).pipe(
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

                            return analyticsService.cubeQuery<ContentAttributionEntity>(query).pipe(
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
        )
    );
}
