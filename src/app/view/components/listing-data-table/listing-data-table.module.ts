import { ActionHeaderComponent } from './action-header/action-header';
import { ActionButtonModule } from '../_common/action-button/action-button.module';
import { CommonModule } from '@angular/common';
import { CrudService } from '../../../api/services/crud-service';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormsModule } from '@angular/forms';
import { ListingDataTableComponent } from './listing-data-table-component';
import { LoggerService } from '../../../api/services/logger.service';
import { MessageService } from '../../../api/services/messages-service';
import { NgModule } from '@angular/core';
import { ConfirmDialogModule, DataTableModule, InputTextModule, SplitButtonModule} from 'primeng/primeng';
import { ActionButtonComponent } from '../_common/action-button/action-button.component';

@NgModule({
    declarations: [
        ActionHeaderComponent,
        ListingDataTableComponent,
        ActionButtonComponent
    ],
    exports: [
        ListingDataTableComponent
    ],
    imports: [
        ActionButtonModule,
        CommonModule,
        ConfirmDialogModule,
        DataTableModule,
        DataTableModule,
        FormsModule,
        InputTextModule,
        SplitButtonModule
    ],
    providers: [
        CrudService,
        DotcmsConfig,
        LoggerService,
        MessageService,
    ]
})
export class ListingDataTableModule {}
