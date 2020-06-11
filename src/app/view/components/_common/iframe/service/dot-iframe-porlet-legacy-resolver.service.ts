import { Resolve, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

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
