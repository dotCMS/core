import { throwError as observableThrowError, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Response, Headers } from '@angular/http';

import { ResponseView, HttpCode } from 'dotcms-js';

import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotRenderedPageState } from '../../models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditPageDataService } from './dot-edit-page-data.service';
import { take, switchMap, tap, catchError, map } from 'rxjs/operators';

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
        private dotRouterService: DotRouterService,
        private dotEditPageDataService: DotEditPageDataService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotRenderedPageState> {
        const data = this.dotEditPageDataService.getAndClean();
        if (data) {
            return of(data);
        } else {
            return this.dotPageStateService
                .get({
                    url: route.queryParams.url,
                    viewAs: {
                        language_id: route.queryParams.language_id,
                    }
                })
                .pipe(
                    take(1),
                    switchMap((dotRenderedPageState: DotRenderedPageState) => {
                        const currentSection = route.children[0].url[0].path;
                        const isLayout = currentSection === 'layout';

                        if (isLayout) {
                            return this.checkUserCanGoToLayout(dotRenderedPageState);
                        } else {
                            return of(dotRenderedPageState);
                        }
                    }),
                    catchError((err: ResponseView) => {
                        return this.errorHandler(err).pipe(map(() => null));
                    })
                );
        }
    }

    private checkUserCanGoToLayout(
        dotRenderedPageState: DotRenderedPageState
    ): Observable<DotRenderedPageState> {
        if (!dotRenderedPageState.page.canEdit) {
            return observableThrowError(
                new ResponseView(
                    new Response({
                        body: {},
                        status: HttpCode.FORBIDDEN,
                        headers: null,
                        url: '',
                        merge: null
                    })
                )
            );
        } else if (!dotRenderedPageState.layout) {
            return observableThrowError(
                new ResponseView(
                    new Response({
                        body: {},
                        status: HttpCode.FORBIDDEN,
                        headers: new Headers({
                            'error-key': 'dotcms.api.error.license.required'
                        }),
                        url: '',
                        merge: null
                    })
                )
            );
        } else {
            return of(dotRenderedPageState);
        }
    }

    private errorHandler(err: ResponseView): Observable<any> {
        return this.dotHttpErrorManagerService.handle(err).pipe(
            tap((res: DotHttpErrorHandled) => {
                if (!res.redirected) {
                    this.dotRouterService.goToSiteBrowser();
                }
            })
        );
    }
}
