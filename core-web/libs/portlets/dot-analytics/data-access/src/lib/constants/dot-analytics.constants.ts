export const TIME_RANGE_OPTIONS = {
    today: 'today',
    yesterday: 'yesterday',
    last7days: 'last7days',
    last30days: 'last30days',
    custom: 'custom'
} as const;

/** Reverse mapping for Internal → URL-friendly */
export const TIME_RANGE_CUBEJS_MAPPING = {
    today: 'today',
    yesterday: 'yesterday',
    last7days: 'from 7 days ago to now',
    last30days: 'from 30 days ago to now'
} as const;

/** Dashboard tab identifiers */
export const DASHBOARD_TABS = {
    pageview: 'pageview',
    conversions: 'conversions',
    engagement: 'engagement'
} as const;

export type DashboardTab = (typeof DASHBOARD_TABS)[keyof typeof DASHBOARD_TABS];

/** Dashboard tab configuration */
export interface DashboardTabConfig {
    id: DashboardTab;
    label: string;
}

/** Ordered list of dashboard tabs */
export const DASHBOARD_TAB_LIST: DashboardTabConfig[] = [
    { id: DASHBOARD_TABS.engagement, label: 'analytics.dashboard.tabs.engagement' },
    { id: DASHBOARD_TABS.pageview, label: 'analytics.dashboard.tabs.pageview' },
    { id: DASHBOARD_TABS.conversions, label: 'analytics.dashboard.tabs.conversions' }
];

/**
 * Chart color palette for analytics dashboard.
 * Colors aligned with dotcms-scss/shared/_colors.scss
 */
export const AnalyticsChartColors = {
    // Primary: $color-palette-blue (#1243e3)
    primary: {
        line: '#1243e3',
        fill: 'rgba(18, 67, 227, 0.15)',
        bar: 'rgba(18, 67, 227, 0.6)'
    },
    // Secondary: $color-palette-green (#1ea97c)
    secondary: {
        line: '#1ea97c',
        fill: 'rgba(30, 169, 124, 0.15)',
        bar: 'rgba(30, 169, 124, 0.6)'
    },
    // Tertiary: $color-palette-yellow (#ffb444)
    tertiary: {
        line: '#ffb444',
        fill: 'rgba(255, 180, 68, 0.15)',
        bar: 'rgba(255, 180, 68, 0.6)'
    },
    // Quaternary: $color-palette-fuchsia (#c336e5)
    quaternary: {
        line: '#c336e5',
        fill: 'rgba(195, 54, 229, 0.15)',
        bar: 'rgba(195, 54, 229, 0.6)'
    },
    // Fifth: $color-palette-red (#f65446)
    fifth: {
        line: '#f65446',
        fill: 'rgba(246, 84, 70, 0.15)',
        bar: 'rgba(246, 84, 70, 0.6)'
    },
    // Neutral: Gray for empty/no-data states
    neutral: {
        line: '#E5E7EB',
        fill: 'rgba(229, 231, 235, 0.15)',
        bar: 'rgba(229, 231, 235, 0.6)'
    }
} as const;

/**
 * Array of chart color variants for automatic assignment
 */
export const AnalyticsChartColorVariants = [
    AnalyticsChartColors.primary,
    AnalyticsChartColors.secondary,
    AnalyticsChartColors.tertiary,
    AnalyticsChartColors.quaternary,
    AnalyticsChartColors.fifth
] as const;

/**
 * Centralized bar chart dataset style configuration.
 * Use this for consistent bar styling across all analytics charts.
 */
export const BAR_CHART_STYLE = {
    borderWidth: 0,
    borderRadius: 6
} as const;

/**
 * Creates a bar chart dataset with consistent styling.
 * @param label - Dataset label
 * @param data - Array of data values
 * @param colorKey - Color key from AnalyticsChartColors (default: 'primary')
 * @returns Bar chart dataset configuration
 */
export const createBarDataset = (
    label: string,
    data: number[],
    colorKey: keyof typeof AnalyticsChartColors = 'primary'
) => ({
    label,
    data,
    ...BAR_CHART_STYLE,
    backgroundColor: AnalyticsChartColors[colorKey].line
});
