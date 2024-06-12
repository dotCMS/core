import { CanMatchFn, Route, Routes, UrlSegment } from '@angular/router';
import { DotCMSPagesComponent } from './pages/pages.component';
import { DotCMSPageResolver } from './resolver/dotcms-page.resolver';

import { inject } from '@angular/core';
import { DOTCMS_CLIENT_TOKEN } from './client-token/dotcms-client';
import { DotCMSPageAsset } from '@dotcms/angular';
import { NotFoundComponent } from './pages/notFound/notFound.component';

const canMatchPage: CanMatchFn = async (
  _route: Route,
  segments: UrlSegment[]
) => {
  const client = inject(DOTCMS_CLIENT_TOKEN);
  const url = segments.map((segment) => segment.path).join('/');

  const pageProps = { path: url || '/' };

  try {
    const { entity } = await (client.page.get(pageProps) as Promise<{
      entity: DotCMSPageAsset;
    }>);

    return !!entity;
  } catch (error: any) {

    return !(error?.status === 404);
  }
};

export const routes: Routes = [
  {
    path: '**',
    resolve: {
      // This should be called `context`.
      context: DotCMSPageResolver,
    },
    canMatch: [canMatchPage],
    component: DotCMSPagesComponent, // Render the page
    runGuardsAndResolvers: 'always', // Run the resolver on every navigation. Even if the URL hasn't changed.
  },
  {
    path: '**',
    component: NotFoundComponent
  },
];
