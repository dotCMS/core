import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    AnalyticsApiResponse,
    AnalyticsEventResponse,
    CubeJSQuery,
    DeviceBrowserData,
    GetRangeSiteEventParams,
    GetTotalEventsDayGranularityParams,
    GetTotalEventsNonDayGranularityParams,
    GetTotalEventsParams,
    GetTotalEventsWithoutGranularity,
    GetUniqueVisitorsDayGranularityParams,
    GetUniqueVisitorsNonDayGranularityParams,
    GetUniqueVisitorsParams,
    GetUniqueVisitorsWithoutGranularity,
    TopContentData,
    TotalEventsByBucketRow,
    TotalEventsByDayData,
    TotalEventsData,
    TotalEventsNonDayBucketRow,
    UniqueVisitorsByBucketRow,
    UniqueVisitorsByDayData,
    UniqueVisitorsData,
    UniqueVisitorsNonDayBucketRow
} from '../../index';

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
    readonly #HEALTH_URL = '/api/v1/analytics/check';
    readonly #http = inject(HttpClient);

    #healthCache$: Observable<HealthStatusTypes> | null = null;

    /**
     * Checks analytics availability via the health endpoint.
     * Always makes a fresh HTTP request.
     *
     * @returns Observable of HealthStatusTypes (AVAILABLE or NOT_AVAILABLE)
     */
    healthCheck(): Observable<HealthStatusTypes> {
        return this.#http.get<{ available: string }>(this.#HEALTH_URL).pipe(
            map((response) =>
                response.available === 'true'
                    ? HealthStatusTypes.AVAILABLE
                    : HealthStatusTypes.NOT_AVAILABLE
            ),
            catchError(() => of(HealthStatusTypes.AVAILABLE))
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
    /** Per-day buckets. */
    getTotalEvents(params: GetTotalEventsDayGranularityParams): Observable<TotalEventsByDayData[]>;
    getTotalEvents(
        params: GetTotalEventsNonDayGranularityParams
    ): Observable<TotalEventsNonDayBucketRow[]>;
    getTotalEvents(
        params: GetTotalEventsParams
    ): Observable<TotalEventsData | TotalEventsByBucketRow[]> {
        const httpParams = this.#buildTotalEventsParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<TotalEventsData | TotalEventsByBucketRow[]>>
            >(`${this.#EVENT_URL}/total-events`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches unique visitors — aggregate when `granularity` is omitted.
     */
    getUniqueVisitors(params: GetUniqueVisitorsWithoutGranularity): Observable<UniqueVisitorsData>;
    getUniqueVisitors(
        params: GetUniqueVisitorsDayGranularityParams
    ): Observable<UniqueVisitorsByDayData[]>;
    getUniqueVisitors(
        params: GetUniqueVisitorsNonDayGranularityParams
    ): Observable<UniqueVisitorsNonDayBucketRow[]>;
    getUniqueVisitors(
        params: GetUniqueVisitorsParams
    ): Observable<UniqueVisitorsData | UniqueVisitorsByBucketRow[]> {
        const httpParams = this.#buildUniqueVisitorsParams(params);

        return this.#http
            .get<
                DotCMSResponse<
                    AnalyticsEventResponse<UniqueVisitorsData | UniqueVisitorsByBucketRow[]>
                >
            >(`${this.#EVENT_URL}/unique-visitors`, { params: httpParams })
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
     * Fetches pageviews by device and browser from the new analytics event endpoint.
     *
     * @param params - Date range plus optional `siteId` and `eventType`
     */
    getPageviewsByDeviceBrowser(params: GetRangeSiteEventParams): Observable<DeviceBrowserData[]> {
        const httpParams = this.#buildRangeSiteEventParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<DeviceBrowserData[]>>
            >(`${this.#EVENT_URL}/pageviews-by-device-browser`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    #buildRangeParams(
        rangeParams: GetTotalEventsParams | GetUniqueVisitorsParams | GetRangeSiteEventParams
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
