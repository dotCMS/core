import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentType, DotCMSResponse } from '@dotcms/dotcms-models';

@Service({ autoProvided: false })
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
            .get<DotCMSResponse<DotCMSContentType[]>>(`/api/v1/page/types?filter=${keyword}`)
            .pipe(map(({ entity }) => entity));
    }
}
