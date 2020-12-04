import { pluck } from 'rxjs/operators';
import { CoreWebService } from 'dotcms-js';
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
    public postData(baseUrl: string, data: any): Observable<any> {
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
    public putData(baseUrl: string, data: any): Observable<any> {
        return this.coreWebService
            .requestView({
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
    getDataById(baseUrl: string, id: string, pick = 'entity'): Observable<any> {
        return this.coreWebService
            .requestView({
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
    delete(baseUrl: string, id: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: 'DELETE',
                url: `${baseUrl}/${id}`
            })
            .pipe(pluck('entity'));
    }
}
