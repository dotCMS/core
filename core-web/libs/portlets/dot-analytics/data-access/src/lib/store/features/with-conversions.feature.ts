import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { format } from 'date-fns';
import { forkJoin, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { FiltersState } from './with-filters.feature';

import { DotAnalyticsService } from '../../services/dot-analytics.service';
import {
    ContentAttributionData,
    ConversionOverviewData,
    ConvertingVisitorsEntity,
    RequestState,
    TimeRangeInput,
    TotalEventsByDayData,
    TotalEventsData,
    UniqueVisitorsByDayData
} from '../../types';
import {
    createInitialRequestState,
    fillMissingApiDates,
    toApiRangeParams,
    TrafficVsConversionsDayData
} from '../../utils/data/analytics-data.utils';

function zipDailyUniqueVisitorsForTrafficChart(
    visitors: UniqueVisitorsByDayData[],
    converting: UniqueVisitorsByDayData[]
): TrafficVsConversionsDayData[] {
    const convertingByDay = new Map(converting.map((c) => [c.day, c.uniqueVisitors]));

    return visitors.map((v) => ({
        day: v.day,
        uniqueVisitors: v.uniqueVisitors,
        uniqueConvertingVisitors: convertingByDay.get(v.day) ?? 0
    }));
}

function analyticsResponseBodyMessage(error: HttpErrorResponse): string | null {
    const body = error.error;
    if (typeof body === 'string' && body.trim()) {
        const trimmed = body.trim();
        if (trimmed.startsWith('<')) {
            return null;
        }
        return trimmed;
    }
    if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message: unknown }).message;
        if (typeof m === 'string' && m.trim()) {
            return m.trim();
        }
    }
    return null;
}

