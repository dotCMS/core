import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import {
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    RequestState,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../index';
import { TIME_RANGE_OPTIONS } from '../constants';
import { DotAnalyticsService } from '../services/dot-analytics.service';

/**
 * Main dashboard store state
 */
export interface DotAnalyticsDashboardState {
    timeRange: TimeRangeInput;

    // Individual request states
    totalPageViews: RequestState<TotalPageViewsEntity>;
    uniqueVisitors: RequestState<UniqueVisitorsEntity>;
    topPagePerformance: RequestState<TopPagePerformanceEntity>;
    pageViewTimeLine: RequestState<PageViewTimeLineEntity[]>;
    pageViewDeviceBrowsers: RequestState<PageViewDeviceBrowsersEntity[]>;
    topPagesTable: RequestState<TopPerformaceTableEntity[]>;
}

/**
 * Initial store state
 */
const initialState: DotAnalyticsDashboardState = {
    timeRange: TIME_RANGE_OPTIONS.last7days,
    totalPageViews: { status: ComponentStatus.INIT, data: null, error: null },
    uniqueVisitors: { status: ComponentStatus.INIT, data: null, error: null },
    topPagePerformance: { status: ComponentStatus.INIT, data: null, error: null },
    pageViewTimeLine: { status: ComponentStatus.INIT, data: null, error: null },
    pageViewDeviceBrowsers: { status: ComponentStatus.INIT, data: null, error: null },
    topPagesTable: { status: ComponentStatus.INIT, data: null, error: null }
};

export const DotAnalyticsDashboardStore = signalStore(
    withState(initialState),
    withMethods(
        (
            store,
            analyticsService = inject(DotAnalyticsService),
            dotMessageService = inject(DotMessageService)
        ) => {
            return {
                setTimeRange: (timeRange: TimeRangeInput) => {
                    patchState(store, { timeRange });
                },

                // Total Page Views
                loadTotalPageViews: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                totalPageViews: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService.totalPageViews(timeRange, currentSiteId).pipe(
                                tapResponse({
                                    next: (data: TotalPageViewsEntity) => {
                                        patchState(store, {
                                            totalPageViews: {
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
                                                'analytics.error.loading.total-pageviews'
                                            );
                                        patchState(store, {
                                            totalPageViews: {
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

                // Unique Visitors
                loadUniqueVisitors: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                uniqueVisitors: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService.uniqueVisitors(timeRange, currentSiteId).pipe(
                                tapResponse({
                                    next: (data: UniqueVisitorsEntity) => {
                                        patchState(store, {
                                            uniqueVisitors: {
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
                                                'analytics.error.loading.unique-visitors'
                                            );
                                        patchState(store, {
                                            uniqueVisitors: {
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

                // Top Page Performance
                loadTopPagePerformance: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                topPagePerformance: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService
                                .topPagePerformance(timeRange, currentSiteId)
                                .pipe(
                                    tapResponse({
                                        next: (data: TopPagePerformanceEntity) => {
                                            patchState(store, {
                                                topPagePerformance: {
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
                                                    'analytics.error.loading.top-page-performance'
                                                );
                                            patchState(store, {
                                                topPagePerformance: {
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

                // Page View Timeline
                loadPageViewTimeLine: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                pageViewTimeLine: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService.pageViewTimeLine(timeRange, currentSiteId).pipe(
                                tapResponse({
                                    next: (data: PageViewTimeLineEntity[]) => {
                                        patchState(store, {
                                            pageViewTimeLine: {
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
                                                'analytics.error.loading.pageviews-timeline'
                                            );
                                        patchState(store, {
                                            pageViewTimeLine: {
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

                // Page View Device Browsers
                loadPageViewDeviceBrowsers: rxMethod<{
                    timeRange: TimeRangeInput;
                    currentSiteId: string;
                }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                pageViewDeviceBrowsers: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService
                                .pageViewDeviceBrowsers(timeRange, currentSiteId)
                                .pipe(
                                    tapResponse({
                                        next: (data: PageViewDeviceBrowsersEntity[]) => {
                                            patchState(store, {
                                                pageViewDeviceBrowsers: {
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
                                                    'analytics.error.loading.device-breakdown'
                                                );
                                            patchState(store, {
                                                pageViewDeviceBrowsers: {
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

                // Top Pages Table
                loadTopPagesTable: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                topPagesTable: {
                                    status: ComponentStatus.LOADING,
                                    data: null,
                                    error: null
                                }
                            })
                        ),
                        switchMap(({ timeRange, currentSiteId }) => {
                            return analyticsService
                                .getTopPagePerformanceTable(timeRange, currentSiteId)
                                .pipe(
                                    tapResponse({
                                        next: (data: TopPerformaceTableEntity[]) => {
                                            patchState(store, {
                                                topPagesTable: {
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
                                                    'analytics.error.loading.top-pages-table'
                                                );
                                            patchState(store, {
                                                topPagesTable: {
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
                )
            };
        }
    ),

    withMethods((store) => ({
        /**
         * Coordinated method to load all dashboard data.
         * Calls all individual load methods while maintaining their independent states.
         */
        loadAllDashboardData: (timeRange: TimeRangeInput, currentSiteId: string) => {
            store.loadTotalPageViews({ timeRange, currentSiteId });
            store.loadUniqueVisitors({ timeRange, currentSiteId });
            store.loadTopPagePerformance({ timeRange, currentSiteId });
            store.loadPageViewTimeLine({ timeRange, currentSiteId });
            store.loadPageViewDeviceBrowsers({ timeRange, currentSiteId });
            store.loadTopPagesTable({ timeRange, currentSiteId });
        }
    })),

    withHooks({
        onInit: (store, globalStore = inject(GlobalStore)) => {
            // Auto-load data when both timeRange and currentSiteId are available
            effect(() => {
                const timeRange = store.timeRange();
                const currentSiteId = globalStore.currentSiteId();

                // Only load data if we have a valid site ID
                if (currentSiteId) {
                    store.loadAllDashboardData(timeRange, currentSiteId);
                }
            });
        }
    })
);
