import { Routes } from '@angular/router';
import { PageComponent } from './dotcms/pages/page/page';
import { BlogListingComponent } from './dotcms/pages/blog-listing/blog-listing.component';
import { BlogComponent } from './dotcms/pages/blog/blog.component';
import { ActivityComponent } from './dotcms/pages/activity/activity.component';

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
    redirectTo: '/blog',
  },
  {
    path: 'activities/:slug',
    component: ActivityComponent,
  },
  {
    path: '**',
    component: PageComponent,
  },
];
