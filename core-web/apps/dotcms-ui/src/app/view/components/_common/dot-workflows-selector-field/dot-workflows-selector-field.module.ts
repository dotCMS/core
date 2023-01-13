import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

@NgModule({
    imports: [CommonModule, MultiSelectModule, FormsModule, DotPipesModule],
    declarations: [DotWorkflowsSelectorFieldComponent],
    exports: [DotWorkflowsSelectorFieldComponent]
})
export class DotWorkflowsSelectorFieldModule {}
