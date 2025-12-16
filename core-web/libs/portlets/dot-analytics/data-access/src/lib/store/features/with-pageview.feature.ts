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

import { map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    createInitialRequestState,
    DEFAULT_COUNT_LIMIT,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    RequestState,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';
import { createCubeQuery } from '../../utils/cube/cube-query-builder.util';
import {
    determineGranularityForTimeRange,
    fillMissingDates,
    toTimeRangeCubeJS
} from '../../utils/data/analytics-data.utils';

/**
 * State interface for the Pageview feature.
 * Contains all pageview-related data states.
 */
export interface PageviewState {
    totalPageViews: RequestState<TotalPageViewsEntity>;
    uniqueVisitors: RequestState<UniqueVisitorsEntity>;
    topPagePerformance: RequestState<TopPagePerformanceEntity>;
    pageViewTimeLine: RequestState<PageViewTimeLineEntity[]>;
    pageViewDeviceBrowsers: RequestState<PageViewDeviceBrowsersEntity[]>;
    topPagesTable: RequestState<TopPerformaceTableEntity[]>;
}

/**
 * Initial state for the Pageview feature.
 */
const initialPageviewState: PageviewState = {
    totalPageViews: createInitialRequestState(),
    uniqueVisitors: createInitialRequestState(),
    topPagePerformance: createInitialRequestState(),
    pageViewTimeLine: createInitialRequestState(),
    pageViewDeviceBrowsers: createInitialRequestState(),
    topPagesTable: createInitialRequestState()
};

/**
 * Signal Store Feature for managing pageview analytics data.
 *
 * This feature provides:
 * - State management for all pageview-related metrics
 * - Methods to load individual metrics (builds CubeJS queries directly)
 * - Auto-loading via effect when timeRange or siteId changes
 *
 * @returns Signal store feature with pageview state, methods, and hooks
 */
export function withPageview() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialPageviewState),
        withMethods(
            (
                store,
                analyticsService = inject(DotAnalyticsService),
                dotMessageService = inject(DotMessageService)
            ) => ({
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
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .measures(['totalRequest'])
                                .siteId(currentSiteId)
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService.cubeQuery<TotalPageViewsEntity>(query).pipe(
                                map((entities) => entities[0]),
                                tapResponse(
                                    (data) => {
                                        patchState(store, {
                                            totalPageViews: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
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
                                )
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
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .measures(['totalUsers'])
                                .siteId(currentSiteId)
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange))
                                .build();

                            return analyticsService.cubeQuery<UniqueVisitorsEntity>(query).pipe(
                                map((entities) => entities[0]),
                                tapResponse(
                                    (data) => {
                                        patchState(store, {
                                            uniqueVisitors: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
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
                                )
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
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .dimensions(['path', 'pageTitle'])
                                .measures(['totalRequest'])
                                .siteId(currentSiteId)
                                .orderBy('totalRequest', 'desc')
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange))
                                .limit(1)
                                .build();

                            return analyticsService.cubeQuery<TopPagePerformanceEntity>(query).pipe(
                                map((entities) => entities[0]),
                                tapResponse(
                                    (data) => {
                                        patchState(store, {
                                            topPagePerformance: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
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
                                )
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
                            const granularity = determineGranularityForTimeRange(timeRange);
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .measures(['totalRequest'])
                                .siteId(currentSiteId)
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange), granularity)
                                .build();

                            return analyticsService.cubeQuery<PageViewTimeLineEntity>(query).pipe(
                                map((entities) =>
                                    fillMissingDates(entities, timeRange, granularity)
                                ),
                                tapResponse(
                                    (data) => {
                                        patchState(store, {
                                            pageViewTimeLine: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            pageViewTimeLine: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.pageviews-timeline'
                                                    )
                                            }
                                        });
                                    }
                                )
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
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .dimensions(['userAgent'])
                                .measures(['totalRequest'])
                                .siteId(currentSiteId)
                                .orderBy('totalRequest', 'desc')
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange))
                                .limit(DEFAULT_COUNT_LIMIT)
                                .build();

                            return analyticsService
                                .cubeQuery<PageViewDeviceBrowsersEntity>(query)
                                .pipe(
                                    tapResponse(
                                        (data) => {
                                            patchState(store, {
                                                pageViewDeviceBrowsers: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        (error: HttpErrorResponse) => {
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
                                    )
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
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .dimensions(['path', 'pageTitle'])
                                .measures(['totalRequest'])
                                .siteId(currentSiteId)
                                .orderBy('totalRequest', 'desc')
                                .timeRange('createdAt', toTimeRangeCubeJS(timeRange))
                                .limit(DEFAULT_COUNT_LIMIT)
                                .build();

                            return analyticsService.cubeQuery<TopPerformaceTableEntity>(query).pipe(
                                tapResponse(
                                    (data) => {
                                        patchState(store, {
                                            topPagesTable: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    (error: HttpErrorResponse) => {
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
                                )
                            );
                        })
                    )
                )
            })
        ),
        withMethods((store, globalStore = inject(GlobalStore)) => ({
            /**
             * Coordinated method to load all pageview data.
             * Calls all individual load methods while maintaining their independent states.
             */
            loadAllPageviewData(): void {
                const timeRange = store.timeRange();
                const currentSiteId = globalStore.currentSiteId();

                if (currentSiteId) {
                    store.loadTotalPageViews({ timeRange, currentSiteId });
                    store.loadUniqueVisitors({ timeRange, currentSiteId });
                    store.loadTopPagePerformance({ timeRange, currentSiteId });
                    store.loadPageViewTimeLine({ timeRange, currentSiteId });
                    store.loadPageViewDeviceBrowsers({ timeRange, currentSiteId });
                    store.loadTopPagesTable({ timeRange, currentSiteId });
                }
            }
        })),
        withHooks({
            onInit: (store, globalStore = inject(GlobalStore)) => {
                // Auto-load data when timeRange or currentSiteId changes
                effect(() => {
                    // Read signals to establish reactivity
                    store.timeRange();
                    globalStore.currentSiteId();

                    // loadAllPageviewData handles the validation internally
                    store.loadAllPageviewData();
                });
            }
        })
    );
}
