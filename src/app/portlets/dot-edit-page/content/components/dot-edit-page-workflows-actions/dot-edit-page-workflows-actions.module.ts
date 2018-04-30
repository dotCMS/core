import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { ButtonModule, MenuModule } from 'primeng/primeng';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        MenuModule
    ],
    exports: [DotEditPageWorkflowsActionsComponent],
    declarations: [DotEditPageWorkflowsActionsComponent]
})
export class DotEditPageWorkflowsActionsModule {}
