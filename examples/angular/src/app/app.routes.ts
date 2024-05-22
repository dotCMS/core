import { Routes } from '@angular/router';
import { DotCMSPagesComponent } from './pages/pages.component';
import { DotCMSPageResolver } from './lib/resolver/dotcms-page.resolver';

export const routes: Routes = [
  {
    path: '**',
    resolve: {
      // This should be called `context`.
      context: DotCMSPageResolver,
    },
    component: DotCMSPagesComponent,
    runGuardsAndResolvers: 'always' // Run the resolver on every navigation. Even if the URL hasn't changed.
  },
];
