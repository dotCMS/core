import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenuModule } from 'primeng/contextmenu';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotCrudService } from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';
import {
    DotActionMenuButtonComponent,
    DotIconModule,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotSafeHtmlPipe,
    DotStringFormatPipe
} from '@dotcms/ui';

import { ActionHeaderModule } from './action-header/action-header.module';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';

@NgModule({
    declarations: [DotListingDataTableComponent],
    exports: [DotListingDataTableComponent],
    imports: [
        ActionHeaderModule,
        CommonModule,
        DotRelativeDatePipe,
        TableModule,
        FormsModule,
        InputTextModule,
        DotActionMenuButtonComponent,
        DotIconModule,
        RouterModule,
        DotSafeHtmlPipe,
        CheckboxModule,
        ContextMenuModule,
        DotMessagePipe,
        DotStringFormatPipe
    ],
    providers: [DotCrudService, DotcmsConfigService, LoggerService]
})
export class DotListingDataTableModule {}
