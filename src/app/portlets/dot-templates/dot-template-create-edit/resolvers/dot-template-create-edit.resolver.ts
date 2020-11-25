import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotTemplate } from '@models/dot-edit-layout-designer';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate> {
    constructor(private service: DotTemplatesService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        return this.service.getById(route.paramMap.get('id'));
    }
}
