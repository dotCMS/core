import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { EmaAppConfigurationService } from '@dotcms/data-access';

export const editPageGuard: CanActivateFn = (route) => {
    const router = inject(Router);

    return inject(EmaAppConfigurationService)
        .get(route.queryParams.url)
        .pipe(
            map((value) => {
                if (value) {
                    router.navigate(['edit-ema'], {
                        queryParams: getUpdatedQueryParams(route)
                    });

                    return false;
                }

                return true;
            })
        );
};

function getUpdatedQueryParams(route: ActivatedRouteSnapshot) {
    const newQueryParams = {
        ...route.queryParams,
        language_id: route.queryParams.language_id || 1,
        url: route.queryParams.url.substring(1) || 'index',
        'com.dotmarketing.persona.id':
            route.queryParams['com.dotmarketing.persona.id'] || 'modes.persona.no.persona'
    };

    delete newQueryParams['device_inode'];

    return newQueryParams;
}
