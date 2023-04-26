import { Routes } from '@angular/router';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

export const DotExperimentsShellRoutes: Routes = [
    {
        path: '',
        component: DotExperimentsShellComponent,

        children: [
            {
                path: ':pageId',
                title: 'experiment.container.list.title',
                loadComponent: async () =>
                    (await import('../dot-experiments-list/dot-experiments-list.component'))
                        .DotExperimentsListComponent
            },
            {
                path: 'configuration/:pageId/:experimentId',
                title: 'experiment.container.configuration.title',
                loadComponent: async () =>
                    (
                        await import(
                            '../dot-experiments-configuration/dot-experiments-configuration.component'
                        )
                    ).DotExperimentsConfigurationComponent
            },
            {
                path: 'reports',
                title: 'experiment.container.report.title',
                loadChildren: async () =>
                    (await import('../dot-experiments-reports/dot-experiments-reports.routes'))
                        .DotExperimentsReportsRoutes
            }
        ]
    }
];
