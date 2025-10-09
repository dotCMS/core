import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    output,
    untracked
} from '@angular/core';

import { MessageService } from 'primeng/api';
import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import {
    DotContentDriveMoveItems,
    DotContentDriveUploadFiles,
    DotTreeFolderComponent
} from '@dotcms/portlets/content-drive/ui';

import { SUCCESS_MESSAGE_LIFE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { DotContentDriveTreeTogglerComponent } from '../dot-content-drive-toolbar/components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';

@Component({
    selector: 'dot-content-drive-sidebar',
    templateUrl: './dot-content-drive-sidebar.component.html',
    styleUrl: './dot-content-drive-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent, DotContentDriveTreeTogglerComponent]
})
export class DotContentDriveSidebarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);

    readonly $loading = this.#store.sidebarLoading;
    readonly $folders = this.#store.folders;
    readonly $selectedNode = this.#store.selectedNode;
    readonly $currentSite = this.#store.currentSite;

    readonly uploadFiles = output<DotContentDriveUploadFiles>();

    readonly getSiteFoldersEffect = effect(() => {
        const currentSite = this.$currentSite();
        if (!currentSite) {
            return;
        }

        // Use untracked to prevent path changes from triggering this effect
        // Only reload folders when the site changes, not when user selects nodes
        untracked(() => {
            this.#store.loadFolders();
        });
    });

    /**
     * Handles node selection events
     *
     * @param {TreeNodeSelectEvent} event - The tree node select event
     */
    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const { node } = event;
        const { path } = node.data;

        this.#store.setPath(path);
    }

    /**
     * Handles node expansion events and loads child folders
     *
     * @param {TreeNodeExpandEvent} event - The tree node expand event
     */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const { node } = event;
        const { hostname, path } = node.data;
        const fullPath = `${hostname}${path}`;

        if (node.children?.length > 0 || node.leaf) {
            node.expanded = true;
            return;
        }

        node.loading = true;
        this.#store.loadChildFolders(fullPath).subscribe(({ folders }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
            node.children = [...folders];
            this.#store.updateFolders([...this.$folders()]);
        });
    }

    /**
     * Handles node collapse events
     * Prevents collapse of the special 'ALL_FOLDER' node
     *
     * @param {TreeNodeCollapseEvent} event - The tree node collapse event
     */
    protected onNodeCollapse(event: TreeNodeCollapseEvent): void {
        const { node } = event;

        if (node.key === 'ALL_FOLDER') {
            node.expanded = true;
            return;
        }
    }

    /**
     * Handles when items are moved to a folder
     *
     * @param {DotContentDriveMoveItems} event - The move items event
     */
    protected moveItemsToFolder(event: DotContentDriveMoveItems): void {
        const dragItems = this.#store.dragItems().map((item) => item.inode);

        const path = event.targetFolder.path.length > 0 ? event.targetFolder.path : '/';

        const pathToMove = `//${event.targetFolder.hostname}${path}`;

        const cleanPath = path.includes('/') ? path.split('/').filter(Boolean).pop() : path;

        const folderName = cleanPath.length > 0 ? cleanPath : pathToMove;

        const assetCount = dragItems.length;

        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get(
                'content-drive.move-to-folder-in-progress',
                folderName
            ),
            detail: this.#dotMessageService.get(
                'content-drive.move-to-folder-in-progress-detail',
                assetCount.toString(),
                `${assetCount > 1 ? 's ' : ' '}`
            )
        });

        this.#dotWorkflowActionsFireService
            .bulkFire({
                additionalParams: {
                    assignComment: {
                        assign: '',
                        comment: ''
                    },
                    pushPublish: {},
                    additionalParamsMap: {
                        _path_to_move: pathToMove
                    }
                },
                contentletIds: dragItems,
                workflowActionId: 'dd4c4b7c-e9d3-4dc0-8fbf-36102f9c6324' //Move to folder action
            })
            .subscribe(({ successCount }) => {
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('content-drive.move-to-folder-success'),
                    detail: this.#dotMessageService.get(
                        'content-drive.move-to-folder-success-detail',
                        successCount.toString(),
                        `${successCount > 1 ? 's ' : ' '}`,
                        folderName
                    ),
                    life: SUCCESS_MESSAGE_LIFE
                });

                this.#store.cleanDragItems();
                this.#store.loadItems();
            });
    }
}
