import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take, tap } from 'rxjs/operators';

import { DotApp } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsPageResolver
 * @implements {Resolve<Observable<DotApp>>}
 */
@Injectable()
export class DotAppsConfigurationResolver implements Resolve<Observable<DotApp>> {
    private dotAppsService = inject(DotAppsService);
    readonly #globalStore = inject(GlobalStore);

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp> {
        const appsKey = route.paramMap.get('appKey');

        return this.dotAppsService.getConfigurationList(appsKey).pipe(
            take(1),
            tap((apps) => {
                this.#globalStore.addNewBreadcrumb({
                    label: apps.name,
                    target: '_self',
                    url: `/dotAdmin/#/apps/${apps.key}`
                });
            })
        );
    }
}
