import { Component, AfterViewInit, OnInit } from '@angular/core';
import { DotWorkflowTaskDetailService } from '../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '../../api/services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template: '<dot-workflow-task-detail (close)="onCloseWorkflowTaskEditor()"></dot-workflow-task-detail>'
})
export class DotWorkflowTaskComponent implements AfterViewInit, OnInit {
    constructor(
        private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService,
        private dotMessageService: DotMessageService,
        private dotNavigationService: DotNavigationService,
        private route: ActivatedRoute
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages(['workflow.task.dialog.header'])
            .pipe(take(1))
            .subscribe();
    }

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.dotWorkflowTaskDetailService.view({
                header: this.dotMessageService.get('workflow.task.dialog.header'),
                id: this.route.snapshot.params.id
            });
        }, 0);
    }

    /**
     * Handle close event from the iframe
     *
     * @memberof DotWorkflowTaskComponent
     */
    onCloseWorkflowTaskEditor(): void {
        this.dotNavigationService.goToFirstPortlet();
    }
}
