import { ActionHeaderModule } from './action-header/action-header.module';
import { CommonModule } from '@angular/common';
import { CrudService } from '../../../api/services/crud';
import { DataTableModule, InputTextModule} from 'primeng/primeng';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormsModule } from '@angular/forms';
import { ListingDataTableComponent } from './listing-data-table.component';
import { LoggerService } from '../../../api/services/logger.service';
import { MessageService } from '../../../api/services/messages-service';
import { NgModule } from '@angular/core';

@NgModule({
    declarations: [
        ListingDataTableComponent,
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
        DotcmsConfig,
        LoggerService,
        MessageService,
    ]
})
export class ListingDataTableModule {}
