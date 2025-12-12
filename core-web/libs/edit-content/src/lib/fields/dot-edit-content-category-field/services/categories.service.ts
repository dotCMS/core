import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotCategory } from '@dotcms/dotcms-models';

import { HierarchyParent } from '../models/dot-category-field.models';

export const API_URL = '/api/v1/categories';

export const ITEMS_PER_PAGE = 7000;

export interface GetChildrenParams {
    inode: string;
    per_page: number;
    direction: string;
    showChildrenCount: boolean;
    filter?: string;
    allLevels?: boolean;
    parentList?: boolean;
}

const DEFAULT_PARAMS: Omit<GetChildrenParams, 'inode'> = {
    per_page: 7000,
    direction: 'ASC',
    showChildrenCount: true,
    allLevels: false,
    parentList: true
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
    getChildren(inode: string, params: Partial<GetChildrenParams> = {}): Observable<DotCategory[]> {
        const mergedParams = this.mergeParams({ ...params, inode });
        const httpParams = this.toHttpParams(mergedParams);

        return this.#http
            .get<DotCMSResponse<DotCategory[]>>(`${API_URL}/children`, {
                params: httpParams
            })
            .pipe(map((x: DotCMSResponse<DotCategory[]>) => x?.entity));
    }

    /**
     * Retrieves the complete hierarchy for the given selected keys.
     *
     *
     * @return {Observable<DotCategory[]>} - An Observable that emits the complete hierarchy as an array of DotCategory objects.
     * @param keys
     */
    getSelectedHierarchy(keys: string[]): Observable<HierarchyParent[]> {
        return this.#http
            .post<DotCMSResponse<HierarchyParent[]>>(`${API_URL}/hierarchy`, { keys })
            .pipe(map((x: DotCMSResponse<HierarchyParent[]>) => x?.entity));
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
            .set('direction', params.direction)
            .set('parentList', params.parentList);

        if (!params.filter) {
            // No add showChildrenCount when we use filter
            httpParams = httpParams.set('showChildrenCount', params.showChildrenCount.toString());
        } else {
            httpParams = httpParams.set('filter', params.filter).set('allLevels', 'true');
        }

        return httpParams;
    }
}
