/**
 * API entity types for analytics responses
 * TODO: Move dashboard specific types here (e.g. Engagement types)
 */

/**
 * Total page views entity response
 */
export interface TotalPageViewsEntity {
    'EventSummary.totalEvents': string;
}

/**
 * Unique visitors entity response
 */
export interface UniqueVisitorsEntity {
    'EventSummary.uniqueVisitors': string;
}

/**
 * Top page performance entity response
 */
export interface TopPagePerformanceEntity {
    'EventSummary.totalEvents': string;
    'EventSummary.title': string;
    'EventSummary.identifier': string;
}

/**
 * Top performance table entity response
 */
export interface TopPerformanceTableEntity {
    'EventSummary.totalEvents': string;
    'EventSummary.title': string;
    'EventSummary.identifier': string;
}

/**
 * Page view timeline entity response
 */
export interface PageViewTimeLineEntity {
    'EventSummary.totalEvents': string;
    'EventSummary.day': string;
    'EventSummary.day.day': string;
}

/**
 * Page view device browsers entity response
 */
export interface PageViewDeviceBrowsersEntity {
    'request.count': string;
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
 * Total conversions entity response
 */
export interface TotalConversionsEntity {
    'EventSummary.totalEvents': string;
}

/**
 * Converting visitors entity response
 */
export interface ConvertingVisitorsEntity {
    'EventSummary.uniqueVisitors': string;
    'EventSummary.uniqueConvertingVisitors': string;
}

/**
 * Content attribution entity response for content conversions table.
 */
export interface ContentAttributionEntity {
    'ContentAttribution.eventType': string;
    'ContentAttribution.identifier': string;
    'ContentAttribution.title': string;
    'ContentAttribution.sumConversions': string;
    'ContentAttribution.sumEvents': string;
}

/**
 * Top attributed content item for conversions overview.
 */
export interface TopAttributedContentItem {
    conv_rate: string;
    conversions: string;
    event_type: string;
    identifier: string;
    title: string;
}

/**
 * Conversions overview entity response for conversions overview table.
 * Shows conversion names with their totals, rates, and top attributed content.
 */
export interface ConversionsOverviewEntity {
    'Conversion.conversionName': string;
    'Conversion.totalConversion': string;
    'Conversion.convRate': string;
    'Conversion.topAttributedContent': TopAttributedContentItem[];
}

/**
 * Chart type options available for visualization.
 * Using const assertion for chart type management.
 */
const ChartType = {
    LINE: 'line',
    PIE: 'pie'
} as const;

/**
 * Chart type options available for visualization.
 */
export type ChartType = (typeof ChartType)[keyof typeof ChartType];

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
    type?: 'bar' | 'line';
    label: string;
    data: number[];
    borderColor?: string;
    backgroundColor?: string | string[];
    borderWidth?: number;
    borderRadius?: number;
    fill?: boolean;
    tension?: number;
    cubicInterpolationMode?: 'default' | 'monotone';
    order?: number;
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
    TOTAL_USERS: 'request.totalUsers',
    PAGE_TITLE: 'request.pageTitle',
    CREATED_AT: 'request.createdAt',
    CREATED_AT_DAY: 'request.createdAt.day',
    USER_AGENT: 'request.userAgent'
} as const;

/**
 * Analytics entity field keys for data extraction.
 */
export type AnalyticsKeys = (typeof AnalyticsKeys)[keyof typeof AnalyticsKeys];

/**
 * Default count limit for analytics queries.
 */
export const DEFAULT_COUNT_LIMIT = 50;
