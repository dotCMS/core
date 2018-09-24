import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';
import { DotWorkflowTaskDetailComponent } from './dot-workflow-task-detail.component';

@NgModule({
    imports: [CommonModule, DotIframeDialogModule],
    declarations: [DotWorkflowTaskDetailComponent],
    exports: [DotWorkflowTaskDetailComponent],
    providers: [DotWorkflowTaskDetailService]
})
export class DotWorkflowTaskDetailModule {}
