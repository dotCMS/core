import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';

export const dotAnalyticsHealthCheckResolver = (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
) => {
    const dotExperimentsService = inject(DotExperimentsService);

    return dotExperimentsService.healthCheck();
};
