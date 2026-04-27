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
    DEFAULT_COUNT_LIMIT,
    DEFAULT_GRANULARITY,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    RequestState,
    TopPagePerformanceEntity,
    TopPerformanceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';
import { createCubeQuery } from '../../utils/cube/cube-query-builder.util';
import {
    createEmptyAnalyticsEntity,
    createInitialRequestState,
    fillMissingDates,
    toTimeRangeCubeJS
} from '../../utils/data/analytics-data.utils';
import { pageviewApiEvents } from '../events';

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
    topPagesTable: RequestState<TopPerformanceTableEntity[]>;
}

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
 * State transitions are driven by reducers reacting to per-metric
 * `*Requested` / `*Loaded` / `*Failed` events from `pageviewApiEvents`.
 * HTTP work happens in `withEventHandlers` which listens to `*Requested`
 * events, runs the CubeJS query via `DotAnalyticsService`, and dispatches
 * `*Loaded` (success) or `*Failed` (error). `switchMap` per metric
 * cancels stale requests when filters change.
 *
 * Loading is triggered indirectly by the autoload handler in
 * `../handlers/with-autoload.handlers.ts`, which fans out filter intents
 * and global site changes into the matching `*Requested` events.
 *
 * @returns Signal store feature wiring pageview state to its reducer and HTTP handlers
 */
