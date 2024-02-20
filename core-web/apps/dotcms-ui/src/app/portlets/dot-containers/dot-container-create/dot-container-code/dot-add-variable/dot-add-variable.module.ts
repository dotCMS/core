import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';

import { DotMessagePipe } from '@dotcms/ui';

import { DotAddVariableComponent } from './dot-add-variable.component';
import { DotFieldsService } from './services/dot-fields.service';

@NgModule({
    declarations: [DotAddVariableComponent],
    imports: [CommonModule, DotMessagePipe, ButtonModule, DataViewModule],
    providers: [DotFieldsService],
    exports: [DotAddVariableComponent]
})
export class DotAddVariableModule {}
