import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotContentletsModule } from './dot-contentlets/dot-contentlets.module';
import { DotPortletDetailComponent } from './dot-portlet-detail.component';
import { DotWorkflowTaskModule } from './dot-workflow-task/dot-workflow-task.module';

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
