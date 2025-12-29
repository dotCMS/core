import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentType, DotCMSResponse } from '@dotcms/dotcms-models';

@Injectable()
export class DotPageTypesService {
    private http = inject(HttpClient);

    /**
     * Returns Content Type data of type page and urlMap
     *
     * @param string keyword
     * @returns {Observable<DotCMSContentType[]>}
     * @memberof DotPageTypesService
     */
    getPages(keyword = ''): Observable<DotCMSContentType[]> {
        return this.http
            .get<DotCMSResponse<DotCMSContentType[]>>(`/api/v1/page/types?filter=${keyword}`)
            .pipe(map((response) => response.entity));
    }
}
