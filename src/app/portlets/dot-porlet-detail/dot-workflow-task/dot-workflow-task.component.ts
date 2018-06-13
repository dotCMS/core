import { Component, AfterViewInit } from '@angular/core';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template: '<dot-workflow-task-detail (close)="onCloseWorkflowTaskEditor()"></dot-workflow-task-detail>'
})
export class DotWorkflowTaskComponent implements AfterViewInit {
    constructor(
        private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute
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
     * Handle close event from the iframe
     *
     * @memberof DotWorkflowTaskComponent
     */
    onCloseWorkflowTaskEditor(): void {
        this.dotRouterService.gotoPortlet('/c/workflow');
    }
}
