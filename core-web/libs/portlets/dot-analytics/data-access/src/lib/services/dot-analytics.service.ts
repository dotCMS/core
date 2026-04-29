import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    AnalyticsApiResponse,
    AnalyticsEventResponse,
    ApiGranularity,
    ApiRangeParams,
    CubeJSQuery,
    DeviceBrowserData,
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
     * Supports predefined ranges (`?range=last_7_days`) or custom dates (`?from=...&to=...`).
     *
     * @param rangeParams - Object with either `range` or `from`+`to` query params
     * @param granularity - Optional granularity (e.g. 'day', 'hour')
     * @returns Observable of TotalEventsData (single object) or TotalEventsByDayData[] (array with granularity)
     */
    getTotalEvents(rangeParams: ApiRangeParams): Observable<TotalEventsData>;
    getTotalEvents(
        rangeParams: ApiRangeParams,
        granularity: ApiGranularity
    ): Observable<TotalEventsByDayData[]>;
    getTotalEvents(
        rangeParams: ApiRangeParams,
        granularity?: ApiGranularity
    ): Observable<TotalEventsData | TotalEventsByDayData[]> {
        let params = this.#buildRangeParams(rangeParams);
        if (granularity) {
            params = params.set('granularity', granularity);
        }

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<TotalEventsData | TotalEventsByDayData[]>>
            >(`${this.#EVENT_URL}/total-events`, { params })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches unique visitors from the new analytics event endpoint.
     * Supports predefined ranges (`?range=last_7_days`) or custom dates (`?from=...&to=...`).
     *
     * @param rangeParams - Object with either `range` or `from`+`to` query params
     * @param granularity - Optional granularity (e.g. 'day', 'hour')
     * @returns Observable of UniqueVisitorsData (single object) or UniqueVisitorsByDayData[] (array with granularity)
     */
    getUniqueVisitors(rangeParams: ApiRangeParams): Observable<UniqueVisitorsData>;
    getUniqueVisitors(
        rangeParams: ApiRangeParams,
        granularity: ApiGranularity
    ): Observable<UniqueVisitorsByDayData[]>;
    getUniqueVisitors(
        rangeParams: ApiRangeParams,
        granularity?: ApiGranularity
    ): Observable<UniqueVisitorsData | UniqueVisitorsByDayData[]> {
        let params = this.#buildRangeParams(rangeParams);
        if (granularity) {
            params = params.set('granularity', granularity);
        }

        return this.#http
            .get<
                DotCMSResponse<
                    AnalyticsEventResponse<UniqueVisitorsData | UniqueVisitorsByDayData[]>
                >
            >(`${this.#EVENT_URL}/unique-visitors`, { params })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches top content from the new analytics event endpoint.
     * Returns an array of content items ordered by total events descending.
     *
     * @param rangeParams - Object with either `range` or `from`+`to` query params
     * @returns Observable of TopContentData[]
     */
    getTopContent(rangeParams: ApiRangeParams): Observable<TopContentData[]> {
        const params = this.#buildRangeParams(rangeParams);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<TopContentData[]>>
            >(`${this.#EVENT_URL}/top-content`, { params })
            .pipe(map((response) => response.entity.data));
    }

    /**
     * Fetches pageviews by device and browser from the new analytics event endpoint.
     * Returns an array of items with browser, device, and total count.
     *
     * @param rangeParams - Object with either `range` or `from`+`to` query params
     * @returns Observable of DeviceBrowserData[]
     */
    getPageviewsByDeviceBrowser(rangeParams: ApiRangeParams): Observable<DeviceBrowserData[]> {
        const params = this.#buildRangeParams(rangeParams);

        return this.#http
            .get<
                DotCMSResponse<AnalyticsEventResponse<DeviceBrowserData[]>>
            >(`${this.#EVENT_URL}/pageviews-by-device-browser`, { params })
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
