import { Routes } from '@angular/router';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

export const DotExperimentsShellRoutes: Routes = [
    {
        path: ':pageId',
        component: DotExperimentsShellComponent,

        children: [
            {
                path: '',
                title: 'experiment.container.list.title',
                loadComponent: async () =>
                    (await import('../dot-experiments-list/dot-experiments-list.component'))
                        .DotExperimentsListComponent
            },
            {
                path: ':experimentId/configuration',
                title: 'experiment.container.configuration.title',
                loadComponent: async () =>
                    (
                        await import(
                            '../dot-experiments-configuration/dot-experiments-configuration.component'
                        )
                    ).DotExperimentsConfigurationComponent
            },
            {
                path: ':experimentId/reports',
                title: 'experiment.container.report.title',
                loadComponent: async () =>
                    (await import('../dot-experiments-reports/dot-experiments-reports.component'))
                        .DotExperimentsReportsComponent
            }
        ]
    }
];
