import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';
import {
    DotLegacyTemplateAdditionalActionsComponent
} from './layout/components/dot-template-additional-actions/dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { EditLayoutResolver } from './layout/services/dot-edit-layout-resolver/dot-edit-layout-resolver.service';
import { DotEditContentResolver } from './content/services/dot-edit-content-resolver.service';

const dotEditPage: Routes = [
    {
        component: DotEditPageMainComponent,
        path: '',
        children: [
            {
                path: '',
                redirectTo: './content'
            },
            {
                loadChildren: 'app/portlets/dot-edit-page/content/dot-edit-content.module#DotEditContentModule',
                path: 'content',
                resolve: {
                    content: DotEditContentResolver
                },

            },
            {
                loadChildren: 'app/portlets/dot-edit-page/layout/dot-edit-layout.module#DotEditLayoutModule',
                path: 'layout',
                resolve: {
                    content: EditLayoutResolver
                },
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
