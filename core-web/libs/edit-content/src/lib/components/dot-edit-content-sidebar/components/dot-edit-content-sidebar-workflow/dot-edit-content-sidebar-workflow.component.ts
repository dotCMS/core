import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotCMSWorkflowStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Component that displays the workflow status of a content item.
 *
 * @export
 * @class DotEditContentSidebarWorkflowComponent
 */
@Component({
    selector: 'dot-edit-content-sidebar-workflow',
    standalone: true,
    imports: [DotMessagePipe, SkeletonModule, SkeletonModule],
    providers: [DotWorkflowService],
    templateUrl: './dot-edit-content-sidebar-workflow.component.html',
    styleUrl: './dot-edit-content-sidebar-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarWorkflowComponent {
    /**
     * The workflow status of the content item.
     *
     * @type {DotCMSWorkflowStatus}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $workflow = input<DotCMSWorkflowStatus>({} as DotCMSWorkflowStatus, {
        alias: 'workflow'
    });

    /**
     * Whether the workflow status is loading.
     *
     * @type {boolean}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $isLoading = input<boolean>(true, {
        alias: 'isLoading'
    });
}
