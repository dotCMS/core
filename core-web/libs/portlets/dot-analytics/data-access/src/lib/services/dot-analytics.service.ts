import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    ANALYTICS_CONVERSION_CONTENT_ATTRIBUTION_URL,
    ANALYTICS_CONVERSION_URL,
    AnalyticsApiResponse,
    AnalyticsEventResponse,
    ContentAttributionApiEntity,
    ContentAttributionData,
    ConversionOverviewData,
    ConversionsOverviewApiEntity,
    CubeJSQuery,
    HealthEntity,
    BrowserBreakdownData,
    DeviceBreakdownData,
    EngagementGroupByField,
    GetContentAttributionParams,
    GetConversionsOverviewParams,
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
    readonly #EVENT_URL = '/api/v1/analytics/event';
    readonly #SESSION_URL = '/api/v1/analytics/session';
    readonly #HEALTH_URL = '/api/v1/analytics/health';
    readonly #http = inject(HttpClient);

    #healthCache$: Observable<HealthStatusTypes> | null = null;

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
     * Cached version of healthCheck. Uses shareReplay to avoid
     * multiple HTTP calls across guards/components in the same navigation.
     *
     * @returns Observable of HealthStatusTypes (cached)
     */
    healthCheckWithCache(): Observable<HealthStatusTypes> {
        if (!this.#healthCache$) {
            this.#healthCache$ = this.healthCheck().pipe(shareReplay(1));
        }

        return this.#healthCache$;
    }

    /**
     * Clears the cached health check result, forcing a fresh request on next call.
     */
    clearHealthCache(): void {
        this.#healthCache$ = null;
    }

    /**
     * Fetches total events — aggregate when `granularity` is omitted.
     */
    getTotalEvents(params: GetTotalEventsWithoutGranularity): Observable<TotalEventsData>;
    /** Time series: each row uses `day` as bucket label (see {@link TotalEventsByDayData}). */
    getTotalEvents(params: GetTotalEventsWithGranularity): Observable<TotalEventsByDayData[]>;
    getTotalEvents(
        params: GetTotalEventsParams
    ): Observable<TotalEventsData | TotalEventsByDayData[]> {
        const httpParams = this.#buildTotalEventsParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<TotalEventsData | TotalEventsByDayData[]>>
            >(`${this.#EVENT_URL}/total-events`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches unique visitors — aggregate when `granularity` is omitted.
     */
    getUniqueVisitors(params: GetUniqueVisitorsWithoutGranularity): Observable<UniqueVisitorsData>;
    /** Time series: each row uses `day` as bucket label (see {@link UniqueVisitorsByDayData}). */
    getUniqueVisitors(
        params: GetUniqueVisitorsWithGranularity
    ): Observable<UniqueVisitorsByDayData[]>;
    getUniqueVisitors(
        params: GetUniqueVisitorsParams
    ): Observable<UniqueVisitorsData | UniqueVisitorsByDayData[]> {
        const httpParams = this.#buildUniqueVisitorsParams(params);

        return this.#http
            .get<
                DotCMSResponse<
                    AnalyticsEventResponse<UniqueVisitorsData | UniqueVisitorsByDayData[]>
                >
            >(`${this.#EVENT_URL}/unique-visitors`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches content conversion attribution rows via `/api/v1/analytics/conversion/content/attribution`
     * (analytics proxy → upstream `/v1/conversion/content/attribution`).
     */
    getContentAttribution(
        params: GetContentAttributionParams
    ): Observable<ContentAttributionData[]> {
        const httpParams = this.#buildContentAttributionParams(params);

        return this.#http
            .get<DotCMSResponse<ContentAttributionApiEntity>>(
                ANALYTICS_CONVERSION_CONTENT_ATTRIBUTION_URL,
                {
                    params: httpParams
                }
            )
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches conversions overview rows from `/api/v1/analytics/conversion`.
     */
    getConversionsOverview(
        params: GetConversionsOverviewParams
    ): Observable<ConversionOverviewData[]> {
        const httpParams = this.#buildConversionsOverviewParams(params);

        return this.#http
            .get<DotCMSResponse<ConversionsOverviewApiEntity>>(ANALYTICS_CONVERSION_URL, {
                params: httpParams
            })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches top content from the new analytics event endpoint.
     * Returns content ordered by total events descending.
     *
     * @param params - Date range plus optional `siteId` and `eventType` query params
     */
    getTopContent(params: GetRangeSiteEventParams): Observable<TopContentData[]> {
        const httpParams = this.#buildRangeSiteEventParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<TopContentData[]>>
            >(`${this.#EVENT_URL}/top-content`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches pageviews grouped by device (`groupBy=device`).
     */
    getPageviewsByDeviceBrowser(
        params: GetRangeSiteEventParams & { groupBy: 'device' }
    ): Observable<DeviceBreakdownData[]>;
    /**
     * Fetches pageviews grouped by browser (`groupBy=browser`).
     */
    getPageviewsByDeviceBrowser(
        params: GetRangeSiteEventParams & { groupBy: 'browser' }
    ): Observable<BrowserBreakdownData[]>;
    getPageviewsByDeviceBrowser(
        params: GetPageviewsByDeviceBrowserParams
    ): Observable<DeviceBreakdownData[] | BrowserBreakdownData[]> {
        const httpParams = this.#buildPageviewsByDeviceBrowserParams(params);

        return this.#http
            .get<
                DotCMSResponse<
                    AnalyticsEventResponse<DeviceBreakdownData[] | BrowserBreakdownData[]>
                >
            >(`${this.#EVENT_URL}/pageviews-by-device-browser`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches session engagement — aggregate when `granularity` is omitted.
     */
    getSessionEngagement(params: GetSessionEngagementAggregate): Observable<SessionEngagementData>;
    /** Time series: each row includes a `day` bucket label. */
    getSessionEngagement(
        params: GetSessionEngagementByDay
    ): Observable<SessionEngagementByDayData[]>;
    getSessionEngagement(
        params: GetSessionEngagementAggregate | GetSessionEngagementByDay
    ): Observable<SessionEngagementData | SessionEngagementByDayData[]> {
        const httpParams = this.#buildSessionEngagementParams(params);

        return this.#http
            .get<
                DotCMSResponse<
                    AnalyticsEventResponse<SessionEngagementData | SessionEngagementByDayData[]>
                >
            >(`${this.#SESSION_URL}/engagement`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches session engagement grouped by a dimension (device, browser, language).
     * Normalizes the varying backend field name (`category` / `browser` / `language`) to `name`.
     */
    getSessionEngagementGroupBy(
        params: GetSessionEngagementGrouped
    ): Observable<SessionEngagementGroupByData[]> {
        const httpParams = this.#buildSessionEngagementParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<Record<string, unknown>[]>>
            >(`${this.#SESSION_URL}/engagement`, { params: httpParams })
            .pipe(
                map((response) => {
                    const groupBy = params.groupBy;
                    const fieldMap: Record<EngagementGroupByField, string> = {
                        device: 'device',
                        browser: 'browser',
                        language: 'language'
                    };
                    const sourceField = fieldMap[groupBy];

                    return response.entity.data.map((item) => {
                        const raw = item[sourceField];
                        const name =
                            raw != null && String(raw).trim() !== '' ? String(raw) : 'Other';

                        return {
                            name,
                            avgEngagedSessionTimeSeconds: Number(
                                item['avgEngagedSessionTimeSeconds'] ?? 0
                            ),
                            engagedSessions: Number(item['engagedSessions'] ?? 0),
                            engagementRate: Number(item['engagementRate'] ?? 0),
                            totalSessions: Number(item['totalSessions'] ?? 0)
                        };
                    });
                })
            );
    }

    #buildSessionEngagementParams(params: GetSessionEngagementParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }
        if ('granularity' in params && params.granularity) {
            httpParams = httpParams.set('granularity', params.granularity);
        }
        if ('groupBy' in params && params.groupBy) {
            httpParams = httpParams.set('groupBy', params.groupBy);
        }

        return httpParams;
    }

    #buildRangeParams(
        rangeParams:
            | GetTotalEventsParams
            | GetUniqueVisitorsParams
            | GetRangeSiteEventParams
            | GetSessionEngagementParams
            | GetContentAttributionParams
            | GetConversionsOverviewParams
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

    #buildTotalEventsParams(params: GetTotalEventsParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);
        if (params.granularity) {
            httpParams = httpParams.set('granularity', params.granularity);
        }
        if (params.eventType) {
            httpParams = httpParams.set('eventType', params.eventType);
        }
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildUniqueVisitorsParams(params: GetUniqueVisitorsParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);
        if (params.granularity) {
            httpParams = httpParams.set('granularity', params.granularity);
        }
        if (params.eventType) {
            httpParams = httpParams.set('eventType', params.eventType);
        }
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildContentAttributionParams(params: GetContentAttributionParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);

        if (params.eventType) {
            httpParams = httpParams.set('eventType', params.eventType);
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
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildConversionsOverviewParams(params: GetConversionsOverviewParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);

        if (params.conversionName) {
            httpParams = httpParams.set('conversionName', params.conversionName);
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
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildRangeSiteEventParams(params: GetRangeSiteEventParams): HttpParams {
        let httpParams = this.#buildRangeParams(params);
        if (params.eventType) {
            httpParams = httpParams.set('eventType', params.eventType);
        }
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildPageviewsByDeviceBrowserParams(params: GetPageviewsByDeviceBrowserParams): HttpParams {
        return this.#buildRangeSiteEventParams(params).set('groupBy', params.groupBy);
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
