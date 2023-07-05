import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotWorkflowsActionsService, DotWorkflowService } from '@dotcms/data-access';

import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';

@NgModule({
    imports: [CommonModule, ButtonModule, MenuModule],
    exports: [DotEditPageWorkflowsActionsComponent],
    declarations: [DotEditPageWorkflowsActionsComponent],
    providers: [DotWorkflowsActionsService, DotWorkflowService, DotWorkflowEventHandlerService]
})
export class DotEditPageWorkflowsActionsModule {}
