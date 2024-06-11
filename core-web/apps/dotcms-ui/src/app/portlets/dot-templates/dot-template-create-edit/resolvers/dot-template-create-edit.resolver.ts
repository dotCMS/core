import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { DotRouterService } from '@dotcms/data-access';
import { DotTemplate } from '@dotcms/dotcms-models';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate> {
    constructor(
        private service: DotTemplatesService,
        private dotRouterService: DotRouterService
    ) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        const inode = route.paramMap.get('inode');

        return inode
            ? this.service.getFiltered(inode).pipe(
                  map((templates: DotTemplate[]) => {
                      if (templates.length) {
                          const firstTemplate = templates.find((t) => t.inode === inode);
                          if (firstTemplate) {
                              return firstTemplate;
                          }
                      }

                      this.dotRouterService.gotoPortlet('templates');
                  })
              )
            : this.service.getById(route.paramMap.get('id'));
    }
}
