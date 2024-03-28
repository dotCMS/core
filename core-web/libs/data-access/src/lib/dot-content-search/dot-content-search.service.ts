import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

export enum ESOrderDirectionSearch {
    ASC = 'ASC',
    DESC = 'DESC'
}

export interface EsQueryParamsSearch {
    itemsPerPage?: number;
    filter?: string;
    lang?: string;
    offset?: string | number;
    query: string;
    sortField?: string;
    limit?: number;
    sortOrder?: ESOrderDirectionSearch;
}

@Injectable({
    providedIn: 'root'
})
export class DotContentSearchService {
    constructor(private http: HttpClient) {}

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param EsQueryParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    public get<T>({ query, limit = 0, offset = 0 }: EsQueryParamsSearch): Observable<T> {
        return this.http
            .post('/api/content/_search', {
                query,
                sort: 'score,modDate desc',
                limit,
                offset
            })
            .pipe(pluck('entity'));
    }
}
