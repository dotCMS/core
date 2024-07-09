import { inject, InjectionToken } from '@angular/core';
import { CanMatchFn, Route, Router, UrlSegment } from '@angular/router';

import { DotCmsClient, getPageRequestParams } from '@dotcms/client';
import { DOTCMS_CLIENT_TOKEN, DotCMSPageAsset } from '@dotcms/angular';

export const canMatchPage: CanMatchFn = async (
  route: Route,
  segments: UrlSegment[]
) => {
  const router = inject(Router);

  // TODO: WE NEED TO FIX THIS SOMEHOW
  // IF WE DONT DO THIS WE WILL GET A TYPE ERROR ON DEVELOPMENT BECAUSE OF TYPES FROM OUR SYM LINK, COULD BE ANGULAR VERSIONS
  const client = inject<DotCmsClient>(
    DOTCMS_CLIENT_TOKEN as unknown as InjectionToken<DotCmsClient>
  );

  try {
    const { queryParams } = router.getCurrentNavigation()?.initialUrl || {};
    const path = segments?.map((segment) => segment.path)?.join('/') || '/';
    const pageProps = getPageRequestParams({
      path,
      params: queryParams,
    });

    const { entity } = (await client.page.get(pageProps)) as {
      entity: DotCMSPageAsset;
    };

    const { vanityUrl } = entity;

    if (vanityUrl?.permanentRedirect || vanityUrl?.temporaryRedirect) {
      return router.createUrlTree([vanityUrl.forwardTo]);
    }

    // Add the page asset to the route data
    // so it can be used by the DotCMSPageResolver and avoid fetching it again.
    route.data = { ...route.data, pageAsset: entity };

    if (vanityUrl) {
      const vanityPagePros = { ...pageProps, path: vanityUrl.forwardTo };
      const pageResponse = await (client.page.get(vanityPagePros) as Promise<{
        entity: DotCMSPageAsset;
      }>);
      // Add the page asset to the route data
      // so it can be used by the DotCMSPageResolver and avoid fetching it again.
      route.data = { ...route.data, pageAsset: pageResponse.entity };
    }

    return !!entity;
  } catch (error: any) {
    console.error(error); // Log the error
    route.data = { ...route.data, pageAsset: { layout: {} } }; // Add the page asset to the route data
    return !(error?.status === 404); // If the page is not found, return false.
  }
};
