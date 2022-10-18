import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule, Routes } from '@angular/router';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

const routes: Routes = [
    {
        path: '',
        component: DotExperimentsShellComponent,
        children: [
            {
                path: '',
                loadChildren: async () =>
                    (await import('../dot-experiments-list/dot-experiments-list.module'))
                        .DotExperimentsListModule
            }
        ]
    }
];

@NgModule({
    imports: [CommonModule, RouterModule.forChild(routes), HttpClientModule]
})
export class DotExperimentsShellRoutingModule {}
