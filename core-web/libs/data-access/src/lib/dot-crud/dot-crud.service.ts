import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

/**
 * Provides util listing methods
 * @export
 * @class CrudService
 */
@Injectable()
export class DotCrudService {
    private http = inject(HttpClient);

    /**
     * Will do a POST request and return the response to the url provide
     * and the data as body of the request.
     * @param string baseUrl
     * @param * data
     * @returns Observable<any>
     * @memberof CrudService
     */
    public postData<T, K>(baseUrl: string, data: K): Observable<T> {
        // Ensure URL starts with /api/ if it doesn't already
        const url = this.normalizeUrl(baseUrl);

        return this.http
            .post<DotCMSResponse<T>>(url, data)
            .pipe(map((response) => response.entity));
    }

    /**
     * Will do a PUT request and return the response to the url provide
     * and the data as body of the request.
     * @param string baseUrl
     * @param * data
     * @returns Observable<any>
     * @memberof CrudService
     */
    public putData<T>(baseUrl: string, data: unknown): Observable<T> {
        const url = this.normalizeUrl(baseUrl);

        return this.http.put<DotCMSResponse<T>>(url, data).pipe(map((response) => response.entity));
    }

    /**
     * Get item by id from the data loaded
     *
     * @param {string} baseUrl
     * @param {string} id
     * @param {string} [pick='entity']
     * @returns {Observable<any>}
     * @memberof DotCrudService
     */
    getDataById<T>(baseUrl: string, id: string, pick = 'entity'): Observable<T> {
        const url = this.normalizeUrl(`${baseUrl}/id/${id}`);

        return this.http.get<Record<string, T>>(url).pipe(map((response) => response[pick]));
    }

    /**
     * Delete item by id from the data loaded
     *
     * @param string baseUrl
     * @param string id
     * @returns Observable<any>
     * @memberof CrudService
     */
    delete<T>(baseUrl: string, id: string): Observable<T> {
        const url = this.normalizeUrl(`${baseUrl}/${id}`);

        return this.http.delete<DotCMSResponse<T>>(url).pipe(map((response) => response.entity));
    }

    /**
     * Normalizes URL to ensure it starts with /api/ if needed
     * @private
     */
    private normalizeUrl(url: string): string {
        if (url.startsWith('/api/') || url.startsWith('/')) {
            return url;
        }

        if (url.startsWith('v1/') || url.startsWith('v2/') || url.startsWith('v3/')) {
            return `/api/${url}`;
        }

        return url;
    }
}
