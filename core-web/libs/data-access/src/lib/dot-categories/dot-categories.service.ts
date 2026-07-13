import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, DotCategory } from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';

export interface DotCategoriesPaginationParams {
    filter?: string;
    page?: number;
    per_page?: number;
    orderby?: string;
    direction?: 'ASC' | 'DESC';
}

export interface DotCategoryImportResult {
    successCount: number;
    skippedCount: number;
    fails: string[];
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

/**
 * Form data for updating an existing category.
 * Extends DotCategoryForm with a required inode, since a PUT without inode is meaningless.
 */
export interface DotCategoryUpdateForm extends DotCategoryForm {
    inode: string;
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
     * Retrieves a single category by its inode or key. The endpoint resolves by inode first, then
     * key, so a caller holding a persisted category inode (e.g. restored from a URL) can look up
     * its display name. GET /api/v1/categories/{idOrKey}.
     * @param idOrKey - Category inode or key.
     * @returns Observable with the resolved category.
     */
    getCategory(idOrKey: string): Observable<DotCategory> {
        return this.#http
            .get<DotCMSAPIResponse<DotCategory>>(`/api/v1/categories/${idOrKey}`)
            .pipe(map((response) => response.entity));
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
    updateCategory(form: DotCategoryUpdateForm): Observable<DotCMSAPIResponse<DotCategory>> {
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

    /**
     * Exports categories as a CSV file download.
     * @param contextInode - Optional parent inode to scope the export.
     * @returns Observable that triggers the file download.
     */
    exportCategories(contextInode?: string | null): Observable<void> {
        let params = new HttpParams();
        if (contextInode) {
            params = params.set('contextInode', contextInode);
        }

        return this.#http
            .get('/api/v1/categories/_export', {
                params,
                responseType: 'blob',
                observe: 'response'
            })
            .pipe(
                map((response) => {
                    const blob = response.body;
                    if (!blob) return;
                    const contentDisposition = response.headers.get('Content-Disposition') || '';
                    const match = contentDisposition.match(/filename="?([^";\s]+)"?/);
                    const fileName = match?.[1] || 'categories.csv';
                    getDownloadLink(blob, fileName).click();
                })
            );
    }

    /**
     * Updates the sort order of one or more categories.
     * @param categoryData - Map of category inode to new sort order value.
     * @param params - Current pagination/filter context for the refreshed response.
     * @returns Observable with the refreshed paginated category list.
     */
    updateSortOrder(
        categoryData: Record<string, number>,
        params: DotCategoriesPaginationParams & { parentInode?: string | null }
    ): Observable<DotCMSAPIResponse<DotCategory[]>> {
        return this.#http.put<DotCMSAPIResponse<DotCategory[]>>('/api/v1/categories/_sort', {
            categoryData,
            parentInode: params.parentInode ?? null,
            filter: params.filter,
            page: params.page,
            perPage: params.per_page,
            direction: params.direction,
            orderBy: params.orderby
        });
    }

    /**
     * Imports categories from a CSV file.
     * @param file - The CSV file to import.
     * @param exportType - Whether to replace or merge categories.
     * @param contextInode - Optional parent inode to scope the import.
     * @returns Observable with the import result.
     */
    importCategories(
        file: File,
        exportType: 'replace' | 'merge',
        contextInode?: string | null
    ): Observable<DotCMSAPIResponse<DotCategoryImportResult>> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('exportType', exportType);
        if (contextInode) {
            formData.append('contextInode', contextInode);
        }

        return this.#http.post<DotCMSAPIResponse<DotCategoryImportResult>>(
            '/api/v1/categories/_import',
            formData
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
