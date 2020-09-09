import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { ButtonModule, MenuModule } from 'primeng/primeng';
import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';

@NgModule({
    imports: [CommonModule, ButtonModule, MenuModule],
    exports: [DotEditPageWorkflowsActionsComponent],
    declarations: [DotEditPageWorkflowsActionsComponent],
    providers: [DotWorkflowsActionsService, DotWorkflowService, DotWorkflowEventHandlerService]
})
export class DotEditPageWorkflowsActionsModule {}
