import { CommonModule } from '@angular/common';
import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { DotWorkflowTaskDetailModule } from '../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';

const routes: Routes = [
    {
        component: DotWorkflowTaskComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotWorkflowTaskComponent],
    imports: [CommonModule, DotWorkflowTaskDetailModule, RouterModule.forChild(routes)],
    exports: [],
    providers: []
})
export class DotWorkflowTaskModule {}
