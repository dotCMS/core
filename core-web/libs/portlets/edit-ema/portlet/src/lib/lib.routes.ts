import { Route } from '@angular/router';

import {
    CanDeactivateGuardService,
    DotContentletLockerService,
    DotESContentService,
    DotEditPageResolver,
    DotExperimentsService,
    DotFavoritePageService,
    DotPageRenderService,
    DotPageStateService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver
} from '@dotcms/portlets/dot-experiments/data-access';
import {
    DotEnterpriseLicenseResolver,
    DotPushPublishEnvironmentsResolver,
    portletHaveLicenseResolver
} from '@dotcms/ui';

import { DotEmaShellComponent } from './dot-ema-shell/dot-ema-shell.component';
import { editEmaGuard } from './services/guards/edit-ema.guard';

export const DotEmaRoutes: Route[] = [
    {
        path: '',
        canActivate: [editEmaGuard],
        component: DotEmaShellComponent,
        providers: [
            DotEditPageResolver,
            DotPageStateService,
            DotContentletLockerService,
            DotPageRenderService,
            DotFavoritePageService,
            DotESContentService,
            DotExperimentsService
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
                    DotEnterpriseLicenseResolver,
                    DotExperimentExperimentResolver,
                    DotPushPublishEnvironmentsResolver,
                    DotExperimentsConfigResolver
                ],
                loadChildren: async () =>
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
