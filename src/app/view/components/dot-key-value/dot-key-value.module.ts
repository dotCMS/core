import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { DotKeyValueComponent } from './dot-key-value.component';
import { DotKeyValueTableRowModule } from '@components/dot-key-value/dot-key-value-table-row/dot-key-value-table-row.module';
import { DotKeyValueTableInputRowModule } from './dot-key-value-table-input-row/dot-key-value-table-input-row.module';

@NgModule({
    imports: [
        CommonModule,
        TableModule,
        DotKeyValueTableInputRowModule,
        DotKeyValueTableRowModule
    ],
    exports: [DotKeyValueComponent],
    providers: [],
    declarations: [DotKeyValueComponent]
})
export class DotKeyValueModule {}
