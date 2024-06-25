import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';

import { DotCategoryFieldCategory } from '../models/dot-category-field.models';

export const API_URL = '/api/v1/categories';

export const ITEMS_PER_PAGE = 7000;

/**
 * CategoriesService class.
 *
 * This service is responsible for retrieving the children of a given inode.
 */
@Injectable()
export class CategoriesService {
    readonly #http: HttpClient = inject(HttpClient);

    /**
     * Retrieves the children of a given inode.
     *
     * @param {string} inode - The inode of the parent node.
     * @returns {Observable<DotCategory[]>} - An Observable that emits the children of the given inode as an array of DotCategory objects.
     */
    getChildren(inode: string): Observable<DotCategoryFieldCategory[]> {
        const params = new HttpParams()
            .set('per_page', ITEMS_PER_PAGE)
            .set('direction', 'ASC')
            .set('inode', inode)
            .set('showChildrenCount', 'true');

        return this.#http
            .get<DotCMSResponse<DotCategoryFieldCategory[]>>(`${API_URL}/children`, {
                params
            })
            .pipe(pluck('entity'));
    }
}
