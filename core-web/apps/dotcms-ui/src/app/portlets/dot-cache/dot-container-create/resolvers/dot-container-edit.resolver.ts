import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotContainerEntity } from '@dotcms/dotcms-models';
import { DotCacheService } from '@services/dot-containers/dot-cache.service';

@Injectable()
export class DotContainerEditResolver implements Resolve<DotContainerEntity> {
    constructor(private service: DotCacheService) {}

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotContainerEntity> {
        return this.service.getById(route.paramMap.get('id'), 'working', true);
    }
}
