import { pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Provides util listing methods
 * @export
 * @class CrudService
 */
@Injectable()
export class DotCrudService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Will do a POST request and return the response to the url provide
     * and the data as body of the request.
     * @param string baseUrl
     * @param * data
     * @returns Observable<any>
     * @memberof CrudService
     */
    public postData<T, K>(baseUrl: string, data: K): Observable<T> {
        return this.coreWebService
            .requestView({
                body: data,
                method: 'POST',
                url: `${baseUrl}`
            })
            .pipe(pluck('entity'));
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
        return this.coreWebService
            .requestView<T>({
                body: data,
                method: 'PUT',
                url: `${baseUrl}`
            })
            .pipe(pluck('entity'));
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
        return this.coreWebService
            .requestView<T>({
                url: `${baseUrl}/id/${id}`
            })
            .pipe(pluck(pick));
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
        return this.coreWebService
            .requestView<T>({
                method: 'DELETE',
                url: `${baseUrl}/${id}`
            })
            .pipe(pluck('entity'));
    }
}
