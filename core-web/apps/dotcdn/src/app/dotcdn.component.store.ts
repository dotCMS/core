import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { map, mergeMap, switchMapTo, tap } from 'rxjs/operators';

import {
    ChartData,
    ChartPeriod,
    DotCDNState,
    DotCDNStats,
    DotChartStats,
    Loader,
    LoadingState,
    PurgeReturnData
} from './app.models';
import { DotCDNService } from './dotcdn.service';

@Injectable()
export class DotCDNStore extends ComponentStore<DotCDNState> {
    private readonly dotCdnService = inject(DotCDNService);
    selectedPeriod: SelectItem<string> = { value: ChartPeriod.Last15Days };

    constructor() {
        super({
            chartBandwidthData: {
                labels: [],
                datasets: []
            },
            chartRequestsData: {
                labels: [],
                datasets: []
            },
            cdnDomain: '',
            statsData: [],
            isChartLoading: false,
            isPurgeUrlsLoading: false,
            isPurgeZoneLoading: false
        });
        this.getChartStats(this.selectedPeriod.value);
    }

    readonly vm$ = this.select(
        ({ isChartLoading, chartBandwidthData, chartRequestsData, statsData, cdnDomain }) => ({
            chartBandwidthData,
            chartRequestsData,
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
        ) => {
            return {
                ...state,
                chartBandwidthData: chartData.chartBandwidthData,
                chartRequestsData: chartData.chartRequestsData,
                cdnDomain: chartData.cdnDomain,
                statsData: chartData.statsData
            };
        }
    );

    /**
     *  Handles the chart data fetching
     *
     * @memberof DotCDNStore
     */
    getChartStats = this.effect((period$: Observable<string>): Observable<DotCDNStats> => {
        return period$.pipe(
            mergeMap((period: string) => {
                // Dispatch the loading state
                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.CHART
                });

                return this.dotCdnService.requestStats(period).pipe(
                    tapResponse(
                        (data: DotCDNStats) => {
                            // Now the chart is loaded
                            this.dispatchLoading({
                                loadingState: LoadingState.LOADED,
                                loader: Loader.CHART
                            });

                            const {
                                statsData,
                                chartData: [chartBandwidthData, chartRequestsData],
                                cdnDomain
                            } = this.getChartStatsData(data);

                            this.updateChartState({
                                chartBandwidthData,
                                chartRequestsData,
                                statsData,
                                cdnDomain
                            });
                        },
                        (_error) => {
                            // TODO: Handle error
                        }
                    )
                );
            })
        );
    });

    /**
     *  Dispatches a loading state
     *
     * @memberof DotCDNStore
     */
    readonly dispatchLoading = this.updater(
        (state, action: { loadingState: string; loader: string }) => {
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
            }
        }
    );

    /**
     * Purges the CDN cache
     *
     * @param {boolean} [invalidateAll=false]
     * @param {string[]} [urls]
     * @return {*}  {(Observable<ResponseView<any>> | void)}
     * @memberof DotCDNStore
     */

    purgeCDNCache(urls: string[]): Observable<PurgeReturnData> {
        const loading$ = of(
            this.dispatchLoading({
                loadingState: LoadingState.LOADING,
                loader: Loader.PURGE_URLS
            })
        );

        return loading$.pipe(
            switchMapTo(
                this.dotCdnService.purgeCache(urls).pipe(
                    tap(() => {
                        this.dispatchLoading({
                            loadingState: LoadingState.LOADED,
                            loader: Loader.PURGE_URLS
                        });
                    })
                )
            )
        );
    }

    purgeCDNCacheAll(): void {
        const $loading = of(
            this.dispatchLoading({
                loadingState: LoadingState.LOADING,
                loader: Loader.PURGE_PULL_ZONE
            })
        );

        $loading
            .pipe(
                switchMapTo(this.dotCdnService.purgeCacheAll()),
                map((x: any) => x?.bodyJsonObject)
            )
            .subscribe(() => {
                this.dispatchLoading({
                    loadingState: LoadingState.LOADED,
                    loader: Loader.PURGE_PULL_ZONE
                });
            });
    }

    private getChartStatsData({ stats }: DotCDNStats) {
        const chartData: ChartData[] = [
            {
                labels: this.getLabels(stats.bandwidthUsedChart),
                datasets: [
                    {
                        label: 'Bandwidth Used',
                        data: Object.values(stats.bandwidthUsedChart).map((values) =>
                            (values / 1e6).toFixed(2).toString()
                        ),
                        borderColor: '#6f5fa3',
                        fill: false
                    }
                ]
            },
            {
                labels: this.getLabels(stats.requestsServedChart),
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
            }
        ];

        const statsData: DotChartStats[] = [
            {
                label: 'Bandwidth Used',
                value: stats.bandwidthPretty,
                icon: 'insert_chart_outlined'
            },
            {
                label: 'Requests Served',
                value: `${stats.totalRequestsServed}`,
                icon: 'file_download'
            },
            {
                label: 'Cache Hit Rate',
                value: `${stats.cacheHitRate.toFixed(2)}%`,
                icon: 'file_download'
            }
        ];

        return { chartData, statsData, cdnDomain: stats.cdnDomain };
    }

    private formatDate(date) {
        return new Date(date).toLocaleDateString('en-GB', {
            month: '2-digit',
            day: '2-digit'
        });
    }

    /**
     *
     * This private method is responsible for transforming the keys from bandwidthUsedChart in order to make it more readable
     * It takes the timestamp and removes the time from the string
     */
    private getLabels(data: { [key: string]: number }): string[] {
        return Object.keys(data).map((label) => {
            return this.formatDate(label.split('T')[0]);
        });
    }
}
