import { Routes } from '@angular/router';
import { DotCMSPagesComponent } from './pages/pages.component';
import { NotFoundComponent } from './pages/notFound/notFound.component';

export const routes: Routes = [
  {
    path: '**',
    component: DotCMSPagesComponent,
    runGuardsAndResolvers: 'always'
  },
  { 
    path: '',
    pathMatch: 'full',
    component: DotCMSPagesComponent,
    runGuardsAndResolvers: 'always'
  }, // Default route

  // {
  //   path: '**',
  //   component: NotFoundComponent
  // },
];
