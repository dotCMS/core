import { pluck, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { RequestMethod } from '@angular/http';
import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs';

/**
 * Handle license information of current logged in user
 * @export
 * @class DotLicenseService
 */
@Injectable()
export class DotLicenseService {
    private licenseURL: string;

    constructor(private coreWebService: CoreWebService) {
        this.licenseURL = 'v1/appconfiguration';
    }

    /**
     * Gets if current user has an enterprise license
     *
     * @returns {Observable<boolean>}
     * @memberof DotLicenseService
     */
    isEnterprise(): Observable<boolean> {
        return this.getLicense().pipe(map((license) => license['level'] >= 200));
    }

    private getLicense(): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.licenseURL
            })
            .pipe(pluck('entity', 'config', 'license'));
    }
}
