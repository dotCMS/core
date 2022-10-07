import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotContainerEntity } from '@models/container/dot-container.model';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';

@Injectable()
export class DotContainerCreateEditResolver implements Resolve<DotContainerEntity> {
    constructor(private service: DotContainersService) {}

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotContainerEntity> {
        return this.service.getById(route.paramMap.get('id'));
    }
}
