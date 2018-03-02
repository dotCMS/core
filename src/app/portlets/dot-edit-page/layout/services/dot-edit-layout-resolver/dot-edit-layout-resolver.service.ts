import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import { ResponseView } from 'dotcms-js/dotcms-js';

import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPageView } from '../../../shared/models/dot-page-view.model';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { PageViewService } from '../../../../../api/services/page-view/page-view.service';

@Injectable()
export class EditLayoutResolver implements Resolve<any> {
    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private pageViewService: PageViewService
    ) {}

    /**
     * Route resolver for layout/:url that resolves into a PageView object
     * @param {ActivatedRouteSnapshot} route
     * @returns {Observable<any>}
     * @memberof LayoutResolver
     */
    resolve(route: ActivatedRouteSnapshot): Observable<DotPageView> {
        return this.pageViewService.get(route.queryParams.url).catch((err: ResponseView) => {
            this.dotHttpErrorManagerService.handle(err).subscribe((res: any) => {
                if (!res.redirected) {
                    this.dotRouterService.gotoPortlet('/c/site-browser');
                }
            });
            return Observable.of(null);
        });
    }
}
