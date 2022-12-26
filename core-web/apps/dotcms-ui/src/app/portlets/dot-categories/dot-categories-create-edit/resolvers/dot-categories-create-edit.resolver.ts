import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { DotCategoriesUtillService } from '@dotcms/app/api/services/dot-categories/dot-categories-utill.service';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';

@Injectable()
export class DotCategoriesCreateEditResolver implements Resolve<DotCategory> {
    constructor(private service: DotCategoriesUtillService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotCategory> {
        return this.service.getById(route.paramMap.get('id'));
    }
}
