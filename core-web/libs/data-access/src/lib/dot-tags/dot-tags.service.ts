import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotTag } from '@dotcms/dotcms-models';

// Response type for endpoints that return bodyJsonObject with tags
interface DotTagsResponse {
    bodyJsonObject: { [key: string]: DotTag };
}

/**
 * Provide util methods to get Tags available in the system.
 * @export
 * @class DotTagsService
 */
@Injectable({
    providedIn: 'root'
})
export class DotTagsService {
    private http = inject(HttpClient);

    /**
     * Get tags suggestions
     * @returns Observable<DotTag[]>
     * @memberof DotTagDotTagsServicesService
     */
    getSuggestions(name?: string): Observable<DotTag[]> {
        return this.http.get<DotTagsResponse>(`/api/v1/tags${name ? `?name=${name}` : ''}`).pipe(
            map((response) => response.bodyJsonObject),
            map((tags: { [key: string]: DotTag }) => {
                return Object.entries(tags).map(([_key, value]) => value);
            })
        );
    }
}
