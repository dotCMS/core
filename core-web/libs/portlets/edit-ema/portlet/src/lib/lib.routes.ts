import { Route } from '@angular/router';

import {
    CanDeactivateGuardService,
    DotContentletLockerService,
    DotESContentService,
    DotFavoritePageService,
    DotPageRenderService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver,
    DotExperimentsService
} from '@dotcms/portlets/dot-experiments/data-access';
import { portletHaveLicenseResolver } from '@dotcms/ui';
import { DotPageStateService } from '@portlets/dot-edit-page/content/services/dot-page-state/dot-page-state.service';
import { DotEditPageResolver } from '@portlets/dot-edit-page/shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import {
    DotEnterpriseLicenseResolver,
    DotPushPublishEnvironmentsResolver
} from '@portlets/shared/resolvers';

import { DotEmaShellComponent } from './dot-ema-shell/dot-ema-shell.component';
import { editEmaGuard } from './services/guards/edit-ema.guard';

export const DotEmaRoutes: Route[] = [
    {
        path: '',
        canActivate: [editEmaGuard],
        component: DotEmaShellComponent,
        providers: [
            DotEditPageResolver, // Resolver
            DotPageStateService, // State of the Page?
            DotContentletLockerService, // If the content is locked
            DotPageRenderService, // ?
            DotFavoritePageService, // Favorite
            DotESContentService, // Pallet I think
            DotExperimentsService // Used inside the resolver
        ],
        resolve: {
            haveLicense: portletHaveLicenseResolver,
            content: DotEditPageResolver
        },
        runGuardsAndResolvers: 'always',
        children: [
            {
                path: 'content',
                loadComponent: () =>
                    import('./edit-ema-editor/edit-ema-editor.component').then(
                        (mod) => mod.EditEmaEditorComponent
                    )
            },
            {
                path: 'layout',
                loadComponent: () =>
                    import('./edit-ema-layout/edit-ema-layout.component').then(
                        (mod) => mod.EditEmaLayoutComponent
                    ),
                canDeactivate: [CanDeactivateGuardService]
            },
            {
                path: 'rules/:pageId',
                loadChildren: () => import('@dotcms/dot-rules').then((m) => m.DotRulesModule)
            },
            {
                path: 'experiments',
                providers: [
                    DotExperimentsService,
                    DotEnterpriseLicenseResolver,
                    DotExperimentExperimentResolver,
                    DotPushPublishEnvironmentsResolver,
                    DotExperimentsConfigResolver
                ],
                loadChildren: async () =>
                    //TODO: move all the core-web/apps/dotcms-ui/src/app/view/components/_common
                    // folder with components reused in experiments to a Library to
                    // avoid this circular dependency

                    // eslint-disable-next-line @nx/enforce-module-boundaries
                    (await import('@dotcms/portlets/dot-experiments/portlet'))
                        .DotExperimentsPortletRoutes
            },

            {
                path: '**',
                redirectTo: 'content',
                pathMatch: 'full'
            },
            {
                path: '',
                redirectTo: 'content',
                pathMatch: 'full'
            }
        ]
    }
];
