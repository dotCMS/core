import { MetricFormat } from './common.types';
import { ChartData } from './entities.types';

/** Data point for sparkline with date and value */
export interface SparklineDataPoint {
    date: string;
    value: number;
}

/** Current and previous period data for sparkline comparison */
export interface EngagementSparklineData {
    current: SparklineDataPoint[];
    previous: SparklineDataPoint[] | null;
}

export interface EngagementKPI {
    /** Raw numeric value (the component handles formatting based on `format`) */
    value: number;
    /** Display format hint. Default is 'number'. */
    format?: MetricFormat;
    /** Trend percentage. Undefined when no prior data exists for comparison. */
    trend?: number;
    label: string;
    /** Optional subtitle text */
    subtitle?: string;
}

export interface EngagementKPIs {
    totalSessions: EngagementKPI;
    engagementRate: EngagementKPI;
    avgInteractions: EngagementKPI;
    avgSessionTime: EngagementKPI;
    conversionRate: EngagementKPI;
}

export interface EngagementPlatformMetrics {
    name: string;
    views: number;
    percentage: number;
    time: string;
}

export interface EngagementPlatforms {
    device: EngagementPlatformMetrics[];
    browser: EngagementPlatformMetrics[];
    language: EngagementPlatformMetrics[];
}

export interface EngagementData {
    kpis: EngagementKPIs;
    trend: ChartData;
    breakdown: ChartData;
    platforms: EngagementPlatforms;
}
