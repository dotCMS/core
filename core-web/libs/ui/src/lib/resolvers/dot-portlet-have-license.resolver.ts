import { inject } from '@angular/core';
import { RouterStateSnapshot } from '@angular/router';

import { DotLicenseService } from '@dotcms/data-access';

export const portletHaveLicenseResolver = (_route, state: RouterStateSnapshot) => {
    const licenseService = inject(DotLicenseService);

    return licenseService.canAccessEnterprisePortlet(state.url);
};
