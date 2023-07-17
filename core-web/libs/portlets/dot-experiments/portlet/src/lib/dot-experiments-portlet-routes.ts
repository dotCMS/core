import { Routes } from '@angular/router';

import { ExperimentsConfigProperties } from '@dotcms/dotcms-models';
import { DotExperimentsConfigResolver } from '@dotcms/portlets/dot-experiments/data-access';
import {
    DotEnterpriseLicenseResolver,
    DotPushPublishEnvironmentsResolver
} from '@portlets/shared/resolvers';

import { DotExperimentsAnalyticAppMisconfigurationComponent } from './dot-experiments-analytic-app-misconfiguration/dot-experiments-analytic-app-misconfiguration.component';
import { DotExperimentsShellComponent } from './dot-experiments-shell/dot-experiments-shell.component';
import { analyticsAppGuard } from './shared/guards/dot-experiments-analytic-app.guard';

export const DotExperimentsPortletRoutes: Routes = [
    {
        path: 'analytic-app-misconfiguration',
        component: DotExperimentsAnalyticAppMisconfigurationComponent,
        title: 'experiments.container.no-analytic-app-configured.title'
    },
    {
        path: ':pageId',
        component: DotExperimentsShellComponent,
        resolve: {
            isEnterprise: DotEnterpriseLicenseResolver,
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver
        },

        canActivateChild: [analyticsAppGuard],

        children: [
            {
                path: '',
                title: 'experiment.container.list.title',
                loadComponent: async () =>
                    (await import('./dot-experiments-list/dot-experiments-list.component'))
                        .DotExperimentsListComponent
            },
            {
                path: ':experimentId/configuration',
                title: 'experiment.container.configuration.title',
                resolve: {
                    config: DotExperimentsConfigResolver
                },
                data: {
                    experimentsConfigProps: [
                        ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION,
                        ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION
                    ]
                },
                loadComponent: async () =>
                    (
                        await import(
                            './dot-experiments-configuration/dot-experiments-configuration.component'
                        )
                    ).DotExperimentsConfigurationComponent
            },
            {
                path: ':experimentId/reports',
                title: 'experiment.container.report.title',
                loadComponent: async () =>
                    (await import('./dot-experiments-reports/dot-experiments-reports.component'))
                        .DotExperimentsReportsComponent
            }
        ]
    }
];
