import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

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
            }
        ]
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotPagesRoutingModule {}
