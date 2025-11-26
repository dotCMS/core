import { forkJoin, Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Resolve } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { DotLicenseService, PushPublishService } from '@dotcms/data-access';
import { DotEnvironment } from '@dotcms/dotcms-models';

@Injectable()
export class DotTemplateListResolver implements Resolve<[boolean, boolean]> {
    dotLicenseService = inject(DotLicenseService);
    pushPublishService = inject(PushPublishService);

    resolve(): Observable<[boolean, boolean]> {
        return forkJoin([
            this.dotLicenseService.isEnterprise(),
            this.pushPublishService.getEnvironments().pipe(
                map((environments: DotEnvironment[]) => !!environments.length),
                take(1)
            )
        ]);
    }
}
