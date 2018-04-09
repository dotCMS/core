import { MultiSelectModule } from 'primeng/primeng';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [CommonModule, MultiSelectModule, FormsModule],
    declarations: [DotWorkflowsSelectorFieldComponent],
    exports: [DotWorkflowsSelectorFieldComponent]
})
export class DotWorkflowsSelectorFieldModule {}
