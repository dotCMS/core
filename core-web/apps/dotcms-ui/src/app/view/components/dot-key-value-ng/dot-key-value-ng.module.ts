import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TableModule } from 'primeng/table';

import { DotKeyValueTableRowModule } from '@components/dot-key-value-ng/dot-key-value-table-row/dot-key-value-table-row.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotKeyValueComponent } from './dot-key-value-ng.component';
import { DotKeyValueTableInputRowModule } from './dot-key-value-table-input-row/dot-key-value-table-input-row.module';

@NgModule({
    imports: [
        CommonModule,
        TableModule,
        DotKeyValueTableInputRowModule,
        DotKeyValueTableRowModule,
        DotPipesModule
    ],
    exports: [DotKeyValueComponent],
    providers: [],
    declarations: [DotKeyValueComponent]
})
export class DotKeyValueModule {}
