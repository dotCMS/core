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

/** {@link ApiRangeParams} plus optional filters, without a time bucket (no `granularity`). */
export type GetTotalEventsWithoutGranularity = ApiRangeParams &
    Omit<GetTotalEventsFilters, 'granularity'> & { granularity?: never };

/** {@link ApiRangeParams} plus optional filters including a required bucket `granularity`. */
export type GetTotalEventsWithGranularity = ApiRangeParams &
    Omit<GetTotalEventsFilters, 'granularity'> & {
        granularity: ApiGranularity;
    };

/** When `granularity` is `'day'`, responses use `{ day, totalEvents }` rows. */
export type GetTotalEventsDayGranularityParams = ApiRangeParams &
    Omit<GetTotalEventsFilters, 'granularity'> & { granularity: 'day' };

/** Non-`day` granularities yield bucket fields `hour`, `week`, or `month` respectively. */
export type GetTotalEventsNonDayGranularityParams = ApiRangeParams &
    Omit<GetTotalEventsFilters, 'granularity'> & {
        granularity: Exclude<ApiGranularity, 'day'>;
    };

/** Arguments for GET `total-events`. */
export type GetTotalEventsParams = GetTotalEventsWithoutGranularity | GetTotalEventsWithGranularity;

/**
 * Optional filters for GET `unique-visitors` (`granularity`, `siteId` only).
 * The analytics event API does not support `eventType` on this route; counts include all event
 * kinds the backend aggregates (e.g. not limited to pageviews). See product/API docs before
 * interpreting trends if new event kinds are tracked.
 */
export interface GetUniqueVisitorsFilters {
    granularity?: ApiGranularity;
    siteId?: string;
}

/** Unique visitors aggregate (no buckets). */
export type GetUniqueVisitorsWithoutGranularity = ApiRangeParams &
    Omit<GetUniqueVisitorsFilters, 'granularity'> & { granularity?: never };

/** Bucketed unique visitors. */
export type GetUniqueVisitorsWithGranularity = ApiRangeParams &
    Omit<GetUniqueVisitorsFilters, 'granularity'> & {
        granularity: ApiGranularity;
    };

export type GetUniqueVisitorsDayGranularityParams = ApiRangeParams &
    Omit<GetUniqueVisitorsFilters, 'granularity'> & { granularity: 'day' };

export type GetUniqueVisitorsNonDayGranularityParams = ApiRangeParams &
    Omit<GetUniqueVisitorsFilters, 'granularity'> & {
        granularity: Exclude<ApiGranularity, 'day'>;
    };

/** Arguments for GET `unique-visitors`. */
export type GetUniqueVisitorsParams =
    | GetUniqueVisitorsWithoutGranularity
    | GetUniqueVisitorsWithGranularity;

/** Optional filters for GET `top-content`. */
export interface GetTopContentFilters {
    siteId?: string;
    eventType?: AnalyticsEventType;
}

/**
 * Shared query shape for `top-content` and `pageviews-by-device-browser` (range + optional `siteId` / `eventType`).
 */
export type GetRangeSiteEventParams = ApiRangeParams & GetTopContentFilters;

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

/**
 * One time-bucket row per `granularity` when the event API returns a series (not an aggregate).
 * Field names match the requested granularity.
 */
export type TotalEventsByBucketRow =
    | { hour: string; totalEvents: number }
    | { day: string; totalEvents: number }
    | { week: string; totalEvents: number }
    | { month: string; totalEvents: number };

/** Convenience alias for the common `granularity: 'day'` case. */
export type TotalEventsByDayData = Extract<TotalEventsByBucketRow, { day: string }>;

export type TotalEventsNonDayBucketRow = Exclude<TotalEventsByBucketRow, TotalEventsByDayData>;

/** Unique visitors (no granularity) */
export interface UniqueVisitorsData {
    uniqueVisitors: number;
}

export type UniqueVisitorsByBucketRow =
    | { hour: string; uniqueVisitors: number }
    | { day: string; uniqueVisitors: number }
    | { week: string; uniqueVisitors: number }
    | { month: string; uniqueVisitors: number };

export type UniqueVisitorsByDayData = Extract<UniqueVisitorsByBucketRow, { day: string }>;

export type UniqueVisitorsNonDayBucketRow = Exclude<
    UniqueVisitorsByBucketRow,
    UniqueVisitorsByDayData
>;

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
