import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row.component';
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
        DotPipesModule
    ],
    exports: [DotKeyValueTableInputRowComponent],
    declarations: [DotKeyValueTableInputRowComponent]
})
export class DotKeyValueTableInputRowModule {}
