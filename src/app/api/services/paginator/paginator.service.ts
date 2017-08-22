import { CoreWebService } from '../core-web-service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod, URLSearchParams } from '@angular/http';

/**
 * Provides util listing methods
 * @export
 * @class PaginatorService
 */
@Injectable()
export class PaginatorService {

    public static readonly LINK_HEADER_NAME = 'Link';
    public static readonly PAGINATION_PER_PAGE_HEADER_NAME = 'X-Pagination-Per-Page';
    public static readonly PAGINATION_CURRENT_PAGE_HEADER_NAME = 'X-Pagination-Current-Page';
    public static readonly PAGINATION_MAX_LINK_PAGES_HEADER_NAME = 'X-Pagination-Link-Pages';
    public static readonly PAGINATION_TOTAL_ENTRIES_HEADER_NAME = 'X-Pagination-Total-Entries';

    public links: Links = {};

    public paginationPerPage: number;
    public currentPage: number;
    public maxLinksPage: number;
    public totalRecords: number;

    private _url: string;
    private _filter: string;
    private _sortField: string;
    private _sortOrder: OrderDirection;
    private _extraParams: URLSearchParams = new URLSearchParams();

    constructor(private coreWebService: CoreWebService) {
    }

    get url(): string{
        return this._url;
    }

    set url(url: string){
        if (this._url !== url) {
            this.links = {};
            this._url = url;
        }
    }

    get filter(): string{
        return this._filter;
    }

    set filter(filter: string) {
        if (this._filter !== filter) {
            this.links = {};
            this._filter = filter;
        }
    }

    addExtraParams(name: string, value: any): void {
        if (value !== null) {
            this.extraParams.append(name, value.toString());
        }
    }

    get extraParams(): URLSearchParams {
        return this._extraParams;
    }

    get sortField(): string{
        return this._sortField;
    }

    set sortField(sortField: string){
        if (this._sortField !== sortField) {
            this.links = {};
            this._sortField = sortField;
        }
    }

    get sortOrder(): OrderDirection{
        return this._sortOrder;
    }

    set sortOrder(sortOrder: OrderDirection){
        if (this._sortOrder !== sortOrder) {
            this.links = {};
            this._sortOrder = sortOrder;
        }
    }

    /**
     * Send a pagination request with url as base URL, if url is null or undefined then
     * it use the url property value instead.
     * Also it use the values of sortField, sortOrder and filter properties like query parameters,
     * so the finally url will be:
     * <url>/orderby=[sortField-value]&direction=[sortOrder-value]&filter=[filter-value]
     * @param url base url
     */
    public get(url?: string): Observable<any[]> {
        let params: URLSearchParams = new URLSearchParams();

        if (this.filter) {
            params.set('filter', `${this.filter}`);
        }

        if (this.sortField) {
            params.set('orderby', this.sortField);
        }

        if (this.sortOrder) {
            params.set('direction', OrderDirection[this.sortOrder]);
        }

        if (this.extraParams) {
            params.appendAll(this.extraParams);
        }

        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            search: params,
            url: url || this.url
        }).map(response => {
            this.setLinks(response.header(PaginatorService.LINK_HEADER_NAME));
            this.paginationPerPage = parseInt(response.header(PaginatorService.PAGINATION_PER_PAGE_HEADER_NAME), 10);
            this.currentPage = parseInt(response.header(PaginatorService.PAGINATION_CURRENT_PAGE_HEADER_NAME), 10);
            this.maxLinksPage = parseInt(response.header(PaginatorService.PAGINATION_MAX_LINK_PAGES_HEADER_NAME), 10);
            this.totalRecords = parseInt(response.header(PaginatorService.PAGINATION_TOTAL_ENTRIES_HEADER_NAME), 10);
            return response.entity;
        });
    }

    /**
     * Request the last page
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getLastPage(): Observable<any[]> {
        return this.get(this.links.last);
    }

    /**
     * Request the first page
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getFirstPage(): Observable<any[]> {
        return this.get(this.links.first);
    }

    /**
     * request the  pageParam page
     * @param {number} [pageParam=1] Page to request
     * @returns {Observable<any[]>}
     * @memberof PaginatorServic
     */
    public getPage(pageParam = 1): Observable<any[]> {
        let urlPage = this.links['x-page'] ? this.links['x-page'].replace('pageValue', String(pageParam))
                        : undefined;
        return this.get(urlPage);
    }

    /**
     * Request the current page
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getCurrentPage(): Observable<any[]> {
        return this.getPage(this.currentPage);
    }

    /**
     * Request the next page
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getNextPage(): Observable<any[]> {
        return this.get(this.links.next);
    }

    /**
     * Request the previous page
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getPrevPage(): Observable<any[]> {
        return this.get(this.links.prev);
    }

    /**
     * Use the offset to request a page.
     * @param {number} offset Offset to be request
     * @returns {Observable<any[]>}
     * @memberof PaginatorService
     */
    public getWithOffset(offset: number): Observable<any[]> {
        let page = this.getPageFromOffset(offset);
        return this.getPage(page);
    }

    private getPageFromOffset(offset: number): number {
        return parseInt(String(offset / this.paginationPerPage), 10) + 1;
    }

    private setLinks(linksString: string): void {
        let linkSplit = linksString.split(',');

        linkSplit.forEach(linkRel => {
            let linkrealSplit = linkRel.split(';');
            let url = linkrealSplit[0].substring(1, linkrealSplit[0].length - 1);
            let relSplit = linkrealSplit[1].split('=');
            let rel = relSplit[1].substring(1, relSplit[1].length - 1);
            this.links[rel] = url;
        });
    }
}

interface Links {
    first?: string;
    last?: string;
    next?: string;
    'x-page'?: string;
    prev?: string;
}

export enum OrderDirection {
    ASC = 1,
    DESC = -1
}