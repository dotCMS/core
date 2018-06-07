import { CommonModule } from '@angular/common';


import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';

const routes: Routes = [
    {
        component: DotWorkflowTaskComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotWorkflowTaskComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(routes),
    ],
    exports: [],
    providers: []
})
export class DotWorkflowTaskModule {}
