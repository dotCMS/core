import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    ANALYTICS_CONTENT_URL,
    ANALYTICS_EVENTS_URL,
    ANALYTICS_SESSIONS_URL,
    AnalyticsApiResponse,
    AnalyticsQueryResponse,
    ApiRangeParams,
    ContentAttributionData,
    CubeJSQuery,
    HealthEntity,
    BrowserBreakdownData,
    DeviceBreakdownData,
    GetContentAttributionParams,
    GetPageviewsByDeviceBrowserParams,
    GetRangeSiteEventParams,
    GetSessionEngagementAggregate,
    GetSessionEngagementByDay,
    GetSessionEngagementGrouped,
    GetSessionEngagementParams,
    GetTotalEventsParams,
    GetTotalEventsWithGranularity,
    GetTotalEventsWithoutGranularity,
    GetUniqueVisitorsParams,
    GetUniqueVisitorsWithGranularity,
    GetUniqueVisitorsWithoutGranularity,
    SessionEngagementByDayData,
    SessionEngagementData,
    SessionEngagementGroupByData,
    TopContentData,
    TotalEventsByDayData,
    TotalEventsData,
    UniqueVisitorsByDayData,
    UniqueVisitorsData
} from '../../index';

function isAnalyticsHealthAvailable(available: string | boolean | undefined): boolean {
    if (available === true) {
        return true;
    }
    if (available === false || available == null) {
        return false;
    }
    return String(available).trim().toLowerCase() === 'true';
}

/** Reads the 8 session engagement metric fields off a domain-query row; field names match 1:1. */
function toSessionEngagementData(row: Record<string, string | number>): SessionEngagementData {
    return {
        avgEngagedSessionTimeSeconds: Number(row['avgEngagedSessionTimeSeconds'] ?? 0),
        avgInteractionsPerEngagedSession: Number(row['avgInteractionsPerEngagedSession'] ?? 0),
        avgSessionTimeSeconds: Number(row['avgSessionTimeSeconds'] ?? 0),
        conversionRate: Number(row['conversionRate'] ?? 0),
        engagedConversionSessions: Number(row['engagedConversionSessions'] ?? 0),
        engagedSessions: Number(row['engagedSessions'] ?? 0),
        engagementRate: Number(row['engagementRate'] ?? 0),
        totalSessions: Number(row['totalSessions'] ?? 0)
    };
}

/**
 * Generic analytics service for CubeJS queries and health checks.
 *
 * @example
 * ```typescript
 * // In store
 * const query = createCubeQuery()
 *     .measures(['totalRequest'])
 *     .pageviews()
 *     .siteId(siteId)
 *     .build();
 *
 * analyticsService.cubeQuery<TotalPageViewsEntity>(query).pipe(
 *     map(entities => entities[0])
 * );
 * ```
 */
@Injectable({
    providedIn: 'root'
})
export class DotAnalyticsService {
    readonly #BASE_URL = '/api/v1/analytics/content/_query/cube';
    readonly #HEALTH_URL = '/api/v1/analytics/health';
    readonly #http = inject(HttpClient);

