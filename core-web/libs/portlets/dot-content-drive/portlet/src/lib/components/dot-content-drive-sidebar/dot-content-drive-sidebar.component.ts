import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    output,
    untracked,
    viewChild
} from '@angular/core';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import {
    ALL_FOLDER,
    DotContentDriveMoveItems,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeItem,
    DotTreeFolderComponent
} from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { DotContentDriveTreeTogglerComponent } from '../dot-content-drive-toolbar/components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';
/**
 * @description DotContentDriveSidebarComponent is the component that renders the sidebar for the content drive
 *
 * @export
 * @class DotContentDriveSidebarComponent
 */
@Component({
    selector: 'dot-content-drive-sidebar',
    templateUrl: './dot-content-drive-sidebar.component.html',
    styleUrl: './dot-content-drive-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent, DotContentDriveTreeTogglerComponent]
})
export class DotContentDriveSidebarComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly $loading = this.#store.sidebarLoading;
    readonly $folders = this.#store.folders;
    readonly $selectedNode = this.#store.selectedNode;
    readonly $currentSite = this.#store.currentSite;

    readonly uploadFiles = output<DotContentDriveUploadFiles>();
    readonly moveItems = output<DotContentDriveMoveItems>();

    readonly treeFolder = viewChild<DotTreeFolderComponent>('treeFolder');
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
     * Handles selected node that comes from the table (fromTable flag)
     * Expands the path to the node and scrolls it into view
     * This is a signalMethod that automatically subscribes to the signal when called in constructor
     *
     * @param {DotFolderTreeNodeItem} selectedNode - The selected node with fromTable flag
     */
    readonly handleSelectedNodeFromTable = signalMethod<DotFolderTreeNodeItem>((selectedNode) => {
        if (!selectedNode?.data?.fromTable) {
            return;
        }

        const segments = selectedNode.data.path.split('/').filter(Boolean).slice(0, -1);

        this.recursiveExpandOneNode(segments);

        this.treeFolder()
            ?.elementRef.nativeElement.querySelector(`[data-id="${selectedNode.data.id}"]`)
            ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });

    constructor() {
        // Call signalMethod with the signal - it will automatically subscribe to changes
        this.handleSelectedNodeFromTable(this.$selectedNode);
    }
    /**
     * Handles node selection events
     *
     * @param {TreeNodeSelectEvent} event - The tree node select event
     */
    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const { node } = event;

        this.#store.setSelectedNode(node);
    }

    /**
     * Handles node expansion events and loads child folders
     *
     * @param {TreeNodeExpandEvent} event - The tree node expand event
     */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const { node } = event;
        const { hostname, path } = node.data;

        if (node.children?.length > 0 || node.leaf) {
            node.expanded = true;
            return;
        }

        node.loading = true;
        this.#store.loadChildFolders(path, hostname).subscribe(({ folders }) => {
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

        if (node.key === ALL_FOLDER.key) {
            node.expanded = true;
            return;
        }
    }

    /**
     * Recursively expands one node
     *
     * @param {string[]} segments - The segments of the path
     * @param {DotFolderTreeNodeItem[]} nodes - The nodes to expand
     * @returns {void}
     */
    recursiveExpandOneNode(
        segments: string[],
        nodes: DotFolderTreeNodeItem[] = this.$folders()
    ): void {
        if (segments.length === 0) {
            return;
        }

        const node = nodes.find((node) => node.data.path.includes(segments[0]));

        if (!node) {
            return;
        }

        this.onNodeExpand({
            originalEvent: new Event('click'),
            node: node
        });

        this.recursiveExpandOneNode(segments.slice(1), node.children);
    }
}
