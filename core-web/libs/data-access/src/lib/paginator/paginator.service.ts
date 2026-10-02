import { Observable } from 'rxjs';

import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { take, map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

export enum OrderDirection {
    ASC = 1,
    DESC = -1
}

interface Links {
    first?: string;
    last?: string;
    next?: string;
    'x-page'?: string;
    prev?: string;
    // `setLinks` parses the `Link` header and stores whatever `rel` values the
    // server sends, which is not limited to the names above.
    [key: string]: string | undefined;
}

interface PaginatiorServiceParams {
    filter?: string;
    searchParam?: string;
    orderby?: string;
    direction?: OrderDirection;
    per_page?: string;
}

/**
 * Provides util listing methods
 * @export
 * @class PaginatorService
 */
@Injectable()
export class PaginatorService {
    private readonly http = inject(HttpClient);

    public static readonly LINK_HEADER_NAME = 'Link';
    public static readonly PAGINATION_PER_PAGE_HEADER_NAME = 'X-Pagination-Per-Page';
    public static readonly PAGINATION_CURRENT_PAGE_HEADER_NAME = 'X-Pagination-Current-Page';
    public static readonly PAGINATION_MAX_LINK_PAGES_HEADER_NAME = 'X-Pagination-Link-Pages';
    public static readonly PAGINATION_TOTAL_ENTRIES_HEADER_NAME = 'X-Pagination-Total-Entries';

    public links: Links = {};

    public paginationPerPage = 40;
    // Populated from the response headers on the first request, so these hold
    // zero values until one completes.
    public currentPage = 0;
    public maxLinksPage = 0;
    public totalRecords = 0;

    private _url = '';
    private _filter = '';
    private _searchParam = '';
    private _sortField = '';
    // Deliberately left undefined rather than defaulting to a member: `getParams`
    // gates `direction` on truthiness, and OrderDirection.ASC === 1 would start
    // sending a param that was previously omitted.
    private _sortOrder?: OrderDirection;
    private _extraParams: Map<string, string> = new Map();

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

    set searchParam(searchParam: string) {
        if (this._searchParam !== searchParam) {
            this.links = searchParam.length > 0 ? {} : this.links;
            this._searchParam = searchParam;
        }
    }

    get searchParam(): string {
        return this._searchParam;
    }

    /**
     * Set value of extra parameters of the eventual request.
     * @param string name
     * @param value
     *
     * @memberof DotThemeSelectorComponent
     */
    setExtraParams<T = Record<string, unknown>>(name: string, value?: T): void {
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

    /**
     * Reset extra parameters of the eventual request.
     *
     * @memberof PaginatorService
     */
    public resetExtraParams(): void {
        this.extraParams.clear();
    }

    get extraParams(): Map<string, string> {
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

    get sortOrder(): OrderDirection | undefined {
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
    public get<T>(url?: string): Observable<T> {
        const params: Record<string, unknown> = {
            ...this.getParams(),
            ...this.getObjectFromMap(this.extraParams)
        };

        const cleanURL = this.sanitizeQueryParams(url, params);
        const requestUrl = cleanURL || this.url;

        return this.http
            .get<DotCMSResponse<T>>(requestUrl, {
                params: params as Record<string, string>,
                observe: 'response'
            })
            .pipe(
                map((response: HttpResponse<DotCMSResponse<T>>) => {
                    this.setLinks(response.headers.get(PaginatorService.LINK_HEADER_NAME));
                    this.paginationPerPage = parseInt(
                        response.headers.get(PaginatorService.PAGINATION_PER_PAGE_HEADER_NAME) ??
                            '',
                        10
                    );
                    this.currentPage = parseInt(
                        response.headers.get(
                            PaginatorService.PAGINATION_CURRENT_PAGE_HEADER_NAME
                        ) ?? '',
                        10
                    );
                    this.maxLinksPage = parseInt(
                        response.headers.get(
                            PaginatorService.PAGINATION_MAX_LINK_PAGES_HEADER_NAME
                        ) ?? '',
                        10
                    );
                    this.totalRecords = parseInt(
                        response.headers.get(
                            PaginatorService.PAGINATION_TOTAL_ENTRIES_HEADER_NAME
                        ) ?? '',
                        10
                    );

                    return response.body?.entity ?? ([] as unknown as T);
                }),
                take(1)
            );
    }

    /**
     * Request the last page
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getLastPage<T>(): Observable<T> {
        return this.get(this.links.last);
    }

    /**
     * Request the first page
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getFirstPage<T>(): Observable<T> {
        return this.get(this.links.first);
    }

    /**
     * request the  pageParam page
     * @param number [pageParam=1] Page to request
     * @returns Observable<T>
     * @memberof PaginatorServic
     */
    public getPage<T>(pageParam = 1): Observable<T> {
        const urlPage = this.links['x-page']
            ? this.links['x-page'].replace('pageValue', String(pageParam))
            : undefined;

        return this.get(urlPage);
    }

    /**
     * Request the current page
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getCurrentPage<T>(): Observable<T> {
        return this.getPage(this.currentPage);
    }

    /**
     * Request the next page
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getNextPage<T>(): Observable<T> {
        return this.get(this.links.next);
    }

    /**
     * Request the previous page
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getPrevPage<T>(): Observable<T> {
        return this.get(this.links.prev);
    }

    /**
     * Use the offset to request a page.
     * @param number offset Offset to be request
     * @returns Observable<T>
     * @memberof PaginatorService
     */
    public getWithOffset<T>(offset: number): Observable<T> {
        const page = this.getPageFromOffset(offset);

        return this.getPage(page);
    }

    private getPageFromOffset(offset: number): number {
        return parseInt(String(offset / this.paginationPerPage), 10) + 1;
    }

    private setLinks(linksString: string | null): void {
        const linkSplit = linksString?.split(',') || [];

        linkSplit.forEach((linkRel) => {
            const linkrealSplit = linkRel.split(';');
            const url = linkrealSplit[0].substring(1, linkrealSplit[0].length - 1);
            const relSplit = linkrealSplit[1].split('=');
            const rel = relSplit[1].substring(1, relSplit[1].length - 1);
            this.links[rel] = url.trim();
        });
    }

    private getParams(): PaginatiorServiceParams {
        const params = new Map();

        if (this.filter) {
            params.set('filter', this.filter);
        }

        if (this.searchParam) {
            params.set('searchParam', this.searchParam);
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

        return this.getObjectFromMap<PaginatiorServiceParams>(params);
    }

    private getObjectFromMap<T = { [key: string]: Record<string, unknown> }>(
        map: Map<string, unknown>
    ): T {
        const result = Array.from(map).reduce(
            (obj, [key, value]) => Object.assign(obj, { [key]: value }),
            {}
        );

        return result as T;
    }

    /**
     *
     * Use to remove repeated query params in the url
     * @private
     * @param {string} [url='']
     * @param {Record<string, unknown>} params
     * @return {*}  {string}
     * @memberof PaginatorService
     */
    private sanitizeQueryParams(url = '', params: Record<string, unknown>): string {
        const urlArr = url?.split('?');
        const baseUrl = urlArr[0];
        const queryParams = urlArr[1];

        if (!queryParams) {
            return url;
        }

        const searchParams = new URLSearchParams(queryParams);

        for (const property in params) {
            searchParams.delete(property);
        }

        return searchParams.toString() ? `${baseUrl}?${searchParams.toString()}` : baseUrl;
    }
}
