import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { take } from 'rxjs/operators';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsListResolver
 * @implements {Resolve<DotApps[]>}
 */
@Injectable()
export class DotAppsListResolver implements Resolve<boolean> {
    constructor(private dotLicenseService: DotLicenseService) {}

    resolve(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.dotLicenseService.canAccessEnterprisePortlet(state.url).pipe(take(1));
    }
}
