import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotCreateContentletComponent } from '@components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from '@components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.resolver.service';

import { DotPagesComponent } from './dot-pages.component';

const routes: Routes = [
    {
        component: DotPagesComponent,
        path: '',
        children: [
            {
                loadChildren: () =>
                    import('@portlets/dot-porlet-detail/dot-portlet-detail.module').then(
                        (m) => m.DotPortletDetailModule
                    ),
                path: ':asset',
                data: {
                    reuseRoute: false
                }
            },
            {
                path: 'new/:contentType',
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
