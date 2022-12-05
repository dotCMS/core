import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import { LayoutEditorCanDeactivateGuardService } from '@dotcms/app/api/services/guards/layout-editor-can-deactivate-guard.service';

import { DotFeatureFlagResolver } from '@portlets/shared/resolvers/dot-feature-flag-resolver.service';
import { FeaturedFlags } from '@dotcms/dotcms-models';

const dotEditPage: Routes = [
    {
        component: DotEditPageMainComponent,
        path: '',
        resolve: {
            content: DotEditPageResolver,
            featuredFlag: DotFeatureFlagResolver
        },
        data: {
            featuredFlagToCheck: FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS
        },

        runGuardsAndResolvers: 'always',
        children: [
            {
                path: '',
                redirectTo: './content',
                pathMatch: 'full'
            },
            {
                path: 'content',
                loadChildren: () =>
                    import('@portlets/dot-edit-page/content/dot-edit-content.module').then(
                        (m) => m.DotEditContentModule
                    )
            },
            {
                path: 'layout',
                loadChildren: () =>
                    import('@portlets/dot-edit-page/layout/dot-edit-layout.module').then(
                        (m) => m.DotEditLayoutModule
                    ),
                canDeactivate: [LayoutEditorCanDeactivateGuardService]
            },
            {
                path: 'rules/:pageId',
                loadChildren: () =>
                    import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule)
            },

            // TODO: create a guard global of isEnterprise
            {
                path: 'experiments',
                loadChildren: async () =>
                    (
                        await import(
                            '../dot-experiments/dot-experiments-shell/dot-experiments-shell.module'
                        )
                    ).DotExperimentsShellModule
            }
        ]
    },
    {
        path: 'layout/template/:id/:tabName',
        loadChildren: () =>
            import(
                '@portlets/dot-edit-page/layout/components/dot-template-additional-actions/dot-template-additional-actions.module'
            ).then((m) => m.DotTemplateAdditionalActionsModule)
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(dotEditPage)]
})
export class DotEditPageRoutingModule {}
