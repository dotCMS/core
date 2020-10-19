import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
        TableModule,
        DotIconButtonModule,
        DotPipesModule
    ],
    exports: [DotKeyValueTableRowComponent],
    declarations: [DotKeyValueTableRowComponent]
})
export class DotKeyValueTableRowModule {}
