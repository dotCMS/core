import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take } from 'rxjs/operators';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotApps } from '@models/dot-apps/dot-apps.model';

/**
 * Returns app configuration detail from the api
 *
 * @export
 * @class DotAppsConfigurationDetailResolver
 * @implements {Resolve<DotAppsResolverData>}
 */
@Injectable()
export class DotAppsConfigurationDetailResolver implements Resolve<DotApps> {
    constructor(
        private dotAppsService: DotAppsService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotApps> {
        const appKey = route.paramMap.get('appKey');
        const id = route.paramMap.get('id');
        return this.dotAppsService.getConfiguration(appKey, id).pipe(take(1));
    }
}
