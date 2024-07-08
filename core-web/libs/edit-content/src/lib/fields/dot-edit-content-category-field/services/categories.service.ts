import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';

import { DotCategoryFieldCategory } from '../models/dot-category-field.models';

export const API_URL = '/api/v1/categories';

export const ITEMS_PER_PAGE = 7000;

export interface GetChildrenParams {
    inode: string;
    per_page: number;
    direction: string;
    showChildrenCount: boolean;
    filter?: string;
    allLevels?: boolean;
}

const DEFAULT_PARAMS: Omit<GetChildrenParams, 'inode'> = {
    per_page: 7000,
    direction: 'ASC',
    showChildrenCount: true,
    allLevels: false
};

/**
 * CategoriesService class.
 *
 * This service is responsible for retrieving the children of a given inode.
 */
@Injectable()
export class CategoriesService {
    readonly #http = inject(HttpClient);

    /**
     * Retrieves the children of a given inode.
     *
     * @param {string} inode - The inode of the parent node.
     * @param params
     * @returns {Observable<DotCategory[]>} - An Observable that emits the children of the given inode as an array of DotCategory objects.
     */
    getChildren(
        inode: string,
        params: Partial<GetChildrenParams> = {}
    ): Observable<DotCategoryFieldCategory[]> {
        const mergedParams = this.mergeParams({ ...params, inode });
        const httpParams = this.toHttpParams(mergedParams);

        return this.#http
            .get<DotCMSResponse<DotCategoryFieldCategory[]>>(`${API_URL}/children`, {
                params: httpParams
            })
            .pipe(pluck('entity'));
    }

    /**
     * Merges default parameters with provided parameters.
     *
     * @param {Partial<GetChildrenParams>} params - The provided parameters.
     * @returns {GetChildrenParams} - The merged parameters.
     */
    private mergeParams(params: Partial<GetChildrenParams>): GetChildrenParams {
        return { ...DEFAULT_PARAMS, ...params } as GetChildrenParams;
    }

    /**
     * Converts an object to HttpParams.
     *
     * @param {GetChildrenParams} params - The parameters object.
     * @returns {HttpParams} - The HttpParams object.
     */
    private toHttpParams(params: GetChildrenParams): HttpParams {
        let httpParams = new HttpParams()
            .set('inode', params.inode)
            .set('per_page', params.per_page.toString())
            .set('direction', params.direction);

        if (!params.filter) {
            // No add showChildrenCount when we use filter
            httpParams = httpParams.set('showChildrenCount', params.showChildrenCount.toString());
        } else {
            httpParams = httpParams.set('filter', params.filter).set('allLevels', 'true');
        }

        return httpParams;
    }
}
