import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { PushPublishService } from '@services/push-publish/push-publish.service';

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
