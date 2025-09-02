/* eslint-disable no-console */
import { CommonModule } from "@angular/common";
import { Component, ViewChild, effect, inject, signal } from "@angular/core";

import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';

import { DotCurrentUserService, DotEventsService, DotHttpErrorManagerService, DotMessageService, DotRenderMode, DotWorkflowActionsFireService, DotWorkflowEventHandlerService, DotWorkflowsActionsService } from "@dotcms/data-access";
import { DotCMSWorkflowActionEvent, DotContentDriveItem } from '@dotcms/dotcms-models';

import { DotContentDriveContextMenu } from "../../shared/models";
import { DotContentDriveStore } from "../../store/dot-content-drive.store";

export interface ContextMenuData {
    event: Event;
    contentlet: DotContentDriveItem;
}

@Component({
    selector: 'dot-folder-list-context-menu',
    templateUrl: './dot-folder-list-context-menu.component.html',
    standalone: true,
    imports: [CommonModule, ContextMenuModule]
})
export class DotFolderListViewContextMenuComponent {
    @ViewChild('contextMenu') contextMenu: ContextMenu;
    
    #dotMessageService = inject(DotMessageService);
    #workflowsActionsService = inject(DotWorkflowsActionsService);
    #workflowActionsFireService = inject(DotWorkflowActionsFireService);
    #dotEventsService = inject(DotEventsService);
    #httpErrorManagerService = inject(DotHttpErrorManagerService);
    #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    #dotCurrentUser = inject(DotCurrentUserService);
    #store = inject(DotContentDriveStore);

    $contextMenuData = this.#store.contextMenu;

    readonly rightClickEffect = effect(() => {
        const contextMenuData = this.$contextMenuData();

        console.log('called effect', contextMenuData);

        if (contextMenuData && !this.contextMenu?.visible()) {
            this.getMenuItems(contextMenuData);
        }
    });

    items = signal([]);

    $memoizedMenuItems = signal({});
    $showAddToBundle = signal(false);


    async getMenuItems({ triggeredEvent, contentlet }: DotContentDriveContextMenu) {

        if (!triggeredEvent || !contentlet) {
            return;
        }

        this.items.set([]);
        
        const memoizedMenuItems = this.$memoizedMenuItems();

        if (memoizedMenuItems[contentlet.inode]) {
            this.items.set(memoizedMenuItems[contentlet.inode]);
            // this.#showContextMenuAndReset(triggeredEvent);
            this.contextMenu?.show(triggeredEvent);
            return;
        }

        const workflowActions = await this.#workflowsActionsService.getByInode(contentlet.inode, DotRenderMode.LISTING).toPromise();

        const actionsMenu = [];
        workflowActions.map(action => {
            const menuItem = {
                label: `${this.#dotMessageService.get(action.name)}`,
                command: () => {
                    if (!(action.actionInputs?.length > 0)) {
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
            }

            actionsMenu.push(menuItem);

        })

        actionsMenu.push({
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            command: () => {
                console.log("Called add to bundle");
                this.#store.setShowAddToBundle(true)
            }
        });

        this.items.set(actionsMenu);
        this.$memoizedMenuItems.set({...this.$memoizedMenuItems(), [contentlet.inode]: this.items()});

        // this.#showContextMenuAndReset(triggeredEvent);

        this.contextMenu?.show(triggeredEvent);
    }

    // #showContextMenuAndReset(event: Event) {

    //     if (this.contextMenu && !this.contextMenu.visible()) {
    //         this.contextMenu?.show(event);
    //     }
    //     // Reset the right click event
    //     // this.#store.setContextMenu(null);
    // }


    #fireWorkflowAction(contentletInode: string, actionId: string): void {
        const value = this.#dotMessageService.get('Workflow-executed');
        this.#workflowActionsFireService.fireTo({ actionId, inode: contentletInode }).subscribe(
            (payload) => this.#dotEventsService.notify('save-page', { payload, value }),
            // TODO: Check if this is necessary, can be a console.error?
            (error) => this.#httpErrorManagerService.handle(error, true)
        );
    }

    // async #getUserPagePermissions(contentlet: DotContentDriveItem) {
    //     const loggedUser = await this.#dotCurrentUser.getCurrentUser().toPromise();

    //     return loggedUser.;

    // }

    tryToHide() {
        console.log('tryToHide!!!!');
        // debugger;
        // this.contextMenu?.hide();
        // this.contextMenu?.hide();
        this.#store.patchContextMenu({ triggeredEvent: null });
    }
}