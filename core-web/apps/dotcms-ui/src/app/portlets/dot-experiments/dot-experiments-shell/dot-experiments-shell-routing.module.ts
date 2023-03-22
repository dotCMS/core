import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ServerErrorInterceptor } from '@shared/interceptors/server-error.interceptor';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

const routes: Routes = [
    {
        path: '',
        component: DotExperimentsShellComponent,

        children: [
            {
                path: '',
                title: 'experiment.container.list.title',
                loadChildren: async () =>
                    (await import('../dot-experiments-list/dot-experiments-list.module'))
                        .DotExperimentsListModule
            },
            {
                path: 'configuration',
                title: 'experiment.container.configuration.title',
                loadChildren: async () =>
                    (
                        await import(
                            '../dot-experiments-configuration/dot-experiments-configuration.module'
                        )
                    ).DotExperimentsConfigurationModule
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

@NgModule({
    imports: [CommonModule, RouterModule.forChild(routes)],
    providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: ServerErrorInterceptor,
            multi: true
        }
    ]
})
export class DotExperimentsShellRoutingModule {}
