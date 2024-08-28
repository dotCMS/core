import { Routes } from '@angular/router';
import { DotCMSPagesComponent } from './pages/pages.component';

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
  }
];
