import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotDevice } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get the Devices & dimensions.
 * @export
 * @class DotDevicesService
 */
@Injectable()
export class DotDevicesService {
    private coreWebService = inject(CoreWebService);

    /**
     * Return available devices.
     * @returns Observable<DotDevice[]>
     * @memberof DotDevicesService
     */
    get(): Observable<DotDevice[]> {
        return this.coreWebService
            .requestView({
                url: [
                    'api',
                    'content',
                    'respectFrontendRoles/false',
                    'render/false',
                    'query/+contentType:previewDevice +live:true +deleted:false +working:true',
                    'limit/40/orderby/title'
                ].join('/')
            })
            .pipe(map((x) => x?.contentlets));
    }
}
