import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { EmaAppConfigurationService } from '@dotcms/data-access';

export const emaAppFnGuard: CanActivateFn = (route, _state) => {
    const service = inject(EmaAppConfigurationService);

    service.get(route.queryParams.url).subscribe((res) => {
        // eslint-disable-next-line no-console
        console.log('guard', res);
    });

    return true;
};
