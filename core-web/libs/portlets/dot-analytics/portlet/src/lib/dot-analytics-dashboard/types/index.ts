// Import shared types from data-access
import { DateRange, TimeRange, TimeRangeInput } from '@dotcms/portlets/dot-analytics/data-access';

// Re-export shared types for consumers
export { DateRange, TimeRange, TimeRangeInput };

/** Union type for supported chart types in the analytics dashboard */
export type ChartType = 'line' | 'pie' | 'bar' | 'doughnut';

/** Configuration for chart data structure compatible with Chart.js */
export interface ChartData {
    /** Labels for chart axes or segments */
    labels?: string[];
    /** Array of datasets to display in the chart */
    datasets: {
        /** Display name for the dataset */
        label?: string;
        /** Numeric data points */
        data: number[];
        /** Background colors for chart elements */
        backgroundColor?: string | string[];
        /** Border colors for chart elements */
        borderColor?: string | string[];
        /** Width of chart element borders */
        borderWidth?: number;
        /** Whether to fill area under line charts */
        fill?: boolean;
        /** Smoothness of line curves (0-1) */
        tension?: number;
    }[];
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
