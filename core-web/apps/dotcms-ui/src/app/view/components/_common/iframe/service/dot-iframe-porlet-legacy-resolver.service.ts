import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { DotLicenseService } from '@dotcms/data-access';
import { Observable } from 'rxjs';

/**
 *
 * @export
 * @class DotIframePortletLegacyResolver
 * @implements {Resolve<boolean>}
 */
@Injectable()
export class DotIframePortletLegacyResolver implements Resolve<boolean> {
    constructor(private dotLicenseService: DotLicenseService) {}

    resolve(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.dotLicenseService.canAccessEnterprisePortlet(state.url);
    }
}
