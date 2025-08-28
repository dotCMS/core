import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

import { DotWorkflowTaskDetailModule } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';

@NgModule({
    declarations: [DotWorkflowTaskComponent],
    imports: [CommonModule, DotWorkflowTaskDetailModule],
    exports: [DotWorkflowTaskComponent],
    providers: []
})
export class DotWorkflowTaskModule {}
