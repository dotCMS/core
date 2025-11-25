import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { map } from 'rxjs/operators';

import { PushPublishService } from '@dotcms/data-access';
import { DotEnvironment } from '@dotcms/dotcms-models';

/**
 * Resolver to get push publish environments
 *
 * @export
 * @class DotPushPublishEnvironmentsResolver
 * @implements {Resolve<Observable<DotEnvironment[]>>}
 */
@Injectable()
export class DotPushPublishEnvironmentsResolver implements Resolve<Observable<DotEnvironment[]>> {
    constructor(private readonly pushPublishService: PushPublishService) {}

    resolve() {
        //When the is no environments, the endpoint returns [{id: "0", name: ""}]
        return this.pushPublishService
            .getEnvironments()
            .pipe(
                map((environments: DotEnvironment[]) =>
                    environments.filter((environment: DotEnvironment) => environment.id !== '0')
                )
            );
    }
}
