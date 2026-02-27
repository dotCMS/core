import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, DotCMSContentType } from '@dotcms/dotcms-models';

@Injectable()
export class DotPageTypesService {
    readonly #http = inject(HttpClient);

    /**
     * Returns Content Type data of type page and urlMap
     *
     * @param string keyword
     * @returns {Observable<DotCMSContentType[]>}
     * @memberof DotPageTypesService
     */
    getPageContentTypes(keyword = ''): Observable<DotCMSContentType[]> {
        return this.#http
            .get<DotCMSAPIResponse<DotCMSContentType[]>>(`/api/v1/page/types?filter=${keyword}`)
            .pipe(map(({ entity }) => entity));
    }
}
