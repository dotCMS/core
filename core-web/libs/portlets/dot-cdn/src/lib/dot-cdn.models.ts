import { ChartOptions } from 'chart.js';

export interface ChartDataSet {
    label: string;
    data: string[];
    borderColor?: string;
    fill?: boolean;
}

export interface DotCDNStats {
    stats: {
        bandwidthPretty: string;
        bandwidthUsedChart: { [key: string]: number };
        requestsServedChart: { [key: string]: number };
        cacheHitRateChart: { [key: string]: number };
        originResponseTimeChart: { [key: string]: number };
        error4xxChart: { [key: string]: number };
        error5xxChart: { [key: string]: number };
        cacheHitRate: number;
        averageOriginResponseTime: number;
        dateFrom: string;
        dateTo: string;
        geographicDistribution: unknown;
        totalBandwidthUsed: number;
        totalRequestsServed: number;
        cdnDomain: string;
    };
}

export interface ChartData {
    labels: string[];
    datasets: ChartDataSet[];
}

export interface DotChartStats {
    label: string;
    value: string;
    icon: string;
}

export interface PurgeUrlOptions {
    hostId: string;
    invalidateAll: boolean;
    urls?: string[];
}

export interface DotCDNState {
    chartBandwidthData: ChartData;
    chartRequestsData: ChartData;
    chartCacheHitRateData: ChartData;
    chartErrorData: ChartData;
    statsData: DotChartStats[];
    isChartLoading: boolean;
    cdnDomain: string;
    isPurgeUrlsLoading: boolean;
    isPurgeZoneLoading: boolean;
}

export type CdnChartOptions = {
    bandwidthUsedChart: ChartOptions;
    requestsServedChart: ChartOptions;
    cacheHitRateChart: ChartOptions;
    errorChart: ChartOptions;
};

export interface PurgeReturnData {
    entity: { [key: string]: string | boolean };
    errors: string[];
    messages: string[];
    permissions: string[];
    i18nMessagesMap: { [key: string]: string };
}

export const enum LoadingState {
    IDLE = 'IDLE',
    LOADING = 'LOADING',
    LOADED = 'LOADED'
}

export const enum Loader {
    CHART = 'CHART',
    PURGE_URLS = 'PURGE_URLS',
    PURGE_PULL_ZONE = 'PURGE_PULL_ZONE'
}
