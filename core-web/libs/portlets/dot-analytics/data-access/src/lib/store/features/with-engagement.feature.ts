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
import {
    createInitialRequestState,
    getPreviousPeriod,
    toApiRangeParams
} from '../../utils/data/analytics-data.utils';
import {
    toEngagementBreakdownChartData,
    toEngagementKPIs,
    toEngagementPlatforms,
    toEngagementSparklineData
} from '../../utils/data/engagement-data.utils';

import type {
    ChartData,
    EngagementKPIs,
    EngagementPlatforms,
    EngagementSparklineData,
    GetSessionEngagementByDay,
    RequestState,
    SessionEngagementByDayData,
    SessionEngagementGroupByData,
    TimeRangeInput
} from '../../types';

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
 *
 * `_load*` methods are store-internal; consumers should call `loadEngagementData()` only.
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
                                const rangeParams = toApiRangeParams(timeRange);
                                const previousPeriod = getPreviousPeriod(timeRange);
                                const previousRangeParams = toApiRangeParams(previousPeriod);

                                // Let forkJoin fail on HTTP errors so tapResponse.error can surface ERROR state.
                                return forkJoin({
                                    current: analyticsService.getSessionEngagement({
                                        ...rangeParams,
                                        siteId: currentSiteId
                                    }),
                                    previous: analyticsService.getSessionEngagement({
                                        ...previousRangeParams,
                                        siteId: currentSiteId
                                    })
                                }).pipe(
                                    tapResponse({
                                        next: ({ current, previous }) => {
                                            patchState(store, {
                                                engagementKpis: {
                                                    status: ComponentStatus.LOADED,
                                                    data: toEngagementKPIs(current, previous),
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                engagementKpis: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement-kpis',
                                                            'Failed to load engagement KPIs'
                                                        )
                                                }
                                            });
                                        }
                                    })
                                );
                            })
                        )
                    ),

                    /**
                     * Loads breakdown (engaged vs bounced doughnut) from current period totals.
                     * Intentionally repeats the aggregate session-engagement request used by KPIs so this slice
                     * can load and error independently (parallel with other loaders on `loadEngagementData`).
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
                                const rangeParams = toApiRangeParams(timeRange);

                                return analyticsService
                                    .getSessionEngagement({
                                        ...rangeParams,
                                        siteId: currentSiteId
                                    })
                                    .pipe(
                                        tapResponse({
                                            next: (data) => {
                                                patchState(store, {
                                                    engagementBreakdown: {
                                                        status: ComponentStatus.LOADED,
                                                        data: toEngagementBreakdownChartData(
                                                            data.totalSessions,
                                                            data.engagedSessions
                                                        ),
                                                        error: null
                                                    }
                                                });
                                            },
                                            error: (error: HttpErrorResponse) => {
                                                patchState(store, {
                                                    engagementBreakdown: {
                                                        status: ComponentStatus.ERROR,
                                                        data: null,
                                                        error:
                                                            error?.message ||
                                                            getErrorMessage(
                                                                'analytics.error.loading.engagement-breakdown',
                                                                'Failed to load breakdown data'
                                                            )
                                                    }
                                                });
                                            }
                                        })
                                    );
                            })
                        )
                    ),

                    /**
                     * Loads sparkline data (conversion rate per day) for current and previous period.
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
                                const rangeParams = toApiRangeParams(timeRange);
                                const previousPeriod = getPreviousPeriod(timeRange);
                                const previousRangeParams = toApiRangeParams(previousPeriod);

                                const currentDayParams: GetSessionEngagementByDay = {
                                    ...rangeParams,
                                    granularity: 'day',
                                    siteId: currentSiteId
                                };
                                const previousDayParams: GetSessionEngagementByDay = {
                                    ...previousRangeParams,
                                    granularity: 'day',
                                    siteId: currentSiteId
                                };

                                return forkJoin({
                                    current:
                                        analyticsService.getSessionEngagement(currentDayParams),
                                    previous: analyticsService
                                        .getSessionEngagement(previousDayParams)
                                        .pipe(
                                            catchError(() => of([] as SessionEngagementByDayData[]))
                                        )
                                }).pipe(
                                    tapResponse({
                                        next: ({ current, previous }) => {
                                            patchState(store, {
                                                engagementSparkline: {
                                                    status: ComponentStatus.LOADED,
                                                    data: {
                                                        current: toEngagementSparklineData(current),
                                                        previous:
                                                            previous.length > 0
                                                                ? toEngagementSparklineData(
                                                                      previous
                                                                  )
                                                                : null
                                                    },
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            patchState(store, {
                                                engagementSparkline: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement-sparkline',
                                                            'Failed to load sparkline data'
                                                        )
                                                }
                                            });
                                        }
                                    })
                                );
                            })
                        )
                    ),

                    /**
                     * Loads platforms (device, browser, language) in parallel via groupBy.
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
                                const rangeParams = toApiRangeParams(timeRange);

                                return forkJoin({
                                    device: analyticsService
                                        .getSessionEngagementGroupBy({
                                            ...rangeParams,
                                            groupBy: 'device',
                                            siteId: currentSiteId
                                        })
                                        .pipe(
                                            catchError(() =>
                                                of([] as SessionEngagementGroupByData[])
                                            )
                                        ),
                                    browser: analyticsService
                                        .getSessionEngagementGroupBy({
                                            ...rangeParams,
                                            groupBy: 'browser',
                                            siteId: currentSiteId
                                        })
                                        .pipe(
                                            catchError(() =>
                                                of([] as SessionEngagementGroupByData[])
                                            )
                                        ),
                                    language: analyticsService
                                        .getSessionEngagementGroupBy({
                                            ...rangeParams,
                                            groupBy: 'language',
                                            siteId: currentSiteId
                                        })
                                        .pipe(
                                            catchError(() =>
                                                of([] as SessionEngagementGroupByData[])
                                            )
                                        )
                                }).pipe(
                                    map(({ device, browser, language }) =>
                                        toEngagementPlatforms(device, browser, language)
                                    ),
                                    tapResponse({
                                        next: (platforms) =>
                                            patchState(store, {
                                                engagementPlatforms: {
                                                    status: ComponentStatus.LOADED,
                                                    data: platforms,
                                                    error: null
                                                }
                                            }),
                                        error: (error: HttpErrorResponse) =>
                                            patchState(store, {
                                                engagementPlatforms: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error:
                                                        error?.message ||
                                                        getErrorMessage(
                                                            'analytics.error.loading.engagement-platforms',
                                                            'Failed to load platforms data'
                                                        )
                                                }
                                            })
                                    })
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
