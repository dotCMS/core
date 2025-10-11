import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { catchError, map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotTemplate } from '@dotcms/dotcms-models';

import { DotTemplatesService } from '../../../../api/services/dot-templates/dot-templates.service';

@Injectable()
export class DotTemplateCreateEditResolver implements Resolve<DotTemplate> {
    private service = inject(DotTemplatesService);
    private dotRouterService = inject(DotRouterService);

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<DotTemplate> {
        const inode = route.paramMap.get('inode');
        const id = route.paramMap.get('id');

        if (inode) {
            return this.service.getFiltered(inode).pipe(
                map((templates: DotTemplate[]) => {
                    if (templates.length) {
                        const firstTemplate = templates.find((t) => t.inode === inode);
                        if (firstTemplate) {
                            return firstTemplate;
                        }
                    }

                    console.error(
                        `DotTemplateCreateEditResolver: Template with inode ${inode} not found`
                    );
                    this.dotRouterService.gotoPortlet('templates');
                    return null;
                }),
                catchError((error) => {
                    console.error(
                        `DotTemplateCreateEditResolver: Failed to get template by inode ${inode}`,
                        error
                    );
                    this.dotRouterService.gotoPortlet('templates');
                    return of(null);
                })
            );
        } else if (id) {
            return this.service.getById(id).pipe(
                catchError((error) => {
                    console.error(
                        `DotTemplateCreateEditResolver: Failed to get template by id ${id}`,
                        error
                    );
                    this.dotRouterService.gotoPortlet('templates');
                    return of(null);
                })
            );
        } else {
            console.error(
                'DotTemplateCreateEditResolver: No inode or id provided in route parameters'
            );
            this.dotRouterService.gotoPortlet('templates');
            return of(null);
        }
    }
}