function conversionsFeatureErrorMessage(
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
 * State interface for the Conversions feature.
 * Contains all conversion-related data states.
 */
export interface ConversionsState {
    /** Total conversions metric */
    totalConversions: RequestState<TotalEventsData>;
    /** Converting visitors metric (includes uniqueVisitors and uniqueConvertingVisitors) */
    convertingVisitors: RequestState<ConvertingVisitorsEntity>;
    /** Site-wide conversion rate */
    conversionRate: RequestState<number>;
    /** Conversion trend timeline data */
    conversionTrend: RequestState<TotalEventsByDayData[]>;
    /** Traffic vs conversions comparison data (per day) */
    trafficVsConversions: RequestState<TrafficVsConversionsDayData[]>;
    /** Content attribution table data */
    contentConversions: RequestState<ContentAttributionData[]>;
    /** Conversions overview table data */
    conversionsOverview: RequestState<ConversionOverviewData[]>;
}

/**
 * Initial state for the Conversions feature.
 */
const initialConversionsState: ConversionsState = {
    totalConversions: createInitialRequestState(),
    convertingVisitors: createInitialRequestState(),
    conversionRate: createInitialRequestState(),
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTotalEvents({
                                    ...rangeParams,
                                    eventType: 'conversion',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                totalConversions: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            patchState(store, {
                                                totalConversions: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: conversionsFeatureErrorMessage(
                                                        error,
                                                        dotMessageService,
                                                        'analytics.error.loading.total-conversions'
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getTotalEvents({
                                    ...rangeParams,
                                    granularity: 'day',
                                    eventType: 'conversion',
                                    siteId: currentSiteId
                                })
                                .pipe(
                                    map((items) =>
                                        fillMissingApiDates(
                                            items as TotalEventsByDayData[],
                                            timeRange,
                                            'day',
                                            (date) => ({
                                                day: format(date, 'yyyy-MM-dd'),
                                                totalEvents: 0
                                            })
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
                                        error: (error: unknown) => {
                                            patchState(store, {
                                                conversionTrend: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: conversionsFeatureErrorMessage(
                                                        error,
                                                        dotMessageService,
                                                        'analytics.error.loading.conversion-trend'
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return forkJoin({
                                uniqueVisitorsData: analyticsService.getUniqueVisitors({
                                    ...rangeParams,
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                }),
                                uniqueConvertingData: analyticsService.getUniqueVisitors({
                                    ...rangeParams,
                                    eventType: 'conversion',
                                    siteId: currentSiteId
                                })
                            }).pipe(
                                map(({ uniqueVisitorsData, uniqueConvertingData }) => ({
                                    uniqueVisitors: uniqueVisitorsData.uniqueVisitors,
                                    uniqueConvertingVisitors: uniqueConvertingData.uniqueVisitors
                                })),
                                tapResponse({
                                    next: (data: ConvertingVisitorsEntity) => {
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: unknown) => {
                                        patchState(store, {
                                            convertingVisitors: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: conversionsFeatureErrorMessage(
                                                    error,
                                                    dotMessageService,
                                                    'analytics.error.loading.converting-visitors'
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
                 * Loads traffic vs conversions chart data (per day).
                 * Returns uniqueVisitors (bars) and unique converting visitors (line) per day.
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return forkJoin({
                                visitors: analyticsService.getUniqueVisitors({
                                    ...rangeParams,
                                    granularity: 'day',
                                    eventType: 'pageview',
                                    siteId: currentSiteId
                                }),
                                converting: analyticsService.getUniqueVisitors({
                                    ...rangeParams,
                                    granularity: 'day',
                                    eventType: 'conversion',
                                    siteId: currentSiteId
                                })
                            }).pipe(
                                map(({ visitors, converting }) => {
                                    const visitorsArr = visitors as UniqueVisitorsByDayData[];
                                    const convertingArr = converting as UniqueVisitorsByDayData[];

                                    const filledVisitors = fillMissingApiDates(
                                        visitorsArr,
                                        timeRange,
                                        'day',
                                        (d) => ({
                                            day: format(d, 'yyyy-MM-dd'),
                                            uniqueVisitors: 0
                                        })
                                    );
                                    const filledConverting = fillMissingApiDates(
                                        convertingArr,
                                        timeRange,
                                        'day',
                                        (d) => ({
                                            day: format(d, 'yyyy-MM-dd'),
                                            uniqueVisitors: 0
                                        })
                                    );

                                    return zipDailyUniqueVisitorsForTrafficChart(
                                        filledVisitors,
                                        filledConverting
                                    );
                                }),
                                tapResponse({
                                    next: (data: TrafficVsConversionsDayData[]) => {
                                        patchState(store, {
                                            trafficVsConversions: {
                                                status: ComponentStatus.LOADED,
                                                data,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: unknown) => {
                                        patchState(store, {
                                            trafficVsConversions: {
                                                status: ComponentStatus.ERROR,
                                                data: null,
                                                error: conversionsFeatureErrorMessage(
                                                    error,
                                                    dotMessageService,
                                                    'analytics.error.loading.traffic-vs-conversions'
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
                 * Loads content attribution table data.
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getContentAttribution({
                                    ...rangeParams,
                                    siteId: currentSiteId,
                                    page: 1,
                                    pageSize: 20,
                                    orderBy: 'attributionCount',
                                    orderDir: 'desc'
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                contentConversions: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            patchState(store, {
                                                contentConversions: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: conversionsFeatureErrorMessage(
                                                        error,
                                                        dotMessageService,
                                                        'analytics.error.loading.content-conversions'
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
                 * Loads conversions overview table data.
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
                            const rangeParams = toApiRangeParams(timeRange);

                            return analyticsService
                                .getConversionsOverview({
                                    ...rangeParams,
                                    siteId: currentSiteId,
                                    page: 1,
                                    pageSize: 20,
                                    orderBy: 'totalConversions',
                                    orderDir: 'desc'
                                })
                                .pipe(
                                    tapResponse({
                                        next: (data) => {
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.LOADED,
                                                    data,
                                                    error: null
                                                }
                                            });
                                        },
                                        error: (error: unknown) => {
                                            patchState(store, {
                                                conversionsOverview: {
                                                    status: ComponentStatus.ERROR,
                                                    data: null,
                                                    error: conversionsFeatureErrorMessage(
                                                        error,
                                                        dotMessageService,
                                                        'analytics.error.loading.conversions-overview'
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
                 * Loads all conversions data.
                 * This method is called when the conversions tab is activated.
                 */
                loadConversionsData(): void {
                    const currentSiteId = globalStore.currentSiteId();
                    const timeRange = store.timeRange();

                    if (!currentSiteId) {
                        patchState(store, {
                            totalConversions: createInitialRequestState(),
                            convertingVisitors: createInitialRequestState(),
                            conversionRate: createInitialRequestState(),
                            conversionTrend: createInitialRequestState(),
                            trafficVsConversions: createInitialRequestState(),
                            contentConversions: createInitialRequestState(),
                            conversionsOverview: createInitialRequestState()
                        });
                        return;
                    }

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
