import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
// import { DotFieldVariablesService } from '../service/dot-field-variables.service';
// import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule, InputTextModule } from 'primeng/primeng';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotContentTypeFieldsVariablesTableRowComponent } from './dot-content-type-fields-variables-table-row.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        InputTextModule,
        FormsModule,
        TableModule,
        // DotActionButtonModule,
        DotIconButtonModule
    ],
    exports: [DotContentTypeFieldsVariablesTableRowComponent],
    // providers: [DotFieldVariablesService],
    declarations: [DotContentTypeFieldsVariablesTableRowComponent]
})
export class DotContentTypeFieldsVariablesTableRowModule {}
