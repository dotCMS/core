import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotWorkflowsSelectorFieldComponent } from './dot-workflows-selector-field.component';

@NgModule({
    imports: [CommonModule, MultiSelectModule, FormsModule, DotSafeHtmlPipe, DotMessagePipe],
    declarations: [DotWorkflowsSelectorFieldComponent],
    exports: [DotWorkflowsSelectorFieldComponent]
})
export class DotWorkflowsSelectorFieldModule {}
