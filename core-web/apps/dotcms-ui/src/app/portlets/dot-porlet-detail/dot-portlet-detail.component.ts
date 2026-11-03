import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

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

    isWorkflow = false;
    isContent = false;

    ngOnInit() {
        const currentPortlet: string = this.route.parent.snapshot.params.id;
        this.isWorkflow = currentPortlet === 'workflow';
        this.isContent = !this.isWorkflow;
    }
}
