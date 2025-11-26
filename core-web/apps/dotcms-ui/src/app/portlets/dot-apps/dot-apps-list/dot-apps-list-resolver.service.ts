import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { map, mergeMap, take } from 'rxjs/operators';

import { DotLicenseService } from '@dotcms/data-access';
import { DotApp, DotAppsListResolverData } from '@dotcms/dotcms-models';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsListResolver
 * @implements {Resolve<DotApp[]>}
 */
@Injectable()
export class DotAppsListResolver implements Resolve<DotAppsListResolverData> {
    private dotLicenseService = inject(DotLicenseService);
    private dotAppsService = inject(DotAppsService);

    resolve(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<DotAppsListResolverData> {
        return this.dotLicenseService.canAccessEnterprisePortlet(state.url).pipe(
            take(1),
            mergeMap((enterpriseLicense: boolean) => {
                if (enterpriseLicense) {
                    return this.dotAppsService.get().pipe(
                        take(1),
                        map((apps: DotApp[]) => {
                            return {
                                isEnterpriseLicense: enterpriseLicense,
                                apps: apps
                            };
                        })
                    );
                }

                return of({
                    isEnterpriseLicense: false,
                    apps: []
                });
            })
        );
    }
}
