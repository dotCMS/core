import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotMessagePipe } from '@dotcms/ui';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

import { DotPipesModule } from '../../../pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, MultiSelectModule, FormsModule, DotPipesModule, DotMessagePipe],
    declarations: [DotWorkflowsSelectorFieldComponent],
    exports: [DotWorkflowsSelectorFieldComponent]
})
export class DotWorkflowsSelectorFieldModule {}
