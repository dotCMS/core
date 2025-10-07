import { Observable } from 'rxjs';

import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';

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
    templateUrl: './dot-workflow-task-detail.component.html',
    standalone: false
})
export class DotWorkflowTaskDetailComponent implements OnInit {
    private dotWorkflowTaskDetailService = inject(DotWorkflowTaskDetailService);

    @Output()
    shutdown: EventEmitter<unknown> = new EventEmitter();

    @Output()
    custom: EventEmitter<CustomEvent<Record<string, unknown>>> = new EventEmitter();

    url$: Observable<string>;
    header$: Observable<string>;

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
        this.shutdown.emit();
    }
}
