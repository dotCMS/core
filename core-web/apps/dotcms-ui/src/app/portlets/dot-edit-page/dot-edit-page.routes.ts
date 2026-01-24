import { Routes } from '@angular/router';

import { CanDeactivateGuardService, DotEditPageResolver } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotExperimentExperimentResolver } from '@dotcms/portlets/dot-experiments/data-access';

import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';

import { DotFeatureFlagResolver } from '../shared/resolvers/dot-feature-flag-resolver.service';

export const dotEditPageRoutes: Routes = [
    {
        component: DotEditPageMainComponent,
        path: '',
        resolve: {
            content: DotEditPageResolver,
            featuredFlags: DotFeatureFlagResolver,
            experiment: DotExperimentExperimentResolver
        },
        data: {
            featuredFlagsToCheck: [
                FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS,
                FeaturedFlags.FEATURE_FLAG_SEO_PAGE_TOOLS
            ]
        },
        // needed to allow navigation from the page menu in the edit mode. See https://github.com/dotCMS/core/pull/25509
        runGuardsAndResolvers: 'always',
        children: [
            {
                path: '',
                redirectTo: './content',
                pathMatch: 'full'
            },
            {
                path: 'content',
                loadComponent: () =>
                    import('./content/dot-edit-content.component').then(
                        (m) => m.DotEditContentComponent
                    )
            },
            {
                path: 'layout',
                loadComponent: () =>
                    import('./layout/dot-edit-layout/dot-edit-layout.component').then(
                        (m) => m.DotEditLayoutComponent
                    ),
                canDeactivate: [CanDeactivateGuardService]
            },
            {
                path: 'rules/:pageId',
                loadChildren: () => import('@dotcms/dot-rules').then((m) => m.DotRulesModule)
            },
            {
                path: 'experiments',
                loadChildren: async () =>
                    (await import('@dotcms/portlets/dot-experiments/portlet'))
                        .DotExperimentsPortletRoutes
            }
        ]
    },
    {
        path: 'layout/template/:id/:tabName',
        loadComponent: () =>
            import('./layout/components/dot-template-additional-actions/dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component').then(
                (m) => m.DotLegacyTemplateAdditionalActionsComponent
            )
    }
];
