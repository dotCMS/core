import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, DotTag, DotTagsPaginatedResponse } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Tags available in the system.
 * @export
 * @class DotTagsService
 */
@Injectable({
    providedIn: 'root'
})
export class DotTagsService {
    readonly #http = inject(HttpClient);

    /**
     * Get tags suggestions
     * @returns Observable<DotTag[]>
     * @memberof DotTagDotTagsServicesService
     */
    getSuggestions(name?: string): Observable<DotTag[]> {
        const params = name ? new HttpParams().set('name', name) : new HttpParams();
        return this.#http
            .get<Record<string, DotTag>>(`/api/v1/tags`, { params })
            .pipe(map((tags) => Object.values(tags)));
    }

    /**
     * Retrieves tags based on the provided name.
     * @param name - The name of the tags to retrieve.
     * @returns An Observable that emits an array of tag labels.
     */
    getTags(name: string): Observable<DotTag[]> {
        const params = new HttpParams().set('name', name);

        return this.#http
            .get<DotCMSAPIResponse<DotTag[]>>('/api/v2/tags', { params })
            .pipe(map((response) => response.entity));
    }

    /**
     * Retrieves tags with pagination, filtering, and sorting.
     * @param params - Query parameters for the paginated request.
     * @returns Observable with entity array and pagination metadata.
     */
    getTagsPaginated(params: {
        filter?: string;
        page?: number;
        per_page?: number;
        orderBy?: string;
        direction?: string;
    }): Observable<DotTagsPaginatedResponse> {
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

        if (params.orderBy) {
            httpParams = httpParams.set('orderBy', params.orderBy);
        }

        if (params.direction) {
            httpParams = httpParams.set('direction', params.direction);
        }

        return this.#http.get<DotTagsPaginatedResponse>('/api/v2/tags', {
            params: httpParams
        });
    }

    /**
     * Creates one or more tags.
     * @param tags - Array of tag data to create.
     * @returns Observable with the created tags.
     */
    createTag(tags: { name: string; siteId?: string }[]): Observable<DotCMSAPIResponse<DotTag[]>> {
        return this.#http.post<DotCMSAPIResponse<DotTag[]>>('/api/v2/tags', tags);
    }

    /**
     * Updates an existing tag.
     * @param tagId - The ID of the tag to update.
     * @param data - The updated tag data (tagName and siteId).
     * @returns Observable with the updated tag.
     */
    updateTag(
        tagId: string,
        data: { tagName: string; siteId: string }
    ): Observable<DotCMSAPIResponse<DotTag>> {
        return this.#http.put<DotCMSAPIResponse<DotTag>>(`/api/v2/tags/${tagId}`, data);
    }

    /**
     * Deletes tags by their IDs.
     * @param tagIds - Array of tag IDs to delete.
     * @returns Observable with bulk result containing success/failure counts.
     */
    deleteTags(
        tagIds: string[]
    ): Observable<DotCMSAPIResponse<{ successCount: number; fails: unknown[] }>> {
        return this.#http.request<DotCMSAPIResponse<{ successCount: number; fails: unknown[] }>>(
            'DELETE',
            '/api/v2/tags',
            { body: tagIds }
        );
    }
}
