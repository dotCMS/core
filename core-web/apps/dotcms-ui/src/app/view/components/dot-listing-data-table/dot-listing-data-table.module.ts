import { ActionHeaderModule } from './action-header/action-header.module';
import { CommonModule } from '@angular/common';
import { DotCrudService } from '@services/dot-crud/dot-crud.service';
import { DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';
import { FormsModule } from '@angular/forms';
import { DotListingDataTableComponent } from './dot-listing-data-table.component';
import { NgModule } from '@angular/core';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { DotActionMenuButtonModule } from '../_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotIconModule } from '@dotcms/ui';
import { RouterModule } from '@angular/router';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenuModule } from 'primeng/contextmenu';

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
