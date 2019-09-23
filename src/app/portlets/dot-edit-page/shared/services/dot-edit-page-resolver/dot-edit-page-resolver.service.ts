import { throwError, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Response, Headers } from '@angular/http';

import { ResponseView, HttpCode } from 'dotcms-js';
import { tap, switchMap, filter, catchError, map } from 'rxjs/operators';

import { DotPageRenderState } from '../../models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotPageRenderOptions } from '@services/dot-page-render/dot-page-render.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

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
                catchError((err: ResponseView) => {
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
            return throwError(
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
}
