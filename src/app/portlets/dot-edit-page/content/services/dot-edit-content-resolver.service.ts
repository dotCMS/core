import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { ResponseView } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { DotHttpErrorManagerService } from '../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPageStateService } from './dot-page-state/dot-page-state.service';

/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<DotRenderedPageState>}
 */
@Injectable()
export class DotEditContentResolver implements Resolve<DotRenderedPageState> {
    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private dotPageStateService: DotPageStateService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotRenderedPageState> {
        return this.dotPageStateService.get(route.queryParams.url).catch((err: ResponseView) => {
            this.dotHttpErrorManagerService.handle(err).subscribe((res: any) => {
                if (!res.redirected) {
                    this.dotRouterService.gotoPortlet('/c/site-browser');
                }
            });
            return Observable.of(null);
        });
    }
}
