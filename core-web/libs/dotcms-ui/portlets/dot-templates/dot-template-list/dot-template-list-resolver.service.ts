import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { DotLicenseService } from '../../../../../apps/dotcms-ui/src/app/api/services/dot-license/dot-license.service';
import { PushPublishService } from '../../../../../apps/dotcms-ui/src/app/api/services/push-publish/push-publish.service';
import { DotEnvironment } from '../../../../../apps/dotcms-ui/src/app/shared/models/dot-environment/dot-environment';

@Injectable()
export class DotTemplateListResolver implements Resolve<[boolean, boolean]> {
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
