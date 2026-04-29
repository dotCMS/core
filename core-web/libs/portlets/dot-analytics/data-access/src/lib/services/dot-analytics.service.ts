import { Observable, of } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import {
    AnalyticsApiResponse,
    AnalyticsEventResponse,
    CubeJSQuery,
    TotalEventsByDayData,
    TotalEventsData
} from '../../index';
import { ApiRangeParams } from '../utils/data/analytics-data.utils';

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
        granularity: string
    ): Observable<TotalEventsByDayData[]>;
    getTotalEvents(
        rangeParams: ApiRangeParams,
        granularity?: string
    ): Observable<TotalEventsData | TotalEventsByDayData[]> {
        let params = new HttpParams();

        if (rangeParams.range) {
            params = params.set('range', rangeParams.range);
        }
        if (rangeParams.from) {
            params = params.set('from', rangeParams.from);
        }
        if (rangeParams.to) {
            params = params.set('to', rangeParams.to);
        }
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
