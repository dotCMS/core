import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { CoreWebService } from 'dotcms-js';

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
                    'query/+contentType:previewDevice +live:true +deleted:false +working:true'
                ].join('/')
            })
            .pipe(pluck('contentlets'));
    }
}
