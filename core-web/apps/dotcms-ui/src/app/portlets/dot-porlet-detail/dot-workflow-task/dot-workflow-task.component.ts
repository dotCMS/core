import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotWorkflowTaskDetailService } from '@components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMessageService, DotRouterService, DotIframeService } from '@dotcms/data-access';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template:
        '<dot-workflow-task-detail (shutdown)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)"></dot-workflow-task-detail>',
    standalone: false
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
