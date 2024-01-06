import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { EmaAppConfigurationService } from '@dotcms/data-access';

export const editEmaGuard: CanActivateFn = (route, _state) => {
    const router = inject(Router);

    return inject(EmaAppConfigurationService)
        .get(route.queryParams.url)
        .pipe(
            map((value) => {
                if (value) {
                    return true;
                }

                return router.createUrlTree(['/pages']);
            })
        );
};
