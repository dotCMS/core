import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { DotLicenseService } from '@dotcms/data-access';

@Injectable()
export class DotEnterpriseLicenseResolver implements Resolve<Observable<boolean>> {
    constructor(private readonly dotLicenseService: DotLicenseService) {}

    resolve(route: ActivatedRouteSnapshot) {
        return this.dotLicenseService.isEnterprise();
    }
}
