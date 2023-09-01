import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { tap } from 'rxjs/operators';

import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';

/**
 * Guard for check if all the necessary to track, record and retrieve information
 * from Experiments infrastructure.
 */
export const AnalyticsAppGuard = () => {
    const router = inject(Router);

    return inject(DotExperimentsService)
        .healthCheck()
        .pipe(
            tap((value) => {
                return value === HealthStatusTypes.OK
                    ? true
                    : router.navigate(['/edit-page/experiments/analytic-app-misconfiguration'], {
                          queryParamsHandling: 'merge',
                          state: { healthStatus: value }
                      });
            })
        );
};
