import { Routes } from '@angular/router';

import { canMatchPage } from './guards/can-match-page.guard';
import { DotCMSPageResolver } from './resolver/dotcms-page.resolver';

import { DotCMSPagesComponent } from './pages/pages.component';
import { NotFoundComponent } from './pages/notFound/notFound.component';

export const routes: Routes = [
  {
    path: '**',
      resolve: {
      // This should be called `context`.
      context: DotCMSPageResolver,
    },
    canMatch: [canMatchPage],
    component: DotCMSPagesComponent,
    runGuardsAndResolvers: 'always', // Run the resolver on every navigation. Even if the URL hasn't changed.
  },
  {
    path: '**',
    component: NotFoundComponent
  },
];
