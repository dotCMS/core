import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotApps } from '@dotcms/dotcms-models';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

/**
 * Returns app configuration detail from the api
 *
 * @export
 * @class DotAppsConfigurationDetailResolver
 * @implements {Resolve<DotAppsResolverData>}
 */
@Injectable()
export class DotAppsConfigurationDetailResolver implements Resolve<DotApps> {
    constructor(private dotAppsService: DotAppsService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotApps> {
        const appKey = route.paramMap.get('appKey');
        const id = route.paramMap.get('id');

        return this.dotAppsService.getConfiguration(appKey, id).pipe(take(1));
    }
}
