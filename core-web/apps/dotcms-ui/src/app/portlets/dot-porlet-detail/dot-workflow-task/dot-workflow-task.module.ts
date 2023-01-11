import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowTaskDetailModule } from '@components/dot-workflow-task-detail/dot-workflow-task-detail.module';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

@NgModule({
    declarations: [DotWorkflowTaskComponent],
    imports: [CommonModule, DotWorkflowTaskDetailModule],
    exports: [DotWorkflowTaskComponent],
    providers: []
})
export class DotWorkflowTaskModule {}
