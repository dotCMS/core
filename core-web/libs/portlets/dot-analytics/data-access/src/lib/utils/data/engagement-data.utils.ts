import { AnalyticsChartColors, BAR_CHART_STYLE } from '../../constants';

import type {
    ChartData,
    EngagementDailyEntity,
    EngagementKPIs,
    EngagementPlatformMetrics,
    EngagementPlatforms,
    SessionsByBrowserDailyEntity,
    SessionsByDeviceDailyEntity,
    SparklineDataPoint
} from '../../types';

const EMPTY_KPIS: EngagementKPIs = {
    totalSessions: { value: 0, trend: 0, label: 'Total Sessions' },
    engagementRate: {
        value: 0,
        trend: 0,
        label: 'Engagement Rate',
        subtitle: '0 Engaged Sessions'
    },
    avgInteractions: { value: 0, trend: 0, label: 'Avg Interactions (Engaged)' },
    avgSessionTime: { value: '0m 0s', trend: 0, label: 'Average Session Time' },
    conversionRate: { value: '0%', trend: 0, label: 'Conversion Rate' }
};

function parseNum(s: string | undefined | null): number {
    if (!s) return 0;
    const n = Number(s);
    return Number.isFinite(n) ? n : 0;
}

/**
 * Format seconds to "Xm Ys" for display.
 */
