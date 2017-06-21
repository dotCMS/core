import { ActionHeaderModule } from './action-header/action-header.module';
import { CommonModule } from '@angular/common';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormsModule } from '@angular/forms';
import { FormatDateService } from '../../../api/services/format-date-service';
import { ListingDataTableComponent } from './listing-data-table.component';
import { LoggerService } from '../../../api/services/logger.service';
import { MessageService } from '../../../api/services/messages-service';
import { NgModule } from '@angular/core';
import { ConfirmDialogModule, DataTableModule, InputTextModule, SplitButtonModule} from 'primeng/primeng';

@NgModule({
    declarations: [
        ListingDataTableComponent
    ],
    exports: [
        ListingDataTableComponent
    ],
    imports: [
        ActionHeaderModule,
        CommonModule,
        DataTableModule,
        FormsModule,
        InputTextModule,
    ],
    providers: [
        CrudService,
        FormatDateService,
        DotcmsConfig,
        LoggerService,
        MessageService,
    ]
})
export class ListingDataTableModule {}
