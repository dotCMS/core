import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { ESSearchParams, ESSearchResponse } from '@dotcms/dotcms-models';

@Injectable({ providedIn: 'root' })
export class DotEsSearchService {
    readonly #http = inject(HttpClient);

    /**
     * Executes an ES DSL query via POST /api/es/search.
     * The endpoint has @Consumes(APPLICATION_JSON) — body must be sent as JSON string.
     */
    search(query: string, params: ESSearchParams): Observable<ESSearchResponse> {
        let body: unknown;
        try {
            body = JSON.parse(query);
        } catch {
            return throwError(() => new SyntaxError('Invalid JSON query'));
        }

        let httpParams = new HttpParams().set('depth', 1).set('live', params.live ?? true);

        if (params.userid) {
            httpParams = httpParams.set('userid', params.userid);
        }

        return this.#http.post<ESSearchResponse>('/api/es/search', body, {
            params: httpParams
        });
    }
}
