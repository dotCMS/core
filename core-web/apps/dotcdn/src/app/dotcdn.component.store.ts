import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';

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
    selectedPeriod: SelectItem<string> = { value: ChartPeriod.Last15Days };

    constructor(
        private readonly dotCdnService: DotCDNService,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly dotMessageService: DotMessageService
    ) {
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
        this.dotMessageService.init();
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
            tap(() => {
                // Dispatch the loading state
                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.CHART
                });
            }),
            switchMap((period: string) =>
                this.dotCdnService.requestStats(period).pipe(
                    tapResponse(
                        (data: DotCDNStats) => {
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

                            // Now the chart is loaded
                            this.dispatchLoading({
                                loadingState: LoadingState.LOADED,
                                loader: Loader.CHART
                            });
                        },
                        (error: HttpErrorResponse) => {
                            this.dotHttpErrorManagerService.handle(error);
                            this.dispatchLoading({
                                loadingState: LoadingState.IDLE,
                                loader: Loader.CHART
                            });
                        }
                    )
                )
            )
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
        return of(urls).pipe(
            tap(() => {
                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.PURGE_URLS
                });
            }),
            switchMap((urls: string[]) =>
                this.dotCdnService.purgeCache(urls).pipe(
                    tapResponse(
                        () => {
                            this.dispatchLoading({
                                loadingState: LoadingState.LOADED,
                                loader: Loader.PURGE_URLS
                            });
                        },
                        (error: HttpErrorResponse) => {
                            this.dotHttpErrorManagerService.handle(error);
                            this.dispatchLoading({
                                loadingState: LoadingState.IDLE,
                                loader: Loader.PURGE_URLS
                            });
                        }
                    )
                )
            )
        );
    }

    purgeCDNCacheAll() {
        of('*')
            .pipe(
                tap(() => {
                    this.dispatchLoading({
                        loadingState: LoadingState.LOADING,
                        loader: Loader.PURGE_PULL_ZONE
                    });
                }),
                switchMap(() =>
                    this.dotCdnService.purgeCacheAll().pipe(
                        tapResponse(
                            () => {
                                this.dispatchLoading({
                                    loadingState: LoadingState.LOADED,
                                    loader: Loader.PURGE_PULL_ZONE
                                });
                            },
                            (error: HttpErrorResponse) => {
                                this.dotHttpErrorManagerService.handle(error);
                                this.dispatchLoading({
                                    loadingState: LoadingState.IDLE,
                                    loader: Loader.PURGE_PULL_ZONE
                                });
                            }
                        )
                    )
                )
            )
            .subscribe();
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
