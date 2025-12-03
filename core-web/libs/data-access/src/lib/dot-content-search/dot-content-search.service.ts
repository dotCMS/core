import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
    sort?: string;
    sortOrder?: ESOrderDirectionSearch;
}
export interface DotContentSearchParams {
    globalSearch?: string;
    systemSearchableFields?: Record<string, unknown>;
    searchableFieldsByContentType?: Record<string, Record<string, unknown>>;
    page?: number;
    perPage?: number;
}
export interface DotContentSearchResponse {
    entity: {
        jsonObjectView: {
            contentlets: DotCMSContentlet[];
        };
        resultsSize: number;
    };
}

interface DotApiResponse<T> {
    entity: T;
}

@Injectable({
    providedIn: 'root'
})
export class DotContentSearchService {
    readonly #http = inject(HttpClient);

    /**
     * Returns a list of contentlets from Elastic Search endpoint
     * @param EsQueryParams params
     * @returns Observable<ESContent>
     * @memberof DotESContentService
     */
    public get<T>({
        query,
        limit = 0,
        offset = 0,
        sort = 'score,modDate desc'
    }: EsQueryParamsSearch): Observable<T> {
        return this.#http
            .post<DotApiResponse<T>>('/api/content/_search', {
                query,
                sort,
                limit,
                offset
            })
            .pipe(map((res) => res?.entity));
    }

    /**
     * Searches for contentlets using the new content search API
     * @param params Search parameters including globalSearch, systemSearchableFields, page, and perPage
     * @returns Observable with an array of DotCMSContentlet objects
     */
    search(params: DotContentSearchParams): Observable<DotContentSearchResponse['entity']> {
        const payload: Partial<DotContentSearchParams> = {};

        if (params.globalSearch !== undefined) {
            payload.globalSearch = params.globalSearch;
        }

        if (params.systemSearchableFields !== undefined) {
            payload.systemSearchableFields = params.systemSearchableFields;
        }

        if (params.searchableFieldsByContentType !== undefined) {
            payload.searchableFieldsByContentType = params.searchableFieldsByContentType;
        }

        if (params.page !== undefined) {
            payload.page = params.page;
        }

        if (params.perPage !== undefined) {
            payload.perPage = params.perPage;
        }

        return this.#http
            .post<DotContentSearchResponse>('/api/v1/content/search', payload)
            .pipe(map((res) => res?.entity));
    }
}
