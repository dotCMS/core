import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { DotTemplate } from '@portlets/dot-edit-page/shared/models';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { Observable } from 'rxjs';

@Injectable()
export class DotTemplateDesignerResolver implements Resolve<DotTemplate> {
    constructor(private service: DotTemplatesService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        return this.service.getByInode(route.paramMap.get('inode'));
    }
}
