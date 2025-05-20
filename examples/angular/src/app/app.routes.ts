import { Routes } from '@angular/router';
import { DotCMSPageComponent } from './pages/dot-cms-page/dot-cms-page.component';
import { BlogComponent } from './pages/blog/blog.component';

export const routes: Routes = [
    {
        path: 'blog/post/:slug',
        component: BlogComponent
    },
    {
        path: '**',
        component: DotCMSPageComponent
    }
];
