import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { MenuModule } from 'primeng/menu';
import { MultiSelectModule } from 'primeng/multiselect';
import { RippleModule } from 'primeng/ripple';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotRelativeDatePipe } from '@pipes/dot-relative-date/dot-relative-date.pipe';
import { DotExperimentsListRoutingModule } from '@portlets/dot-experiments/dot-experiments-list/dot-experiments-list-routing.module';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

import { DotExperimentsEmptyExperimentsComponent } from './components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from './components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsListTableComponent } from './components/dot-experiments-list-table/dot-experiments-list-table.component';
import { DotExperimentsStatusFilterComponent } from './components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { DotExperimentsListComponent } from './dot-experiments-list.component';

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
        DotDropdownDirective,

        // PrimeNG
        DotDynamicDirective,
        SkeletonModule,
        ButtonModule,
        MultiSelectModule,
        TableModule,
        MenuModule,
        ConfirmDialogModule,
        ConfirmPopupModule,
        ToastModule,
        DotRelativeDatePipe,
        RippleModule
    ],
    providers: [DotExperimentsService]
})
export class DotExperimentsListModule {}
