import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, shareReplay } from 'rxjs/operators';

import { HealthStatusTypes } from '@dotcms/dotcms-models';

import { AnalyticsApiResponse, CubeJSQuery } from '../../index';

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
