import { inject } from '@angular/core';
import { RouterStateSnapshot } from '@angular/router';

import { DotLicenseService } from '@dotcms/data-access';

export const editEmaResolver = (_route, state: RouterStateSnapshot) => {
    const licenseService = inject(DotLicenseService);

    return licenseService.canAccessEnterprisePortlet(state.url);
};
