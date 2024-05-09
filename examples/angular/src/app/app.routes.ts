import { Routes } from '@angular/router';
import { AppComponent } from './app.component';
import { DotCMSPageResolver } from './lib/resolver/dotcms-page.resolver';

export const routes: Routes = [
  {
    path: '**',
    resolve: {
      data: DotCMSPageResolver,
    },
    component: AppComponent,
  },
];
