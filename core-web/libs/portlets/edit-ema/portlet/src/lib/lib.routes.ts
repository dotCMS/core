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

import { DotEmaShellComponent } from './dot-ema-shell/dot-ema-shell.component';
import { DotActionUrlService } from './services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from './services/dot-page-api.service';
import { editEmaGuard } from './services/guards/edit-ema.guard';
import { UVEStore } from './store/dot-uve.store';

export const DotEmaRoutes: Route[] = [
    {
        path: '',
        canActivate: [editEmaGuard],
        component: DotEmaShellComponent,
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
