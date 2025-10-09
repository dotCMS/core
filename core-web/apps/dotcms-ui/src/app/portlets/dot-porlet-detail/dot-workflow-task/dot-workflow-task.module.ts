import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';

@NgModule({
    declarations: [DotWorkflowTaskComponent],
    imports: [CommonModule, DotWorkflowTaskDetailComponent],
    exports: [DotWorkflowTaskComponent],
    providers: []
})
export class DotWorkflowTaskModule {}
