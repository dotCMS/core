import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { SkeletonModule } from 'primeng/skeleton';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotWorkflowState } from '../../../../models/dot-edit-content.model';

interface WorkflowSelection {
    schemeOptions: SelectItem[];
    isWorkflowSelected: boolean;
}

const DEFAULT_WORKFLOW_SELECTION: WorkflowSelection = {
    schemeOptions: [],
    isWorkflowSelected: false
} as const;

/**
 * Component that displays the workflow status of a content item.
 *
 * @export
 * @class DotEditContentSidebarWorkflowComponent
 */
@Component({
    selector: 'dot-edit-content-sidebar-workflow',
    imports: [
        DotMessagePipe,
        SkeletonModule,
        ButtonModule,
        DialogModule,
        DropdownModule,
        FormsModule
    ],
    templateUrl: './dot-edit-content-sidebar-workflow.component.html',
    styleUrl: './dot-edit-content-sidebar-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarWorkflowComponent {
    $workflowSelection = input<WorkflowSelection>(DEFAULT_WORKFLOW_SELECTION, {
        alias: 'workflowSelection'
    });

    /**
     * Whether the dialog should be shown.
     *
     * @type {boolean}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    readonly $showDialog = model<boolean>(false, {
        alias: 'showDialog'
    });

    /**
     * Output event to select the workflow.
     *
     * @type {Output<string>}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    readonly onSelectWorkflow = output<string>();

    /**
     * Output event to reset the workflow.
     *
     * @type {Output<DotCMSWorkflowAction>}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    readonly onResetWorkflow = output<string>();

    /**
     * The selected workflow.
     *
     * @type {string}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $selectedWorkflow = model<SelectItem | null>();

    /**
     * The workflow status of the content item.
     *
     * @type {DotCMSWorkflowStatus}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $workflow = input<DotWorkflowState | null>(null, {
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

    /**
     * Whether the reset action should be shown.
     *
     * @type {boolean}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $resetWorkflowAction = input<DotCMSWorkflowAction | null>(null, {
        alias: 'resetWorkflowAction'
    });

    /**
     * Whether the workflow selection dialog should be available.
     */
    $showWorkflowSelection = computed(() => {
        const { schemeOptions } = this.$workflowSelection();
        const workflow = this.$workflow();

        const hasMultipleWorkflows = schemeOptions.length > 1;
        const isResetOrNew = workflow.contentState === 'reset' || workflow.contentState === 'new';
        const hasNoResetAction = !workflow.resetAction;

        return hasMultipleWorkflows && isResetOrNew && hasNoResetAction;
    });

    /**
     * Shows the dialog to select the workflow.
     *
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    showDialog() {
        this.$showDialog.set(true);
    }

    /**
     * Selects the workflow.
     *
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    selectWorkflow() {
        this.$showDialog.set(false);
        this.onSelectWorkflow.emit(this.$selectedWorkflow().value);
    }

    /**
     * Closes the dialog.
     *
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    closeDialog() {
        this.$showDialog.set(false);
    }
}
