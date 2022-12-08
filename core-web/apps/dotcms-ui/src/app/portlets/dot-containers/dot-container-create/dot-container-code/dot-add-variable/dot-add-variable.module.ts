import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DotAddVariableComponent } from './dot-add-variable.component';

@NgModule({
    declarations: [DotAddVariableComponent],
    imports: [CommonModule, DotMessagePipeModule, ButtonModule, DataViewModule],
    exports: [DotAddVariableComponent]
})
export class DotAddVariableModule {}
