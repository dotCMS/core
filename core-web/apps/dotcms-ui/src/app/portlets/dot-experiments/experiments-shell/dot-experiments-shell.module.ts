import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { RouterModule, Routes } from '@angular/router';
import { DotExperimentsListModule } from '../experiments-list/dot-experiments-list.module';
import { DotExperimentsListComponent } from '../experiments-list/dot-experiments-list.component';
import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotExperimentsStore } from '../shared/stores/dot-experiments-store.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';

const routes: Routes = [
    { path: '', redirectTo: 'list', pathMatch: 'full' },
    {
        path: '',
        component: DotExperimentsShellComponent,
        children: [
            {
                path: 'list',
                component: DotExperimentsListComponent
            }
        ]
    },

    { path: '**', pathMatch: 'full', redirectTo: 'list' }
];

@NgModule({
    declarations: [DotExperimentsShellComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(routes),
        DotLoadingIndicatorModule,

        DotExperimentsListModule
    ],
    providers: [DotExperimentsStore]
})
export class DotExperimentsShellModule {}
