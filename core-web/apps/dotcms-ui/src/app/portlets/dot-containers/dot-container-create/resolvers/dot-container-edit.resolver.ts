import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotContainerEntity } from '@dotcms/dotcms-models';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';

@Injectable()
export class DotContainerEditResolver implements Resolve<DotContainerEntity> {
    private service = inject(DotContainersService);

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotContainerEntity> {
        return this.service.getById(route.paramMap.get('id'), 'working', true);
    }
}
