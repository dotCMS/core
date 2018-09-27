import { Component, AfterViewInit } from '@angular/core';
import { DotWorkflowTaskDetailService } from '@components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template:
        '<dot-workflow-task-detail (close)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)"></dot-workflow-task-detail>'
})
export class DotWorkflowTaskComponent implements AfterViewInit {
    constructor(
        private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        private dotIframeService: DotIframeService
    ) {}

    ngAfterViewInit(): void {
        this.dotMessageService
            .getMessages(['workflow.task.dialog.header'])
            .pipe(take(1))
            .subscribe(() => {
                this.dotWorkflowTaskDetailService.view({
                    header: this.dotMessageService.get('workflow.task.dialog.header'),
                    id: this.route.snapshot.params.asset
                });
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
     * Habdle custom event from the dot-workflow-task-detail
     *
     * @param {CustomEvent} $event
     * @memberof DotWorkflowTaskComponent
     */
    onCustomEvent($event: CustomEvent): void {
        if ($event.detail.name === 'edit-task-executed-workflow') {
            this.onCloseWorkflowTaskEditor();
        }
    }
}
