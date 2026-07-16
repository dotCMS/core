import { Routes } from '@angular/router';

import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from './components/dot-create-contentlet/dot-create-contentlet.resolver.service';

export const dotContentletEditorRoutes: Routes = [
    {
        component: DotCreateContentletComponent,
        path: ':contentType',
        resolve: {
            url: DotCreateContentletResolver
        }
    },
    {
        path: '',
        redirectTo: '/c/content',
        pathMatch: 'full'
    }
];
