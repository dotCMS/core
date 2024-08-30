import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { from, Observable, shareReplay } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/angular';

import { PageError } from '../pages.component';
import { DOTCMS_CLIENT_TOKEN } from '../../app.config';

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
  ): Observable<{
    page: DotCMSPageAsset | { error: PageError };
    nav: DotcmsNavigationItem | null;
  }> {
    if (!this.navObservable) {
      this.navObservable = this.fetchNavigation(route);
    }

    return this.fetchPage(route, config).pipe(
      switchMap((page) =>
        this.navObservable.pipe(
          map((nav) => ({ page, nav }))
        )
      )
    );
  }

  private fetchNavigation(
    route: ActivatedRoute
  ): Observable<DotcmsNavigationItem | null> {
    return from(
      this.client.nav
        .get({
          path: '/',
          depth: 2,
          languageId: route.snapshot.params['languageId'] || 1,
        })
        .then((response) => (response as any).entity)
        .catch((e) => {
          console.error(`Error fetching navigation: ${e.message}`);
          return null;
        })
    ).pipe(shareReplay(1));
  }

  private fetchPage(
    route: ActivatedRoute,
    config: any
  ): Observable<DotCMSPageAsset | { error: PageError }> {
    const queryParams = route.snapshot.queryParamMap;
    const url = route.snapshot.url.map((segment) => segment.path).join('/');
    const path = url || '/';

    const pageParams = getPageRequestParams({
      path,
      params: queryParams,
    });

    return from(
      this.client.page
        .get({ ...pageParams, ...config.params })
        .then((response) => {
          if (!(response as any).layout) {
            return { error: { message: 'You might be using an advanced template, or your dotCMS instance might lack an enterprise license.', status: 'Page without layout' } };
          }

          return response as DotCMSPageAsset
        })
        .catch((e) => {
          console.error(`Error fetching page: ${e.message}`);
          const error: PageError = {
            message: e.message,
            status: e.status,
          };
          return { error };
        })
    );
  }
}
