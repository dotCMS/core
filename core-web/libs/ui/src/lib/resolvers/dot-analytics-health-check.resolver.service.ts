import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { DotExperimentsService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';

@Injectable()
export class DotAnalyticsHealthCheckResolver implements Resolve<Observable<HealthStatusTypes>> {
    dotExperimentsService = inject(DotExperimentsService);

    resolve() {
        return inject(DotExperimentsService).healthCheck();
    }
}
