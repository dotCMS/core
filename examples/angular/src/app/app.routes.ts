import { Routes } from '@angular/router';
import { DotCMSPageComponent } from './pages/dot-cms-page/dot-cms-page.component';
import { BlogComponent } from './pages/blog/blog.component';
import { BlogListingComponent } from './pages/blog-listing/blog-listing.component';
export const routes: Routes = [
  {
    path: 'blog/post/:slug',
    component: BlogComponent,
  },
  {
    path: 'blog',
    component: BlogListingComponent,
  },
  {
    path: 'blog/index',
    redirectTo: 'blog',
  },
  {
    path: '**',
    component: DotCMSPageComponent,
  },
];
