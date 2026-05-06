import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { format, subDays } from 'date-fns';
import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { finalize, mergeMap, switchMap } from 'rxjs/operators';

import { CdnDateFilter } from './dot-cdn-filters/dot-cdn-filters.component';
import {
    ChartData,
    DotCDNState,
    DotCDNStats,
    DotChartStats,
    Loader,
    LoadingState,
    PurgeReturnData
} from './dot-cdn.models';
import { DotCDNService, StatsRequest } from './dot-cdn.service';

const DEFAULT_FILTER: CdnDateFilter = {
    dateFrom: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
    dateTo: format(new Date(), 'yyyy-MM-dd'),
    hourly: false
};

@Injectable()
export class DotCDNStore extends ComponentStore<DotCDNState> {
    private readonly dotCdnService = inject(DotCDNService);

    constructor() {
        super({
            chartBandwidthData: { labels: [], datasets: [] },
            chartRequestsData: { labels: [], datasets: [] },
            chartCacheHitRateData: { labels: [], datasets: [] },
            chartErrorData: { labels: [], datasets: [] },
            cdnDomain: '',
            statsData: [],
            isChartLoading: false,
            isPurgeUrlsLoading: false,
            isPurgeZoneLoading: false
        });
        this.loadStats(DEFAULT_FILTER);
    }

    readonly vm$ = this.select(
        ({ isChartLoading, chartBandwidthData, chartRequestsData,
           chartCacheHitRateData, chartErrorData, statsData, cdnDomain }) => ({
            chartBandwidthData,
            chartRequestsData,
            chartCacheHitRateData,
            chartErrorData,
            statsData,
            isChartLoading,
            cdnDomain
        })
    );

    readonly vmPurgeLoaders$ = this.select(({ isPurgeUrlsLoading, isPurgeZoneLoading }) => ({
        isPurgeUrlsLoading,
        isPurgeZoneLoading
    }));

    readonly updateChartState = this.updater(
        (
            state,
            chartData: Omit<
                DotCDNState,
                'isChartLoading' | 'isPurgeUrlsLoading' | 'isPurgeZoneLoading'
            >
        ) => ({
            ...state,
            chartBandwidthData: chartData.chartBandwidthData,
            chartRequestsData: chartData.chartRequestsData,
            chartCacheHitRateData: chartData.chartCacheHitRateData,
            chartErrorData: chartData.chartErrorData,
            cdnDomain: chartData.cdnDomain,
            statsData: chartData.statsData
        })
    );

