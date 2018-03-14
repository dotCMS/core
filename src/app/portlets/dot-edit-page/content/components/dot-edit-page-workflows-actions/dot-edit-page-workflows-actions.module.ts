import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { SplitButtonModule } from 'primeng/primeng';

@NgModule({
    imports: [
        CommonModule,
        SplitButtonModule,
    ],
    exports: [DotEditPageWorkflowsActionsComponent],
    declarations: [DotEditPageWorkflowsActionsComponent]
})
export class DotEditPageWorkflowsActionsModule {}
