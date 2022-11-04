import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

const routes: Routes = [
    {
        path: '',
        component: DotExperimentsShellComponent,

        children: [
            {
                path: '',
                title: 'Experiments List',
                loadChildren: async () =>
                    (await import('../dot-experiments-list/dot-experiments-list.module'))
                        .DotExperimentsListModule
            },
            {
                path: 'configuration',
                title: 'Experiment Configuration',
                loadChildren: async () =>
                    (
                        await import(
                            '../dot-experiments-configuration/dot-experiments-configuration.module'
                        )
                    ).DotExperimentsConfigurationModule
            }
        ]
    }
];

@NgModule({
    imports: [CommonModule, RouterModule.forChild(routes)]
})
export class DotExperimentsShellRoutingModule {}
