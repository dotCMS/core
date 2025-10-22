import { Routes } from '@angular/router';

import { DotPagesComponent } from './dot-pages.component';
import {
    newEditContentForContentTypeGuard,
    newEditContentForContentletGuard
} from './guards/dot-pages.guard';

import { DotCreateContentletComponent } from '../../view/components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from '../../view/components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.resolver.service';
import { DotContentletEditorService } from '../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

export const dotPagesRoutes: Routes = [
    {
        component: DotPagesComponent,
        path: '',
        providers: [DotCreateContentletResolver, DotContentletEditorService],
        children: [
            {
                loadComponent: () =>
                    import('../dot-porlet-detail/dot-portlet-detail.component').then(
                        (m) => m.DotPortletDetailComponent
                    ),
                path: ':asset',
                canActivate: [newEditContentForContentletGuard],
                data: {
                    reuseRoute: false
                }
            },
            {
                path: 'new/:contentType',
                canActivate: [newEditContentForContentTypeGuard],
                component: DotCreateContentletComponent,
                resolve: {
                    url: DotCreateContentletResolver
                }
            }
        ]
    }
];
