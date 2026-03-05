import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
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
    TimeRangeInput
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
 * Each slice (KPIs, trend chart, breakdown, platforms) loads independently and has its own loading/error state.
 */
export function withEngagement() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialEngagementState),
        withMethods(
            (
                store,
                globalStore = inject(GlobalStore),
                analyticsService = inject(DotAnalyticsService),
                dotMessageService = inject(DotMessageService)
            ) => {
                const getErrorMessage = (key: string, fallback: string) =>
                    dotMessageService.get(key) || fallback;

                return {
                    /**
                     * Loads KPIs: current + previous period totals for trend calculation.
                     */
                    _loadEngagementKpis: rxMethod<{
                        timeRange: TimeRangeInput;
                        currentSiteId: string;
                    }>(
                        pipe(
                            tap(() =>
                                patchState(store, {
                                    engagementKpis: {
                                        status: ComponentStatus.LOADING,
                                        data: null,
                                        error: null
                                    }
                                })
                            ),
                            switchMap(({ timeRange, currentSiteId }) => {
                                const dateRange = toTimeRangeCubeJS(timeRange);
                                const previousRange = getPreviousPeriod(timeRange);

                                const currentQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(currentSiteId)
                                    .measures(ENGAGEMENT_DAILY_MEASURES)
                                    .timeRange('day', dateRange)
                                    .build();

                                const previousQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(currentSiteId)
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
                                    tapResponse(
                                        ({ current, previous }) => {
                                            patchState(store, {
                                                engagementKpis: {
                                                    status: ComponentStatus.LOADED,
                                                    data: toEngagementKPIs(current, previous),
                                                    error: null
                                                }
                                            });
                                        },
                                        (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                engagementKpis: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement',
                                                            'Failed to load engagement KPIs'
                                                        )
                                                }
                                            });
                                        }
                                    )
                                );
                            })
                        )
                    ),

                    /**
                     * Loads breakdown (engaged vs bounced doughnut) from current period totals.
                     */
                    _loadEngagementBreakdown: rxMethod<{
                        timeRange: TimeRangeInput;
                        currentSiteId: string;
                    }>(
                        pipe(
                            tap(() =>
                                patchState(store, {
                                    engagementBreakdown: {
                                        status: ComponentStatus.LOADING,
                                        data: null,
                                        error: null
                                    }
                                })
                            ),
                            switchMap(({ timeRange, currentSiteId }) => {
                                const dateRange = toTimeRangeCubeJS(timeRange);

                                const query = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(currentSiteId)
                                    .measures(['totalSessions', 'engagedSessions'])
                                    .timeRange('day', dateRange)
                                    .build();

                                return analyticsService
                                    .cubeQuery<EngagementDailyEntity>(query)
                                    .pipe(
                                        tapResponse(
                                            (rows) => {
                                                const row = rows?.[0];
                                                const total = row
                                                    ? Number(
                                                          row['EngagementDaily.totalSessions'] ?? 0
                                                      )
                                                    : 0;
                                                const engaged = row
                                                    ? Number(
                                                          row['EngagementDaily.engagedSessions'] ??
                                                              0
                                                      )
                                                    : 0;
                                                patchState(store, {
                                                    engagementBreakdown: {
                                                        status: ComponentStatus.LOADED,
                                                        data: toEngagementBreakdownChartData(
                                                            total,
                                                            engaged
                                                        ),
                                                        error: null
                                                    }
                                                });
                                            },
                                            (error: HttpErrorResponse) => {
                                                patchState(store, {
                                                    engagementBreakdown: {
                                                        status: ComponentStatus.ERROR,
                                                        data: null,
                                                        error:
                                                            error?.message ||
                                                            getErrorMessage(
                                                                'analytics.error.loading.engagement',
                                                                'Failed to load breakdown data'
                                                            )
                                                    }
                                                });
                                            }
                                        )
                                    );
                            })
                        )
                    ),

                    /**
                     * Loads sparkline data (engagement/conversion rate per day) for current and previous period.
                     */
                    _loadEngagementSparkline: rxMethod<{
                        timeRange: TimeRangeInput;
                        currentSiteId: string;
                    }>(
                        pipe(
                            tap(() =>
                                patchState(store, {
                                    engagementSparkline: {
                                        status: ComponentStatus.LOADING,
                                        data: null,
                                        error: null
                                    }
                                })
                            ),
                            switchMap(({ timeRange, currentSiteId }) => {
                                const dateRange = toTimeRangeCubeJS(timeRange);
                                const previousRange = getPreviousPeriod(timeRange);

                                const currentQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(currentSiteId)
                                    .measures([
                                        ...ENGAGEMENT_TREND_MEASURES,
                                        ...ENGAGEMENT_SPARKLINE_MEASURES
                                    ])
                                    .timeRange('day', dateRange, 'day')
                                    .build();

                                const previousQuery = createCubeQuery()
                                    .fromCube('EngagementDaily')
                                    .siteId(currentSiteId)
                                    .measures([
                                        ...ENGAGEMENT_TREND_MEASURES,
                                        ...ENGAGEMENT_SPARKLINE_MEASURES
                                    ])
                                    .timeRange('day', previousRange, 'day')
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
                                    tapResponse(
                                        ({ current, previous }) => {
                                            patchState(store, {
                                                engagementSparkline: {
                                                    status: ComponentStatus.LOADED,
                                                    data: {
                                                        current: toEngagementSparklineData(
                                                            current ?? []
                                                        ),
                                                        previous:
                                                            previous?.length > 0
                                                                ? toEngagementSparklineData(
                                                                      previous
                                                                  )
                                                                : null
                                                    },
                                                    error: null
                                                }
                                            });
                                        },
                                        (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                engagementSparkline: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement',
                                                            'Failed to load sparkline data'
                                                        )
                                                }
                                            });
                                        }
                                    )
                                );
                            })
                        )
                    ),

                    /**
                     * Loads platforms (device, browser) in parallel.
                     */
                    _loadEngagementPlatforms: rxMethod<{
                        timeRange: TimeRangeInput;
                        currentSiteId: string;
                    }>(
                        pipe(
                            tap(() =>
                                patchState(store, {
                                    engagementPlatforms: {
                                        status: ComponentStatus.LOADING,
                                        data: null,
                                        error: null
                                    }
                                })
                            ),
                            switchMap(({ timeRange, currentSiteId }) => {
                                const dateRange = toTimeRangeCubeJS(timeRange);

                                const deviceQuery = createCubeQuery()
                                    .fromCube('SessionsByDeviceDaily')
                                    .siteId(currentSiteId)
                                    .measures(SESSIONS_BY_MEASURES)
                                    .dimensions(SESSIONS_BY_DEVICE_DIMENSIONS)
                                    .timeRange('day', dateRange)
                                    .build();

                                const browserQuery = createCubeQuery()
                                    .fromCube('SessionsByBrowserDaily')
                                    .siteId(currentSiteId)
                                    .measures(SESSIONS_BY_MEASURES)
                                    .dimensions(SESSIONS_BY_BROWSER_DIMENSIONS)
                                    .timeRange('day', dateRange)
                                    .build();

                                return forkJoin({
                                    device: analyticsService.cubeQuery<SessionsByDeviceDailyEntity>(
                                        deviceQuery
                                    ),
                                    browser:
                                        analyticsService.cubeQuery<SessionsByBrowserDailyEntity>(
                                            browserQuery
                                        )
                                }).pipe(
                                    map(({ device, browser }) =>
                                        toEngagementPlatforms(device, browser)
                                    ),
                                    tapResponse(
                                        (platforms) =>
                                            patchState(store, {
                                                engagementPlatforms: {
                                                    status: ComponentStatus.LOADED,
                                                    data: platforms,
                                                    error: null
                                                }
                                            }),
                                        (error: HttpErrorResponse) =>
                                            patchState(store, {
                                                engagementPlatforms: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement',
                                                            'Failed to load platforms data'
                                                        )
                                                }
                                            })
                                    )
                                );
                            })
                        )
                    ),

                    /**
                     * Loads all engagement data. Dispatches independent requests per block.
                     */
                    loadEngagementData(): void {
                        const currentSiteId = globalStore.currentSiteId();
                        const timeRange = store.timeRange();

                        if (!currentSiteId) {
                            return;
                        }

                        const payload = { timeRange, currentSiteId };
                        this._loadEngagementKpis(payload);
                        this._loadEngagementSparkline(payload);
                        this._loadEngagementBreakdown(payload);
                        this._loadEngagementPlatforms(payload);
                    }
                };
            }
        )
    );
}
