import { PageViewResolver } from './dot-edit-page-resolver.service';
import { NgModule } from '@angular/core';
import { DotEditLayoutComponent } from './layout/dot-edit-layout/dot-edit-layout.component';
import { RouterModule, Routes } from '@angular/router';
import {
    DotLegacyTemplateAdditionalActionsComponent
// tslint:disable-next-line:max-line-length
} from './layout/dot-template-additional-actions/dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';

const dotEditPage: Routes = [
    {
        component: DotEditLayoutComponent,
        path: ''
    },
    {
        component: DotEditLayoutComponent,
        path: 'layout',
        resolve: {
            pageView: PageViewResolver
        }
    },
    {
        component: DotEditLayoutComponent,
        path: 'layout/:url'
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
