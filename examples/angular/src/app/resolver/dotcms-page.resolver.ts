import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { DotCMSPageAsset, DotcmsNavigationItem } from '@dotcms/angular';
import { inject } from '@angular/core';
import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';

/**
 * This resolver is used to fetch the page and navigation data from dotCMS.
 *
 * @param {ActivatedRouteSnapshot} route
 * @param {RouterStateSnapshot} _state
 * @return {*}
 */
export const DotCMSPageResolver = async (
  route: ActivatedRouteSnapshot
): Promise<{
  pageAsset: DotCMSPageAsset;
  nav: DotcmsNavigationItem;
}> => {
  const client = inject(DOTCMS_CLIENT_TOKEN);
  const queryParams = route.queryParams;

  const pageAsset = route.data?.['pageAsset'] as DotCMSPageAsset;

  const navProps = {
    path: '/',
    depth: 2,
    languageId: queryParams['language_id'],
  };

  const navRequest = client.nav.get(navProps) as Promise<{
    entity: DotcmsNavigationItem;
  }>;

  const [navResponse] = await Promise.all([
    navRequest,
  ]);

  const nav = navResponse.entity;

  return { pageAsset, nav };
};
