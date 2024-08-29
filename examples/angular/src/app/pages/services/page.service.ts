import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { from, Observable, shareReplay } from 'rxjs';
import { map, switchMap, filter } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/angular';

import { DOTCMS_CLIENT_TOKEN } from '../../client-token/dotcms-client';
import { PageApiOptions, PageError } from '../pages.component';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);
  private navObservable!: Observable<DotcmsNavigationItem | null>;

  /**
   * Get the page and navigation for the given route and config.
   * 
   * @param {ActivatedRoute} route
   * @param {*} config
   * @return {*}  {(Observable<{ page: DotCMSPageAsset | { error: PageError }; nav: DotcmsNavigationItem }>)}
   * @memberof PageService
   */
  getPageAndNavigation(
    route: ActivatedRoute,
    config: any
  ): Observable<{ page: DotCMSPageAsset | { error: PageError }; nav: DotcmsNavigationItem }> {
    if (!this.navObservable) {
      this.navObservable = this.fetchNavigation(route);
    }

    return this.fetchPage(route, config).pipe(
      switchMap((page) =>
        this.navObservable.pipe(
          filter((nav): nav is DotcmsNavigationItem => nav !== null),
          map((nav) => ({ page, nav }))
        )
      )
    );
  }

  private fetchNavigation(route: ActivatedRoute): Observable<DotcmsNavigationItem | null> {
    return from(
      this.client.nav
        .get({
          path: '/',
          depth: 2,
          languageId: route.snapshot.params['languageId'] || 1
        })
        .then((response) => (response as any).entity)
    ).pipe(shareReplay(1));
  }

  private fetchPage(
    route: ActivatedRoute,
    config: any
  ): Observable<DotCMSPageAsset | { error: PageError }> {
    const queryParams = route.snapshot.queryParamMap;
    const url = route.snapshot.url.map((segment) => segment.path).join('/');
    const path = queryParams.get('path') || url || '/';

    const pageParams = getPageRequestParams({
      path,
      params: queryParams,
    });

    return from(
      this.client.page
        .get({ ...pageParams, ...config.params })
        .then((response) => response as DotCMSPageAsset)
        .catch((e) => {
          console.error(e);
          const error: PageError = {
            message: e.message,
            status: e.status,
          }
          return { error };
        })
    );
  }
}
