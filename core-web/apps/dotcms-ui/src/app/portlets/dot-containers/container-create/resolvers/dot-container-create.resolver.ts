import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotTemplate } from '@models/dot-edit-layout-designer';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

@Injectable()
export class DotContainerCreateEditResolver implements Resolve<DotTemplate> {
    constructor(private service: DotTemplatesService, private dotRouterService: DotRouterService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        return this.service.getById(route.paramMap.get('id'));
    }
}
