import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { mergeMap, switchMap, tap } from 'rxjs/operators';

import {
    ChartData,
    ChartPeriod,
    DotCDNState,
    DotCDNStats,
    DotChartStats,
    Loader,
    LoadingState,
    PurgeReturnData
} from './dot-cdn.models';
import { DotCDNService } from './dot-cdn.service';

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

    getChartStats = this.effect((period$: Observable<string>): Observable<DotCDNStats> => {
        return period$.pipe(
            mergeMap((period: string) => {
                // TODO: Remove mock handling before merging
                if (period.startsWith('mock-')) {
                    const mockData = DotCDNStore.generateMockData(period);
                    this.dispatchLoading({ loadingState: LoadingState.LOADED, loader: Loader.CHART });
                    const {
                        statsData,
                        chartData: [chartBandwidthData, chartRequestsData],
                        cdnDomain
                    } = this.getChartStatsData(mockData);
                    this.updateChartState({ chartBandwidthData, chartRequestsData, statsData, cdnDomain });

                    return of(mockData);
                }

                this.dispatchLoading({
                    loadingState: LoadingState.LOADING,
                    loader: Loader.CHART
                });

                return this.dotCdnService.requestStats(period).pipe(
                    tapResponse({
                        next: (data: DotCDNStats) => {
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
                        error: (_error) => {
                            // TODO: Handle error
                        }
                    })
                );
            })
        );
    });

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

        $loading.pipe(switchMap(() => this.dotCdnService.purgeCacheAll())).subscribe(() => {
            this.dispatchLoading({
                loadingState: LoadingState.LOADED,
                loader: Loader.PURGE_PULL_ZONE
            });
        });
    }

    private getChartStatsData({ stats }: DotCDNStats) {
        const bandwidthValues = Object.values(stats.bandwidthUsedChart);
        const { divisor, unit } = DotCDNStore.pickBandwidthUnit(bandwidthValues);

        const chartData: ChartData[] = [
            {
                labels: this.getLabels(stats.bandwidthUsedChart),
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
                value: DotCDNStore.formatNumber(stats.totalRequestsServed),
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

    /**
     * Pick the best bandwidth unit based on the max value in the dataset.
     */
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

    /**
     * Format large numbers with commas (e.g. 1,234,567).
     */
    private static formatNumber(value: number): string {
        return value.toLocaleString('en-US');
    }

    private formatDate(date: string): string {
        return new Date(date).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        });
    }

    private getLabels(data: { [key: string]: number }): string[] {
        return Object.keys(data).map((label) => {
            return this.formatDate(label.split('T')[0]);
        });
    }

    // TODO: Remove before merging — mock data for visual testing
    private static generateMockData(mode: string): DotCDNStats {
        const multipliers: Record<string, { bw: number; req: number }> = {
            'mock-high': { bw: 5e9, req: 500000 },
            'mock-medium': { bw: 80e6, req: 25000 },
            'mock-low': { bw: 500e3, req: 200 }
        };
        const { bw, req } = multipliers[mode] || multipliers['mock-medium'];

        const bandwidthUsedChart: Record<string, number> = {};
        const requestsServedChart: Record<string, number> = {};
        let totalBw = 0;
        let totalReq = 0;

        for (let i = 14; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            const key = date.toISOString().split('T')[0] + 'T00:00:00';
            const bwVal = Math.round(bw * (0.5 + Math.random()));
            const reqVal = Math.round(req * (0.5 + Math.random()));
            bandwidthUsedChart[key] = bwVal;
            requestsServedChart[key] = reqVal;
            totalBw += bwVal;
            totalReq += reqVal;
        }

        const prettyBw = totalBw >= 1e9
            ? (totalBw / 1e9).toFixed(2) + ' GB'
            : totalBw >= 1e6
                ? (totalBw / 1e6).toFixed(2) + ' MB'
                : (totalBw / 1e3).toFixed(2) + ' KB';

        return {
            stats: {
                bandwidthPretty: prettyBw,
                bandwidthUsedChart,
                requestsServedChart,
                cacheHitRate: 72.5 + Math.random() * 20,
                dateFrom: Object.keys(bandwidthUsedChart)[0],
                dateTo: Object.keys(bandwidthUsedChart)[14],
                geographicDistribution: {},
                totalBandwidthUsed: totalBw,
                totalRequestsServed: totalReq,
                cdnDomain: 'https://mock-cdn.example.com'
            }
        };
    }
}
