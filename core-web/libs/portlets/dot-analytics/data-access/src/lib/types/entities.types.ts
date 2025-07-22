/**
 * API entity types for analytics responses
 */

/**
 * Total page views entity response
 */
export interface TotalPageViewsEntity {
    'request.totalRequest': string;
}

/**
 * Unique visitors entity response
 */
export interface UniqueVisitorsEntity {
    'request.totalUser': string;
}

/**
 * Top page performance entity response
 */
export interface TopPagePerformanceEntity {
    'request.totalRequest': string;
    'request.pageTitle': string;
    'request.path': string;
}

/**
 * Top performance table entity response
 */
export interface TopPerformaceTableEntity {
    'request.totalRequest': string;
    'request.pageTitle': string;
    'request.path': string;
}

/**
 * Page view timeline entity response
 */
export interface PageViewTimeLineEntity {
    'request.totalRequest': string;
    'request.createdAt': string;
    'request.createdAt.day': string;
}

/**
 * Page view device browsers entity response
 */
export interface PageViewDeviceBrowsersEntity {
    'request.totalRequest': string;
    'request.userAgent': string;
}

/**
 * Browser types for analytics filtering
 */
export interface BrowserEntity {
    'request.userAgent': string;
    'request.totalRequest': string;
}

/**
 * Chart type options available for visualization.
 * Using const assertion for chart type management.
 */
const ChartType = {
    LINE: 'line',
    PIE: 'pie'
} as const;

export type ChartType = (typeof ChartType)[keyof typeof ChartType];

/**
 * Time period options for analytics filtering.
 * Using const assertion for time period management.
 */
const TimePeriod = {
    LAST_7_DAYS: 'from 7 days ago to now',
    LAST_30_DAYS: 'from 30 days ago to now'
} as const;

export type TimePeriod = (typeof TimePeriod)[keyof typeof TimePeriod];

/**
 * API dimension fields for analytics responses.
 * Using const assertion for field name management.
 * These are the field names returned from API responses, not CubeJS queries.
 */
const ApiDimensionField = {
    PATH: 'path',
    PAGE_TITLE: 'pageTitle',
    USER_AGENT: 'userAgent',
    CREATED_AT: 'createdAt',
    EVENT_TYPE: 'eventType'
} as const;

export type ApiDimensionField = (typeof ApiDimensionField)[keyof typeof ApiDimensionField];

/**
 * Chart dataset interface for data visualization.
 * Defines the structure for chart datasets used in analytics components.
 */
export interface ChartDataset {
    label: string;
    data: number[];
    borderColor?: string;
    backgroundColor?: string | string[];
    borderWidth?: number;
    fill?: boolean;
    tension?: number;
}

/**
 * Chart data interface compatible with Chart.js
 */
export interface ChartData {
    labels: string[];
    datasets: ChartDataset[];
}

/**
 * Table data transformation interface for top pages
 */
export interface TablePageData {
    pageTitle: string;
    path: string;
    views: number;
}

/**
 * Analytics entity field keys for data extraction.
 * Using const assertion for analytics key management.
 */
const AnalyticsKeys = {
    TOTAL_REQUEST: 'request.totalRequest',
    TOTAL_SESSIONS: 'request.totalSessions',
    TOTAL_USER: 'request.totalUser',
    PAGE_TITLE: 'request.pageTitle',
    CREATED_AT: 'request.createdAt',
    CREATED_AT_DAY: 'request.createdAt.day',
    USER_AGENT: 'request.userAgent'
} as const;

export type AnalyticsKeys = (typeof AnalyticsKeys)[keyof typeof AnalyticsKeys];
