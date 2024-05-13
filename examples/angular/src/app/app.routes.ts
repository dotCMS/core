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
  },
];
