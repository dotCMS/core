import { ChartData } from './entities.types';

/** Data point for sparkline with date and value */
export interface SparklineDataPoint {
    date: string;
    value: number;
}

export interface EngagementKPI {
    value: number | string;
    trend: number;
    label: string;
    /** Optional subtitle text */
    subtitle?: string;
    /** Optional sparkline data points for trend visualization */
    sparklineData?: SparklineDataPoint[];
}

export interface EngagementKPIs {
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
