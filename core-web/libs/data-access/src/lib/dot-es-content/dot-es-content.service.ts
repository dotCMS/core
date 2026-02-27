import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { take, map } from 'rxjs/operators';

import { DotCMSResponse, ESContent } from '@dotcms/dotcms-models';

export enum ESOrderDirection {
    ASC = 'ASC',
    DESC = 'DESC'
}
export interface queryEsParams {
    itemsPerPage?: number;
    filter?: string;
    lang?: string;
    offset?: string;
    query: string;
    sortField?: string;
    sortOrder?: ESOrderDirection | string;
}
/**
 * Provides util listing methods to get contentlets data from Elastic Search endpoint
 * @export
 * @class DotESContentService
 */
@Injectable()
export class DotESContentService {
    private http = inject(HttpClient);

    private _paginationPerPage = 40;
    private _offset = '0';
    private _url = '/api/content/_search';
    private _defaultQueryParams = { '+languageId': '1', '+deleted': 'false', '+working': 'true' };
    private _sortField = 'modDate';
    private _sortOrder: ESOrderDirection | string = ESOrderDirection.DESC;
    private _extraParams: Map<string, string> = new Map(Object.entries(this._defaultQueryParams));

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param queryEsParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    public get(params: queryEsParams): Observable<ESContent> {
        this.setBaseParams(params);

        const queryParams = this.getESQuery(params, this.getObjectFromMap(this._extraParams));

        return this.http
            .post<DotCMSResponse<ESContent>>(this._url, JSON.stringify(queryParams))
            .pipe(
                map((response) => response.entity),
                take(1)
            );
    }

    private setExtraParams(name: string, value?: string | number): void {
        if (value !== null && value !== undefined) {
            this._extraParams.set(name, value.toString());
        }
    }

    private getESQuery(
        params: queryEsParams,
        extraParams: { [key: string]: string | number }
    ): {
        [key: string]: string | number;
    } {
        const query = {
            query: `${params.query} ${JSON.stringify(extraParams).replace(/["{},]/g, ' ')}`,
            sort: `${this._sortField || ''} ${this._sortOrder || ''}`,
            limit: this._paginationPerPage,
            offset: this._offset
        };

        return query;
    }

    private setBaseParams(params: queryEsParams): void {
        this._extraParams.clear();
        this._paginationPerPage = params.itemsPerPage || this._paginationPerPage;
        this._sortField = params.sortField || this._sortField;
        this._sortOrder = params.sortOrder || this._sortOrder;
        this._offset = params.offset || this._offset;

        if (params.lang) this.setExtraParams('+languageId', params.lang);

        let filterValue = params.filter || '';
        if (filterValue && filterValue.indexOf(' ') > 0) {
            filterValue = `'${filterValue.replace(/'/g, "\\'")}'`;
        }

        if (filterValue) this.setExtraParams('+title', `${filterValue}*`);
    }

    private getObjectFromMap(map: Map<string, string>): { [key: string]: string | number } {
        const result = Array.from(map).reduce(
            (obj, [key, value]) => Object.assign(obj, { [key]: value }),
            {}
        );

        return result;
    }
}
