import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { DotTemplate } from '@dotcms/dotcms-models';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate> {
    constructor(private service: DotTemplatesService, private dotRouterService: DotRouterService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        const inode = route.paramMap.get('inode');

        return inode
            ? this.service.getFiltered(inode).pipe(
                  map((templates: DotTemplate[]) => {
                      if (templates.length) {
                          return templates[0];
                      } else {
                          this.dotRouterService.gotoPortlet('templates');
                      }
                  })
              )
            : this.service.getById(route.paramMap.get('id'));
    }
}
