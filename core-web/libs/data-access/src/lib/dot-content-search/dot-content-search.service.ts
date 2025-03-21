import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

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
    sortOrder?: ESOrderDirectionSearch;
}

export interface DotContentSearchParams {
    globalSearch?: string;
    systemSearchableFields?: Record<string, unknown>;
    searchableFieldsByContentType?: Record<string, Record<string, unknown>>;
    page?: number;
    perPage?: number;
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
    public get<T>({ query, limit = 0, offset = 0 }: EsQueryParamsSearch): Observable<T> {
        return this.#http
            .post('/api/content/_search', {
                query,
                sort: 'score,modDate desc',
                limit,
                offset
            })
            .pipe(pluck('entity'));
    }

    /**
     * Searches for contentlets using the new content search API
     * @param params Search parameters including globalSearch, systemSearchableFields, page, and perPage
     * @returns Observable with an array of DotCMSContentlet objects
     */
    search(params: DotContentSearchParams): Observable<DotCMSContentlet[]> {
        const payload: Partial<DotContentSearchParams> = {};

        if (params.globalSearch ?? null) {
            payload.globalSearch = params.globalSearch;
        }

        if (params.systemSearchableFields ?? null) {
            payload.systemSearchableFields = params.systemSearchableFields;
        }

        if (params.searchableFieldsByContentType ?? null) {
            payload.searchableFieldsByContentType = params.searchableFieldsByContentType;
        }

        if (params.page ?? null) {
            payload.page = params.page;
        }

        if (params.perPage ?? null) {
            payload.perPage = params.perPage;
        }

        return this.#http
            .post('/api/v1/content/search', payload)
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }
}
