import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  DOTCMS_CLIENT_TOKEN,
  DotCMSPageAsset,
  DotcmsNavigationItem,
} from '@dotcms/angular';
import { inject, InjectionToken } from '@angular/core';

import { DotCmsClient } from '@dotcms/client';

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
  // TODO: WE NEED TO FIX THIS SOMEHOW
  // IF WE DONT DO THIS WE WILL GET A TYPE ERROR ON DEVELOPMENT BECAUSE THE TOKEN USES THE TYPES FROM CORE WEB AND THE INJECT FUNCTION IS USING THE TYPES FROM THE EXAMPLE
  const client = inject<DotCmsClient>(
    DOTCMS_CLIENT_TOKEN as unknown as InjectionToken<DotCmsClient>
  );
  const pageAsset = route.data['pageAsset'] as DotCMSPageAsset;

  const { language_id } = route.queryParams;

  const navProps = {
    path: '/',
    depth: 2,
    languageId: language_id,
  };

  const navResponse = (await client.nav.get(navProps)) as {
    entity: DotcmsNavigationItem;
  };
  const nav = navResponse?.entity;

  return { pageAsset, nav };
};
