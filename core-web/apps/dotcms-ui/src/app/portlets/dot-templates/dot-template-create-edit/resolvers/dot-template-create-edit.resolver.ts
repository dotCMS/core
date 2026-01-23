import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotTemplate } from '@dotcms/dotcms-models';

import { DotTemplatesService } from '../../../../api/services/dot-templates/dot-templates.service';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate> {
    private service = inject(DotTemplatesService);
    private dotRouterService = inject(DotRouterService);

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        const inode = route.paramMap.get('inode');

        return inode
            ? this.service.getFiltered({ filter: inode }).pipe(
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
