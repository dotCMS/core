import { Observable } from 'rxjs';

import { inject, Service } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

/**
 * Returns app configuration detail from the api
 *
 * @export
 * @class DotAppsConfigurationDetailResolver
 * @implements {Resolve<DotAppsResolverData>}
 */
@Service({ autoProvided: false })
export class DotAppsConfigurationDetailResolver implements Resolve<DotApp> {
    private dotAppsService = inject(DotAppsService);

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp> {
        const appKey = route.paramMap.get('appKey');
        const id = route.paramMap.get('id');

        return this.dotAppsService.getConfiguration(appKey, id).pipe(take(1));
    }
}