export function withPageview() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialPageviewState),
        withReducer(
            // totalPageViews
            on<PageviewState>(pageviewApiEvents.totalPageViewsRequested, () => ({
                totalPageViews: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.totalPageViewsLoaded, ({ payload }) => ({
                totalPageViews: { status: ComponentStatus.LOADED, data: payload.data, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.totalPageViewsFailed, ({ payload }) => ({
                totalPageViews: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // uniqueVisitors
            on<PageviewState>(pageviewApiEvents.uniqueVisitorsRequested, () => ({
                uniqueVisitors: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.uniqueVisitorsLoaded, ({ payload }) => ({
                uniqueVisitors: { status: ComponentStatus.LOADED, data: payload.data, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.uniqueVisitorsFailed, ({ payload }) => ({
                uniqueVisitors: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // topPagePerformance
            on<PageviewState>(pageviewApiEvents.topPagePerformanceRequested, () => ({
                topPagePerformance: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.topPagePerformanceLoaded, ({ payload }) => ({
                topPagePerformance: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<PageviewState>(pageviewApiEvents.topPagePerformanceFailed, ({ payload }) => ({
                topPagePerformance: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // pageViewTimeLine
            on<PageviewState>(pageviewApiEvents.pageViewTimeLineRequested, () => ({
                pageViewTimeLine: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.pageViewTimeLineLoaded, ({ payload }) => ({
                pageViewTimeLine: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<PageviewState>(pageviewApiEvents.pageViewTimeLineFailed, ({ payload }) => ({
                pageViewTimeLine: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // pageViewDeviceBrowsers
            on<PageviewState>(pageviewApiEvents.pageViewDeviceBrowsersRequested, () => ({
                pageViewDeviceBrowsers: {
                    status: ComponentStatus.LOADING,
                    data: null,
                    error: null
                }
            })),
            on<PageviewState>(pageviewApiEvents.pageViewDeviceBrowsersLoaded, ({ payload }) => ({
                pageViewDeviceBrowsers: {
                    status: ComponentStatus.LOADED,
                    data: payload.data,
                    error: null
                }
            })),
            on<PageviewState>(pageviewApiEvents.pageViewDeviceBrowsersFailed, ({ payload }) => ({
                pageViewDeviceBrowsers: {
                    status: ComponentStatus.ERROR,
                    data: null,
                    error: payload.error
                }
            })),

            // topPagesTable
            on<PageviewState>(pageviewApiEvents.topPagesTableRequested, () => ({
                topPagesTable: { status: ComponentStatus.LOADING, data: null, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.topPagesTableLoaded, ({ payload }) => ({
                topPagesTable: { status: ComponentStatus.LOADED, data: payload.data, error: null }
            })),
            on<PageviewState>(pageviewApiEvents.topPagesTableFailed, ({ payload }) => ({
                topPagesTable: {
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
                loadTotalPageViews$: events.on(pageviewApiEvents.totalPageViewsRequested).pipe(
                    switchMap(({ payload }) => {
                        const query = createCubeQuery()
                            .fromCube('EventSummary')
                            .pageviews()
                            .measures(['totalEvents'])
                            .siteId(payload.currentSiteId)
                            .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                            .build();

                        return analyticsService.cubeQuery<TotalPageViewsEntity>(query).pipe(
                            map((entities) => entities[0]),
                            mapResponse({
                                next: (data) => pageviewApiEvents.totalPageViewsLoaded({ data }),
                                error: (error: HttpErrorResponse) =>
                                    pageviewApiEvents.totalPageViewsFailed({
                                        error:
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.total-pageviews'
                                            )
                                    })
                            })
                        );
                    })
                ),

                loadUniqueVisitors$: events.on(pageviewApiEvents.uniqueVisitorsRequested).pipe(
                    switchMap(({ payload }) => {
                        const query = createCubeQuery()
                            .fromCube('EventSummary')
                            .pageviews()
                            .measures(['uniqueVisitors'])
                            .siteId(payload.currentSiteId)
                            .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                            .build();

                        return analyticsService.cubeQuery<UniqueVisitorsEntity>(query).pipe(
                            map((entities) => entities[0]),
                            mapResponse({
                                next: (data) => pageviewApiEvents.uniqueVisitorsLoaded({ data }),
                                error: (error: HttpErrorResponse) =>
                                    pageviewApiEvents.uniqueVisitorsFailed({
                                        error:
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.unique-visitors'
                                            )
                                    })
                            })
                        );
                    })
                ),

                loadTopPagePerformance$: events
                    .on(pageviewApiEvents.topPagePerformanceRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('EventSummary')
                                .pageviews()
                                .dimensions(['identifier', 'title'])
                                .measures(['totalEvents'])
                                .siteId(payload.currentSiteId)
                                .orderBy('totalEvents', 'desc')
                                .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                                .limit(1)
                                .build();

                            return analyticsService.cubeQuery<TopPagePerformanceEntity>(query).pipe(
                                map((entities) => entities[0]),
                                mapResponse({
                                    next: (data) =>
                                        pageviewApiEvents.topPagePerformanceLoaded({ data }),
                                    error: (error: HttpErrorResponse) =>
                                        pageviewApiEvents.topPagePerformanceFailed({
                                            error:
                                                error.message ||
                                                dotMessageService.get(
                                                    'analytics.error.loading.top-page-performance'
                                                )
                                        })
                                })
                            );
                        })
                    ),

                loadPageViewTimeLine$: events.on(pageviewApiEvents.pageViewTimeLineRequested).pipe(
                    switchMap(({ payload }) => {
                        const query = createCubeQuery()
                            .fromCube('EventSummary')
                            .pageviews()
                            .measures(['totalEvents'])
                            .siteId(payload.currentSiteId)
                            .timeRange(
                                'day',
                                toTimeRangeCubeJS(payload.timeRange),
                                DEFAULT_GRANULARITY
                            )
                            .build();

                        return analyticsService.cubeQuery<PageViewTimeLineEntity>(query).pipe(
                            map((entities) =>
                                fillMissingDates<PageViewTimeLineEntity>(
                                    entities,
                                    payload.timeRange,
                                    DEFAULT_GRANULARITY,
                                    createEmptyAnalyticsEntity
                                )
                            ),
                            mapResponse({
                                next: (data) => pageviewApiEvents.pageViewTimeLineLoaded({ data }),
                                error: (error: HttpErrorResponse) =>
                                    pageviewApiEvents.pageViewTimeLineFailed({
                                        error:
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.pageviews-timeline'
                                            )
                                    })
                            })
                        );
                    })
                ),

                loadPageViewDeviceBrowsers$: events
                    .on(pageviewApiEvents.pageViewDeviceBrowsersRequested)
                    .pipe(
                        switchMap(({ payload }) => {
                            const query = createCubeQuery()
                                .fromCube('request')
                                .pageviews()
                                .dimensions(['userAgent'])
                                .measures(['count'])
                                .siteId(payload.currentSiteId)
                                .orderBy('totalRequest', 'desc')
                                .timeRange('createdAt', toTimeRangeCubeJS(payload.timeRange))
                                .limit(DEFAULT_COUNT_LIMIT)
                                .build();

                            return analyticsService
                                .cubeQuery<PageViewDeviceBrowsersEntity>(query)
                                .pipe(
                                    mapResponse({
                                        next: (data) =>
                                            pageviewApiEvents.pageViewDeviceBrowsersLoaded({
                                                data
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            pageviewApiEvents.pageViewDeviceBrowsersFailed({
                                                error:
                                                    error.message ||
                                                    dotMessageService.get(
                                                        'analytics.error.loading.device-breakdown'
                                                    )
                                            })
                                    })
                                );
                        })
                    ),

                loadTopPagesTable$: events.on(pageviewApiEvents.topPagesTableRequested).pipe(
                    switchMap(({ payload }) => {
                        const query = createCubeQuery()
                            .fromCube('EventSummary')
                            .pageviews()
                            .dimensions(['identifier', 'title'])
                            .measures(['totalEvents'])
                            .siteId(payload.currentSiteId)
                            .orderBy('totalEvents', 'desc')
                            .timeRange('day', toTimeRangeCubeJS(payload.timeRange))
                            .limit(DEFAULT_COUNT_LIMIT)
                            .build();

                        return analyticsService.cubeQuery<TopPerformanceTableEntity>(query).pipe(
                            mapResponse({
                                next: (data) => pageviewApiEvents.topPagesTableLoaded({ data }),
                                error: (error: HttpErrorResponse) =>
                                    pageviewApiEvents.topPagesTableFailed({
                                        error:
                                            error.message ||
                                            dotMessageService.get(
                                                'analytics.error.loading.top-pages-table'
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
