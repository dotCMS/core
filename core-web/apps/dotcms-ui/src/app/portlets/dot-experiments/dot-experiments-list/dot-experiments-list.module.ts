import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

//PrimeNg
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { TableModule } from 'primeng/table';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ToastModule } from 'primeng/toast';

import { DotIconModule } from '@dotcms/ui';
import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotExperimentsListSkeletonComponent } from './components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsEmptyExperimentsComponent } from './components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsStatusFilterComponent } from './components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { FormsModule } from '@angular/forms';
import { DotExperimentsListTableComponent } from './components/dot-experiments-list-table/dot-experiments-list-table.component';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { MenuModule } from 'primeng/menu';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotExperimentsListRoutingModule } from '@portlets/dot-experiments/dot-experiments-list/dot-experiments-list-routing.module';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@NgModule({
    declarations: [
        DotExperimentsListComponent,
        DotExperimentsListSkeletonComponent,
        DotExperimentsEmptyExperimentsComponent,
        DotExperimentsStatusFilterComponent,
        DotExperimentsListTableComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        // DotCMS
        DotExperimentsListRoutingModule,
        DotIconModule,
        UiDotIconButtonTooltipModule,
        DotActionMenuButtonModule,
        UiDotIconButtonModule,
        DotMessagePipeModule,
        DotExperimentsUiHeaderComponent,

        // PrimeNG
        DotDynamicDirective,
        SkeletonModule,
        ButtonModule,
        MultiSelectModule,
        TableModule,
        MenuModule,
        ConfirmDialogModule,
        ConfirmPopupModule,
        ToastModule
    ],
    providers: [DotExperimentsService]
})
export class DotExperimentsListModule {}
