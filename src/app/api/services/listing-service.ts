import { Injectable } from '@angular/core';
import { CoreWebService } from './core-web-service';
import { RequestMethod } from '@angular/http';
import { Observable } from 'rxjs/Observable';

/**
 * Provides util listing methods
 * @export
 * @class ListingService
 */
@Injectable()
export class ListingService {

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Load data from backend with pagination
     * @param {string} baseUrl Url without pagination query parameters
     * @param {number} limit Number of items to return, if it is -1 then return all the items
     * @param {number} offset Offset to start, if it is -1 then start with the first items
     * @returns {Observable<PaginationResponse>} response
     * @memberOf ListingService
     */
    loadData(baseUrl: string, limit: number, offset: number): Observable<PaginationResponse> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: `${baseUrl}?limit=${limit}&offset=${offset}`
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