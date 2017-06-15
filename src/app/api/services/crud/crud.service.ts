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
     * Load data from backend with pagination
     * @param {string} baseUrl Url without pagination query parameters
     * @param {number} limit Number of items to return, if it is -1 then return all the items
     * @param {number} offset Offset to start, if it is -1 then start with the first items
     * @param {sortOrder} sortOrder order with the follow sintax: fieldName-direction, direction could be 'asc' or 'desc'
     * @param {query} query text to filter
     * @returns {Observable<PaginationResponse>} response
     * @memberOf CrudService
     */
    loadData(baseUrl: string, limit: number, offset: number, sortField?: string,
                sortOrder?: OrderDirection, query?: string): Observable<PaginationResponse> {

        let params: URLSearchParams = new URLSearchParams();
        params.set('limit', String(limit));
        params.set('offset', String(offset));

        if (sortField) {
            let sortOrderString = sortOrder === OrderDirection.ASC ? 'asc' : 'desc';
            params.set('orderby', `${sortField}-${sortOrderString}`);
        }

        if (query) {
            params.set('query', `${query}`);
        }

        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            search: params,
            url: baseUrl
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

export enum OrderDirection {
    ASC,
    DESC
}