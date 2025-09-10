import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Injectable()
export class DotPageTypesService {
    private coreWebService = inject(CoreWebService);

    /**
     * Returns Content Type data of type page and urlMap
     *
     * @param string keyword
     * @returns {Observable<DotCMSContentType[]>}
     * @memberof DotPageTypesService
     */
    getPages(keyword = ''): Observable<DotCMSContentType[]> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/page/types?filter=${keyword}`
            })
            .pipe(take(1), pluck('entity'));
    }
}
