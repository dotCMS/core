import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, pluck } from 'rxjs/operators';

@Injectable()
export class DotActionUrlService {
    private http = inject(HttpClient);

    /**
     * Get the url to create a contentlet
     *
     * @param {string} contentTypeVariable
     * @return {*}  {Observable<string>}
     * @memberof DotActionUrlService
     */
    getCreateContentletUrl(contentTypeVariable: string): Observable<string> {
        return this.http
            .get<{ entity: string }>(`/api/v1/portlet/_actionurl/${contentTypeVariable}`)
            .pipe(
                pluck('entity'),
                catchError(() => {
                    return EMPTY;
                })
            );
    }
}
