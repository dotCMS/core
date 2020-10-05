import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { map, mergeMap, take } from 'rxjs/operators';
import { DotApps, DotAppsListResolverData } from '@shared/models/dot-apps/dot-apps.model';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsListResolver
 * @implements {Resolve<DotApps[]>}
 */
@Injectable()
export class DotAppsListResolver implements Resolve<DotAppsListResolverData> {
    constructor(
        private dotLicenseService: DotLicenseService,
        private dotAppsService: DotAppsService
    ) {}

    resolve(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<DotAppsListResolverData> {
        return this.dotLicenseService.canAccessEnterprisePortlet(state.url).pipe(
            take(1),
            mergeMap((enterpriseLicense: boolean) => {
                return this.dotAppsService.get().pipe(
                    take(1),
                    map((apps: DotApps[]) => {
                        return {
                            isEnterpriseLicense: enterpriseLicense,
                            apps: apps
                        };
                    })
                );
            })
        );
    }
}
