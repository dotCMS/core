// Import shared types from data-access
import { DateRange, TimeRange, TimeRangeInput } from '@dotcms/portlets/dot-analytics/data-access';

// Re-export shared types for consumers
export { DateRange, TimeRange, TimeRangeInput };

/**
 * Chart color palette for analytics dashboard.
 * Uses consistent colors across all charts.
 */
export const AnalyticsChartColors = {
    // Primary colors for main metrics
    primary: {
        line: 'rgb(99, 102, 241)', // Indigo
        fill: 'rgba(99, 102, 241, 0.15)',
        bar: 'rgba(99, 102, 241, 0.6)'
    },
    // Secondary colors for comparison/secondary metrics
    secondary: {
        line: 'rgb(34, 197, 94)', // Green
        fill: 'rgba(34, 197, 94, 0.15)',
        bar: 'rgba(34, 197, 94, 0.6)'
    },
    // Tertiary colors for additional metrics
    tertiary: {
        line: 'rgb(249, 115, 22)', // Orange
        fill: 'rgba(249, 115, 22, 0.15)',
        bar: 'rgba(249, 115, 22, 0.6)'
    },
    // Quaternary colors
    quaternary: {
        line: 'rgb(236, 72, 153)', // Pink
        fill: 'rgba(236, 72, 153, 0.15)',
        bar: 'rgba(236, 72, 153, 0.6)'
    },
    // Additional colors
    fifth: {
        line: 'rgb(14, 165, 233)', // Sky blue
        fill: 'rgba(14, 165, 233, 0.15)',
        bar: 'rgba(14, 165, 233, 0.6)'
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
 * Get color properties for a dataset by index.
 * Cycles through available colors if index exceeds array length.
 *
 * @param index - Dataset index
 * @param type - Chart type ('line' | 'bar')
 * @returns Color properties for the dataset
 */
export const getAnalyticsChartColors = (
    index: number,
    type: 'line' | 'bar' = 'line'
): { borderColor: string; backgroundColor: string } => {
    const colors = AnalyticsChartColorVariants[index % AnalyticsChartColorVariants.length];

    return {
        borderColor: colors.line,
        backgroundColor: type === 'bar' ? colors.bar : colors.fill
    };
};

/** Union type for supported chart types in the analytics dashboard */
export type ChartType = 'line' | 'pie' | 'bar' | 'doughnut';

/**
 * Extended dataset configuration supporting combo charts (mixed bar + line).
 * Used for type checking when datasets have individual chart types.
 */
export interface ComboChartDataset {
    /** Chart type for this specific dataset (for combo charts) */
    type?: 'bar' | 'line';
    /** Display name for the dataset */
    label?: string;
    /** Numeric data points */
    data: number[];
    /** Y-axis to use ('y' for left, 'y1' for right) - enables dual Y-axes */
    yAxisID?: 'y' | 'y1';
    /** Border color for the dataset */
    borderColor?: string | string[];
    /** Background color for the dataset */
    backgroundColor?: string | string[];
    /** Whether to fill area under line charts */
    fill?: boolean;
    /** Render order (lower = rendered first, appears behind) */
    order?: number;
    /** Border width */
    borderWidth?: number;
    /** Smoothness of line curves (0-1) */
    tension?: number;
    /** Border radius for bar charts (rounded corners) */
    borderRadius?: number;
}

/**
 * Unified chart data structure compatible with Chart.js.
 * Supports simple charts (1 dataset), multiple datasets, and combo charts.
 *
 * For combo charts:
 * - Add `type` property to each dataset ('bar', 'line')
 * - Optionally add `yAxisID` for dual Y-axes ('y' left, 'y1' right)
 */
export interface ChartData {
    /** Labels for chart axes or segments */
    labels?: string[];
    /** Array of datasets to display in the chart */
    datasets: ComboChartDataset[];
}

/** Chart configuration options extending Chart.js options */
export interface ChartOptions {
    /** Whether chart should adapt to container size */
    responsive?: boolean;
    /** Whether to maintain aspect ratio on resize */
    maintainAspectRatio?: boolean;
    /** Interaction configuration */
    interaction?: {
        /** Interaction mode */
        mode?: 'point' | 'nearest' | 'index' | 'dataset' | 'x' | 'y';
        /** Whether interaction requires intersection with element */
        intersect?: boolean;
        /** Axis for interaction detection */
        axis?: 'x' | 'y' | 'xy';
    };
    /** Plugin configurations */
    plugins?: {
        /** Title configuration */
        title?: {
            /** Whether to show title */
            display: boolean;
            /** Title text */
            text: string;
        };
        /** Legend configuration */
        legend?: {
            /** Whether to show legend */
            display: boolean;
            /** Legend position */
            position?: 'top' | 'bottom' | 'left' | 'right';
            /** Legend labels configuration */
            labels?: {
                /** Use point style instead of rectangles */
                usePointStyle?: boolean;
                /** Point style type */
                pointStyle?:
                    | 'circle'
                    | 'cross'
                    | 'crossRot'
                    | 'dash'
                    | 'line'
                    | 'rect'
                    | 'rectRounded'
                    | 'rectRot'
                    | 'star'
                    | 'triangle';
                /** Width of legend box */
                boxWidth?: number;
                /** Height of legend box */
                boxHeight?: number;
                /** Padding between legend items */
                padding?: number;
                /** Font configuration */
                font?: {
                    /** Font size */
                    size?: number;
                };
                /** Custom label generation function */
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                generateLabels?: (chart: any) => any[];
            };
        };
        /** Tooltip configuration */
        tooltip?: {
            /** Tooltip mode */
            mode?: 'point' | 'nearest' | 'index' | 'dataset' | 'x' | 'y';
            /** Whether tooltip requires intersection with element */
            intersect?: boolean;
            /** Tooltip callback functions */
            callbacks?: {
                /** Custom label callback */
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                label?: (context: any) => string;
                /** Custom title callback */
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                title?: (context: any) => string;
            };
        };
    };
    /** Scale configurations for axes */
    scales?: Record<string, unknown>;
}

/** Configuration for table column display and behavior */
export interface TableColumn {
    /** Data field key to display */
    field: string;
    /** Column header text */
    header: string;
    /** Data type for formatting */
    type?: 'text' | 'number' | 'percentage' | 'link';
    /** Text alignment in column */
    alignment?: 'left' | 'center' | 'right';
    /** Whether column is sortable */
    sortable?: boolean;
    /** Column width (CSS value: %, px, rem, etc.) */
    width?: string;
}

/** Data structure for metric card display */
export interface MetricData {
    /** Metric display name */
    name: string;
    /** Metric value (number or formatted string) */
    value: string | number;
    /** Optional secondary text */
    subtitle?: string;
    /** PrimeIcons icon name (without 'pi-' prefix) */
    icon?: string;
}

/**
 * Event types matching the Analytics SDK (DotCMSPredefinedEventType)
 * @see core-web/libs/sdk/analytics/src/lib/core/shared/constants/dot-analytics.constants.ts
 */
export type AnalyticsEventType = 'pageview' | 'content_click' | 'content_impression' | 'conversion';

/** Data structure for content conversion table rows */
export interface ContentConversionData {
    /** Event type (matches SDK event names) */
    eventType: AnalyticsEventType;
    /** Content identifier (URL path or content ID) */
    identifier: string;
    /** Content title/description */
    title: string;
    /** Total count/views */
    count: number;
    /** Number of conversions */
    conversions: number;
    /** Conversion rate percentage */
    conversionRate: number;
}

/** Option structure for dropdown filters */
export interface FilterOption {
    /** Display text for the option */
    label: string;
    /** Internal value for the option */
    value: TimeRange;
}

/** Data structure for page analytics */
export interface PageData {
    /** Page URL path */
    path: string;
    /** Page title */
    title: string;
    /** Number of page views */
    views: number;
    /** Percentage of total views */
    percentage: number;
}

/** Complete mock data structure for analytics dashboard */
export interface DashboardMockData {
    /** Array of metric cards data */
    metrics: MetricData[];
    /** Array of top performing pages */
    topPages: PageData[];
    /** Line chart data for pageviews over time */
    pageviewsTimeline: ChartData;
    /** Pie chart data for device/browser breakdown */
    deviceBreakdown: ChartData;
    /** Table column configuration for top pages */
    topPagesTableConfig: TableColumn[];
}
