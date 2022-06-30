import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPortletDetailComponent } from './dot-portlet-detail.component';
import { Routes, RouterModule } from '@angular/router';
import { DotWorkflowTaskModule } from './dot-workflow-task/dot-workflow-task.module';
import { DotContentletsModule } from './dot-contentlets/dot-contentlets.module';

const routes: Routes = [
    {
        component: DotPortletDetailComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        DotWorkflowTaskModule,
        DotContentletsModule,
        RouterModule.forChild(routes)
    ],
    declarations: [DotPortletDetailComponent]
})
export class DotPortletDetailModule {}
