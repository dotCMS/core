import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsService } from '../services/dot-analytics.service';

// Import types and utils from index
import {
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    transformDeviceBrowsersData,
    transformPageViewTimeLineData,
    transformTopPagesTableData
} from '../utils/data/analytics-data.utils';

import type {
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TimeRange,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../index';

/**
 * Individual request state interface
 */
interface RequestState<T = number> {
    status: ComponentStatus;
    data: T | null;
    error: string | null;
}

/**
 * Metric data structure for dashboard components
 */
export interface MetricData {
    name: string;
    value: number;
    subtitle: string;
    icon: string;
    status: ComponentStatus;
    error: string | null;
}


/**
 * Main dashboard store state
 */
export interface DotAnalyticsDashboardState {
    // Global time range
    timeRange: TimeRange;

    // Individual request states
    totalPageViews: RequestState<TotalPageViewsEntity>;
    uniqueVisitors: RequestState<UniqueVisitorsEntity>;
    topPagePerformance: RequestState<TopPagePerformanceEntity>;
    pageViewTimeLine: RequestState<PageViewTimeLineEntity[]>;
    pageViewDeviceBrowsers: RequestState<PageViewDeviceBrowsersEntity[]>;
    topPagesTable: RequestState<TopPerformaceTableEntity[]>;

    // Global tracking
    lastUpdated: Date | null;
}

/**
 * Base initial request state
 */
const INITIAL_REQUEST_STATE = {
    status: ComponentStatus.INIT,
    data: null,
    error: null
} as const;

/**
 * Initial store state
 */
const initialState: DotAnalyticsDashboardState = {
    timeRange: 'from 7 days ago to now',
    totalPageViews: { ...INITIAL_REQUEST_STATE },
    uniqueVisitors: { ...INITIAL_REQUEST_STATE },
    topPagePerformance: { ...INITIAL_REQUEST_STATE },
    pageViewTimeLine: { ...INITIAL_REQUEST_STATE },
    pageViewDeviceBrowsers: { ...INITIAL_REQUEST_STATE },
    topPagesTable: { ...INITIAL_REQUEST_STATE },
    lastUpdated: null
};

export const DotAnalyticsDashboardStore = signalStore(
    withState(initialState),
    withComputed(({ totalPageViews, uniqueVisitors, topPagePerformance, topPagesTable, pageViewTimeLine, pageViewDeviceBrowsers }) => ({

        // Grouped metrics data
        metricsData: computed((): MetricData[] => [
            {
                name: 'analytics.metrics.total-pageviews',
                value: extractPageViews(totalPageViews.data()),
                subtitle: 'analytics.metrics.total-pageviews.subtitle',
                icon: 'pi-eye',
                status: totalPageViews.status(),
                error: totalPageViews.error()
            },
            {
                name: 'analytics.metrics.unique-visitors',
                value: extractSessions(uniqueVisitors.data()),
                subtitle: 'analytics.metrics.unique-visitors.subtitle',
                icon: 'pi-users',
                status: uniqueVisitors.status(),
                error: uniqueVisitors.error()
            },
            {
                name: 'analytics.metrics.top-page-performance',
                value: extractTopPageValue(topPagePerformance.data()),
                subtitle: extractPageTitle(topPagePerformance.data()),
                icon: 'pi-chart-bar',
                status: topPagePerformance.status(),
                error: topPagePerformance.error()
            }
        ]),

        // Transformed table data ready for display
        topPagesTableData: computed(() => transformTopPagesTableData(topPagesTable.data())),

        // Transformed timeline data ready for display
        pageViewTimeLineData: computed(() => transformPageViewTimeLineData(pageViewTimeLine.data())),

        // Transformed device browsers data ready for display
        pageViewDeviceBrowsersData: computed(() => transformDeviceBrowsersData(pageViewDeviceBrowsers.data()))
    })),

    // All methods in a single withMethods
    withMethods((
        store, analyticsService = inject(DotAnalyticsService),
        dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
    ) => ({

        setTimeRange: (timeRange: TimeRange) => {
            patchState(store, { timeRange });
        },

        // Total Page Views
        loadTotalPageViews: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    totalPageViews: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.totalPageViews(timeRange).pipe(
                        tapResponse(
                            (data: TotalPageViewsEntity) => {
                                patchState(store, {
                                    totalPageViews: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load total page views';
                                patchState(store, {
                                    totalPageViews: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        ),

        // Unique Visitors
        loadUniqueVisitors: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    uniqueVisitors: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.uniqueVisitors(timeRange).pipe(
                        tapResponse(
                            (data: UniqueVisitorsEntity) => {
                                patchState(store, {
                                    uniqueVisitors: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load unique visitors';
                                patchState(store, {
                                    uniqueVisitors: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        ),

        // Top Page Performance
        loadTopPagePerformance: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    topPagePerformance: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.topPagePerformance(timeRange).pipe(
                        tapResponse(
                            (data: TopPagePerformanceEntity) => {
                                patchState(store, {
                                    topPagePerformance: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load top page performance';
                                patchState(store, {
                                    topPagePerformance: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        ),

        // Page View Timeline
        loadPageViewTimeLine: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    pageViewTimeLine: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.pageViewTimeLine(timeRange).pipe(
                        tapResponse(
                            (data: PageViewTimeLineEntity[]) => {
                                patchState(store, {
                                    pageViewTimeLine: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load page view timeline';
                                patchState(store, {
                                    pageViewTimeLine: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        ),

        // Page View Device Browsers
        loadPageViewDeviceBrowsers: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    pageViewDeviceBrowsers: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.pageViewDeviceBrowsers(timeRange).pipe(
                        tapResponse(
                            (data: PageViewDeviceBrowsersEntity[]) => {
                                patchState(store, {
                                    pageViewDeviceBrowsers: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load device browsers data';
                                patchState(store, {
                                    pageViewDeviceBrowsers: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        ),

        // Top Pages Table
        loadTopPagesTable: rxMethod<TimeRange>(
            pipe(
                tap(() => patchState(store, {
                    topPagesTable: { status: ComponentStatus.LOADING, data: null, error: null }
                })),
                switchMap((timeRange) =>
                    analyticsService.getTopPagePerformanceTable(timeRange, 50).pipe(
                        tapResponse(
                            (data: TopPerformaceTableEntity[]) => {

                                patchState(store, {
                                    topPagesTable: { status: ComponentStatus.LOADED, data, error: null },
                                    lastUpdated: new Date()
                                });
                            },
                            (error: HttpErrorResponse) => {
                                const errorMessage = error.message || 'Failed to load top pages table';
                                patchState(store, {
                                    topPagesTable: { status: ComponentStatus.ERROR, data: null, error: errorMessage }
                                });
                                dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
        )

    })
    )
);
