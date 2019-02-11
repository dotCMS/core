import { ActionHeaderModule } from './action-header/action-header.module';
import { CommonModule } from '@angular/common';
import { CrudService } from '@services/crud/crud.service';
import { DotcmsConfig, LoggerService } from 'dotcms-js';
import { FormsModule } from '@angular/forms';
import { FormatDateService } from '@services/format-date-service';
import { ListingDataTableComponent } from './listing-data-table.component';
import { DotMessageService } from '@services/dot-messages-service';
import { NgModule } from '@angular/core';
import { DataTableModule, InputTextModule } from 'primeng/primeng';
import { TableModule } from 'primeng/table';
import { ActionMenuButtonModule } from '../_common/action-menu-button/action-menu-button.module';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';

@NgModule({
    declarations: [ListingDataTableComponent],
    exports: [ListingDataTableComponent],
    imports: [
        ActionHeaderModule,
        CommonModule,
        DataTableModule,
        TableModule,
        FormsModule,
        InputTextModule,
        ActionMenuButtonModule,
        DotIconModule
    ],
    providers: [CrudService, FormatDateService, DotcmsConfig, LoggerService, DotMessageService]
})
export class ListingDataTableModule {}
