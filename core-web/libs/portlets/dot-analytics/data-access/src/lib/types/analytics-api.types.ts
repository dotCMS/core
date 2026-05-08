/**
 * Types for the new Analytics Event API (microservice).
 * Separate from CubeJS types which will be removed in the future.
 */

/** Granularity values accepted by `granularity=` on Analytics Event endpoints (proxy → upstream). */
export type ApiGranularity = 'day' | 'month';

/**
 * Event kinds accepted by the analytics event API (`eventType` query param).
 * Extend this union when the upstream API adds more values.
 */
export type AnalyticsEventType = 'pageview' | 'conversion';

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

/** Arguments for GET `total-events`. */
export type GetTotalEventsParams = GetTotalEventsWithoutGranularity | GetTotalEventsWithGranularity;

/**
 * Optional filters for GET `unique-visitors`.
 * When `eventType` is set, counts are scoped to that event kind (e.g. `pageview`, `conversion`).
 */
export interface GetUniqueVisitorsFilters {
    granularity?: ApiGranularity;
    eventType?: AnalyticsEventType | string;
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

/**
 * Sort columns for GET `/api/v1/analytics/conversion/content/attribution` (analytics proxy → `/v1/conversion/content/attribution`).
 */
export type ContentAttributionOrderBy =
    | 'title'
    | 'identifier'
    | 'eventType'
    | 'attributionRate'
    | 'attributionCount';

/** Query params for GET `/api/v1/analytics/conversion/content/attribution` (analytics proxy → `/v1/conversion/content/attribution`). */
export type GetContentAttributionParams = ApiRangeParams & {
    siteId?: string;
    /** Optional filter such as `content_click` (see API docs). */
    eventType?: string;
    orderBy?: ContentAttributionOrderBy;
    orderDir?: 'asc' | 'desc';
    page?: number;
    pageSize?: number;
};

/** One attribution row returned by the conversion attribution endpoint (see {@link GetContentAttributionParams}). */
export interface ContentAttributionData {
    attributionCount: number;
    attributionRate: number;
    eventType: string;
    events: number;
    identifier: string;
    title: string;
}

/** Sort columns for GET `/api/v1/analytics/conversion`. */
export type ConversionOverviewOrderBy = 'totalConversions' | 'conversionName' | 'conversionRate';

/** Query params for GET `/api/v1/analytics/conversion`. */
export type GetConversionsOverviewParams = ApiRangeParams & {
    siteId?: string;
    conversionName?: string;
    orderBy?: ConversionOverviewOrderBy;
    orderDir?: 'asc' | 'desc';
    page?: number;
    pageSize?: number;
};

/** Top attributed content item nested in conversions overview response. */
export interface ConversionOverviewTopContent {
    attributionCount: number;
    attributionRate: number;
    eventType: string;
    events: number;
    identifier: string;
    title: string;
}

/** One conversions overview row returned by `/api/v1/analytics/conversion`. */
export interface ConversionOverviewData {
    conversionName: string;
    conversionRate: number;
    topContent: ConversionOverviewTopContent[];
    totalConversions: number;
    totalEvents: number;
}

/** Pagination block returned inside `entity` for `/api/v1/analytics/conversion`. */
export interface AnalyticsConversionPagination {
    page: number;
    pageSize: number;
    totalItems: number;
    totalPages: number;
}

/** `entity` payload for GET `/api/v1/analytics/conversion/content/attribution` (data + pagination + query mirror the live API). */
export interface ContentAttributionApiEntity {
    data: ContentAttributionData[];
    pagination?: AnalyticsConversionPagination;
    query?: Record<string, string>;
}

/** `entity` body for `/api/v1/analytics/conversion`. */
export interface ConversionsOverviewApiEntity {
    data: ConversionOverviewData[];
    pagination: AnalyticsConversionPagination;
    params?: Record<string, string>;
}

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
 * One time-bucket row when the Event API returns a series (`granularity` set).
 * The JSON field is always **`day`** (yyyy-MM-dd); for `granularity: 'month'`, values are bucket
 * starts (typically the first day of each month), not necessarily every calendar day in the range.
 */
export interface TotalEventsByDayData {
    day: string;
    totalEvents: number;
}

/** Unique visitors (no granularity) */
export interface UniqueVisitorsData {
    uniqueVisitors: number;
}

/** One bucket row for unique-visitors series; **`day`** is the bucket label (same semantics as {@link TotalEventsByDayData}). */
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

// ---------------------------------------------------------------------------
// Session Engagement API — GET /api/v1/analytics/session/engagement
// ---------------------------------------------------------------------------

/** Dimensions the engagement endpoint can group by. */
export type EngagementGroupByField = 'device' | 'browser' | 'language';

/** Aggregate engagement (no granularity, no groupBy). */
export type GetSessionEngagementAggregate = ApiRangeParams & {
    siteId?: string;
    granularity?: never;
    groupBy?: never;
};

/** Time-series engagement (granularity = 'day'). */
export type GetSessionEngagementByDay = ApiRangeParams & {
    siteId?: string;
    granularity: 'day';
    groupBy?: never;
};

/** Grouped engagement (groupBy set). */
export type GetSessionEngagementGrouped = ApiRangeParams & {
    siteId?: string;
    granularity?: never;
    groupBy: EngagementGroupByField;
};

/** Union of all valid param combinations. */
export type GetSessionEngagementParams =
    | GetSessionEngagementAggregate
    | GetSessionEngagementByDay
    | GetSessionEngagementGrouped;

/** Aggregate engagement response (no granularity). */
export interface SessionEngagementData {
    avgEngagedSessionTimeSeconds: number;
    avgInteractionsPerEngagedSession: number;
    avgSessionTimeSeconds: number;
    conversionRate: number;
    engagedConversionSessions: number;
    engagedSessions: number;
    engagementRate: number;
    totalSessions: number;
}

/** One day bucket when `granularity=day`. */
export interface SessionEngagementByDayData extends SessionEngagementData {
    day: string;
}

/** Normalized groupBy response — backend returns `category` / `browser` / `language`, we map to `name`. */
export interface SessionEngagementGroupByData {
    name: string;
    avgEngagedSessionTimeSeconds: number;
    engagedSessions: number;
    engagementRate: number;
    totalSessions: number;
}
