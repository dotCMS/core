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
    DotIconModule,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotSafeHtmlPipe,
    DotStringFormatPipe
} from '@dotcms/ui';

import { ActionHeaderModule } from './action-header/action-header.module';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';

import { DotActionMenuButtonModule } from '../_common/dot-action-menu-button/dot-action-menu-button.module';

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
        DotActionMenuButtonModule,
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
