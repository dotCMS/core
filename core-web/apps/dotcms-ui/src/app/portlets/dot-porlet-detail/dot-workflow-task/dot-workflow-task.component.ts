import {
    Component,
    DestroyRef,
    Injector,
    OnInit,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { distinctUntilChanged, map, take } from 'rxjs/operators';

import { DotMessageService, DotRouterService, DotIframeService } from '@dotcms/data-access';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { refusePanelUrl } from '../../../api/util/open-edit-content-panel';
import { PORTLET_FOLDER_PARAM, portletPanelUrl$ } from '../../../api/util/portlet-panel-url';
import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

@Component({
    selector: 'dot-workflow-task',
    providers: [DotWorkflowTaskDetailService],
    template:
        '<dot-workflow-task-detail (shutdown)="onCloseWorkflowTaskEditor()" (custom)="onCustomEvent($event)" (shown)="onTaskShown()" />',
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
    readonly #router = inject(Router);
    readonly #injector = inject(Injector);

    /** The task the route names. */
    #taskId = '';

    /** Whether the panel the URL names is followed; from the task dialog's first show on. */
    #followingPanelUrl = false;

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
        // The task is viewed again on every save in a side panel, with the panel still open, and
        // on a close after the legacy editor, whichever path opened the panel: "Edit Content", or
        // the task's URL, which the portlet behind reopens (#38303, FR-025a).
        this.#destroyRef.onDestroy(
            this.dotCustomEventHandlerService.onPanelEvents({
                saved: () => this.#viewTask(),
                closed: () => this.#viewTask()
            })
        );

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
     * The task dialog has shown. A side panel the task's URL names opens now, and on every app
     * navigation that changes it, through the same path as "Edit Content": after the dialog, so
     * the panel lands above it whichever loaded first, the menu call the dialog waits for or the
     * panel's chunk (#38303, FR-025a). The portlet behind leaves it to this while the task shows.
     */
    onTaskShown(): void {
        if (this.#followingPanelUrl) {
            return;
        }

        this.#followingPanelUrl = true;
        portletPanelUrl$(this.route, this.#router)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ request, refused }) => {
                if (request) {
                    this.dotCustomEventHandlerService.openContent(request);
                } else if (refused) {
                    void refusePanelUrl(this.#injector, [PORTLET_FOLDER_PARAM]);
                }
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
     * side panel, above the dialog and written to the task's URL (#38303, FR-025a). The task is
     * viewed again on every save in the panel, and on a close after the legacy editor (registered
     * in {@link ngOnInit}), so the detail shows the content's current title and step.
     *
     * @param data The content's inode and content type, as the detail sent them.
     */
    #editContent({ inode, contentType }: { inode: string; contentType: string }): void {
        this.dotCustomEventHandlerService.openContent({ mode: 'edit', inode, contentType });
    }

    /** Shows the route's task in the dialog, or loads it again when it is shown already. */
    #viewTask(): void {
        this.dotWorkflowTaskDetailService.view({ id: this.#taskId });
    }
}
