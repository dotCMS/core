import { Component, DestroyRef, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { distinctUntilChanged, map, take } from 'rxjs/operators';

import { DotMessageService, DotRouterService, DotIframeService } from '@dotcms/data-access';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

@Component({
    selector: 'dot-workflow-task',
    providers: [DotWorkflowTaskDetailService],
    template:
        '<dot-workflow-task-detail (shutdown)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)" />',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotWorkflowTaskDetailComponent]
})
export class DotWorkflowTaskComponent implements OnInit {
    private dotWorkflowTaskDetailService = inject(DotWorkflowTaskDetailService);
    private dotMessageService = inject(DotMessageService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    private dotIframeService = inject(DotIframeService);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    readonly #destroyRef = inject(DestroyRef);

    /** The task the route names. */
    #taskId = '';

    /**
     * Shows the task the route names, and the next one a navigation names: the route is reused
     * across tasks (no `reuseRoute: false`, so Back with this task's side panel open does not
     * rebuild the dialog under the panel, #38303). A navigation that keeps the task shows nothing
     * again.
     *
     * The dialog's header is set once the app's messages have loaded: on a fresh load of a task's
     * URL they are still loading, and `get` would answer with the key (#38303, T177). The task
     * itself shows at once, so the dialog opens before, and stays under, a side panel the URL
     * also names.
     */
    ngOnInit() {
        this.dotMessageService.ready$
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe(() =>
                this.dotWorkflowTaskDetailService.setHeader(
                    this.dotMessageService.get('workflow.task.dialog.header')
                )
            );

        this.route.params
            .pipe(
                map((params) => params['asset'] as string),
                distinctUntilChanged(),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((taskId) => {
                this.#taskId = taskId;
                this.#viewTask();
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
     * Handles an event the task detail JSP sent: its close events close the dialog, its "Edit
     * Content" opens the content in the side panel, and every other event goes to
     * `DotCustomEventHandlerService`.
     *
     * @param $event The detail's `ng-event`.
     */
    onCustomEvent($event: CustomEvent): void {
        const { name, data } = $event.detail;

        if (['edit-task-executed-workflow', 'close'].includes(name)) {
            this.onCloseWorkflowTaskEditor();
        } else if (name === 'edit-contentlet') {
            this.#editContent(data);
        } else {
            this.dotCustomEventHandlerService.handle($event);
        }
    }

    /**
     * The detail's "Edit Content" (`view_workflow_task.jsp`, `doEdit`) opens the content in the
     * side panel, above the dialog and written to the task's URL (#38303, FR-025a). Once the panel
     * saves or closes, after the task list behind the dialog reloads, the task is viewed again so
     * the detail shows the content's current step.
     *
     * @param data The content's inode and content type, as the detail sent them.
     */
    #editContent({ inode, contentType }: { inode: string; contentType: string }): void {
        const viewTask = () => this.#viewTask();

        this.dotCustomEventHandlerService.openContent(
            { mode: 'edit', inode, contentType },
            { saved: viewTask, closed: viewTask }
        );
    }

    /** Shows the route's task in the dialog, or loads it again when it is shown already. */
    #viewTask(): void {
        this.dotWorkflowTaskDetailService.view({ id: this.#taskId });
    }
}
