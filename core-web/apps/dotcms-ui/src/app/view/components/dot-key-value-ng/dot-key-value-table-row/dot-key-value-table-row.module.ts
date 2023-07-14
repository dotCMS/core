import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
        TableModule,
        UiDotIconButtonModule,
        DotPipesModule,
        DotMessagePipe
    ],
    exports: [DotKeyValueTableRowComponent],
    declarations: [DotKeyValueTableRowComponent]
})
export class DotKeyValueTableRowModule {}
