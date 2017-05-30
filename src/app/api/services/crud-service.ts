import { Injectable } from '@angular/core';
import { CoreWebService } from './core-web-service';
import { RequestMethod } from '@angular/http';
import { Observable } from 'rxjs/Observable';

/**
 * Provides util listing methods
 * @export
 * @class CrudService
 */
@Injectable()
export class CrudService {

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Load data from backend with pagination
     * @param {string} baseUrl Url without pagination query parameters
     * @param {number} limit Number of items to return, if it is -1 then return all the items
     * @param {number} offset Offset to start, if it is -1 then start with the first items
     * @returns {Observable<PaginationResponse>} response
     * @memberOf CrudService
     */
    loadData(baseUrl: string, limit: number, offset: number): Observable<PaginationResponse> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: `${baseUrl}?limit=${limit}&offset=${offset}`
        }).pluck('entity');
    }

    /**
     * Will do a POST request and return the response to the url provide
     * and the data as body of the request.
     * @param {string} baseUrl
     * @param {*} data
     * @returns {Observable<any>}
     * @memberof CrudService
     */
    postData(baseUrl: string, data: any): Observable<any> {
        return this.coreWebService.requestView({
            body: data,
            method: RequestMethod.Post,
            url: `${baseUrl}`
        }).pluck('entity');
    }
}

/**
 * Response with pagination data. The response contains:
 * - items: items returned
 * - totalRecords: total number of items.
 * @export
 * @interface PaginationResponse
 */
export interface PaginationResponse {
    items: any[];
    totalRecords: number;
}