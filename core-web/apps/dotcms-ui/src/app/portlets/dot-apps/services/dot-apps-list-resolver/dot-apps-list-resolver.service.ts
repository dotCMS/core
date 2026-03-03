import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsListResolver
 * @implements {Resolve<DotApp[]>}
 */
@Injectable()
export class DotAppsListResolver implements Resolve<DotApp[]> {
    private dotAppsService = inject(DotAppsService);

    resolve(_route: ActivatedRouteSnapshot): Observable<DotApp[]> {
        return this.dotAppsService.get().pipe(take(1));
    }
}
