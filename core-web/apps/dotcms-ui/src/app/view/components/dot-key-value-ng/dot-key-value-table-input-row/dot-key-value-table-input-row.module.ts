import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
        DotPipesModule,
        DotMessagePipe
    ],
    exports: [DotKeyValueTableInputRowComponent],
    declarations: [DotKeyValueTableInputRowComponent]
})
export class DotKeyValueTableInputRowModule {}
