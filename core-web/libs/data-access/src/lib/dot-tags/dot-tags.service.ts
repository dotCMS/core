import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotTag, DotCMSResponseJsonObject } from '@dotcms/dotcms-models';

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
        const httpOptions = name ? { params: new HttpParams().set('name', name) } : {};

        return this.http
            .get<DotCMSResponseJsonObject<{ [key: string]: DotTag }>>('/api/v1/tags', httpOptions)
            .pipe(
                map((response) => response.bodyJsonObject),
                map((tags: { [key: string]: DotTag }) => {
                    return Object.entries(tags).map(([_key, value]) => value);
                })
            );
    }
}
