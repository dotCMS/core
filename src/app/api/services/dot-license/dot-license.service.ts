import { Injectable } from '@angular/core';
import { DotcmsConfig } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';

@Injectable()
export class DotLicenseService {
    constructor(private dotcmsConfig: DotcmsConfig) {}

    /**
     * Gets if current user has an enterprise license
     *
     * @returns {Observable<boolean>}
     * @memberof DotLicenseService
     */
    isEnterpriseLicense(): Observable<boolean> {
        return this.dotcmsConfig
            .getConfig()
            .take(1)
            .pluck('license')
            .map((license) => license['level'] >= 200);
    }
}
