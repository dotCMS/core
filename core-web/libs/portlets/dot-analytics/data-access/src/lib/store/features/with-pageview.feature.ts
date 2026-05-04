import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { format } from 'date-fns';
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
    DeviceBrowserData,
    PageViewDeviceBrowsersEntity,
    RequestState,
    TimeRangeInput,
    TopContentData,
    TotalEventsByDayData,
    TopPagePerformanceEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';
import {
    createInitialRequestState,
    fillMissingApiDates,
    toApiRangeParams
} from '../../utils/data/analytics-data.utils';

/**
 * State interface for the Pageview feature.
 * Contains all pageview-related data states.
 */
export interface PageviewState {
    totalPageViews: RequestState<TotalPageViewsEntity>;
    uniqueVisitors: RequestState<UniqueVisitorsEntity>;
    topPagePerformance: RequestState<TopPagePerformanceEntity>;
    pageViewTimeLine: RequestState<TotalEventsByDayData[]>;
    pageViewDeviceBrowsers: RequestState<PageViewDeviceBrowsersEntity[]>;
    topPagesTable: RequestState<TopContentData[]>;
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

function analyticsResponseBodyMessage(error: HttpErrorResponse): string | null {
    const body = error.error;
    if (typeof body === 'string' && body.trim()) {
        return body.trim();
    }
    if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message: unknown }).message;
        if (typeof m === 'string' && m.trim()) {
            return m.trim();
        }
    }
    return null;
}

/** User-facing message: prefer API body `message`, then `Error.message`, else i18n (never Angular's generic `HttpErrorResponse.message` alone). */
function pageviewFeatureErrorMessage(
    error: unknown,
    dotMessageService: DotMessageService,
    i18nKey: string
): string {
    if (error instanceof HttpErrorResponse) {
        return analyticsResponseBodyMessage(error) ?? dotMessageService.get(i18nKey);
    }
    if (error instanceof Error && error.message) {
        return error.message;
    }
    return dotMessageService.get(i18nKey);
}

/**
 * Signal Store Feature for managing pageview analytics data.
 *
 * This feature provides:
 * - State management for all pageview-related metrics
 * - Methods to load individual metrics (builds CubeJS queries directly)
 * - Coordinated method to load all pageview data
 *
 * @returns Signal store feature with pageview state and methods
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
                _loadTotalPageViews: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTotalEvents({
                                    ...rangeParams,
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                totalPageViews: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            const errorMessage = pageviewFeatureErrorMessage(
                                                error,
                                                dotMessageService,
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
                _loadUniqueVisitors: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
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
                            const rangeParams = toApiRangeParams(timeRange);

                            // Unique-visitors endpoint has no eventType filter; totals reflect whatever the backend aggregates.
                            return analyticsService
                                .getUniqueVisitors({
                                    ...rangeParams,
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                uniqueVisitors: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            const errorMessage = pageviewFeatureErrorMessage(
                                                error,
                                                dotMessageService,
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
                _loadTopPagePerformance: rxMethod<{
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTopContent({
                                    ...rangeParams,
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    map(
                                        (
                                            items: TopContentData[]
                                        ): TopPagePerformanceEntity | null => {
                                            if (!items?.length) {
                                                return null;
                                            }

                                            const top = items[0];

                                            return {
                                                identifier: top.identifier,
                                                title: top.title,
                                                totalEvents: top.totalEvents
                                            };
                                        }
                                    ),
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                topPagePerformance: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            const errorMessage = pageviewFeatureErrorMessage(
                                                error,
                                                dotMessageService,
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
                _loadPageViewTimeLine: rxMethod<{
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTotalEvents({
                                    ...rangeParams,
                                    granularity: 'day',
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    map((items) =>
                                        fillMissingApiDates(items, timeRange, 'day', (date) => ({
                                            day: format(date, 'yyyy-MM-dd'),
                                            totalEvents: 0
                                        }))
                                    ),
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                pageViewTimeLine: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            patchState(store, {
                                                pageViewTimeLine: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: pageviewFeatureErrorMessage(
                                                        error,
                                                        dotMessageService,
                                                        'analytics.error.loading.pageviews-timeline'
                                                    )
                                                }
                                            });
                                        }
                                    })
                                );
                        })
                    )
                ),

                // Page View Device Browsers
                _loadPageViewDeviceBrowsers: rxMethod<{
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getPageviewsByDeviceBrowser({
                                    ...rangeParams,
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    map(
                                        (
                                            items: DeviceBrowserData[]
                                        ): PageViewDeviceBrowsersEntity[] =>
                                            items.map((item) => ({
                                                browser: item.browser,
                                                device: item.device,
                                                total: item.total
                                            }))
                                    ),
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                pageViewDeviceBrowsers: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            const errorMessage = pageviewFeatureErrorMessage(
                                                error,
                                                dotMessageService,
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
                _loadTopPagesTable: rxMethod<{ timeRange: TimeRangeInput; currentSiteId: string }>(
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTopContent({
                                    ...rangeParams,
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                topPagesTable: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            const errorMessage = pageviewFeatureErrorMessage(
                                                error,
                                                dotMessageService,
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
            })
        ),
        withMethods((store, globalStore = inject(GlobalStore)) => ({
            /**
             * Coordinated method to load all pageview data.
             * Calls all individual load methods while maintaining their independent states.
             *
             * When `GlobalStore.currentSiteId()` is empty, loads are skipped and all pageview
             * slices are reset to initial `INIT` status so the UI does not stay stale or loading.
             */
            loadAllPageviewData(): void {
                const timeRange = store.timeRange();
                const currentSiteId = globalStore.currentSiteId();

                if (!currentSiteId) {
                    patchState(store, {
                        totalPageViews: createInitialRequestState(),
                        uniqueVisitors: createInitialRequestState(),
                        topPagePerformance: createInitialRequestState(),
                        pageViewTimeLine: createInitialRequestState(),
                        pageViewDeviceBrowsers: createInitialRequestState(),
                        topPagesTable: createInitialRequestState()
                    });
                    return;
                }

                store._loadTotalPageViews({ timeRange, currentSiteId });
                store._loadUniqueVisitors({ timeRange, currentSiteId });
                store._loadTopPagePerformance({ timeRange, currentSiteId });
                store._loadPageViewTimeLine({ timeRange, currentSiteId });
                store._loadPageViewDeviceBrowsers({ timeRange, currentSiteId });
                store._loadTopPagesTable({ timeRange, currentSiteId });
            }
        }))
    );
}