export function formatSecondsToTime(seconds: number): string {
    if (!Number.isFinite(seconds) || seconds < 0) return '0m 0s';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}m ${s}s`;
}

/**
 * Compute trend percentage: ((current - previous) / previous) * 100.
 * - When previous is 0 or missing and current > 0: returns 100 (+100%, "subió desde cero").
 * - When both 0: returns 0.
 * - Otherwise: normal percentage change.
 */
export function computeTrendPercent(current: number, previous: number | null | undefined): number {
    const prev = previous ?? 0;
    if (prev === 0) {
        return current > 0 ? 100 : 0;
    }
    return Math.round(((current - prev) / prev) * 1000) / 10;
}

/**
 * Map EngagementDaily totals (current + previous) to EngagementKPIs.
 * Handles empty/insufficient data with defaults.
 */
export function toEngagementKPIs(
    currentRow: EngagementDailyEntity | null,
    previousRow: EngagementDailyEntity | null
): EngagementKPIs | null {
    const current = currentRow ?? {};
    const previous = previousRow ?? {};

    const totalSessionsCur = parseNum(current['EngagementDaily.totalSessions']);

    if (totalSessionsCur === 0) {
        return null;
    }

    const totalSessionsPrev = parseNum(previous['EngagementDaily.totalSessions']);
    const engagedSessionsCur = parseNum(current['EngagementDaily.engagedSessions']);
    const engagementRateCur = parseNum(current['EngagementDaily.engagementRate']);
    const conversionRateCur = parseNum(current['EngagementDaily.conversionRate']);
    const avgInteractionsCur = parseNum(
        current['EngagementDaily.avgInteractionsPerEngagedSession']
    );
    const avgSessionTimeCur = parseNum(current['EngagementDaily.avgSessionTimeSeconds']);

    const engagementRatePrev = parseNum(previous['EngagementDaily.engagementRate']);
    const conversionRatePrev = parseNum(previous['EngagementDaily.conversionRate']);
    const avgInteractionsPrev = parseNum(
        previous['EngagementDaily.avgInteractionsPerEngagedSession']
    );
    const avgSessionTimePrev = parseNum(previous['EngagementDaily.avgSessionTimeSeconds']);

    const engagementRateTrend = computeTrendPercent(engagementRateCur, engagementRatePrev);
    const totalSessionsTrend = computeTrendPercent(totalSessionsCur, totalSessionsPrev);
    const conversionRateTrend = computeTrendPercent(conversionRateCur, conversionRatePrev);
    const avgInteractionsTrend = computeTrendPercent(avgInteractionsCur, avgInteractionsPrev);
    const avgSessionTimeTrend = computeTrendPercent(avgSessionTimeCur, avgSessionTimePrev);

    const engagementRateValue = Math.round(engagementRateCur * 10000) / 100;
    const conversionRateValue = `${Math.round(conversionRateCur * 1000) / 10}%`;

    return {
        totalSessions: {
            value: totalSessionsCur,
            trend: totalSessionsTrend,
            label: 'Total Sessions'
        },
        engagementRate: {
            value: engagementRateValue,
            trend: engagementRateTrend,
            label: 'Engagement Rate',
            subtitle: `${engagedSessionsCur.toLocaleString()} Engaged Sessions`
        },
        avgInteractions: {
            value: avgInteractionsCur,
            trend: avgInteractionsTrend,
            label: 'Avg Interactions (Engaged)'
        },
        avgSessionTime: {
            value: formatSecondsToTime(avgSessionTimeCur),
            trend: avgSessionTimeTrend,
            label: 'Average Session Time'
        },
        conversionRate: {
            value: conversionRateValue,
            trend: conversionRateTrend,
            label: 'Conversion Rate'
        }
    };
}

/**
 * Map EngagementDaily trend-by-day rows to SparklineDataPoint[].
 * Uses conversion rate (engaged_conversion_sessions / total_sessions) per day so the trend
 * varies meaningfully; engagement rate per day is often 100% when engaged_sessions === total_sessions.
 * When only 1 point exists, prepends a synthetic point at 0 so Chart.js draws a line.
 */
export function toEngagementSparklineData(rows: EngagementDailyEntity[]): SparklineDataPoint[] {
    if (!rows?.length) return [];

    const points = rows.map((row) => {
        const day = row['EngagementDaily.day.day'] ?? row['EngagementDaily.day'] ?? '';
        const rate = parseNum(row['EngagementDaily.conversionRate']);
        return {
            date: typeof day === 'string' ? day.slice(0, 10) : '',
            value: Math.round(rate * 100)
        };
    });

    if (points.length === 1) {
        const only = points[0];
        const prevDate = getPreviousDay(only.date);
        points.unshift({ date: prevDate, value: 0 });
    }

    return points;
}

function getPreviousDay(dateStr: string): string {
    const date = new Date(dateStr + 'T00:00:00');
    date.setDate(date.getDate() - 1);
    return date.toISOString().slice(0, 10);
}

/**
 * Map EngagementDaily by-day rows to ChartData for the trend chart.
 * Handles empty array with default empty ChartData.
 */
export function toEngagementTrendChartData(rows: EngagementDailyEntity[]): ChartData {
    if (!rows?.length) {
        return { labels: [], datasets: [] };
    }

    const labels = rows.map((row) => {
        const day = row['EngagementDaily.day.day'] ?? row['EngagementDaily.day'];
        if (typeof day === 'string') return day.slice(0, 10);
        return '';
    });
    const data = rows.map((row) => parseNum(row['EngagementDaily.engagedSessions']));

    return {
        labels,
        datasets: [
            {
                label: 'Trend',
                data,
                backgroundColor: AnalyticsChartColors.primary.line,
                ...BAR_CHART_STYLE
            }
        ]
    };
}

/**
 * Map totalSessions and engagedSessions to doughnut ChartData (Engaged vs Bounced).
 * Returns empty ChartData when totalSessions is 0 so the chart shows empty state.
 */
export function toEngagementBreakdownChartData(
    totalSessions: number,
    engagedSessions: number
): ChartData {
    if (totalSessions === 0) {
        return { labels: [], datasets: [] };
    }
    const bounced = Math.max(0, totalSessions - engagedSessions);
    const engagedPct = Math.round((engagedSessions / totalSessions) * 100);
    const bouncedPct = 100 - engagedPct;

    return {
        labels: [`Engaged Sessions (${engagedPct}%)`, `Bounced Sessions (${bouncedPct}%)`],
        datasets: [
            {
                label: 'Engagement Breakdown',
                data: [engagedSessions, bounced],
                backgroundColor: [AnalyticsChartColors.primary.line, '#000000']
            }
        ]
    };
}

function toPlatformMetrics(
    name: string,
    views: number,
    totalViews: number,
    avgTimeSeconds: number
): EngagementPlatformMetrics {
    const percentage = totalViews > 0 ? Math.round((views / totalViews) * 100) : 0;
    return {
        name,
        views,
        percentage,
        time: formatSecondsToTime(avgTimeSeconds)
    };
}

/**
 * Map SessionsByDeviceDaily rows to device platform metrics.
 */
export function toEngagementPlatformsFromDevice(
    rows: SessionsByDeviceDailyEntity[] | null
): EngagementPlatformMetrics[] {
    if (!rows?.length) return [];
    const total = rows.reduce(
        (sum, r) => sum + parseNum(r['SessionsByDeviceDaily.engagedSessions']),
        0
    );
    return rows.map((row) => {
        const views = parseNum(row['SessionsByDeviceDaily.engagedSessions']);
        const avgSec = parseNum(row['SessionsByDeviceDaily.avgEngagedSessionTimeSeconds']);
        const name = row['SessionsByDeviceDaily.deviceCategory'] ?? 'Other';
        return toPlatformMetrics(name, views, total, avgSec);
    });
}

/**
 * Map SessionsByBrowserDaily rows to browser platform metrics.
 */
export function toEngagementPlatformsFromBrowser(
    rows: SessionsByBrowserDailyEntity[] | null
): EngagementPlatformMetrics[] {
    if (!rows?.length) return [];
    const total = rows.reduce(
        (sum, r) => sum + parseNum(r['SessionsByBrowserDaily.engagedSessions']),
        0
    );
    return rows.map((row) => {
        const views = parseNum(row['SessionsByBrowserDaily.engagedSessions']);
        const avgSec = parseNum(row['SessionsByBrowserDaily.avgEngagedSessionTimeSeconds']);
        const name = row['SessionsByBrowserDaily.browserFamily'] ?? 'Other';
        return toPlatformMetrics(name, views, total, avgSec);
    });
}

/**
 * Build full EngagementPlatforms from device and browser arrays.
 */
export function toEngagementPlatforms(
    deviceRows: SessionsByDeviceDailyEntity[] | null,
    browserRows: SessionsByBrowserDailyEntity[] | null
): EngagementPlatforms {
    return {
        device: toEngagementPlatformsFromDevice(deviceRows),
        browser: toEngagementPlatformsFromBrowser(browserRows)
    };
}

/**
 * Default empty KPIs when request fails or has no data.
 */
export function getEmptyEngagementKPIs(): EngagementKPIs {
    return { ...EMPTY_KPIS };
}

/**
 * Default empty ChartData for trend or breakdown when no data.
 */
export function getEmptyEngagementChartData(): ChartData {
    return { labels: [], datasets: [] };
}

/**
 * Default empty EngagementPlatforms when no data.
 */
export function getEmptyEngagementPlatforms(): EngagementPlatforms {
    return { device: [], browser: [] };
}
