import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Resolve } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { PushPublishService } from '@dotcms/data-access';
import { DotEnvironment } from '@dotcms/dotcms-models';

@Injectable()
export class DotTemplateListResolver implements Resolve<boolean> {
    pushPublishService = inject(PushPublishService);

    resolve(): Observable<boolean> {
        return this.pushPublishService.getEnvironments().pipe(
            map((environments: DotEnvironment[]) => !!environments.length),
            take(1)
        );
    }
}
