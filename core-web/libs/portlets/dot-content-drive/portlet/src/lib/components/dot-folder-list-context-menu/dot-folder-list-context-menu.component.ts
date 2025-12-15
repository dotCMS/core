import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { MenuItem, MessageService } from 'primeng/api';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';

import { lastValueFrom } from 'rxjs';
import { take } from 'rxjs/operators';

import {
    DotContentletService,
    DotMessageService,
    DotRenderMode,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotContentletCanLock,
    DotProcessedWorkflowPayload,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';

import {
    DIALOG_TYPE,
    ERROR_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
} from '../../shared/constants';
import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { isFolder } from '../../utils/functions';

@Component({
    selector: 'dot-folder-list-context-menu',
    templateUrl: './dot-folder-list-context-menu.component.html',
    styleUrl: './dot-folder-list-context-menu.component.scss',
    imports: [ContextMenuModule],
    providers: [DotContentletService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewContextMenuComponent {
    contextMenu = viewChild<ContextMenu>('contextMenu');

    #dotMessageService = inject(DotMessageService);
    #workflowsActionsService = inject(DotWorkflowsActionsService);
    #workflowActionsFireService = inject(DotWorkflowActionsFireService);
    #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    #store = inject(DotContentDriveStore);
    #navigationService = inject(DotContentDriveNavigationService);
    #messageService = inject(MessageService);
    #dotWizardService = inject(DotWizardService);
    #dotContentletService = inject(DotContentletService);

    /** The menu items for the context menu. */
    $items = signal<MenuItem[]>([]);

    /** The memoized menu items for the context menu.
     * Helps to avoid fetching the menu items multiple times.
     * */
    $memoizedMenuItems = signal<Record<string, MenuItem[]>>({});

    /** The context menu data for the store. */
    $contextMenuData = this.#store.contextMenu;

    /**
     * Effect that handles right-click context menu events.
     * When context menu data is available and the menu is not currently visible,
     * it triggers fetching and displaying the menu items.
     */
    readonly rightClickEffect = effect(() => {
        const contextMenuData = this.$contextMenuData();

        if (contextMenuData && !this.contextMenu()?.visible()) {
            this.getMenuItems(contextMenuData);
        }
    });

    /**
     * Effect that clears the memoized menu items when loading state is detected.
     * This ensures the context menu items are regenerated when new content is being loaded.
     * The memoized items are cleared to force a refresh of the menu options.
     */
    readonly statusEffect = effect(() => {
        const status = this.#store.status();

        if (status === DotContentDriveStatus.LOADING) {
            this.$memoizedMenuItems.set({});
        }
    });

    readonly closeOnContextMenuReset = effect(() => {
        const data = this.#store.contextMenu();

        if (!data?.contentlet && this.contextMenu()?.visible()) {
            this.contextMenu()?.hide();
        }
    });

    /**
     * Hides the context menu by clearing the triggered event.
     */
    hideContextMenu() {
        this.#store.patchContextMenu({ triggeredEvent: null });
    }

    /**
     * Retrieves and displays the context menu items for a given contentlet.
     * It checks if the menu has already been memoized and displays it if available.
     * Otherwise, it fetches the workflow actions and builds the menu.
     */
    async getMenuItems({ triggeredEvent, contentlet }: DotContentDriveContextMenu) {
        if (!triggeredEvent || !contentlet) {
            return;
        }

        this.$items.set([]);

        const canLockData = await lastValueFrom(
            this.#dotContentletService.canLock(contentlet.inode)
        );

        const memoizedMenuItems = this.$memoizedMenuItems();

        const key = isFolder(contentlet) ? contentlet.identifier : contentlet.inode;

        if (memoizedMenuItems[key]) {
            this.$items.set(memoizedMenuItems[key]);
            this.contextMenu()?.show(triggeredEvent);
            return;
        }

        const workflowActions = await lastValueFrom(
            this.#workflowsActionsService.getByInode(contentlet.inode, DotRenderMode.LISTING)
        );

        const actionsMenu = [];

        const label =
            contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE ? 'page' : 'content';

        actionsMenu.push({
            label: this.#dotMessageService.get(`content-drive.context-menu.edit-${label}`),
            command: () => {
                this.#navigationService.editContent(contentlet);
            }
        });

        if (canLockData.canLock) {
            actionsMenu.push({
                label: canLockData.locked
                    ? this.#dotMessageService.get('content-drive.context-menu.unlock')
                    : this.#dotMessageService.get('content-drive.context-menu.lock'),
                command: () => {
                    this.#resolveLockAction(contentlet, canLockData);
                }
            });
        }

        workflowActions
            .filter(
                (action) =>
                    action.name !== 'Move' || action.id !== MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
            )
            .map((action) => {
                const menuItem = {
                    label: `${this.#dotMessageService.get(action.name)}`,
                    command: () => this.#executeWorkflowActions(action, contentlet)
                };

                actionsMenu.push(menuItem);
            });

        actionsMenu.push({
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            command: () => {
                this.#store.setShowAddToBundle(true);
            }
        });

        this.$items.set(actionsMenu);
        this.$memoizedMenuItems.set({
            ...this.$memoizedMenuItems(),
            [key]: this.$items()
        });
        this.contextMenu()?.show(triggeredEvent);
    }

    #executeWorkflowActions(workflowAction: DotCMSWorkflowAction, contentlet: DotCMSContentlet) {
        if (workflowAction.actionInputs?.length > 0) {
            this.#openWizard(workflowAction, contentlet);
        } else {
            this.#fireWorkflowAction({
                contentletInode: contentlet.inode,
                actionId: workflowAction.id
            });
        }
    }

    #openWizard(workflowAction: DotCMSWorkflowAction, contentlet: DotCMSContentlet) {
        this.#dotWizardService
            .open<DotWorkflowPayload>(
                this.#dotWorkflowEventHandlerService.setWizardInput(
                    workflowAction,
                    this.#dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: DotWorkflowPayload) => {
                const payload = this.#dotWorkflowEventHandlerService.processWorkflowPayload(
                    data,
                    workflowAction.actionInputs
                );

                this.#store.setStatus(DotContentDriveStatus.LOADING);
                this.#fireWorkflowAction({
                    contentletInode: contentlet.inode,
                    actionId: workflowAction.id,
                    payload
                });
            });
    }

    #fireWorkflowAction({
        contentletInode,
        actionId,
        payload
    }: {
        contentletInode: string;
        actionId: string;
        payload?: DotProcessedWorkflowPayload;
    }) {
        this.#store.setStatus(DotContentDriveStatus.LOADING);
        this.#workflowActionsFireService
            .fireTo({ actionId, inode: contentletInode, data: payload })
            .subscribe(
                () => {
                    this.#store.reloadContentDrive();

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get(
                            'content-drive.toast.workflow-executed'
                        )
                    });
                },
                (error) => {
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.toast.workflow-error'),
                        life: ERROR_MESSAGE_LIFE
                    });
                    this.#store.setStatus(DotContentDriveStatus.LOADED);
                    console.error('Error firing workflow action', error);
                }
            );
    }

    #resolveLockAction(contentlet: DotCMSContentlet, canLockData: DotContentletCanLock) {
        if (canLockData.locked) {
            this.#dotContentletService
                .unlockContent(contentlet.inode)
                .pipe(take(1))
                .subscribe(
                    ({ title }: DotCMSContentlet) => {
                        this.#messageService.add({
                            severity: 'success',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.unlock-success',
                                title
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.unlock-success-detail'
                            )
                        });

                        this.#store.reloadContentDrive();
                    },
                    (error) => {
                        console.error('Error unlocking content', error);
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.unlock-error'
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.unlock-error-detail'
                            ),
                            life: ERROR_MESSAGE_LIFE
                        });
                        console.error('Error unlocking content', error);
                    }
                );
        } else {
            this.#dotContentletService
                .lockContent(contentlet.inode)
                .pipe(take(1))
                .subscribe(
                    ({ title }: DotCMSContentlet) => {
                        this.#messageService.add({
                            severity: 'success',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.lock-success',
                                title
                            ),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.lock-success-detail'
                            )
                        });
                        this.#store.reloadContentDrive();
                    },
                    (error) => {
                        console.error('Error locking content', error);
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('content-drive.toast.lock-error'),
                            detail: this.#dotMessageService.get(
                                'content-drive.toast.lock-error-detail'
                            ),
                            life: ERROR_MESSAGE_LIFE
                        });
                    }
                );
        }
    }
}
