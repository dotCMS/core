import { CoreWebService } from '../core-web-service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod, URLSearchParams } from '@angular/http';

/**
 * Provides util listing methods
 * @export
 * @class CrudService
 */
@Injectable()
export class CrudService {

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Will do a POST request and return the response to the url provide
     * and the data as body of the request.
     * @param {string} baseUrl
     * @param {*} data
     * @returns {Observable<any>}
     * @memberof CrudService
     */
    public postData(baseUrl: string, data: any): Observable<any> {
        return this.coreWebService.requestView({
            body: data,
            method: RequestMethod.Post,
            url: `${baseUrl}`
        }).pluck('entity');
    }

    /**
     * Will do a PUT request and return the response to the url provide
     * and the data as body of the request.
     * @param {string} baseUrl
     * @param {*} data
     * @returns {Observable<any>}
     * @memberof CrudService
     */
    public putData(baseUrl: string, data: any): Observable<any> {
        return this.coreWebService.requestView({
            body: data,
            method: RequestMethod.Put,
            url: `${baseUrl}`
        }).pluck('entity');
    }

    /**
     * Get item by id from the data loaded
     *
     * @param {string} id
     * @returns {Observable<any>}
     *
     * @memberof CrudService
     */
    getDataById(baseUrl: string, id: string): Observable<any> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: `${baseUrl}/id/${id}`
        }).pluck('entity');
    }
}