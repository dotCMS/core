import { catchError, map, pluck, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import {
    DotCategory,
    DotCategoryPayload
} from '@dotcms/app/shared/models/dot-categories/dot-categories.model';

export const CATEGORIES_API_URL = '/api/v1/categories/';

/**
 * Provide util methods to handle categories in the system.
 * @export
 * @class DotCategoriesService
 */
@Injectable()
export class DotCategoriesUtillService {
    constructor(
        private coreWebService: CoreWebService,
        private httpErrorManagerService: DotHttpErrorManagerService
    ) {}

    /**
     * Creates a category
     * @param {DotCategoryRequest} values
     * @returns Observable<DotCategory>
     * @memberof DotCategoriesUtillService
     */

    create(values: DotCategoryPayload): Observable<DotCategory> {
        return this.request<DotCategory>({
            method: 'POST',
            url: CATEGORIES_API_URL,
            body: values
        });
    }

    /**
     * Updates a category
     *
     * @param {DotCategoryPayload} values
     * @returns Observable<DotCategory>
     * @memberof DotCategoriesUtillService
     */
    update(values: DotCategoryPayload): Observable<DotCategory> {
        return this.request<DotCategory>({
            method: 'PUT',
            url: CATEGORIES_API_URL,
            body: values
        });
    }

    /**
     * Get the category, pass the version default working, pass the includeContentType default false
     * @param {string} id
     * @param {string} version
     * @param {boolean} includeContentType
     * @returns {Observable<DotCategory>}
     * @memberof DotCategoriesUtillService
     */
    getById(id: string): Observable<DotCategory> {
        const url = `${CATEGORIES_API_URL}/${id}`;

        return this.request<DotCategory>({
            url
        });
    }

    private request<T>(options: DotRequestOptionsArgs): Observable<T> {
        const response$ = this.coreWebService.requestView<T>(options);

        return response$.pipe(
            pluck('entity'),
            catchError((error: HttpErrorResponse) => {
                return this.httpErrorManagerService.handle(error).pipe(
                    take(1),
                    map(() => null)
                );
            })
        );
    }
}
