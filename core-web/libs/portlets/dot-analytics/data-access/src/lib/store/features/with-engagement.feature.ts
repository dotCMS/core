import { mapResponse } from '@ngrx/operators';
import { signalStoreFeature, type, withState } from '@ngrx/signals';
import { Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import { DEFAULT_GRANULARITY } from '../../types';
import { createCubeQuery } from '../../utils/cube/cube-query-builder.util';
import {
    createInitialRequestState,
    getPreviousPeriod,
    toTimeRangeCubeJS
} from '../../utils/data/analytics-data.utils';
import {
    toEngagementBreakdownChartData,
    toEngagementKPIs,
    toEngagementPlatforms,
    toEngagementSparklineData
} from '../../utils/data/engagement-data.utils';
import { engagementApiEvents } from '../events';

// eslint-disable-next-line no-duplicate-imports
import type {
    ChartData,
    DimensionField,
    EngagementDailyEntity,
    EngagementKPIs,
    EngagementPlatforms,
    EngagementSparklineData,
    RequestState,
    SessionsByBrowserDailyEntity,
    SessionsByDeviceDailyEntity,
    SessionsByLanguageDailyEntity
} from '../../types';

const ENGAGEMENT_DAILY_MEASURES = [
    'totalSessions',
    'engagedSessions',
    'engagedConversionSessions',
    'engagementRate',
    'avgInteractionsPerEngagedSession',
    'avgSessionTimeSeconds',
    'avgEngagedSessionTimeSeconds'
];

const ENGAGEMENT_TREND_MEASURES = ['totalSessions', 'engagedSessions', 'engagementRate'];
/** Sparkline uses conversion rate so the trend varies day-to-day (engagement rate is often 100% when engaged_sessions === total_sessions). */
const ENGAGEMENT_SPARKLINE_MEASURES = ['conversionRate'];

const SESSIONS_BY_MEASURES = ['engagedSessions', 'totalSessions', 'avgEngagedSessionTimeSeconds'];
const SESSIONS_BY_DEVICE_DIMENSIONS: DimensionField[] = ['deviceCategory'];
const SESSIONS_BY_BROWSER_DIMENSIONS: DimensionField[] = ['browserFamily'];
const SESSIONS_BY_LANGUAGE_DIMENSIONS: DimensionField[] = ['localeId'];

/**
 * State interface for the Engagement feature.
 * Multiple slices for independent loading per block.
 */
export interface EngagementState {
    engagementKpis: RequestState<EngagementKPIs>;
    engagementSparkline: RequestState<EngagementSparklineData>;
    engagementBreakdown: RequestState<ChartData>;
    engagementPlatforms: RequestState<EngagementPlatforms>;
}

const initialEngagementState: EngagementState = {
    engagementKpis: createInitialRequestState(),
    engagementSparkline: createInitialRequestState(),
    engagementBreakdown: createInitialRequestState(),
    engagementPlatforms: createInitialRequestState()
};

/**
 * Signal Store Feature for managing engagement analytics data.
 *
 * Three of the four metrics combine multiple parallel queries via
 * `forkJoin`: KPIs (current+previous period), sparkline (current+previous),
 * and platforms (device+browser+language). Each handler dispatches a
 * single `*Loaded` event with the merged shape; reducers stay trivial.
 *
 * @returns Signal store feature for engagement data
 */
export function withEngagement() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialEngagementState),
        withReducer(
            // engagementKpis
            on<EngagementState>(engagementApiEvents.engagementKpisRequested, () => ({
                engagementKpis: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<EngagementState>(engagementApiEvents.engagementKpisLoaded, ({ payload }) => ({
                engagementKpis: { status: ComponentStatus.LOADED, data: payload.data, error: null }
            })),
            on<EngagementState>(engagementApiEvents.engagementKpisFailed, ({ payload }) => ({
                engagementKpis: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // engagementBreakdown
            on<EngagementState>(engagementApiEvents.engagementBreakdownRequested, () => ({
                engagementBreakdown: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<EngagementState>(engagementApiEvents.engagementBreakdownLoaded, ({ payload }) => ({
                engagementBreakdown: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<EngagementState>(engagementApiEvents.engagementBreakdownFailed, ({ payload }) => ({
                engagementBreakdown: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // engagementSparkline
            on<EngagementState>(engagementApiEvents.engagementSparklineRequested, () => ({
                engagementSparkline: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<EngagementState>(engagementApiEvents.engagementSparklineLoaded, ({ payload }) => ({
                engagementSparkline: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<EngagementState>(engagementApiEvents.engagementSparklineFailed, ({ payload }) => ({
                engagementSparkline: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // engagementPlatforms
            on<EngagementState>(engagementApiEvents.engagementPlatformsRequested, () => ({
                engagementPlatforms: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<EngagementState>(engagementApiEvents.engagementPlatformsLoaded, ({ payload }) => ({
                engagementPlatforms: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<EngagementState>(engagementApiEvents.engagementPlatformsFailed, ({ payload }) => ({
                engagementPlatforms: {
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
            ) => {
                const getErrorMessage = (key: string, fallback: string) =>
                    dotMessageService.get(key) || fallback;

                return {
                    loadEngagementKpis$: events
                        .on(engagementApiEvents.engagementKpisRequested)
                        .pipe(
                            switchMap(({ payload }) => {
                                const dateRange = toTimeRangeCubeJS(payload.timeRange);
                                const previousRange = getPreviousPeriod(payload.timeRange);

                                const currentQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(ENGAGEMENT_DAILY_MEASURES)
                                    .timeRange('day', dateRange)
                                    .build();

                                const previousQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(ENGAGEMENT_DAILY_MEASURES)
                                    .timeRange('day', previousRange)
                                    .build();

                                return forkJoin({
                                    current: analyticsService
                                        .cubeQuery<EngagementDailyEntity>(currentQuery)
                                        .pipe(
                                            map((rows) => rows[0] ?? null),
                                            catchError(() => of(null))
                                        ),
                                    previous: analyticsService
                                        .cubeQuery<EngagementDailyEntity>(previousQuery)
                                        .pipe(
                                            map((rows) => rows[0] ?? null),
                                            catchError(() => of(null))
                                        )
                                }).pipe(
                                    mapResponse({
                                        next: ({ current, previous }) =>
                                            engagementApiEvents.engagementKpisLoaded({
                                                data: toEngagementKPIs(current, previous)
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            engagementApiEvents.engagementKpisFailed({
                                                error:
                                                    error?.message ||
                                                    getErrorMessage(
                                                        'analytics.error.loading.engagement',
                                                        'Failed to load engagement KPIs'
                                                    )
                                            })
                                    })
                                );
                            })
                        ),

                    loadEngagementBreakdown$: events
                        .on(engagementApiEvents.engagementBreakdownRequested)
                        .pipe(
                            switchMap(({ payload }) => {
                                const query = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(['totalSessions', 'engagedSessions'])
                                    .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                                    .build();

                                return analyticsService
                                    .cubeQuery<EngagementDailyEntity>(query)
                                    .pipe(
                                        map((rows) => {
                                            const row = rows?.[0];
                                            const total = row
                                                ? Number(row['EngagementDaily.totalSessions'] ?? 0)
                                                : 0;
                                            const engaged = row
                                                ? Number(
                                                      row['EngagementDaily.engagedSessions'] ?? 0
                                                  )
                                                : 0;
                                            return toEngagementBreakdownChartData(total, engaged);
                                        }),
                                        mapResponse({
                                            next: (data) =>
                                                engagementApiEvents.engagementBreakdownLoaded({
                                                    data
                                                }),
                                            error: (error: HttpErrorResponse) =>
                                                engagementApiEvents.engagementBreakdownFailed({
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement',
                                                            'Failed to load breakdown data'
                                                        )
                                                })
                                        })
                                    );
                            })
                        ),

                    loadEngagementSparkline$: events
                        .on(engagementApiEvents.engagementSparklineRequested)
                        .pipe(
                            switchMap(({ payload }) => {
                                const dateRange = toTimeRangeCubeJS(payload.timeRange);
                                const previousRange = getPreviousPeriod(payload.timeRange);

                                const currentQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures([
                                        ...ENGAGEMENT_TREND_MEASURES,
                                        ...ENGAGEMENT_SPARKLINE_MEASURES
                                    ])
                                    .timeRange('day', dateRange, DEFAULT_GRANULARITY)
                                    .build();

                                const previousQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures([
                                        ...ENGAGEMENT_TREND_MEASURES,
                                        ...ENGAGEMENT_SPARKLINE_MEASURES
                                    ])
                                    .timeRange('day', previousRange, DEFAULT_GRANULARITY)
                                    .build();

                                return forkJoin({
                                    current:
                                        analyticsService.cubeQuery<EngagementDailyEntity>(
                                            currentQuery
                                        ),
                                    previous: analyticsService
                                        .cubeQuery<EngagementDailyEntity>(previousQuery)
                                        .pipe(catchError(() => of([])))
                                }).pipe(
                                    mapResponse({
                                        next: ({ current, previous }) =>
                                            engagementApiEvents.engagementSparklineLoaded({
                                                data: {
                                                    current: toEngagementSparklineData(
                                                        current ?? []
                                                    ),
                                                    previous:
                                                        previous?.length > 0
                                                            ? toEngagementSparklineData(previous)
                                                            : null
                                                }
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            engagementApiEvents.engagementSparklineFailed({
                                                error:
                                                    error?.message ||
                                                    getErrorMessage(
                                                        'analytics.error.loading.engagement',
                                                        'Failed to load sparkline data'
                                                    )
                                            })
                                    })
                                );
                            })
                        ),

                    loadEngagementPlatforms$: events
                        .on(engagementApiEvents.engagementPlatformsRequested)
                        .pipe(
                            switchMap(({ payload }) => {
                                const dateRange = toTimeRangeCubeJS(payload.timeRange);

                                const deviceQuery = createCubeQuery()
                                    .fromCube('SessionsByDeviceDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(SESSIONS_BY_MEASURES)
                                    .dimensions(SESSIONS_BY_DEVICE_DIMENSIONS)
                                    .timeRange('day', dateRange)
                                    .build();

                                const browserQuery = createCubeQuery()
                                    .fromCube('SessionsByBrowserDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(SESSIONS_BY_MEASURES)
                                    .dimensions(SESSIONS_BY_BROWSER_DIMENSIONS)
                                    .timeRange('day', dateRange)
                                    .build();

                                const languageQuery = createCubeQuery()
                                    .fromCube('SessionsByLanguageDaily')
                                    .siteId(payload.currentSiteId)
                                    .measures(SESSIONS_BY_MEASURES)
                                    .dimensions(SESSIONS_BY_LANGUAGE_DIMENSIONS)
                                    .timeRange('day', dateRange)
                                    .build();

                                return forkJoin({
                                    device: analyticsService
                                        .cubeQuery<SessionsByDeviceDailyEntity>(deviceQuery)
                                        .pipe(catchError(() => of([]))),
                                    browser: analyticsService
                                        .cubeQuery<SessionsByBrowserDailyEntity>(browserQuery)
                                        .pipe(catchError(() => of([]))),
                                    language: analyticsService
                                        .cubeQuery<SessionsByLanguageDailyEntity>(languageQuery)
                                        .pipe(catchError(() => of([])))
                                }).pipe(
                                    map(({ device, browser, language }) =>
                                        toEngagementPlatforms(device, browser, language)
                                    ),
                                    mapResponse({
                                        next: (data) =>
                                            engagementApiEvents.engagementPlatformsLoaded({ data }),
                                        error: (error: HttpErrorResponse) =>
                                            engagementApiEvents.engagementPlatformsFailed({
                                                error:
                                                    error?.message ||
                                                    getErrorMessage(
                                                        'analytics.error.loading.engagement',
                                                        'Failed to load platforms data'
                                                    )
                                            })
                                    })
                                );
                            })
                        )
                };
            }
        )
    );
}
