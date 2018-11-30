import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
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
        DotIconButtonModule
    ],
    exports: [DotContentTypeFieldsVariablesTableRowComponent],
    declarations: [DotContentTypeFieldsVariablesTableRowComponent]
})
export class DotContentTypeFieldsVariablesTableRowModule {}
