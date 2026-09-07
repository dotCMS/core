import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

/** Flat row rendered in pageview detail modal table body. */
export interface DotAnalyticsPageviewDetailTableRow {
    dimensionLabel: string;
    /** Share of total views for the progress bar / label (0–100). */
    percentage: number;
    /** Absolute view count for that dimension bucket. */
    totalViews: number;
}

/** Payload supplied via PrimeNG {@link DynamicDialogConfig}.data. */
export interface DotAnalyticsPageviewDetailTableDialogData {
    rows: DotAnalyticsPageviewDetailTableRow[];
    /** Message key resolved for the first column header (dimension). */
    firstColumnHeaderKey: string;
}

/**
 * Prepares modal rows — same sort as the bar chart list (percentage descending),
 * without top-N truncation.
 */
export function buildPageviewDetailTableRows(
    data: EngagementPlatformMetrics[]
): DotAnalyticsPageviewDetailTableRow[] {
    return [...data]
        .filter((d) => d.views > 0)
        .sort((a, b) => b.percentage - a.percentage)
        .map((d) => ({
            dimensionLabel: d.name,
            percentage: Math.min(100, Math.max(0, d.percentage)),
            totalViews: d.views
        }));
}
