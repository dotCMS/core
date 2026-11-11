import { inject } from '@angular/core';
import { RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

/**
 * Resolves the Analytics health status for route data.
 *
 * Extracts only the `health` field from the full response so that downstream consumers
 * (`DotExperimentsResultsComponent`) receive a plain {@link HealthStatusTypes} value,
 * preserving the contract that existed before the health response was extended with tier fields.
 */
export const dotAnalyticsHealthCheckResolver = (_route, _state: RouterStateSnapshot) => {
    return inject(DotExperimentsService)
        .healthCheck()
        .pipe(map((r): HealthStatusTypes => r.health));
};
