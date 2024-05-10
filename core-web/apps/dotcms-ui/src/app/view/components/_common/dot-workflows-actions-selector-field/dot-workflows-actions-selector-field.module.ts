import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotWorkflowsActionsSelectorFieldComponent } from './dot-workflows-actions-selector-field.component';
import { DotWorkflowsActionsSelectorFieldService } from './services/dot-workflows-actions-selector-field.service';

@NgModule({
    providers: [DotWorkflowsActionsService, DotWorkflowsActionsSelectorFieldService],
    declarations: [DotWorkflowsActionsSelectorFieldComponent],
    exports: [DotWorkflowsActionsSelectorFieldComponent],
    imports: [CommonModule, DropdownModule, FormsModule, DotSafeHtmlPipe, DotMessagePipe]
})
export class DotWorkflowsActionsSelectorFieldModule {}
