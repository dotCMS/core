import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take } from 'rxjs/operators';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsPageResolver
 * @implements {Resolve<Observable<DotApps>>}
 */
@Injectable()
export class DotAppsConfigurationResolver implements Resolve<Observable<DotApps>> {
    constructor(private dotAppsService: DotAppsService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotApps> {
        const appsKey = route.paramMap.get('appKey');
        return this.dotAppsService.getConfigurationList(appsKey).pipe(take(1));
    }
}
