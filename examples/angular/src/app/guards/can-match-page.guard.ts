import { inject } from '@angular/core';
import { CanMatchFn, Route, Router, UrlSegment } from '@angular/router';

import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';
import { DotCMSPageAsset } from '@dotcms/angular';

export const canMatchPage: CanMatchFn = async (
  route: Route,
  segments: UrlSegment[]
) => {
  const router = inject(Router);
  const client = inject(DOTCMS_CLIENT_TOKEN);

  const path = segments?.map((segment) => segment.path)?.join('/') || '/';
  const initialUrl = router.getCurrentNavigation()?.initialUrl;
  const queryParams = initialUrl?.queryParams || {};

  const { mode, variantName, language_id } = queryParams;
  const personaId = queryParams['com.dotmarketing.persona.id'] || '';

  const pageProps = {
    path,
    mode,
    language_id,
    variantName,
    'com.dotmarketing.persona.id': personaId,
  };

  try {
    const { entity } = (await client.page.get(pageProps)) as {
      entity: DotCMSPageAsset;
    };

    // Add the page asset to the route data
    // so it can be used by the DotCMSPageResolver and avoid fetching it again.
    route.data = { ...route.data, pageAsset: entity };

    return !!entity;
  } catch (error: any) {
    return !(error?.status === 404); // If the page is not found, return false.
  }
};
