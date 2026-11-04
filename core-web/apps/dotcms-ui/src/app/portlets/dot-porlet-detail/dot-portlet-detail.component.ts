import { Component, DestroyRef, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { distinctUntilChanged, map } from 'rxjs/operators';

import { DotEditContentDeepLinkComponent } from './dot-edit-content-deep-link/dot-edit-content-deep-link.component';
import { DotWorkflowTaskComponent } from './dot-workflow-task/dot-workflow-task.component';

/**
 * `/c/:id/:asset`: a workflow task for the Workflow portlet, otherwise a deep link that redirects
 * to the portlet's URL with the side panel params, and the portlet opens the panel (#38303).
 */
@Component({
    selector: 'dot-portlet-detail',
    templateUrl: './dot-portlet-detail.component.html',
    styleUrls: ['./dot-portlet-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotWorkflowTaskComponent, DotEditContentDeepLinkComponent]
})
export class DotPortletDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    readonly #destroyRef = inject(DestroyRef);

    isWorkflow = false;
    isContent = false;

    /**
     * Follows the portlet the parent route names. The route is reused across navigations (it has no
     * `reuseRoute: false`, so Back with a task's side panel open does not rebuild the task dialog,
     * #38303), so a change from `/c/workflow/<task>` to `/c/content/<inode>` keeps this component.
     */
    ngOnInit() {
        this.route.parent.params
            .pipe(
                map((params) => params['id']),
                distinctUntilChanged(),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((portlet) => {
                this.isWorkflow = portlet === 'workflow';
                this.isContent = !this.isWorkflow;
            });
    }
}
