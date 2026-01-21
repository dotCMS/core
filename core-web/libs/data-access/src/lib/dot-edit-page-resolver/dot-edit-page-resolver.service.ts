import { forkJoin, Observable, of, throwError } from 'rxjs';

import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { catchError, filter, map, switchMap } from 'rxjs/operators';

import { HttpCode, Site, SiteService } from '@dotcms/dotcms-js';
import { DotPageRenderOptions, DotPageRenderState } from '@dotcms/dotcms-models';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';
import { DotPageStateService } from '../dot-page-state/dot-page-state.service';
import { DotRouterService } from '../dot-router/dot-router.service';
import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<DotRenderedPageState>}
 */
@Injectable()
export class DotEditPageResolver implements Resolve<DotPageRenderState | null> {
    private dotPageStateService = inject(DotPageStateService);
    private dotRouterService = inject(DotRouterService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private siteService = inject(SiteService);
    private dotSessionStorageService: DotSessionStorageService = inject(DotSessionStorageService);

    resolve(route: ActivatedRouteSnapshot): Observable<DotPageRenderState | null> {
        const data = this.dotPageStateService.getInternalNavigationState();
        const renderOptions = this.getDotPageRenderOptions(route);
        const currentSection = route.children[0].url[0].path;
        const isLayout = currentSection === 'layout';
        const hostId = route.queryParams['host_id'];

        // If we have data, we don't need to request the page again
        const data$ = data ? of(data) : this.getPageRenderState(renderOptions, isLayout);

        return forkJoin([this.setSite(hostId), data$]).pipe(map(([_, pageRender]) => pageRender));
    }

    private checkUserCanGoToLayout(
        dotRenderedPageState: DotPageRenderState
    ): Observable<DotPageRenderState> {
        if (!dotRenderedPageState.page.canEdit) {
            return throwError(
                new HttpErrorResponse({
                    status: HttpCode.FORBIDDEN,
                    url: ''
                })
            );
        } else if (!dotRenderedPageState.layout) {
            return throwError(
                new HttpErrorResponse({
                    status: HttpCode.FORBIDDEN,
                    headers: new HttpHeaders({
                        'error-key': 'dotcms.api.error.license.required'
                    }),
                    url: ''
                })
            );
        } else {
            return of(dotRenderedPageState);
        }
    }

    private getDotPageRenderOptions(route: ActivatedRouteSnapshot): DotPageRenderOptions {
        const queryParams = route.queryParams;
        const renderOptions: DotPageRenderOptions = { url: queryParams['url'] };

        if (queryParams['mode']) {
            renderOptions.mode = queryParams['mode'];
        }

        if (queryParams['language_id']) {
            renderOptions.viewAs = { language: queryParams['language_id'] };
        }

        return renderOptions;
    }

    private setSite(id: string): Observable<Site | null> {
        const currentSiteId = this.siteService.currentSite?.identifier;
        const shouldSwitchSite = id && id !== currentSiteId;

        // If we have a site id and is different from the current one, we switch
        return shouldSwitchSite
            ? this.siteService.switchSiteById(id).pipe(
                  catchError((err: HttpErrorResponse) => {
                      return this.dotHttpErrorManagerService.handle(err).pipe(map(() => null));
                  })
              )
            : of(null);
    }

    private getPageRenderState(
        renderOptions: DotPageRenderOptions,
        isLayout: boolean
    ): Observable<DotPageRenderState | null> {
        return this.dotPageStateService.requestPage(renderOptions).pipe(
            filter((state: DotPageRenderState) => {
                if (!state) this.dotRouterService.goToSiteBrowser();

                return !!state;
            }),
            switchMap((dotRenderedPageState: DotPageRenderState) => {
                return isLayout
                    ? this.checkUserCanGoToLayout(dotRenderedPageState)
                    : of(dotRenderedPageState);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotSessionStorageService.removeVariantId();
                this.dotRouterService.goToSiteBrowser();

                return this.dotHttpErrorManagerService.handle(err).pipe(map(() => null));
            })
        );
    }
}
