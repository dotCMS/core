import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

/** Flat row rendered in engagement detail modal table body. */
export interface DotAnalyticsEngagementDetailTableRow {
    dimensionLabel: string;
    /** Engagement rate portion for the progress bar / label (0–100). */
    percentage: number;
    timeLabel: string;
    engagedSessions: number;
    totalSessions: number;
}

/** Payload supplied via PrimeNG {@link DynamicDialogConfig}.data. */
export interface DotAnalyticsEngagementDetailTableDialogData {
    rows: DotAnalyticsEngagementDetailTableRow[];
    /** Message key resolved for the first column header (dimension). */
    firstColumnHeaderKey: string;
}

/**
 * Prepares modal rows — same filtering and sort as {@link DotAnalyticsBarEngagementChartComponent} chart list, without top-N truncation.
 */
export function buildEngagementDetailTableRows(
    data: EngagementPlatformMetrics[]
): DotAnalyticsEngagementDetailTableRow[] {
    return [...data]
        .filter((d) => Number.isFinite(d.totalSessions) && d.totalSessions > 0)
        .sort((a, b) => b.totalSessions - a.totalSessions)
        .map((d) => {
            const pct = Number.isFinite(d.percentage) ? d.percentage : 0;
            return {
                dimensionLabel: d.name,
                percentage: Math.min(100, Math.max(0, pct)),
                timeLabel: d.time,
                engagedSessions: d.views,
                totalSessions: d.totalSessions
            };
        });
}
