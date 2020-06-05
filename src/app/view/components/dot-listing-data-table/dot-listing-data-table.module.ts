import { ActionHeaderModule } from './action-header/action-header.module';
import { CommonModule } from '@angular/common';
import { DotCrudService } from '@services/dot-crud/dot-crud.service';
import { DotcmsConfigService, LoggerService } from 'dotcms-js';
import { FormsModule } from '@angular/forms';
import { FormatDateService } from '@services/format-date-service';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';
import { NgModule } from '@angular/core';
import { DataTableModule, InputTextModule } from 'primeng/primeng';
import { TableModule } from 'primeng/table';
import { ActionMenuButtonModule } from '../_common/action-menu-button/action-menu-button.module';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';
import { RouterModule } from '@angular/router';
import { DotStringFormatPipe } from '@pipes/dot-string-format/dot-string-format.pipe';

@NgModule({
    declarations: [DotListingDataTableComponent, DotStringFormatPipe],
    exports: [DotListingDataTableComponent],
    imports: [
        ActionHeaderModule,
        CommonModule,
        DataTableModule,
        TableModule,
        FormsModule,
        InputTextModule,
        ActionMenuButtonModule,
        DotIconModule,
        RouterModule
    ],
    providers: [
        DotCrudService,
        FormatDateService,
        DotcmsConfigService,
        LoggerService
    ]
})
export class DotListingDataTableModule {}
