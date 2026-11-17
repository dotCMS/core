import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class DotActionUrlService {
    private http = inject(HttpClient);

    /**
     * Get the url to create a contentlet
     *
     * @param {string} contentTypeVariable
     * @return {*}  {Observable<string>}
     * @memberof DotActionUrlService
     */
    getCreateContentletUrl(
        contentTypeVariable: string,
        language_id: string | number = 1
    ): Observable<string> {
        return this.http
            .get<{
                entity: string;
            }>(
                // Encoded: the variable can come from a URL (Content Drive's `createContent`).
                `/api/v1/portlet/_actionurl/${encodeURIComponent(contentTypeVariable)}?language_id=${language_id}`
            )
            .pipe(map((x) => x?.entity));
    }
}
