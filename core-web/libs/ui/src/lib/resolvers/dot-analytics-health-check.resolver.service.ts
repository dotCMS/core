import { inject } from '@angular/core';
import { RouterStateSnapshot } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';

export const dotAnalyticsHealthCheckResolver = (_route, _state: RouterStateSnapshot) => {
    const dotExperimentsService = inject(DotExperimentsService);

    return dotExperimentsService.healthCheck();
};
