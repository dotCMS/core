import { inject } from '@angular/core';
import { ActivatedRoute, CanMatchFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

/**
 * Guard that protects analytics routes by checking the analytics service health status.
 * If the service is not properly configured, redirects to an error page.
 */
export const analyticsHealthGuard: CanMatchFn = (_route, _segments) => {
    const dotExperimentsService = inject(DotExperimentsService);
    const router = inject(Router);
    const activatedRoute = inject(ActivatedRoute);

    // TODO: Move health check to Analytics Service
    return dotExperimentsService.healthCheck().pipe(
        map((healthStatus) => {
            if (healthStatus === HealthStatusTypes.OK) {
                return true; // Allow access to the route
            }

            // Get isEnterprise from route data (resolved at parent level)
            const isEnterprise = activatedRoute.snapshot.data?.['isEnterprise'] ?? true;

            // Redirect to error page with status information
            router.navigate(['/analytics/error'], {
                queryParams: {
                    status: healthStatus,
                    isEnterprise: isEnterprise
                }
            });

            return false; // Block access to the route
        })
    );
};
