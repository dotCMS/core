/* eslint-disable no-console */
import { CommonModule } from "@angular/common";
import { Component, ViewChild, effect, inject, signal } from "@angular/core";
import { Router } from "@angular/router";

import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';

import { take } from "rxjs/operators";

import { DotContentTypeService, DotEventsService, DotHttpErrorManagerService, DotMessageService, DotRenderMode, DotWorkflowActionsFireService, DotWorkflowEventHandlerService, DotWorkflowsActionsService } from "@dotcms/data-access";
import { DotCMSWorkflowActionEvent, DotContentDriveItem, FeaturedFlags } from '@dotcms/dotcms-models';

import { DotContentDriveContextMenu, DotContentDriveStatus } from "../../shared/models";
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
    #router = inject(Router);
    #store = inject(DotContentDriveStore);
    #dotContentTypeService = inject(DotContentTypeService);

    
    items = signal([]);
    
    $contextMenuData = this.#store.contextMenu;
    $memoizedMenuItems = signal({});
    $showAddToBundle = signal(false);

    readonly rightClickEffect = effect(() => {
        const contextMenuData = this.$contextMenuData();

        if (contextMenuData && !this.contextMenu?.visible()) {
            this.getMenuItems(contextMenuData);
        }
    });

    async getMenuItems({ triggeredEvent, contentlet }: DotContentDriveContextMenu) {

        if (!triggeredEvent || !contentlet) {
            return;
        }

        this.items.set([]);
        
        const memoizedMenuItems = this.$memoizedMenuItems();

        if (memoizedMenuItems[contentlet.inode]) {
            this.items.set(memoizedMenuItems[contentlet.inode]);
            this.contextMenu?.show(triggeredEvent);
            return;
        }

        const workflowActions = await this.#workflowsActionsService.getByInode(contentlet.inode, DotRenderMode.LISTING).toPromise();

        const actionsMenu = [];
        
        if (contentlet.contentType === 'htmlpageasset') {
            actionsMenu.push({
                label: this.#dotMessageService.get('content-drive.context-menu.edit-page'),
                command: () => {
                    const url = contentlet.urlMap || contentlet.url;
                    this.#router.navigate(['/edit-page/content'], { queryParams: { url, language_id: contentlet.languageId } });
                }
            }); 
        } else {
            actionsMenu.push({
                label: this.#dotMessageService.get('content-drive.context-menu.edit-contentlet'),
                command: () => {
                    // Here i need to implement the editContentlet
                    this.#editContentlet(contentlet);
                }
            });
        }

        workflowActions.map(action => {
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
        this.contextMenu?.show(triggeredEvent);
    }

    #fireWorkflowAction(contentletInode: string, actionId: string): void {
        const value = this.#dotMessageService.get('Workflow-executed');
        this.#workflowActionsFireService.fireTo({ actionId, inode: contentletInode }).subscribe(
            (payload) => {
                this.#dotEventsService.notify('save-page', { payload, value });
                this.#store.reloadContentDrive();
            },
            // TODO:.re can be a console.error?
            (error) => this.#httpErrorManagerService.handle(error, true)
        );
    }

    hideContextMenu() {
        this.#store.patchContextMenu({ triggeredEvent: null });
    }

    #editContentlet(contentlet: DotContentDriveItem) {
        this.#dotContentTypeService
        .getContentType(contentlet.contentType)
        .pipe(take(1))
        .subscribe((contentType) => {
            const shouldRedirectToOldContentEditor = !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];
            if (shouldRedirectToOldContentEditor) {
                this.#router.navigate([`c/content/${contentlet.inode}`]);

                return;
            }

            this.#router.navigate([`content/${contentlet.inode}`]);
        });
    }


}