import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal, viewChild } from '@angular/core';

import { MenuItem, MessageService } from 'primeng/api';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';

import {
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSWorkflowActionEvent } from '@dotcms/dotcms-models';

import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

@Component({
    selector: 'dot-folder-list-context-menu',
    templateUrl: './dot-folder-list-context-menu.component.html',
    imports: [CommonModule, ContextMenuModule]
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

        const memoizedMenuItems = this.$memoizedMenuItems();

        if (memoizedMenuItems[contentlet.inode]) {
            this.$items.set(memoizedMenuItems[contentlet.inode]);
            this.contextMenu()?.show(triggeredEvent);
            return;
        }

        const workflowActions = await this.#workflowsActionsService
            .getByInode(contentlet.inode, DotRenderMode.LISTING)
            .toPromise();

        const actionsMenu = [];

        const label =
            contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE ? 'page' : 'content';

        actionsMenu.push({
            label: this.#dotMessageService.get(`content-drive.context-menu.edit-${label}`),
            command: () => {
                this.#navigationService.editContent(contentlet);
            }
        });

        workflowActions.map((action) => {
            const menuItem = {
                label: `${this.#dotMessageService.get(action.name)}`,
                command: () => {
                    if (!(action.actionInputs?.length > 0)) {
                        this.#store.setStatus(DotContentDriveStatus.LOADING);
                        this.#fireWorkflowAction(contentlet.inode, action.id);

                        return;
                    }

                    const wfActionEvent: DotCMSWorkflowActionEvent = {
                        workflow: action,
                        callback: 'ngWorkflowEventCallback',
                        inode: contentlet.inode,
                        selectedInodes: null
                    };

                    this.#dotWorkflowEventHandlerService.open(wfActionEvent);
                }
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
            [contentlet.inode]: this.$items()
        });
        this.contextMenu()?.show(triggeredEvent);
    }

    #fireWorkflowAction(contentletInode: string, actionId: string) {
        this.#workflowActionsFireService.fireTo({ actionId, inode: contentletInode }).subscribe(
            () => {
                this.#store.reloadContentDrive();

                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('content-drive.toast.workflow-executed'),
                    life: 2000
                });
            },
            (error) => {
                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get('content-drive.toast.workflow-error'),
                    life: 2000
                });
                console.error('Error firing workflow action', error);
            }
        );
    }
}
