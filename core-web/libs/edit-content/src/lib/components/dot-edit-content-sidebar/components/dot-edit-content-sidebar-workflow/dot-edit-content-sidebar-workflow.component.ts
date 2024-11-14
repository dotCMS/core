import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { SkeletonModule } from 'primeng/skeleton';

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
    imports: [
        DotMessagePipe,
        SkeletonModule,
        ButtonModule,
        DialogModule,
        DropdownModule,
        FormsModule,
        JsonPipe
    ],

    templateUrl: './dot-edit-content-sidebar-workflow.component.html',
    styleUrl: './dot-edit-content-sidebar-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarWorkflowComponent {
    /**
     * Whether not exist content yet
     *
     * @type {boolean}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $isNewContent = input<boolean>(true, {
        alias: 'isNew'
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
     * The selected workflow.
     *
     * @type {string}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $selectedWorkflow = model<SelectItem>({} as SelectItem);

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

    /**
     * Whether the select workflow warning should be shown.
     *
     * @type {boolean}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $noWorkflowSelectedYet = input<boolean>(false, {
        alias: 'noWorkflowSelectedYet'
    });

    /**
     * The workflow scheme options.
     *
     * @type {Array<{ value: string; label: string }>}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $workflowSchemeOptions = input<SelectItem[]>([], {
        alias: 'workflowSchemeOptions'
    });

    /**
     * Whether to show the workflow selection dialog icon.
     * Shows when content is new and there are multiple workflow options available.
     *
     * @type {Signal<boolean>}
     * @memberof DotEditContentSidebarWorkflowComponent
     */
    $showWorkflowDialogIcon = computed(() => {
        const hasMultipleWorkflows = this.$workflowSchemeOptions().length > 1;

        return this.$isNewContent() && hasMultipleWorkflows;
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
