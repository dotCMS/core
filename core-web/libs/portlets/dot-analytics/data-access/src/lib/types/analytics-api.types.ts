/**
 * Types for the new Analytics Event API (microservice).
 * Separate from CubeJS types which will be removed in the future.
 */

/** Granularity options for the new analytics event API */
export type ApiGranularity = 'hour' | 'day' | 'week' | 'month';

/**
 * Event kinds accepted by the analytics event API (`eventType` query param).
 * Extend this union when the upstream API adds more values.
 */
export type AnalyticsEventType = 'pageview' | 'impressions';

/**
 * Query params for the analytics event API.
 * Either a predefined `range` OR both `from` + `to` (never partial).
 * The API returns 400 if only one of from/to is provided.
 */
export type ApiRangeParams = { range: string } | { from: string; to: string };

/** Optional filters for GET `total-events` (merged with {@link ApiRangeParams}). */
export interface GetTotalEventsFilters {
    granularity?: ApiGranularity;
    eventType?: AnalyticsEventType;
    siteId?: string;
}

/** Single parameter object for total-events: {@link ApiRangeParams} plus optional filters. */
export type GetTotalEventsParams = ApiRangeParams & GetTotalEventsFilters;

/** Optional filters for GET `unique-visitors` (`granularity`, `siteId` only — no `eventType`). */
export interface GetUniqueVisitorsFilters {
    granularity?: ApiGranularity;
    siteId?: string;
}

/** Single parameter object for unique-visitors: {@link ApiRangeParams} plus optional filters. */
export type GetUniqueVisitorsParams = ApiRangeParams & GetUniqueVisitorsFilters;

/** Optional filters for GET `top-content`. */
export interface GetTopContentFilters {
    siteId?: string;
    eventType?: AnalyticsEventType;
}

/** Single parameter object for top-content: {@link ApiRangeParams} plus optional filters. */
export type GetTopContentParams = ApiRangeParams & GetTopContentFilters;

/** Params for GET `pageviews-by-device-browser` (same optional `siteId` / `eventType` as top-content). */
export type GetPageviewsByDeviceBrowserParams = GetTopContentParams;

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

/** Device browser item from /api/v1/analytics/event/pageviews-by-device-browser */
export interface DeviceBrowserData {
    browser: string;
    device: string;
    total: number;
}
