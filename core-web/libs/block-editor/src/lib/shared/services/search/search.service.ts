import { Injectable } from '@angular/core';
import { pluck } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum ESOrderDirection {
    ASC = 'ASC',
    DESC = 'DESC'
}

export interface EsQueryParams {
    itemsPerPage?: number;
    filter?: string;
    lang?: string;
    offset?: string | number;
    query: string;
    sortField?: string;
    limit?: number;
    sortOrder?: ESOrderDirection;
}

@Injectable({
    providedIn: 'root'
})
export class SearchService {
    constructor(private http: HttpClient) {}

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param EsQueryParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    public get<T>({ query, limit = 0, offset = 0 }: EsQueryParams): Observable<T> {
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
