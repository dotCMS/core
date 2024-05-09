import { Routes } from '@angular/router';
import { DotCMSPagesComponent } from './page/dotcms-pages/dotcms-pages.component';
import { DotCMSPageResolver } from './lib/resolver/dotcms-page.resolver';

export const routes: Routes = [
  {
    path: '**',
    resolve: {
      context: DotCMSPageResolver,
    },
    component: DotCMSPagesComponent,
  },
];
