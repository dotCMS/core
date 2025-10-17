import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveNavigationService } from '../../../../shared/services';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import {
    ActionShowConditions,
    DEFAULT_WORKFLOW_ACTIONS,
    getActionConditions,
    ContentDriveWorkflowAction,
    WORKFLOW_ACTION_ID
} from '../../../../utils/workflow-actions';

@Component({
    selector: 'dot-content-drive-workflow-actions',
    imports: [ButtonModule, DotMessagePipe, NgStyle],
    providers: [DotWorkflowActionsFireService],
    templateUrl: './dot-content-drive-workflow-actions.component.html',
    styleUrl: './dot-content-drive-workflow-actions.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveWorkflowActionsComponent {
    readonly $actionConditions = computed<ActionShowConditions>(() =>
        getActionConditions(this.$selectedItems())
    );

    readonly #store = inject(DotContentDriveStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #navigationService = inject(DotContentDriveNavigationService);

    protected readonly $selectedItems = this.#store.selectedItems;
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;

    /**
     * Executes the appropriate workflow action based on the action ID.
     * Handles navigation actions (edit contentlet, edit page) differently from workflow operations.
     *
     * @param id - The unique identifier of the workflow action to execute
     */
    protected onExecuteAction(id: string) {
        switch (id) {
            case WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET:
                this.gotToEditContentlet();
                break;
            case WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE:
                this.gotToEditPage();
                break;
            default:
                this.runWorkflowAction(id);
                break;
        }
    }

    /**
     * Determines whether a workflow action should be visible
     * based on the current selection conditions.
     */
    protected shouldShowAction = (action: ContentDriveWorkflowAction): boolean => {
        const showWhen = action.showWhen;
        const conditions = this.$actionConditions();

        if (!showWhen) return true;

        // Check every defined condition in showWhen.
        // Each must match the corresponding condition in the current state.
        return Object.entries(showWhen).every(([key, expectedValue]) => {
            return conditions[key as keyof ActionShowConditions] === expectedValue;
        });
    };

    /**
     * Navigates to the edit screen for the first selected contentlet.
     * Uses the navigation service to handle the routing.
     */
    private gotToEditContentlet() {
        const item = this.$selectedItems()[0];
        if (!item) return;
        this.#navigationService.editContent(item);
    }

    /**
     * Navigates to the edit screen for the first selected page.
     * Uses the navigation service to handle page-specific routing.
     */
    private gotToEditPage() {
        const item = this.$selectedItems()[0];
        if (!item) return;
        this.#navigationService.editPage(item);
    }

    /**
     * Executes a default workflow action on the selected items.
     * Displays success or error messages based on the operation result
     * and refreshes the item list on success.
     *
     * @param id - The workflow action identifier to execute
     */
    private runWorkflowAction(id: string) {
        this.#dotWorkflowActionsFireService
            .fireDefaultAction({
                action: id,
                inodes: this.$selectedItems().map((item) => item.inode)
            })
            .subscribe({
                next: () => {
                    this.#store.loadItems();
                    this.#messageService.add({
                        severity: 'success',
                        summary: 'Success',
                        detail: this.#dotMessageService.get(
                            'content-drive.dialog.folder.message.create-success'
                        )
                    });
                },
                error: (error) => {
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.toast.workflow-error'),
                        detail: error.message
                    });

                    console.error('Error executing workflow action:', error);
                }
            });
    }
}
