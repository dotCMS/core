import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotMessageService, DotRouterService, DotIframeService } from '@dotcms/data-access';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

@Component({
    selector: 'dot-workflow-task',
    template:
        '<dot-workflow-task-detail (shutdown)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)"></dot-workflow-task-detail>',
    imports: [DotWorkflowTaskDetailComponent]
})
export class DotWorkflowTaskComponent implements OnInit {
    private dotWorkflowTaskDetailService = inject(DotWorkflowTaskDetailService);
    private dotMessageService = inject(DotMessageService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    private dotIframeService = inject(DotIframeService);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);

    ngOnInit() {
        this.dotWorkflowTaskDetailService.view({
            header: this.dotMessageService.get('workflow.task.dialog.header'),
            id: this.route.snapshot.params.asset
        });
    }

    /**
     * Handle close event from the dot-workflow-task-detail
     *
     * @memberof DotWorkflowTaskComponent
     */
    onCloseWorkflowTaskEditor(): void {
        this.dotRouterService.gotoPortlet('/c/workflow');
        this.dotIframeService.reloadData('workflow');
    }

    /**
     * Handle custom event from the dot-workflow-task-detail
     *
     * @param CustomEvent $event
     * @memberof DotWorkflowTaskComponent
     */
    onCustomEvent($event: CustomEvent): void {
        if (['edit-task-executed-workflow', 'close'].includes($event.detail.name)) {
            this.onCloseWorkflowTaskEditor();
        } else {
            this.dotCustomEventHandlerService.handle($event);
        }
    }
}
