import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowTaskDetailComponent } from './dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';

import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';

@NgModule({
    imports: [CommonModule, DotIframeDialogModule],
    declarations: [DotWorkflowTaskDetailComponent],
    exports: [DotWorkflowTaskDetailComponent],
    providers: [DotWorkflowTaskDetailService]
})
export class DotWorkflowTaskDetailModule {}
