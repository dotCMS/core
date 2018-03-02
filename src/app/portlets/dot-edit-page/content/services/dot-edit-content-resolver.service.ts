import { ResponseView } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { EditPageService } from '../../../../api/services/edit-page/edit-page.service';
import { DotRenderedPage } from '../../shared/models/dot-rendered-page.model';
import { DotHttpErrorManagerService } from '../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';

/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<DotRenderedPage>}
 */
@Injectable()
export class EditContentResolver implements Resolve<DotRenderedPage> {
    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private editPageService: EditPageService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotRenderedPage> {
        return this.editPageService.getEdit(route.queryParams.url).catch((err: ResponseView) => {
            this.dotHttpErrorManagerService.handle(err).subscribe((res: any) => {
                if (!res.redirected) {
                    this.dotRouterService.gotoPortlet('/c/site-browser');
                }
            });
            return Observable.of(null);
        });
    }
}
