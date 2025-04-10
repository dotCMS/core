import { Routes } from '@angular/router';
import { DotCMSPagesComponent } from './pages/pages.component';
import { BlogComponent } from './pages/blog/blog.component';

export const routes: Routes = [

  {
    path: 'blog/post/:slug',
    component: BlogComponent
  },
  {
    path: '**',
    component: DotCMSPagesComponent
  },
];
