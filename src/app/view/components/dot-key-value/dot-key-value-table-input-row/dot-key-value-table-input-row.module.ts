import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule, InputTextModule, InputSwitchModule } from 'primeng/primeng';
import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
    ],
    exports: [DotKeyValueTableInputRowComponent],
    declarations: [DotKeyValueTableInputRowComponent]
})
export class DotKeyValueTableInputRowModule {}
