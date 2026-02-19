import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, DotTag } from '@dotcms/dotcms-models';

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
}
