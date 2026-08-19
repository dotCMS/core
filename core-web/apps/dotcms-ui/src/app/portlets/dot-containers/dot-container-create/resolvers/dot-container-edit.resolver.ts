import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { tap } from 'rxjs/operators';

import { DotContainerEntity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';

@Injectable()
export class DotContainerEditResolver implements Resolve<DotContainerEntity | null> {
    private service = inject(DotContainersService);
    private globalStore = inject(GlobalStore);

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotContainerEntity | null> {
        return this.service.getById(route.paramMap.get('id') ?? '', 'working', true).pipe(
            tap((container) => {
                if (!container) {
                    return;
                }

                const { identifier, title } = container.container;
                this.globalStore.addNewBreadcrumb({
                    label: title,
                    target: '_self',
                    url: `/dotAdmin/#/containers/edit/${identifier}`
                });
            })
        );
    }
}
