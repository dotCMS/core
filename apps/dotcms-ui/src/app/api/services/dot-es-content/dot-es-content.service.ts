import { take, map, pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';

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
    sortOrder?: ESOrderDirection;
}

/**
 * Provides util listing methods to get contentlets data from Elastic Search endpoint
 * @export
 * @class DotESContentService
 */
@Injectable()
export class DotESContentService {
    private _paginationPerPage = 40;
    private _offset = '0';
    private _url = '/api/content/_search';
    private _defaultQueryParams = { '+languageId': '1', '+deleted': 'false', '+working': 'true' };
    private _sortField = 'modDate';
    private _sortOrder: ESOrderDirection = ESOrderDirection.DESC;
    private _extraParams: Map<string, string> = new Map(Object.entries(this._defaultQueryParams));

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param queryEsParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    public get(params: queryEsParams): Observable<ESContent> {
        this.setBaseParams(params);
        const queryParams = this.getESQuery(this.getObjectFromMap(this._extraParams));

        return this.coreWebService
            .requestView<ESContent>({
                body: JSON.stringify(queryParams),
                method: 'POST',
                url: this._url
            })
            .pipe(pluck('entity'), take(1));
    }

    private setExtraParams(name: string, value?: string | number): void {
        if (value !== null && value !== undefined) {
            this._extraParams.set(name, value.toString());
        }
    }

    private deleteExtraParams(name: string): void {
        this._extraParams.delete(name);
    }

    private getESQuery(params: {
        [key: string]: string | number;
    }): { [key: string]: string | number } {
        const query = {
            query: JSON.stringify(params).replace(/"|{|}|,/g, ' '),
            sort: `${this._sortField || ''} ${this._sortOrder || ''}`,
            limit: this._paginationPerPage,
            offset: this._offset
        };

        return query;
    }

    private setBaseParams(params: queryEsParams): void {
        this._paginationPerPage = params.itemsPerPage || this._paginationPerPage;
        this._sortField = params.sortField || this._sortField;
        this._sortOrder = params.sortOrder || this._sortOrder;
        this._offset = params.offset || this._offset;
        this.deleteExtraParams('+languageId');
        this.deleteExtraParams('+title');

        // Getting values from Query string param
        const queryParamsArray = params.query.split('+').map((item) => {
            return item.split(':');
        });

        // Populating ExtraParams map
        queryParamsArray.forEach(([param, value]) => {
            if (param.length > 1) this.setExtraParams(`+${param}`, value);
        });

        if (params.lang) this.setExtraParams('+languageId', params.lang);

        let filterValue = '';
        if (params.filter && params.filter.indexOf(' ') > 0) {
            filterValue = `'${params.filter.replace(/'/g, "\\'")}'`;
        }
        this.setExtraParams('+title', `${filterValue || params.filter || ''}*`);
    }

    private getObjectFromMap(map: Map<string, string>): { [key: string]: string | number } {
        let result = Array.from(map).reduce(
            (obj, [key, value]) => Object.assign(obj, { [key]: value }),
            {}
        );

        return result;
    }
}
