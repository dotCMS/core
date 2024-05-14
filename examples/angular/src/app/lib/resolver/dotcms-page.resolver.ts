import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  ResolveFn,
  RouterStateSnapshot,
} from '@angular/router';
import { Observable, from, map, tap } from 'rxjs';

import { DOTCMS_CLIENT_TOKEN } from '../dotcms-client-token';
import { DotCMSNavigationItem, DotCMSPageAsset } from '../models';
import { PageContextService } from '../services/dotcms-context/page-context.service';

/**
 * This resolver is used to fetch the page and navigation data from dotCMS.
 *
 * @param {ActivatedRouteSnapshot} route
 * @param {RouterStateSnapshot} _state
 * @return {*}
 */
export const DotCMSPageResolver: ResolveFn<
  Observable<{
    pageAsset: DotCMSPageAsset;
    nav: DotCMSNavigationItem;
  }>
> = (route: ActivatedRouteSnapshot) => {
  // Get the Service
  const client = inject(DOTCMS_CLIENT_TOKEN);
  const pageContextService = inject(PageContextService);

  const url = route.url.map((segment) => segment.path).join('/');
  const queryParams = route.queryParams;

  const pageProps = {
    path: url || 'index',
    language_id: queryParams['language_id'],
    mode: queryParams['mode'],
    variantName: queryParams['variantName'],
    'com.dotmarketing.persona.id':
      queryParams['com.dotmarketing.persona.id'] || '',
  };

  const navProps = {
    path: '/',
    depth: 2,
    languageId: queryParams['language_id'],
  };

  const pageRequest = client.page
    .get(pageProps)
    .then(({ entity }: { entity: DotCMSPageAsset }) => entity);
  const navRequest = client.nav
    .get(navProps)
    .then(({ entity }: { entity: DotCMSNavigationItem }) => entity);

  return from(Promise.all([pageRequest, navRequest])).pipe(
    map(([pageAsset, nav]) => ({ pageAsset, nav })),
    tap(({ pageAsset }) => pageContextService.setContext(pageAsset))
  );
};
