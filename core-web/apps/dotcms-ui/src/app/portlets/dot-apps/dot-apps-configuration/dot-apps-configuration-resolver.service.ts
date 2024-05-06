import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotApp } from '@dotcms/dotcms-models';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsPageResolver
 * @implements {Resolve<Observable<DotApp>>}
 */
@Injectable()
export class DotAppsConfigurationResolver implements Resolve<Observable<DotApp>> {
    constructor(private dotAppsService: DotAppsService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp> {
        const appsKey = route.paramMap.get('appKey');

        return this.dotAppsService.getConfigurationList(appsKey).pipe(take(1));
    }
}
