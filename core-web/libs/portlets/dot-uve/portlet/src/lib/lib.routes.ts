import { Route } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    CanDeactivateGuardService,
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotLanguagesService,
    DotLicenseService,
    DotPageLayoutService,
    DotPropertiesService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver
} from '@dotcms/portlets/dot-experiments/data-access';
import {
    DotEnterpriseLicenseResolver,
    DotPushPublishEnvironmentsResolver,
    portletHaveLicenseResolver
} from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import { DotUveShellComponent } from './dot-uve-shell/dot-uve-shell.component';
import { DotActionUrlService } from './services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from './services/dot-page-api.service';
import { dotUveGuard } from './services/guards/dot-uve.guard';
import { UVEStore } from './store/dot-uve.store';

export const dotUveRoutes: Route[] = [
    {
        path: '',
        canActivate: [dotUveGuard],
        component: DotUveShellComponent,
        providers: [
            // UVEStore and its direct dependencies (needed for store to persist across child routes)
            UVEStore,
            DotPageApiService,
            DotActionUrlService,
            DotLanguagesService,
            DotWorkflowsActionsService,
            DotPageLayoutService,
            DotAnalyticsTrackerService,
            DotPropertiesService,
            // DotMessageService is providedIn: 'root', so it's available globally
            DotLicenseService,
            LoginService,
            MessageService,
            ConfirmationService,
            DialogService,
            DotContentletLockerService,
            DotESContentService,
            DotExperimentsService,
            DotFavoritePageService,
            DotSeoMetaTagsService,
            DotSeoMetaTagsUtilService,
            {
                provide: WINDOW,
                useValue: window
            }
        ],
        resolve: {
            haveLicense: portletHaveLicenseResolver
        },
        children: [
            {
                path: 'content',
                loadComponent: () =>
                    import('./dot-uve-editor/dot-uve-editor.component').then(
                        (mod) => mod.DotUveEditorComponent
                    )
            },
            {
                path: 'layout',
                loadComponent: () =>
                    import('./dot-uve-layout/dot-uve-layout.component').then(
                        (mod) => mod.DotUveLayoutComponent
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
                    (await import('@dotcms/portlets/dot-experiments/portlet')).dotExperimentsRoutes
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
