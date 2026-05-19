import { inject } from '@angular/core';
import { ActivatedRoute, CanMatchFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotAnalyticsService } from '@dotcms/portlets/dot-analytics/data-access';

/**
 * Guard that protects analytics routes by checking service availability.
 */
export const analyticsHealthGuard: CanMatchFn = (_route, _segments) => {
    const analyticsService = inject(DotAnalyticsService);
    const router = inject(Router);
    const activatedRoute = inject(ActivatedRoute);

    return analyticsService.healthCheck().pipe(
        map((healthStatus) => {
            if (healthStatus === HealthStatusTypes.AVAILABLE) {
                return true;
            }

            const isEnterprise = activatedRoute.snapshot.data?.['isEnterprise'] ?? true;

            router.navigate(['/analytics/error'], {
                queryParams: {
                    status: healthStatus,
                    isEnterprise: isEnterprise
                }
            });

            return false;
        })
    );
};
