import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotApp } from '@dotcms/dotcms-models';

/**
 * Returns app configuration detail from the api
 *
 * @export
 * @class DotAppsConfigurationDetailResolver
 * @implements {Resolve<DotAppsResolverData>}
 */
@Injectable()
export class DotAppsConfigurationDetailResolver implements Resolve<DotApp> {
    constructor(private dotAppsService: DotAppsService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp> {
        const appKey = route.paramMap.get('appKey');
        const id = route.paramMap.get('id');

        return this.dotAppsService.getConfiguration(appKey, id).pipe(take(1));
    }
}
