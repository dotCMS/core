import { take, map } from 'rxjs/operators';
import { CoreWebService, ResponseView } from 'dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export enum OrderDirection {
    ASC = 1,
    DESC = -1
}

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
    private _extraParams: Map<string, any> = new Map();

    constructor(private coreWebService: CoreWebService) {}

    get url(): string {
        return this._url;
    }

    set url(url: string) {
        if (this._url !== url) {
            this.links = {};
            this._url = url;
        }
    }

    get filter(): string {
        return this._filter;
    }

    set filter(filter: string) {
        if (this._filter !== filter) {
            this.links = {};
            this._filter = filter;
        }
    }
    /**
     * Set value of extra parameters of the eventual request.
     * @param string name
     * @param value
     *
     * @memberof DotThemeSelectorComponent
     */
    setExtraParams(name: string, value?: any): void {
        if (value !== null && value !== undefined) {
            this.extraParams.set(name, value.toString());
            this.links = {};
        }
    }

    /**
     * Delete extra parameters of the eventual request.
     * @param string name
     *
     * @memberof DotThemeSelectorComponent
     */
    deleteExtraParams(name: string): void {
        this.extraParams.delete(name);
    }

    get extraParams(): Map<string, any> {
        return this._extraParams;
    }

    get sortField(): string {
        return this._sortField;
    }

    set sortField(sortField: string) {
        if (this._sortField !== sortField) {
            this.links = {};
            this._sortField = sortField;
        }
    }

    get sortOrder(): OrderDirection {
        return this._sortOrder;
    }

    set sortOrder(sortOrder: OrderDirection) {
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
    // tslint:disable-next-line:cyclomatic-complexity
    public get(url?: string): Observable<any> {
        const params = {
            ...this.getParams(),
            ...this.getObjectFromMap(this.extraParams)
        };

        return this.coreWebService
            .requestView({
                params,
                url: url || this.url
            })
            .pipe(
                map((response: ResponseView<any>) => {
                    this.setLinks(response.header(PaginatorService.LINK_HEADER_NAME));
                    this.paginationPerPage = parseInt(
                        response.header(PaginatorService.PAGINATION_PER_PAGE_HEADER_NAME),
                        10
                    );
                    this.currentPage = parseInt(
                        response.header(PaginatorService.PAGINATION_CURRENT_PAGE_HEADER_NAME),
                        10
                    );
                    this.maxLinksPage = parseInt(
                        response.header(PaginatorService.PAGINATION_MAX_LINK_PAGES_HEADER_NAME),
                        10
                    );
                    this.totalRecords = parseInt(
                        response.header(PaginatorService.PAGINATION_TOTAL_ENTRIES_HEADER_NAME),
                        10
                    );
                    return response.entity;
                }),
                take(1)
            );
    }

    /**
     * Request the last page
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getLastPage(): Observable<any[]> {
        return this.get(this.links.last);
    }

    /**
     * Request the first page
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getFirstPage(): Observable<any[]> {
        return this.get(this.links.first);
    }

    /**
     * request the  pageParam page
     * @param number [pageParam=1] Page to request
     * @returns Observable<any[]>
     * @memberof PaginatorServic
     */
    public getPage(pageParam = 1): Observable<any[]> {
        const urlPage = this.links['x-page']
            ? this.links['x-page'].replace('pageValue', String(pageParam))
            : undefined;
        return this.get(urlPage);
    }

    /**
     * Request the current page
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getCurrentPage(): Observable<any[]> {
        return this.getPage(this.currentPage);
    }

    /**
     * Request the next page
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getNextPage(): Observable<any[]> {
        return this.get(this.links.next);
    }

    /**
     * Request the previous page
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getPrevPage(): Observable<any[]> {
        return this.get(this.links.prev);
    }

    /**
     * Use the offset to request a page.
     * @param number offset Offset to be request
     * @returns Observable<any[]>
     * @memberof PaginatorService
     */
    public getWithOffset(offset: number): Observable<any[]> {
        const page = this.getPageFromOffset(offset);
        return this.getPage(page);
    }

    private getPageFromOffset(offset: number): number {
        return parseInt(String(offset / this.paginationPerPage), 10) + 1;
    }

    private setLinks(linksString: string): void {
        const linkSplit = linksString.split(',');

        linkSplit.forEach((linkRel) => {
            const linkrealSplit = linkRel.split(';');
            const url = linkrealSplit[0].substring(1, linkrealSplit[0].length - 1);
            const relSplit = linkrealSplit[1].split('=');
            const rel = relSplit[1].substring(1, relSplit[1].length - 1);
            this.links[rel] = url.trim();
        });
    }

    private getParams(): { [key: string]: any } {
        const params = new Map();

        if (this.filter) {
            params.set('filter', this.filter);
        }

        if (this.sortField) {
            params.set('orderby', this.sortField);
        }

        if (this.sortOrder) {
            params.set('direction', OrderDirection[this.sortOrder]);
        }

        if (this.paginationPerPage) {
            params.set('per_page', String(this.paginationPerPage));
        }
        return this.getObjectFromMap(params);
    }

    private getObjectFromMap(map: Map<string, any>): { [key: string]: any } {
        let result = Array.from(map).reduce(
            (obj, [key, value]) => Object.assign(obj, { [key]: value }),
            {}
        );

        return result;
    }
}

interface Links {
    first?: string;
    last?: string;
    next?: string;
    'x-page'?: string;
    prev?: string;
}
