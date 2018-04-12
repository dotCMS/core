import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';
// tslint:disable-next-line:max-line-length
import { DotLegacyTemplateAdditionalActionsComponent } from './layout/components/dot-template-additional-actions/dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import { DotEditLayoutGuardService } from './shared/services/dot-edit-layout-guard/dot-edit-layout-guard.service';

const dotEditPage: Routes = [
    {
        component: DotEditPageMainComponent,
        path: '',
        resolve: {
            content: DotEditPageResolver
        },
        runGuardsAndResolvers: 'paramsOrQueryParamsChange',
        children: [
            {
                path: '',
                redirectTo: './content'
            },
            {
                loadChildren: 'app/portlets/dot-edit-page/content/dot-edit-content.module#DotEditContentModule',
                path: 'content'

            },
            {
                loadChildren: 'app/portlets/dot-edit-page/layout/dot-edit-layout.module#DotEditLayoutModule',
                path: 'layout',
                canActivate: [DotEditLayoutGuardService]
            }
        ]
    },
    {
        component: DotLegacyTemplateAdditionalActionsComponent,
        path: 'layout/template/:id/:tabName'
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(dotEditPage)]
})
export class DotEditPageRoutingModule {}
