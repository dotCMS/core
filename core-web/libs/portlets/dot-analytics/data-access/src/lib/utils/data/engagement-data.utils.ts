import { AnalyticsChartColors, BAR_CHART_STYLE } from '../../constants';

import type {
    ChartData,
    EngagementKPIs,
    EngagementPlatformMetrics,
    EngagementPlatforms,
    SessionEngagementByDayData,
    SessionEngagementData,
    SessionEngagementGroupByData,
    SparklineDataPoint
} from '../../types';

const EMPTY_KPIS: EngagementKPIs = {
    totalSessions: { value: 0, trend: 0, label: 'Total Sessions' },
    engagementRate: {
        value: 0,
        format: 'percentage',
        trend: 0,
        label: 'Engagement Rate',
        subtitle: '0 Engaged Sessions'
    },
    avgInteractions: { value: 0, trend: 0, label: 'Avg Interactions (Engaged)' },
    avgSessionTime: { value: 0, format: 'time', trend: 0, label: 'Average Session Time' },
    conversionRate: { value: 0, format: 'percentage', trend: 0, label: 'Conversion Rate' }
};

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
 * Map session engagement aggregate data (current + previous) to EngagementKPIs.
 * The new API returns numbers directly and `engagementRate` as a percentage (e.g. 28.57).
 */
export function toEngagementKPIs(
    currentRow: SessionEngagementData | null,
    previousRow: SessionEngagementData | null
): EngagementKPIs | null {
    if (!currentRow || currentRow.totalSessions === 0) {
        return null;
    }

    const hasPriorData = previousRow != null;
    let engagementRateTrend: number | undefined;
    let totalSessionsTrend: number | undefined;
    let conversionRateTrend: number | undefined;
    let avgInteractionsTrend: number | undefined;
    let avgSessionTimeTrend: number | undefined;

    if (hasPriorData) {
        engagementRateTrend = computeTrendPercent(
            currentRow.engagementRate,
            previousRow.engagementRate
        );
        totalSessionsTrend = computeTrendPercent(
            currentRow.totalSessions,
            previousRow.totalSessions
        );
        conversionRateTrend = computeTrendPercent(
            currentRow.conversionRate,
            previousRow.conversionRate
        );
        avgInteractionsTrend = computeTrendPercent(
            currentRow.avgInteractionsPerEngagedSession,
            previousRow.avgInteractionsPerEngagedSession
        );
        avgSessionTimeTrend = computeTrendPercent(
            currentRow.avgSessionTimeSeconds,
            previousRow.avgSessionTimeSeconds
        );
    }

    return {
        totalSessions: {
            value: currentRow.totalSessions,
            trend: totalSessionsTrend,
            label: 'Total Sessions'
        },
        engagementRate: {
            value: Math.round(currentRow.engagementRate * 100) / 100,
            format: 'percentage',
            trend: engagementRateTrend,
            label: 'Engagement Rate',
            subtitle: `${currentRow.engagedSessions.toLocaleString()} Engaged Sessions`
        },
        avgInteractions: {
            value: currentRow.avgInteractionsPerEngagedSession,
            trend: avgInteractionsTrend,
            label: 'Avg Interactions (Engaged)'
        },
        avgSessionTime: {
            value: currentRow.avgSessionTimeSeconds,
            format: 'time',
            trend: avgSessionTimeTrend,
            label: 'Average Session Time'
        },
        conversionRate: {
            /** Same decimal precision as the engagement rate KPI (percentage from API). */
            value: Math.round(currentRow.conversionRate * 100) / 100,
            format: 'percentage',
            trend: conversionRateTrend,
            label: 'Conversion Rate'
        }
    };
}

/**
 * Map session engagement by-day rows to SparklineDataPoint[].
 * Uses conversion rate per day so the trend varies meaningfully.
 * When only 1 point exists, prepends a synthetic point at 0 so Chart.js draws a line.
 */
export function toEngagementSparklineData(
    rows: SessionEngagementByDayData[]
): SparklineDataPoint[] {
    if (!rows?.length) return [];

    const points = rows.map((row) => ({
        date: row.day.slice(0, 10),
        /** Match {@link toEngagementKPIs} conversion rate precision (percentage from API). */
        value: Math.round(row.conversionRate * 100) / 100
    }));

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
 * Map session engagement by-day rows to ChartData for the trend chart.
 */
export function toEngagementTrendChartData(rows: SessionEngagementByDayData[]): ChartData {
    if (!rows?.length) {
        return { labels: [], datasets: [] };
    }

    const labels = rows.map((row) => row.day.slice(0, 10));
    const data = rows.map((row) => row.engagedSessions);

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
                backgroundColor: [
                    AnalyticsChartColors.primary.line,
                    AnalyticsChartColors.neutralDark.line
                ]
            }
        ]
    };
}

/**
 * Map normalized groupBy rows to platform metrics.
 * Works for device, browser, and language since the `name` field is already normalized.
 */
export function toEngagementPlatformMetrics(
    rows: SessionEngagementGroupByData[] | null
): EngagementPlatformMetrics[] {
    if (!rows?.length) return [];
    return rows.map((row) =>
        toPlatformMetrics(
            row.name,
            row.engagedSessions,
            row.engagementRate,
            row.avgEngagedSessionTimeSeconds
        )
    );
}

function toPlatformMetrics(
    name: string,
    views: number,
    engagementRate: number,
    avgTimeSeconds: number
): EngagementPlatformMetrics {
    return {
        name,
        views,
        /** API returns rate as percentage; round to nearest whole percent for display. */
        percentage: Number.isFinite(engagementRate) ? Math.round(engagementRate) : 0,
        time: formatSecondsToTime(avgTimeSeconds)
    };
}

/**
 * Build full EngagementPlatforms from device, browser, and language groupBy arrays.
 */
export function toEngagementPlatforms(
    deviceRows: SessionEngagementGroupByData[] | null,
    browserRows: SessionEngagementGroupByData[] | null,
    languageRows: SessionEngagementGroupByData[] | null
): EngagementPlatforms {
    return {
        device: toEngagementPlatformMetrics(deviceRows),
        browser: toEngagementPlatformMetrics(browserRows),
        language: toEngagementPlatformMetrics(languageRows)
    };
}

/**
 * Default empty KPIs when request fails or has no data.
 * Returns a deep clone so callers can mutate nested KPI objects safely.
 */
export function getEmptyEngagementKPIs(): EngagementKPIs {
    return structuredClone(EMPTY_KPIS);
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
    return { device: [], browser: [], language: [] };
}
