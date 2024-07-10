import { inject } from '@angular/core';
import { CanMatchFn, Route, Router, UrlSegment } from '@angular/router';

import { getPageRequestParams } from '@dotcms/client';
import { DotCMSPageAsset } from '@dotcms/angular';

import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';

export const canMatchPage: CanMatchFn = async (
  route: Route,
  segments: UrlSegment[],
) => {
  const router = inject(Router);
  const client = inject(DOTCMS_CLIENT_TOKEN);

  try {
    const { queryParams } = router.getCurrentNavigation()?.initialUrl || {};
    const path = segments?.map((segment) => segment.path)?.join('/') || '/';
    const pageProps = getPageRequestParams({
      path,
      params: queryParams,
    });

    const pageAsset = (await client.page.get(pageProps)) as DotCMSPageAsset;
    const { vanityUrl } = pageAsset;

    if (vanityUrl?.permanentRedirect || vanityUrl?.temporaryRedirect) {
      return router.createUrlTree([vanityUrl.forwardTo]);
    }

    // Add the page asset to the route data
    // so it can be used by the DotCMSPageResolver and avoid fetching it again.
    route.data = { ...route.data, pageAsset };

    if (vanityUrl) {
      const vanityPagePros = { ...pageProps, path: vanityUrl.forwardTo };
      const pageResponse = await (client.page.get(vanityPagePros) as Promise<{
        entity: DotCMSPageAsset;
      }>);
      // Add the page asset to the route data
      // so it can be used by the DotCMSPageResolver and avoid fetching it again.
      route.data = { ...route.data, pageAsset: pageResponse.entity };
    }

    return !!pageAsset;
  } catch (error: any) {
    console.error(error); // Log the error
    route.data = { ...route.data, pageAsset: { layout: {} } }; // Add the page asset to the route data
    return !(error?.status === 404); // If the page is not found, return false.
  }
};
