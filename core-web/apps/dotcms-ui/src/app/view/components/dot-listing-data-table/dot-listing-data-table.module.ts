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
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { ActionHeaderModule } from './action-header/action-header.module';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';

import { DotRelativeDatePipe } from '../../pipes/dot-relative-date/dot-relative-date.pipe';
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
        DotPipesModule,
        CheckboxModule,
        ContextMenuModule,
        DotMessagePipe
    ],
    providers: [DotCrudService, DotcmsConfigService, LoggerService]
})
export class DotListingDataTableModule {}
