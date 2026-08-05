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

import type {
    TreeNodeCollapseEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import {
    ALL_FOLDER,
    DotContentDriveMoveItems,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeItem,
    DotTreeFolderComponent,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { buildLoadMoreNode } from '../../utils/functions';
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
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent, DotContentDriveTreeTogglerComponent],
    host: { class: 'w-full h-full grid grid-rows-[min-content_1fr]' },
    styles: `
        :host ::ng-deep .p-tree {
            padding: 0 0.75rem 0.75rem;
        }
    `
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
        this.#store.loadChildFolders(path, hostname).subscribe(({ folders, totalEntries }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
            // First page; append a "Load more" node if the level has more children than this page.
            node.children = this.#appendLoadMore(folders, totalEntries, path, hostname, 2);
            this.#store.updateFolders([...this.$folders()]);
        });
    }

    /**
     * Loads the next page of children for a folder level when its "Load more" node is clicked,
     * appending them and refreshing (or removing) the "Load more" node.
     *
     * @param {DotFolderTreeNodeItem} node - The clicked "Load more" node
     */
    protected onLoadMore(node: DotFolderTreeNodeItem): void {
        const { path, hostname, nextPage } = node.data;

        node.loading = true;
        this.#store.updateFolders([...this.$folders()]);

        this.#store
            .loadChildFolders(path, hostname, nextPage)
            .subscribe(({ folders, totalEntries }) => {
                const parent = this.#findNodeByPath(path, this.$folders());
                if (!parent) {
                    return;
                }

                // Keep the already-loaded folders, drop the old "Load more", append the new page.
                const loaded = (parent.children ?? []).filter(
                    (child) => child.data.type !== LOAD_MORE_NODE_TYPE
                );
                const combined = [...loaded, ...folders];

                parent.children = this.#appendLoadMore(
                    combined,
                    totalEntries,
                    path,
                    hostname,
                    (nextPage ?? 1) + 1
                );
                this.#store.updateFolders([...this.$folders()]);
            });
    }

    /**
     * Appends a "Load more" node to a level's children when more folders remain to be loaded.
     *
     * @param {DotFolderTreeNodeItem[]} children - The child folder nodes loaded so far
     * @param {number} totalEntries - Total number of folders in the level
     * @param {string} path - Full path of the parent folder
     * @param {string} hostname - Hostname of the site
     * @param {number} nextPage - The next 1-based page to request
     * @returns {DotFolderTreeNodeItem[]} children, plus a "Load more" node when more remain
     */
    #appendLoadMore(
        children: DotFolderTreeNodeItem[],
        totalEntries: number,
        path: string,
        hostname: string,
        nextPage: number
    ): DotFolderTreeNodeItem[] {
        if (children.length >= totalEntries) {
            return [...children];
        }

        return [
            ...children,
            buildLoadMoreNode(path, hostname, nextPage, totalEntries - children.length)
        ];
    }

    /**
     * Depth-first search for the folder node with the given path (ignoring "Load more" nodes).
     *
     * @param {string} path - Folder path to find
     * @param {DotFolderTreeNodeItem[]} nodes - Nodes to search
     * @returns {DotFolderTreeNodeItem | undefined} the matching node, if any
     */
    #findNodeByPath(
        path: string,
        nodes: DotFolderTreeNodeItem[]
    ): DotFolderTreeNodeItem | undefined {
        for (const node of nodes) {
            if (node.data?.type !== LOAD_MORE_NODE_TYPE && node.data?.path === path) {
                return node;
            }

            const found = node.children ? this.#findNodeByPath(path, node.children) : undefined;
            if (found) {
                return found;
            }
        }

        return undefined;
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
