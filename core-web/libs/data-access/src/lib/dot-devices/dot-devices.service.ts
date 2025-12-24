import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotDevice } from '@dotcms/dotcms-models';

// Response type for content search endpoints that return contentlets
interface DotContentSearchResponse<T> {
    contentlets: T;
}

/**
 * Provide util methods to get the Devices & dimensions.
 * @export
 * @class DotDevicesService
 */
@Injectable()
export class DotDevicesService {
    private http = inject(HttpClient);

    /**
     * Return available devices.
     * @returns Observable<DotDevice[]>
     * @memberof DotDevicesService
     */
    get(): Observable<DotDevice[]> {
        const url = [
            '/api',
            'content',
            'respectFrontendRoles/false',
            'render/false',
            'query/+contentType:previewDevice +live:true +deleted:false +working:true',
            'limit/40/orderby/title'
        ].join('/');

        return this.http
            .get<DotContentSearchResponse<DotDevice[]>>(url)
            .pipe(map((response) => response.contentlets));
    }
}
