import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';

const dotEditPage: Routes = [
    {
        component: DotEditPageMainComponent,
        path: '',
        resolve: {
            content: DotEditPageResolver
        },
        runGuardsAndResolvers: 'always',
        children: [
            {
                path: '',
                redirectTo: './content'
            },
            {
                loadChildren: () =>
                    import('@portlets/dot-edit-page/content/dot-edit-content.module').then(
                        (m) => m.DotEditContentModule
                    ),
                path: 'content'
            },
            {
                loadChildren: () =>
                    import('@portlets/dot-edit-page/layout/dot-edit-layout.module').then(
                        (m) => m.DotEditLayoutModule
                    ),
                path: 'layout'
            },
            {
                loadChildren: () =>
                    import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule),
                path: 'rules/:pageId'
            }
        ]
    },
    {
        loadChildren: () =>
            import(
                '@portlets/dot-edit-page/layout/components/dot-template-additional-actions/dot-template-additional-actions.module'
            ).then((m) => m.DotTemplateAdditionalActionsModule),
        path: 'layout/template/:id/:tabName'
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(dotEditPage)]
})
export class DotEditPageRoutingModule {}