    loadStats = this.effect((filter$: Observable<CdnDateFilter>): Observable<DotCDNStats> => {
        return filter$.pipe(
            mergeMap((filter: CdnDateFilter) => {
                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.CHART
                });

                const request: StatsRequest = {
                    dateFrom: filter.dateFrom,
                    dateTo: filter.dateTo,
                    hourly: filter.hourly
                };

                return this.dotCdnService.requestStats(request).pipe(
                    tapResponse({
                        next: (data: DotCDNStats) => {
                            const result = this.getChartStatsData(data, filter.hourly);
                            this.updateChartState(result);
                        },
                        error: (_error) => undefined
                    }),
                    finalize(() => this.dispatchLoading({
                        loadingState: LoadingState.LOADED,
                        loader: Loader.CHART
                    }))
                );
            })
        );
    });

    readonly dispatchLoading = this.updater(
        (state, action: { loadingState: string; loader: string }): DotCDNState => {
            switch (action.loader) {
                case Loader.CHART:
                    return {
                        ...state,
                        isChartLoading: action.loadingState === LoadingState.LOADING
                    };
                case Loader.PURGE_URLS:
                    return {
                        ...state,
                        isPurgeUrlsLoading: action.loadingState === LoadingState.LOADING
                    };
                case Loader.PURGE_PULL_ZONE:
                    return {
                        ...state,
                        isPurgeZoneLoading: action.loadingState === LoadingState.LOADING
                    };
                default:
                    return state;
            }
        }
    );

    purgeCDNCache(urls: string[]): Observable<PurgeReturnData> {
        const loading$ = of(
            this.dispatchLoading({
                loadingState: LoadingState.LOADING,
                loader: Loader.PURGE_URLS
            })
        );

        return loading$.pipe(
            switchMap(() =>
                this.dotCdnService.purgeCache(urls).pipe(
                    finalize(() => this.dispatchLoading({
                        loadingState: LoadingState.LOADED,
                        loader: Loader.PURGE_URLS
                    }))
                )
            )
        );
    }

    readonly purgeCDNCacheAll = this.effect<void>((trigger$: Observable<void>) => {
        return trigger$.pipe(
            switchMap(() => {
                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.PURGE_PULL_ZONE
                });

                return this.dotCdnService.purgeCacheAll().pipe(
                    tapResponse({
                        next: () => undefined,
                        error: (_error) => undefined
                    }),
                    finalize(() => this.dispatchLoading({
                        loadingState: LoadingState.LOADED,
                        loader: Loader.PURGE_PULL_ZONE
                    }))
                );
            })
        );
    });

    private getChartStatsData({ stats }: DotCDNStats, hourly: boolean) {
        const bandwidthValues = Object.values(stats.bandwidthUsedChart);
        const { divisor, unit } = DotCDNStore.pickBandwidthUnit(bandwidthValues);
        const labelFormatter = hourly
            ? DotCDNStore.formatHourLabel
            : DotCDNStore.formatDayLabel;

        const chartBandwidthData: ChartData = {
            labels: Object.keys(stats.bandwidthUsedChart).map(labelFormatter),
            datasets: [
                {
                    label: `Bandwidth (${unit})`,
                    data: bandwidthValues.map((v) =>
                        (v / divisor).toFixed(2).toString()
                    ),
                    borderColor: '#6f5fa3',
                    fill: false
                }
            ]
        };

        const chartRequestsData: ChartData = {
            labels: Object.keys(stats.requestsServedChart).map(labelFormatter),
            datasets: [
                {
                    label: 'Requests Served',
                    data: Object.values(stats.requestsServedChart).map(
                        (value: number): string => value.toString()
                    ),
                    borderColor: '#FFA726',
                    fill: false
                }
            ]
        };

        const cacheHitKeys = Object.keys(stats.cacheHitRateChart || {});
        const chartCacheHitRateData: ChartData = {
            labels: cacheHitKeys.map(labelFormatter),
            datasets: [
                {
                    label: 'Cache Hit Rate (%)',
                    data: Object.values(stats.cacheHitRateChart || {}).map(
                        (v: number): string => v.toFixed(2)
                    ),
                    borderColor: '#1ea97c',
                    fill: false
                }
            ]
        };

        const errorKeys = Object.keys(stats.error4xxChart || {});
        const chartErrorData: ChartData = {
            labels: errorKeys.map(labelFormatter),
            datasets: [
                {
                    label: '4xx Errors',
                    data: Object.values(stats.error4xxChart || {}).map(
                        (v: number): string => v.toString()
                    ),
                    borderColor: '#FFA726',
                    fill: false
                },
                {
                    label: '5xx Errors',
                    data: Object.values(stats.error5xxChart || {}).map(
                        (v: number): string => v.toString()
                    ),
                    borderColor: '#f65446',
                    fill: false
                }
            ]
        };

        const statsData: DotChartStats[] = [
            {
                label: 'Bandwidth Used',
                value: stats.bandwidthPretty,
                icon: 'insert_chart_outlined'
            },
            {
                label: 'Requests Served',
                value: DotCDNStore.formatNumber(stats.totalRequestsServed),
                icon: 'file_download'
            },
            {
                label: 'Cache Hit Rate',
                value: `${stats.cacheHitRate.toFixed(2)}%`,
                icon: 'speed'
            },
            {
                label: 'Avg Origin Response',
                value: stats.averageOriginResponseTime != null
                    ? `${stats.averageOriginResponseTime}ms` : 'N/A',
                icon: 'timer'
            }
        ];

        return {
            chartBandwidthData,
            chartRequestsData,
            chartCacheHitRateData,
            chartErrorData,
            statsData,
            cdnDomain: stats.cdnDomain
        };
    }

    private static pickBandwidthUnit(values: number[]): { divisor: number; unit: string } {
        const max = Math.max(...values, 0);
        if (max >= 1e9) {
            return { divisor: 1e9, unit: 'GB' };
        } else if (max >= 1e6) {
            return { divisor: 1e6, unit: 'MB' };
        } else if (max >= 1e3) {
            return { divisor: 1e3, unit: 'KB' };
        }

        return { divisor: 1, unit: 'B' };
    }

    private static formatNumber(value: number): string {
        return value.toLocaleString('en-US');
    }

    /**
     * Format for hourly data: "Apr 1 2pm"
     */
    private static formatHourLabel(key: string): string {
        const date = new Date(key);

        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
            + ' ' + date.toLocaleTimeString('en-US', { hour: 'numeric', hour12: true });
    }

    /**
     * Format for daily data: "Apr 1"
     */
    private static formatDayLabel(key: string): string {
        return new Date(key).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        });
    }
}
