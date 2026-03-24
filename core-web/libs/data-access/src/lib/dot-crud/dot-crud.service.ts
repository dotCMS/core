import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

/**
 * @deprecated
 * This class is deprecated. Use HttpClient directly instead of this service.
 * It is recommended to migrate any usage of DotCrudService to native HttpClient.
 * Provides utility listing methods
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
        return this.http
            .post<DotCMSResponse<T>>(baseUrl, data)
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
        return this.http
            .put<DotCMSResponse<T>>(baseUrl, data)
            .pipe(map((response) => response.entity));
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
    getDataById<T>(
        baseUrl: string,
        id: string,
        pick: 'entity' | 'contentlets' | 'tempFiles' = 'entity'
    ): Observable<T> {
        return this.http
            .get<DotCMSResponse<T>>(`${baseUrl}/id/${id}`)
            .pipe(map((response) => response[pick] as T));
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
        return this.http
            .delete<DotCMSResponse<T>>(`${baseUrl}/${id}`)
            .pipe(map((response) => response.entity));
    }
}
