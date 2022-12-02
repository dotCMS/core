import { Injectable } from '@angular/core';
import { pluck, take } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
    limit?: number;
    sortOrder?: ESOrderDirection;
}

Injectable({
    providedIn: 'root'
});
export class SearchService {
    constructor(private http: HttpClient) {}

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param queryEsParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    /* eslint-disable @typescript-eslint/no-explicit-any */
    public get(params: queryEsParams): Observable<any> {
        const body = this.getESQuery(params);

        return this.http
            .post('/api/content/_search', {
                body
            })
            .pipe(pluck('entity'), take(1));
    }

    private getESQuery(params: queryEsParams): {
        [key: string]: string | number;
    } {
        const query = {
            query: JSON.stringify(params).replace(/"|{|}|,/g, ' '),
            sort: 'score,modDate desc',
            limit: 20,
            offset: 0
        };

        return query;
    }
}
