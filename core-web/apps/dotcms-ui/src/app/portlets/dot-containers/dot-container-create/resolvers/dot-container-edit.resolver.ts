import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { DotContainerEntity } from '@dotcms/dotcms-models';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';

@Injectable()
export class DotContainerEditResolver implements Resolve<DotContainerEntity> {
    constructor(private service: DotContainersService) {}

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotContainerEntity> {
        return this.service.getById(route.paramMap.get('id'), 'working', true);
    }
}