    /**
     * Checks Content Analytics availability via `GET /api/v1/analytics/health`.
     * `entity.available` true (boolean) or `"true"` (case-insensitive string) maps to AVAILABLE.
     *
     * Always makes a fresh HTTP request.
     *
     * @returns Observable of HealthStatusTypes (AVAILABLE, NOT_AVAILABLE, or ERROR on failure)
     */
    healthCheck(): Observable<HealthStatusTypes> {
        return this.#http.get<DotCMSResponse<HealthEntity>>(this.#HEALTH_URL).pipe(
            map((response) =>
                isAnalyticsHealthAvailable(response.entity?.available)
                    ? HealthStatusTypes.AVAILABLE
                    : HealthStatusTypes.NOT_AVAILABLE
            ),
            catchError(() => of(HealthStatusTypes.ERROR))
        );
    }

    /**
     * Fetches total events via `GET /api/v1/analytics/events?metrics=totalEvents` (dotCMS/core#36628)
     * — aggregate when `granularity` is omitted, time series when set. The new API's dimension
     * column is named `day` or `month` to match the requested `granularity`; mapped back to the
     * old `day` field either way (see {@link TotalEventsByDayData}).
     */
    getTotalEvents(params: GetTotalEventsWithoutGranularity): Observable<TotalEventsData>;
    getTotalEvents(params: GetTotalEventsWithGranularity): Observable<TotalEventsByDayData[]>;
    getTotalEvents(
        params: GetTotalEventsParams
    ): Observable<TotalEventsData | TotalEventsByDayData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            metrics: ['totalEvents'],
            dimensions: params.granularity ? [params.granularity] : undefined
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_EVENTS_URL, {
                params: httpParams
            })
            .pipe(
                map((response) => {
                    const { rows } = response.entity;
                    if (!params.granularity) {
                        return { totalEvents: Number(rows[0]?.['totalEvents'] ?? 0) };
                    }

                    return rows.map((row) => ({
                        day: String(row[params.granularity as string]),
                        totalEvents: Number(row['totalEvents'] ?? 0)
                    }));
                })
            );
    }

    /**
     * Fetches unique visitors via `GET /api/v1/analytics/events?metrics=uniqueVisitors`
     * (dotCMS/core#36628) — aggregate when `granularity` is omitted, time series when set.
     * Note: `uniqueVisitors` is a windowed `COUNT(DISTINCT)`, so a day-series sum can legitimately
     * exceed the scalar aggregate whenever a visitor returns on multiple days — that's expected,
     * not a bug.
     */
    getUniqueVisitors(params: GetUniqueVisitorsWithoutGranularity): Observable<UniqueVisitorsData>;
    getUniqueVisitors(
        params: GetUniqueVisitorsWithGranularity
    ): Observable<UniqueVisitorsByDayData[]>;
    getUniqueVisitors(
        params: GetUniqueVisitorsParams
    ): Observable<UniqueVisitorsData | UniqueVisitorsByDayData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            metrics: ['uniqueVisitors'],
            dimensions: params.granularity ? [params.granularity] : undefined
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_EVENTS_URL, {
                params: httpParams
            })
            .pipe(
                map((response) => {
                    const { rows } = response.entity;
                    if (!params.granularity) {
                        return { uniqueVisitors: Number(rows[0]?.['uniqueVisitors'] ?? 0) };
                    }

                    return rows.map((row) => ({
                        day: String(row[params.granularity as string]),
                        uniqueVisitors: Number(row['uniqueVisitors'] ?? 0)
                    }));
                })
            );
    }

    /**
     * Fetches content conversion attribution rows via `GET /api/v1/analytics/content`
     * (dotCMS/core#36628), attribution mode — the mode resolved by default when no
     * `conversionName`/attribution-only metrics or dimensions are explicitly requested, which is
     * exactly how this method is called today. The new metric column is named `totalEvents`;
     * mapped back to the old `events` field expected by {@link ContentAttributionData} (read by
     * `transformContentConversionsData`).
     */
    getContentAttribution(
        params: GetContentAttributionParams
    ): Observable<ContentAttributionData[]> {
        const httpParams = this.#buildDomainQueryParams(params);

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_CONTENT_URL, {
                params: httpParams
            })
            .pipe(
                map((response) =>
                    response.entity.rows.map((row) => ({
                        identifier: String(row['identifier']),
                        title: String(row['title']),
                        eventType: String(row['eventType']),
                        events: Number(row['totalEvents'] ?? 0),
                        attributionCount: Number(row['attributionCount'] ?? 0),
                        attributionRate: Number(row['attributionRate'] ?? 0)
                    }))
                )
            );
    }

    /**
     * Fetches top content via `GET /api/v1/analytics/content` (dotCMS/core#36628), top-content
     * mode. `metrics: ['totalEvents']` forces top-content mode (attribution mode is the default
     * otherwise); dimensions are left at the mode's default (`identifier`, `title`) — do NOT add
     * `eventType` as a dimension, that flips the mode to attribution. `eventType` here is a plain
     * filter param, confirmed against `ContentAnalyticsService.fetchTopContent()` to have no effect
     * on mode resolution. `orderBy`/`orderDir` are sent explicitly even though the old caller never
     * did, to avoid relying on a server-side default for the same "descending" behavior the old
     * endpoint returned implicitly. No field rename needed — `identifier`/`title`/`totalEvents`
     * already match {@link TopContentData} exactly.
     *
     * @param params - Date range plus optional `siteId` and `eventType` query params
     */
    getTopContent(params: GetRangeSiteEventParams): Observable<TopContentData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            metrics: ['totalEvents'],
            orderBy: 'totalEvents',
            orderDir: 'desc'
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_CONTENT_URL, {
                params: httpParams
            })
            .pipe(
                map((response) =>
                    response.entity.rows.map((row) => ({
                        identifier: String(row['identifier']),
                        title: String(row['title']),
                        totalEvents: Number(row['totalEvents'] ?? 0)
                    }))
                )
            );
    }

    /**
     * Fetches pageviews grouped by device or browser via
     * `GET /api/v1/analytics/events?metrics=pageviews&dimensions=device|browser` (dotCMS/core#36628).
     * The new metric column is named `pageviews`; mapped back to the old `total` field expected by
     * {@link DeviceBreakdownData}/{@link BrowserBreakdownData} (and the pie-chart transform utils
     * that read them).
     */
    getPageviewsByDeviceBrowser(
        params: GetRangeSiteEventParams & { groupBy: 'device' }
    ): Observable<DeviceBreakdownData[]>;
    getPageviewsByDeviceBrowser(
        params: GetRangeSiteEventParams & { groupBy: 'browser' }
    ): Observable<BrowserBreakdownData[]>;
    getPageviewsByDeviceBrowser(
        params: GetPageviewsByDeviceBrowserParams
    ): Observable<DeviceBreakdownData[] | BrowserBreakdownData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            metrics: ['pageviews'],
            dimensions: [params.groupBy]
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_EVENTS_URL, {
                params: httpParams
            })
            .pipe(
                map((response) => {
                    if (params.groupBy === 'device') {
                        return response.entity.rows.map((row) => ({
                            device: String(row['device']),
                            total: Number(row['pageviews'] ?? 0)
                        }));
                    }

                    return response.entity.rows.map((row) => ({
                        browser: String(row['browser']),
                        total: Number(row['pageviews'] ?? 0)
                    }));
                })
            );
    }

    /**
     * Fetches session engagement via `GET /api/v1/analytics/sessions` (dotCMS/core#36628) —
     * aggregate (scalar, read from `rows[0]`) when `granularity` is omitted, time series
     * (`dimensions=day`) when set. Field names match the old shape 1:1, no renames needed.
     */
    getSessionEngagement(params: GetSessionEngagementAggregate): Observable<SessionEngagementData>;
    getSessionEngagement(
        params: GetSessionEngagementByDay
    ): Observable<SessionEngagementByDayData[]>;
    getSessionEngagement(
        params: GetSessionEngagementAggregate | GetSessionEngagementByDay
    ): Observable<SessionEngagementData | SessionEngagementByDayData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            dimensions: params.granularity ? [params.granularity] : undefined
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_SESSIONS_URL, {
                params: httpParams
            })
            .pipe(
                map((response) => {
                    const { rows } = response.entity;
                    if (!params.granularity) {
                        return toSessionEngagementData(rows[0] ?? {});
                    }

                    return rows.map((row) => ({
                        day: String(row['day']),
                        ...toSessionEngagementData(row)
                    }));
                })
            );
    }

    /**
     * Fetches session engagement grouped by a dimension (device, browser, language) via
     * `GET /api/v1/analytics/sessions?dimensions=device|browser|language` (dotCMS/core#36628).
     * `metrics` MUST be explicitly restricted to the 4 "basic" ones below — the backend's default
     * metric set is all 8, and the other 4 (e.g. `conversionRate`) are invalid on a grouped
     * dimension and return 400. The dimension column is named by `groupBy` itself (e.g. `device`),
     * mapped back to the old `name` field with the same blank→`'Other'` fallback as before;
     * `"n/a"` (a real language bucket) passes through unchanged since it isn't blank.
     */
    getSessionEngagementGroupBy(
        params: GetSessionEngagementGrouped
    ): Observable<SessionEngagementGroupByData[]> {
        const httpParams = this.#buildDomainQueryParams({
            ...params,
            metrics: [
                'totalSessions',
                'engagedSessions',
                'engagementRate',
                'avgEngagedSessionTimeSeconds'
            ],
            dimensions: [params.groupBy]
        });

        return this.#http
            .get<DotCMSResponse<AnalyticsQueryResponse>>(ANALYTICS_SESSIONS_URL, {
                params: httpParams
            })
            .pipe(
                map((response) =>
                    response.entity.rows.map((row) => {
                        const raw = row[params.groupBy];
                        const name =
                            raw != null && String(raw).trim() !== '' ? String(raw) : 'Other';

                        return {
                            name,
                            avgEngagedSessionTimeSeconds: Number(
                                row['avgEngagedSessionTimeSeconds'] ?? 0
                            ),
                            engagedSessions: Number(row['engagedSessions'] ?? 0),
                            engagementRate: Number(row['engagementRate'] ?? 0),
                            totalSessions: Number(row['totalSessions'] ?? 0)
                        };
                    })
                )
            );
    }

    #buildRangeParams(
        rangeParams:
            | GetTotalEventsParams
            | GetUniqueVisitorsParams
            | GetRangeSiteEventParams
            | GetSessionEngagementParams
            | GetContentAttributionParams
            | ApiRangeParams
    ): HttpParams {
        let params = new HttpParams();
        if ('range' in rangeParams) {
            params = params.set('range', rangeParams.range);
        } else {
            params = params.set('from', rangeParams.from);
            params = params.set('to', rangeParams.to);
        }

        return params;
    }

    /**
     * Shared param builder for the three domain-driven query resources (dotCMS/core#36628):
     * `/analytics/events`, `/analytics/sessions`, `/analytics/content`. `metrics`/`dimensions`
     * are sent as single comma-joined query values, not repeated params — confirmed against the
     * upstream Java controllers (`EventsAnalyticsController`, `SessionsAnalyticsController`,
     * `ContentAnalyticsController` all declare `metrics`/`dimensions` as a single `String`).
     */
    #buildDomainQueryParams(
        params: ApiRangeParams & {
            siteId?: string;
            metrics?: string[];
            dimensions?: string[];
            eventType?: string;
            identifier?: string;
            title?: string;
            conversionName?: string;
            orderBy?: string;
            orderDir?: 'asc' | 'desc';
            page?: number;
            pageSize?: number;
        }
    ): HttpParams {
        let httpParams = this.#buildRangeParams(params);
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }
        if (params.eventType) {
            httpParams = httpParams.set('eventType', params.eventType);
        }
        if (params.identifier) {
            httpParams = httpParams.set('identifier', params.identifier);
        }
        if (params.title) {
            httpParams = httpParams.set('title', params.title);
        }
        if (params.conversionName) {
            httpParams = httpParams.set('conversionName', params.conversionName);
        }
        if (params.metrics?.length) {
            httpParams = httpParams.set('metrics', params.metrics.join(','));
        }
        if (params.dimensions?.length) {
            httpParams = httpParams.set('dimensions', params.dimensions.join(','));
        }
        if (params.orderBy) {
            httpParams = httpParams.set('orderBy', params.orderBy);
        }
        if (params.orderDir) {
            httpParams = httpParams.set('orderDir', params.orderDir);
        }
        if (params.page != null) {
            httpParams = httpParams.set('page', String(params.page));
        }
        if (params.pageSize != null) {
            httpParams = httpParams.set('pageSize', String(params.pageSize));
        }

        return httpParams;
    }

    /**
     * Executes a CubeJS query and returns the entity array.
     *
     * @param query - The CubeJS query object
     * @returns Observable of entity array
     */
    cubeQuery<T>(query: CubeJSQuery): Observable<T[]> {
        return this.#http
            .post<AnalyticsApiResponse<T>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }
}
