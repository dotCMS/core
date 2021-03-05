import { Component, OnInit } from '@angular/core';
import { DotWorkflowTaskDetailService } from '@components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template:
        '<dot-workflow-task-detail (close)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)"></dot-workflow-task-detail>'
})
export class DotWorkflowTaskComponent implements OnInit {
    constructor(
        private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        private dotIframeService: DotIframeService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService
    ) {}

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
            $event.detail.data.callback = 'fileActionCallbackFromAngular';
            this.dotCustomEventHandlerService.handle($event);
        }
    }
}
