import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { RouterModule } from '@angular/router';
import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsShellRoutingModule } from '@portlets/dot-experiments/dot-experiments-shell/dot-experiments-shell-routing.module';
import { DotExperimentsListModule } from '@portlets/dot-experiments/dot-experiments-list/dot-experiments-list.module';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DotExperimentsShellStore } from './store/dot-experiments-shell-store.service';

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
    providers: [DotExperimentsShellStore, MessageService]
})
export class DotExperimentsShellModule {}
