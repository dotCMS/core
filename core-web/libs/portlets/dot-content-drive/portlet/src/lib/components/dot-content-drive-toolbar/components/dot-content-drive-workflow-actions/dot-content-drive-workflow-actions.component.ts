import { NgStyle } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { getImageAssetUrl, WINDOW } from '@dotcms/utils';

import { SUCCESS_MESSAGE_LIFE } from '../../../../shared/constants';
import { DotContentDriveStatus } from '../../../../shared/models';
import { DotContentDriveNavigationService } from '../../../../shared/services';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import { isFolder } from '../../../../utils/functions';
import {
    ActionShowConditions,
    ContentDriveWorkflowAction,
    DEFAULT_WORKFLOW_ACTIONS,
    getActionConditions,
    WORKFLOW_ACTION_ID
} from '../../../../utils/workflow-actions';

@Component({
    selector: 'dot-content-drive-workflow-actions',
    imports: [ButtonModule, DotMessagePipe, NgStyle, ConfirmDialogModule],
    providers: [
        DotWorkflowActionsFireService,
        ConfirmationService,
        { provide: WINDOW, useValue: window }
    ],
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
    readonly #confirmationService = inject(ConfirmationService);
    readonly #window = inject(WINDOW);

    protected readonly $selectedItems = this.#store.selectedItems;
    protected readonly DEFAULT_WORKFLOW_ACTIONS = DEFAULT_WORKFLOW_ACTIONS;

    /**
     * Executes the appropriate workflow action based on the action ID.
     * Handles navigation actions (edit contentlet, edit page) differently from workflow operations.
     *
     * @param id - The unique identifier of the workflow action to execute
     */
    protected onExecuteAction(action: ContentDriveWorkflowAction) {
        switch (action.id) {
            case WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET:
                this.gotToEditContentlet();
                break;
            case WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE:
                this.gotToEditPage();
                break;
            case WORKFLOW_ACTION_ID.DOWNLOAD:
                this.download();
                break;
            case WORKFLOW_ACTION_ID.RENAME:
                this.rename();
                break;
            default:
                this.executeWorkflowAction(action);
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
        if (!item || isFolder(item)) return;
        this.#navigationService.editContent(item);
    }

    /**
     * Navigates to the edit screen for the first selected page.
     * Uses the navigation service to handle page-specific routing.
     */
    private gotToEditPage() {
        const item = this.$selectedItems()[0];
        if (!item || isFolder(item)) return;
        this.#navigationService.editPage(item);
    }

    /**
     * Executes a workflow action with optional confirmation dialog.
     * If the action has a confirmation message, displays a confirmation dialog
     * before proceeding with the action execution.
     *
     * @param action - The workflow action to execute
     */
    private executeWorkflowAction(action: ContentDriveWorkflowAction) {
        const { confirmationMessage, id } = action;

        if (confirmationMessage) {
            this.#confirmationService.confirm({
                message: this.#dotMessageService.get(confirmationMessage),
                header: 'Confirmation',
                acceptLabel: this.#dotMessageService.get('dot.common.yes'),
                rejectLabel: this.#dotMessageService.get('dot.common.no'),
                accept: () => {
                    this.performWorkflowAction(id);
                }
            });
        } else {
            this.performWorkflowAction(id);
        }
    }

    /**
     * Performs the actual workflow action execution on the selected items.
     * Fires the workflow action through the service and handles the response
     * by displaying success or error messages and refreshing the item list on success.
     *
     * @param action - The workflow action ID to perform
     */
    private performWorkflowAction(action: string) {
        const inodes = this.$selectedItems().map((item) => item.inode);
        this.beforeExecuteWorkflowAction();
        this.#dotWorkflowActionsFireService.fireDefaultAction({ action, inodes }).subscribe({
            next: () => {
                this.#store.loadItems();
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('content-drive.toast.workflow-executed'),
                    detail: this.#dotMessageService.get(
                        'content-drive.toast.workflow-executed-detail',
                        action
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

    /**
     * Downloads the selected items.
     */
    download() {
        const asset = this.$selectedItems()[0];

        if (!asset || isFolder(asset)) return;

        const assetLink = getImageAssetUrl(asset);

        const downloadLink = new URL(assetLink, this.#window.location.origin);
        downloadLink.searchParams.set('force_download', 'true');

        this.#window.open(downloadLink.toString(), '_self');

        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get(
                'content-drive.toast.download-success',
                asset.title
            ),
            detail: this.#dotMessageService.get('content-drive.toast.download-success-detail'),
            life: SUCCESS_MESSAGE_LIFE
        });
    }

    /**
     * Renames the selected items.
     */
    private rename() {
        console.warn('Rename functionality is under development');
    }

    /**
     * Resets the selected items and status to loading and displays a processing message before executing a workflow action.
     */
    private beforeExecuteWorkflowAction() {
        this.#store.setStatus(DotContentDriveStatus.LOADING);
        this.#store.setSelectedItems([]);
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('content-drive.toast.workflow-in-progress'),
            detail: this.#dotMessageService.get('content-drive.toast.workflow-in-progress-detail')
        });
    }
}
