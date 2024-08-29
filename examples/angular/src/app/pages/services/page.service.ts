import { inject, Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { from, Observable, shareReplay } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { getPageRequestParams } from '@dotcms/client';
import { DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/angular';

import { DOTCMS_CLIENT_TOKEN } from '../../client-token/dotcms-client';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  private readonly client = inject(DOTCMS_CLIENT_TOKEN);
  private navObservable: Observable<DotcmsNavigationItem | null>;

  constructor() {
    this.navObservable = this.fetchNavigation();
  }

  getPage(route: ActivatedRoute): Observable<any> {
    return this.fetchPage(route).pipe(
      switchMap((page) => 
        this.navObservable.pipe(
          map((nav) => ({ page, nav }))
        )
      )
    );
  }

  private fetchNavigation(): Observable<DotcmsNavigationItem | null> {
    return from(
      this.client.nav.get({
        path: '/',
        depth: 2,
        languageId: 1, // Default language ID
      }).then((response) => (response as any).entity)
    ).pipe(
      shareReplay(1)
    );
  }

  private fetchPage(route: ActivatedRoute): Observable<any> {
    const queryParams = route.snapshot.queryParamMap;
    const url = route.snapshot.url.map((segment) => segment.path).join('/');
    const path = queryParams.get('path') || url || '/';

    const pageParams = getPageRequestParams({
      path,
      params: queryParams,
    });

    return from(
      this.client.page.get(pageParams).catch((error) => ({
        error: {
          message: error.message,
          status: error.status,
        },
      }))
    );
  }
}
