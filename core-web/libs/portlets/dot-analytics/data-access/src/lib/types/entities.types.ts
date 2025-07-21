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
