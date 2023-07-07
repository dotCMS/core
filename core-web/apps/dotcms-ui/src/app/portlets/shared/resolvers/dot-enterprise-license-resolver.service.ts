import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { DotLicenseService } from '@dotcms/data-access';

@Injectable()
export class DotEnterpriseLicenseResolver implements Resolve<Observable<boolean>> {
    constructor(private readonly dotLicenseService: DotLicenseService) {}

    resolve() {
        return this.dotLicenseService.isEnterprise();
    }
}
