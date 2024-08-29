import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { from, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/angular';

import { DOTCMS_CLIENT_TOKEN } from '../../client-token/dotcms-client';
import { PageError } from '../pages.component';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);

  /**
   * Get the page and navigation data for the given route.
   * @param route The activated route.
   * @returns An observable that emits the page and navigation data.
   */
  getPage(route: ActivatedRoute): Observable<any> {
    const queryParams = route.snapshot.queryParamMap;
    const url = route.snapshot.url.map((segment) => segment.path).join('/');
    const path = queryParams.get('path') || url || '/';

    const pageParams = getPageRequestParams({
      path,
      params: queryParams,
    });
    const pagePromise = this.client.page.get(pageParams).catch((error) => ({
      error: {
        message: error.message,
        status: error.status,
      },
    })) as Promise<DotCMSPageAsset | { error: PageError }>;

    const navParams = {
      path: '/',
      depth: 2,
      languageId: parseInt(queryParams.get('languageId') || '1'),
    };
    const navPromise = this.client.nav
      .get(navParams)
      .then((response) => (response as any).entity)
      .catch((error) => null) as Promise<DotcmsNavigationItem | null>;

    return from(Promise.all([pagePromise, navPromise])).pipe(
      map(([page, navResponse]) => ({
        page,
        nav: navResponse,
      }))
    );
  }
}
