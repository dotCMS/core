import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotPagesComponent } from './dot-pages.component';
import {
    newEditContentForContentTypeGuard,
    newEditContentForContentletGuard
} from './guards/dot-pages.guard';

import { DotCreateContentletComponent } from '../../view/components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from '../../view/components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.resolver.service';

const routes: Routes = [
    {
        component: DotPagesComponent,
        path: '',
        children: [
            {
                loadChildren: () =>
                    import('../dot-porlet-detail/dot-portlet-detail.module').then(
                        (m) => m.DotPortletDetailModule
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

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotPagesRoutingModule {}
