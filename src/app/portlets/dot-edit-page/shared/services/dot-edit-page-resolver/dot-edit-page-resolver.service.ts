import { throwError, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';

import { HttpCode, DotCMSResponse } from 'dotcms-js';
import { tap, switchMap, filter, catchError, map } from 'rxjs/operators';

import { DotPageRenderState } from '../../models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotPageRenderOptions } from '@services/dot-page-render/dot-page-render.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpResponse, HttpHeaders, HttpErrorResponse } from '@angular/common/http';

/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<DotRenderedPageState>}
 */
@Injectable()
export class DotEditPageResolver implements Resolve<DotPageRenderState> {
    constructor(
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotPageRenderState> {
        const data = this.dotPageStateService.getInternalNavigationState();

        if (data) {
            return of(data);
        } else {
            const options: DotPageRenderOptions = {
                url: route.queryParams.url,
                ...(route.queryParams.language_id
                    ? {
                          viewAs: {
                              language: route.queryParams.language_id
                          }
                      }
                    : {})
            };

            return this.dotPageStateService.requestPage(options).pipe(
                tap((state: DotPageRenderState) => {
                    if (!state) {
                        this.dotRouterService.goToSiteBrowser();
                    }
                }),
                filter((state: DotPageRenderState) => !!state),
                switchMap((dotRenderedPageState: DotPageRenderState) => {
                    const currentSection = route.children[0].url[0].path;
                    const isLayout = currentSection === 'layout';

                    return isLayout
                        ? this.checkUserCanGoToLayout(dotRenderedPageState)
                        : of(dotRenderedPageState);
                }),
                catchError((err: HttpErrorResponse) => {
                    this.dotRouterService.goToSiteBrowser();
                    return this.dotHttpErrorManagerService.handle(err).pipe(map(() => null));
                })
            );
        }
    }

    private checkUserCanGoToLayout(
        dotRenderedPageState: DotPageRenderState
    ): Observable<DotPageRenderState> {
        if (!dotRenderedPageState.page.canEdit) {
            return throwError(
                new HttpErrorResponse(
                    new HttpResponse<DotCMSResponse<any>>({
                        body: null,
                        status: HttpCode.FORBIDDEN,
                        headers: null,
                        url: '',
                    })
                )
            );
        } else if (!dotRenderedPageState.layout) {
            return throwError(
                new HttpErrorResponse(
                    new HttpResponse<DotCMSResponse<any>>({
                        body: null,
                        status: HttpCode.FORBIDDEN,
                        headers: new HttpHeaders({
                            'error-key': 'dotcms.api.error.license.required'
                        }),
                        url: '',
                    })
                )
            );
        } else {
            return of(dotRenderedPageState);
        }
    }
}
