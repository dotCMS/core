/**
 * Types for the new Analytics Event API (microservice).
 * Separate from CubeJS types which will be removed in the future.
 */

/** Granularity options for the new analytics event API */
export type ApiGranularity = 'hour' | 'day' | 'week' | 'month';

/**
 * Query params for the analytics event API.
 * Either a predefined `range` OR both `from` + `to` (never partial).
 * The API returns 400 if only one of from/to is provided.
 */
export type ApiRangeParams = { range: string } | { from: string; to: string };

/** Response wrapper from analytics event endpoints (entity.data field) */
export interface AnalyticsEventResponse<T> {
    data: T;
    params?: Record<string, string>;
    query?: Record<string, string>;
}

/** Total events (no granularity) */
export interface TotalEventsData {
    totalEvents: number;
}

/** Total events by day (with granularity) */
export interface TotalEventsByDayData {
    day: string;
    totalEvents: number;
}

/** Unique visitors (no granularity) */
export interface UniqueVisitorsData {
    uniqueVisitors: number;
}

/** Unique visitors by day (with granularity) */
export interface UniqueVisitorsByDayData {
    day: string;
    uniqueVisitors: number;
}

/** Top content item from /api/v1/analytics/event/top-content */
export interface TopContentData {
    identifier: string;
    title: string;
    totalEvents: number;
}
