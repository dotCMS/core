import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    AnalyticsApiResponse,
    AnalyticsEventResponse,
    ApiRangeParams,
    CubeJSQuery,
    DeviceBrowserData,
    GetPageviewsByDeviceBrowserParams,
    GetRangeSiteEventParams,
    GetTopContentParams,
    GetTotalEventsParams,
    GetUniqueVisitorsParams,
    TopContentData,
    TotalEventsByDayData,
    TotalEventsData,
    UniqueVisitorsByDayData,
    UniqueVisitorsData
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
     * Fetches total events from the new analytics event endpoint.
     *
     * @param params - Date range (`range` or `from`+`to`) plus optional `granularity`, `eventType`, `siteId`
     * @returns Aggregate `{ totalEvents }` or per-bucket rows when `granularity` is sent (runtime shape; typed as union)
     */
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
     * Fetches unique visitors from the new analytics event endpoint.
     *
     * @param params - Date range plus optional `granularity` and `siteId` (no `eventType`)
     * @returns Aggregate `{ uniqueVisitors }` or per-bucket rows when `granularity` is sent (typed as union)
     */
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
     * Fetches top content from the new analytics event endpoint.
     * Returns content ordered by total events descending.
     *
     * @param params - Date range plus optional `siteId` and `eventType` query params
     */
    getTopContent(params: GetTopContentParams): Observable<TopContentData[]> {
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
    getPageviewsByDeviceBrowser(
        params: GetPageviewsByDeviceBrowserParams
    ): Observable<DeviceBrowserData[]> {
        const httpParams = this.#buildRangeSiteEventParams(params);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<DeviceBrowserData[]>>
            >(`${this.#EVENT_URL}/pageviews-by-device-browser`, { params: httpParams })
            .pipe(map((response) => response.entity.data));
    }

    #buildRangeParams(rangeParams: ApiRangeParams): HttpParams {
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
        let httpParams = this.#buildRangeParams(
            'range' in params ? { range: params.range } : { from: params.from, to: params.to }
        );
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
        let httpParams = this.#buildRangeParams(
            'range' in params ? { range: params.range } : { from: params.from, to: params.to }
        );
        if (params.granularity) {
            httpParams = httpParams.set('granularity', params.granularity);
        }
        if (params.siteId) {
            httpParams = httpParams.set('siteId', params.siteId);
        }

        return httpParams;
    }

    #buildRangeSiteEventParams(params: GetRangeSiteEventParams): HttpParams {
        let httpParams = this.#buildRangeParams(
            'range' in params ? { range: params.range } : { from: params.from, to: params.to }
        );
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
