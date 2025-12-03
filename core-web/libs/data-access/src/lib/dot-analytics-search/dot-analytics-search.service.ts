import { JsonObject } from '@angular-devkit/core';
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { AnalyticsQueryType } from '@dotcms/dotcms-models';

interface DotApiResponse<T> {
    entity: T;
}

/**
 * Service for performing analytics search operations.
 */
@Injectable()
export class DotAnalyticsSearchService {
    /**
     * URL for analytics content search with DotCMS standards.
     * @private
     */
    readonly #BASE_URL = '/api/v1/analytics/content/_query';
    /**
     * URL for analytics content search with the cube query standards.
     * @private
     */
    readonly #CUBE_URL = '/api/v1/analytics/content/_query/cube';

    readonly #http = inject(HttpClient);

    /**
     * Performs a POST request to the base URL with the provided query.
     * @param {Record<string, unknown>} query - The query object to be sent in the request body.
     * @param {AnalyticsQueryType} [type=AnalyticsQueryType.DEFAULT] - The type of analytics query to be performed.
     * @returns {Observable<JsonObject[]>} - An observable containing the response from the server.
     */
    get(
        query: Record<string, unknown>,
        type: AnalyticsQueryType = AnalyticsQueryType.DEFAULT
    ): Observable<JsonObject[]> {
        return this.#http
            .post<DotApiResponse<JsonObject[]>>(
                type == AnalyticsQueryType.DEFAULT ? this.#BASE_URL : this.#CUBE_URL,
                query
            )
            .pipe(map((res) => res?.entity));
    }
}
