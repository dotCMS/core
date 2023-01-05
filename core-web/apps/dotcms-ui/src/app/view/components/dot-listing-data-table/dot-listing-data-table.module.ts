import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DotCrudService } from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenuModule } from 'primeng/contextmenu';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { DotActionMenuButtonModule } from '../_common/dot-action-menu-button/dot-action-menu-button.module';
import { ActionHeaderModule } from './action-header/action-header.module';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';

@NgModule({
    declarations: [DotListingDataTableComponent],
    exports: [DotListingDataTableComponent],
    imports: [
        ActionHeaderModule,
        CommonModule,
        TableModule,
        FormsModule,
        InputTextModule,
        DotActionMenuButtonModule,
        DotIconModule,
        RouterModule,
        DotPipesModule,
        CheckboxModule,
        ContextMenuModule
    ],
    providers: [DotCrudService, DotcmsConfigService, LoggerService]
})
export class DotListingDataTableModule {}
