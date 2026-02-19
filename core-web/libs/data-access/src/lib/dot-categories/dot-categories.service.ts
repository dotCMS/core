import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { DotCMSAPIResponse, DotCategory } from '@dotcms/dotcms-models';

export interface DotCategoriesPaginationParams {
    filter?: string;
    page?: number;
    per_page?: number;
    orderby?: string;
    direction?: string;
}

export interface DotCategoryForm {
    categoryName: string;
    key?: string;
    categoryVelocityVarName?: string;
    sortOrder?: number;
    active?: boolean;
    description?: string;
    keywords?: string;
    parent?: string;
    inode?: string;
}

@Injectable({
    providedIn: 'root'
})
export class DotCategoriesService {
    readonly #http = inject(HttpClient);

    /**
     * Retrieves top-level categories with pagination, filtering, and sorting.
     * @param params - Query parameters for the paginated request.
     * @returns Observable with entity array and pagination metadata.
     */
    getCategoriesPaginated(
        params: DotCategoriesPaginationParams
    ): Observable<DotCMSAPIResponse<DotCategory[]>> {
        return this.#http.get<DotCMSAPIResponse<DotCategory[]>>('/api/v1/categories', {
            params: this.#buildParams(params)
        });
    }

    /**
     * Retrieves children categories for a given parent inode.
     * @param inode - The parent category inode.
     * @param params - Pagination parameters.
     * @returns Observable with entity array and pagination metadata.
     */
    getChildrenPaginated(
        inode: string,
        params: DotCategoriesPaginationParams
    ): Observable<DotCMSAPIResponse<DotCategory[]>> {
        let httpParams = this.#buildParams(params);
        httpParams = httpParams.set('inode', inode);

        return this.#http.get<DotCMSAPIResponse<DotCategory[]>>('/api/v1/categories/children', {
            params: httpParams
        });
    }

    /**
     * Creates a new category.
     * @param form - The category form data.
     * @returns Observable with the created category.
     */
    createCategory(form: DotCategoryForm): Observable<DotCMSAPIResponse<DotCategory>> {
        return this.#http.post<DotCMSAPIResponse<DotCategory>>('/api/v1/categories', form);
    }

    /**
     * Updates an existing category.
     * @param form - The category form data (must include inode).
     * @returns Observable with the updated category.
     */
    updateCategory(form: DotCategoryForm): Observable<DotCMSAPIResponse<DotCategory>> {
        return this.#http.put<DotCMSAPIResponse<DotCategory>>('/api/v1/categories', form);
    }

    /**
     * Deletes categories by their inodes.
     * @param inodes - Array of category inodes to delete.
     * @returns Observable with the deletion result.
     */
    deleteCategories(
        inodes: string[]
    ): Observable<DotCMSAPIResponse<{ successCount: number; fails: unknown[] }>> {
        return this.#http.request<DotCMSAPIResponse<{ successCount: number; fails: unknown[] }>>(
            'DELETE',
            '/api/v1/categories',
            { body: inodes }
        );
    }

    #buildParams(params: DotCategoriesPaginationParams): HttpParams {
        let httpParams = new HttpParams();

        if (params.filter) {
            httpParams = httpParams.set('filter', params.filter);
        }

        if (params.page) {
            httpParams = httpParams.set('page', params.page.toString());
        }

        if (params.per_page) {
            httpParams = httpParams.set('per_page', params.per_page.toString());
        }

        if (params.orderby) {
            httpParams = httpParams.set('orderby', params.orderby);
        }

        if (params.direction) {
            httpParams = httpParams.set('direction', params.direction);
        }

        httpParams = httpParams.set('showChildrenCount', 'true');

        return httpParams;
    }
}
