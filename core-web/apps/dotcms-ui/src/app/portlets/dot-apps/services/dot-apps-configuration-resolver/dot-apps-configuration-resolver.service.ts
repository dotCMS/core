import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take, tap } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsPageResolver
 * @implements {Resolve<Observable<DotApp>>}
 */
@Injectable()
export class DotAppsConfigurationResolver implements Resolve<DotApp | null> {
    private dotAppsService = inject(DotAppsService);
    readonly #globalStore = inject(GlobalStore);

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp | null> {
        const appsKey = route.paramMap.get('appKey');

        return this.dotAppsService.getConfigurationList(appsKey ?? '').pipe(
            take(1),
            tap((apps) => {
                if (!apps) {
                    return;
                }

                this.#globalStore.addNewBreadcrumb({
                    label: apps.name,
                    target: '_self',
                    url: `/dotAdmin/#/apps/${apps.key}`
                });
            })
        );
    }
}
