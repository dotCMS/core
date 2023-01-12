import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';

import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';

import { DotAddVariableComponent } from './dot-add-variable.component';

@NgModule({
    declarations: [DotAddVariableComponent],
    imports: [CommonModule, DotMessagePipeModule, ButtonModule, DataViewModule],
    exports: [DotAddVariableComponent]
})
export class DotAddVariableModule {}
