import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

@NgModule({
    imports: [CommonModule, MultiSelectModule, FormsModule, DotPipesModule, DotMessagePipe],
    declarations: [DotWorkflowsSelectorFieldComponent],
    exports: [DotWorkflowsSelectorFieldComponent]
})
export class DotWorkflowsSelectorFieldModule {}
