import { PageViewResolver } from './dot-edit-page-resolver.service';
import { NgModule } from '@angular/core';
import { DotEditLayoutComponent } from './layout/dot-edit-layout/dot-edit-layout.component';
import { RouterModule, Routes } from '@angular/router';
import {
    DotLegacyTemplateAdditionalActionsComponent
// tslint:disable-next-line:max-line-length
} from './layout/dot-template-additional-actions/dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';

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
                loadChildren: 'app/portlets/dot-edit-content/dot-edit-content.module#DotEditContentModule',
                path: 'content',
            },
            {
                loadChildren: 'app/portlets/dot-edit-page/layout/dot-edit-layout/dot-edit-layout.module#DotEditLayoutModule',
                path: 'layout'
            },
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
