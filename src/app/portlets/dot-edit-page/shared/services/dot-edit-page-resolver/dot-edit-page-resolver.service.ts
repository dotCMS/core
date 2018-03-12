import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Response } from '@angular/http';

import { Observable } from 'rxjs/Observable';

import { ResponseView, HttpCode } from 'dotcms-js/dotcms-js';

import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';

/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<DotRenderedPageState>}
 */
@Injectable()
export class DotEditPageResolver implements Resolve<DotRenderedPageState> {
    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotRenderedPageState> {
        return this.dotPageStateService
            .get(route.queryParams.url)
            .map((dotRenderedPageState: DotRenderedPageState) => {
                // TODO: find a way to trow something to make the catch happen
                const currentSection = route.children[0].url[0].path;
                const isLayout = currentSection === 'layout';
                const userCantEditLayout = isLayout && !dotRenderedPageState.page.canEdit;
                if (userCantEditLayout) {
                    this.handleUserEditingOptions();
                }

                return dotRenderedPageState;
            })
            .catch((err: ResponseView) => {
                return this.errorHandler(err);
            });
    }

    private handleUserEditingOptions(): void {
        const response: Response = new Response({
            body: {},
            status: HttpCode.UNAUTHORIZED,
            headers: null,
            url: '',
            merge: null
        });
        this.errorHandler(new ResponseView(response));
    }

    private errorHandler(err: ResponseView): Observable<DotRenderedPageState> {
        this.dotHttpErrorManagerService.handle(err).subscribe((res: any) => {

            if (!res.redirected) {
                this.dotRouterService.gotoPortlet('/c/site-browser');
            }
        });
        return Observable.of(null);
    }
}
