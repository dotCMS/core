import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { RouterModule, Routes } from '@angular/router';
import { DotExperimentsListModule } from '../dot-experiments-list/dot-experiments-list.module';
import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotExperimentsStore } from '../shared/stores/dot-experiments-store.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';

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
