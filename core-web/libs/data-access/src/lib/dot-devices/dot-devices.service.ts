import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotDevice } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get the Devices & dimensions.
 * @export
 * @class DotDevicesService
 */
@Injectable()
export class DotDevicesService {
    constructor(private coreWebService: CoreWebService) {}

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
            .pipe(pluck('contentlets'));
    }
}
