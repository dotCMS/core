import { Observable } from 'rxjs';

import { inject, Service } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotLicenseService } from '@dotcms/data-access';

/**
 *
 * @export
 * @class DotIframePortletLegacyResolver
 * @implements {Resolve<boolean>}
 */
@Service({ autoProvided: false })
export class DotIframePortletLegacyResolver implements Resolve<boolean> {
    private dotLicenseService = inject(DotLicenseService);

    resolve(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.dotLicenseService.canAccessEnterprisePortlet(state.url);
    }
}
