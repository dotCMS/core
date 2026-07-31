import { inject } from '@angular/core';
import { ActivatedRoute, CanActivateFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotAnalyticsService } from '@dotcms/portlets/dot-analytics/data-access';

/**
 * Guard that protects analytics routes by checking service availability.
 *
 * Uses `canActivate` (not `canMatch`): `canMatch` is re-evaluated by the Router during route
 * *recognition* on every navigation that includes the guarded path segment — including switching
 * between the dashboard's tab children, which are still the same already-active parent route.
 * That fired a fresh `/api/v1/analytics/health` request (and blocked navigation on its response)
 * on every single tab click. `canActivate` only runs when the guarded route is newly activated —
 * once when entering `/dashboard` (or `/search`) from outside it, not on every child-only
 * navigation within it.
 */
export const analyticsHealthGuard: CanActivateFn = (_route, _state) => {
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
