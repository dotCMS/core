import { Routes } from '@angular/router';

import { ExperimentsConfigProperties } from '@dotcms/dotcms-models';
import { DotExperimentsConfigResolver } from '@dotcms/portlets/dot-experiments/data-access';
import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotExperimentsAnalyticAppMisconfigurationComponent } from './dot-experiments-analytic-app-misconfiguration/dot-experiments-analytic-app-misconfiguration.component';
import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration/dot-experiments-configuration.component';
import { DotExperimentsListComponent } from './dot-experiments-list/dot-experiments-list.component';
import { DotExperimentsReportsComponent } from './dot-experiments-reports/dot-experiments-reports.component';
import { DotExperimentsShellComponent } from './dot-experiments-shell/dot-experiments-shell.component';
import { AnalyticsAppGuard } from './shared/guards/dot-experiments-analytic-app.guard';

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
        canActivateChild: [AnalyticsAppGuard],
        children: [
            {
                path: '',
                title: 'experiment.container.list.title',
                component: DotExperimentsListComponent
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
                component: DotExperimentsConfigurationComponent
            },
            {
                path: ':experimentId/reports',
                title: 'experiment.container.report.title',
                component: DotExperimentsReportsComponent
            }
        ]
    },
    {
        path: '**',
        redirectTo: 'analytic-app-misconfiguration',
        pathMatch: 'full'
    }
];
