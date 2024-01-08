import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { DotLicenseService } from '@dotcms/data-access';

export const rulesResolver: ResolveFn<boolean> = (
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
): Observable<boolean> => {
    const licenseService = inject(DotLicenseService);
    return licenseService.canAccessEnterprisePortlet(state.url);
};
