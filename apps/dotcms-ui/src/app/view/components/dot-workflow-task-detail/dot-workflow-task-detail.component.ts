import { Component, EventEmitter, Output, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';

/**
 * Allow user to view a workflow task to a DotCMS instance
 *
 * @export
 * @class DotWorkflowTaskDetailComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-workflow-task-detail',
    templateUrl: './dot-workflow-task-detail.component.html'
})
export class DotWorkflowTaskDetailComponent implements OnInit {
    @Output()
    close: EventEmitter<any> = new EventEmitter();

    @Output()
    custom: EventEmitter<any> = new EventEmitter();

    url$: Observable<string>;
    header$: Observable<string>;

    constructor(private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService) {}

    ngOnInit() {
        this.url$ = this.dotWorkflowTaskDetailService.viewUrl$;
        this.header$ = this.dotWorkflowTaskDetailService.header$;
    }

    /**
     * Handle close dialog event
     *
     * @memberof DotWorkflowTaskDetailComponent
     */
    onClose(): void {
        this.dotWorkflowTaskDetailService.clear();
        this.close.emit();
    }
}
