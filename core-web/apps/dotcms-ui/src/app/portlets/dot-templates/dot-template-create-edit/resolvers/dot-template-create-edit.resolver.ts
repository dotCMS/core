import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotTemplate } from '@dotcms/dotcms-models';

import { DotTemplatesService } from '../../../../api/services/dot-templates/dot-templates.service';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate | null> {
    private service = inject(DotTemplatesService);
    private dotRouterService = inject(DotRouterService);

    resolve(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<DotTemplate | null> {
        const inode = route.paramMap.get('inode');

        if (!inode) {
            return this.service.getById(route.paramMap.get('id') ?? '');
        }

        return this.service.getFiltered({ filter: inode }).pipe(
            map((response: { templates: DotTemplate[]; totalRecords: number }) => {
                const firstTemplate = response.templates.find((t) => t.inode === inode);

                if (firstTemplate) {
                    return firstTemplate;
                }

                this.dotRouterService.gotoPortlet('templates');

                return null;
            })
        );
    }
}
