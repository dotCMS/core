import { CommonModule } from "@angular/common";
import { Component, ViewChild, effect, inject, model, signal } from "@angular/core";

import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';

import { DotCurrentUserService, DotEventsService, DotHttpErrorManagerService, DotMessageService, DotRenderMode, DotWorkflowActionsFireService, DotWorkflowEventHandlerService, DotWorkflowsActionsService } from "@dotcms/data-access";
import { DotCMSWorkflowActionEvent, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent } from "@dotcms/ui";

export interface ContextMenuData {
    event: Event;
    contentlet: DotContentDriveItem;
}

@Component({
    selector: 'dot-folder-list-context-menu',
    templateUrl: './dot-folder-list-context-menu.component.html',
    standalone: true,
    imports: [CommonModule, ContextMenuModule, DotAddToBundleComponent]
})
export class DotFolderListViewContextMenuComponent {
    @ViewChild('contextMenu') contextMenu: ContextMenu;
    
    $contextMenuData = model<ContextMenuData | null>(null, { alias: 'contextMenuData' });
    
    #dotMessageService = inject(DotMessageService);
    #workflowsActionsService = inject(DotWorkflowsActionsService);
    #workflowActionsFireService = inject(DotWorkflowActionsFireService);
    #dotEventsService = inject(DotEventsService);
    #httpErrorManagerService = inject(DotHttpErrorManagerService);
    #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    #dotCurrentUser = inject(DotCurrentUserService);


    readonly rightClickEffect = effect(() => {
        const contextMenuData = this.$contextMenuData();
        if (contextMenuData && this.contextMenu) {
            this.getMenuItems(contextMenuData);
        }
    });

    items = signal([]);

    $memoizedMenuItems = signal({});
    $showAddToBundle = signal(false);


    async getMenuItems({ event, contentlet: item }: ContextMenuData) {

        this.items.set([]);
        
        const memoizedMenuItems = this.$memoizedMenuItems();

        if (memoizedMenuItems[item.inode]) {
            this.items.set(memoizedMenuItems[item.inode]);
            this.#showContextMenuAndReset(event);
            return;
        }

        const workflowActions = await this.#workflowsActionsService.getByInode(item.inode, DotRenderMode.LISTING).toPromise();

        const actionsMenu = [];
        workflowActions.map(action => {
            const menuItem = {
                label: `${this.#dotMessageService.get(action.name)}`,
                command: () => {
                    if (!(action.actionInputs?.length > 0)) {
                        this.#fireWorkflowAction(item.inode, action.id);

                        return;
                    }

                    const wfActionEvent: DotCMSWorkflowActionEvent = {
                        workflow: action,
                        callback: 'ngWorkflowEventCallback',
                        inode: item.inode,
                        selectedInodes: null
                    };

                    this.#dotWorkflowEventHandlerService.open(wfActionEvent);
                }
            }

            actionsMenu.push(menuItem);

            this.$memoizedMenuItems.set({...this.$memoizedMenuItems(), [item.inode]: this.items()});
        })

        actionsMenu.push({
            label: this.#dotMessageService.get('contenttypes.content.add_to_bundle'),
            command: () => this.$showAddToBundle.set(true)
        });

        this.items.set(actionsMenu);

        this.#showContextMenuAndReset(event);
    }

    #showContextMenuAndReset(event: Event) {
        this.contextMenu.show(event);
        // Reset the right click event
        this.$contextMenuData.set(null);
    }


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
}