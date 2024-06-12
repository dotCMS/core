import { inject } from '@angular/core';
import { CanMatchFn, Route, UrlSegment } from '@angular/router';

import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client';
import { DotCMSPageAsset } from '@dotcms/angular';

export const canMatchPage: CanMatchFn = async (
  _route: Route,
  segments: UrlSegment[]
) => {
  const client = inject(DOTCMS_CLIENT_TOKEN);
  const path = segments?.map((segment) => segment.path)?.join('/') || '/';

  try {
    const { entity } = (await client.page.get({ path })) as {
      entity: DotCMSPageAsset;
    };

    return !!entity;
  } catch (error: any) {
    return !(error?.status === 404); // If the page is not found, return false.
  }
};
