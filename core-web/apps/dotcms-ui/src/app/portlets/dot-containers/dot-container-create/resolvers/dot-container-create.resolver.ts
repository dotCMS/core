import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotContainer } from '@models/container/dot-container.model';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';

@Injectable()
export class DotContainerCreateEditResolver implements Resolve<DotContainer> {
    constructor(private service: DotContainersService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotContainer> {
        return this.service.getById(route.paramMap.get('id'));
    }
}
