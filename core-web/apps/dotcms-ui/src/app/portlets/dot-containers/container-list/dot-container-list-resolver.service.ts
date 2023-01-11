import { forkJoin, Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { DotLicenseService } from '@dotcms/data-access';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { PushPublishService } from '@services/push-publish/push-publish.service';

@Injectable()
export class DotContainerListResolver implements Resolve<[boolean, boolean]> {
    constructor(
        public dotLicenseService: DotLicenseService,
        public pushPublishService: PushPublishService
    ) {}

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
