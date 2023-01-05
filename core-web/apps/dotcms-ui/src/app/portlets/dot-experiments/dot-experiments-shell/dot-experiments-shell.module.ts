import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { RouterModule } from '@angular/router';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotExperimentsListModule } from '@portlets/dot-experiments/dot-experiments-list/dot-experiments-list.module';
import { DotExperimentsShellRoutingModule } from '@portlets/dot-experiments/dot-experiments-shell/dot-experiments-shell-routing.module';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

@NgModule({
    declarations: [DotExperimentsShellComponent],
    imports: [
        CommonModule,
        RouterModule,

        DotExperimentsShellRoutingModule,
        DotLoadingIndicatorModule,
        DotExperimentsListModule,
        DotExperimentsUiHeaderComponent,

        // PrimeNg
        ToastModule
    ],
    providers: [MessageService]
})
export class DotExperimentsShellModule {}
