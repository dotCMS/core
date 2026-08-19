import { signalMethod } from '@ngrx/signals';

import {
    afterNextRender,
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    Injector,
    output,
    untracked,
    viewChild
} from '@angular/core';

import type {
    TreeNodeCollapseEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { DotContentDriveActionableFolder, TreeNodeLoadMoreData } from '@dotcms/dotcms-models';
import {
    DotContentDriveMoveItems,
    DotContentDriveTreeRightClick,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeContentData,
    DotFolderTreeNodeItem,
    DotTreeFolderComponent,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';
import { ALL_FOLDER } from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { appendLoadMoreNodes, mergeFolderNodePage } from '../../utils/functions';
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
    imports: [DotTreeFolderComponent],
    host: { class: 'w-full h-full grid grid-rows-[min-content_1fr]' },
    styles: `
        :host ::ng-deep .p-tree {
            padding: 0 0.75rem 0.75rem;
        }
    `
})
export class DotContentDriveSidebarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #injector = inject(Injector);

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
        const data = selectedNode?.data;
        if (!data || data.type === LOAD_MORE_NODE_TYPE || !data.fromTable) {
            return;
        }

        const segments = data.path.split('/').filter(Boolean).slice(0, -1);

        this.recursiveExpandOneNode(segments);

        this.#revealNode(selectedNode, 'smooth');
    });

    /**
     * Brings the folder the drive is open on into view once a cold load has rendered.
     *
     * The hierarchy load already expands the tree down to that folder, but a level can be hundreds
     * of folders deep, so on a deep link it was drawn far below the fold with the viewport still at
     * the top. Selecting a node in the tree must not scroll — it is under the cursor already — so
     * this hangs off the load finishing rather than off the selection changing.
     *
     * Keyed off the load finishing rather than off the selection changing, because at the moment
     * the store publishes a cold-loaded selection the tree is not on screen yet — the loading
     * placeholder still is. Both reveals share {@link #revealSelectedNode}.
     *
     * @param {boolean} loading - The sidebar's loading state
     */
    readonly revealSelectedNodeOnLoad = signalMethod<boolean>((loading) => {
        if (loading) {
            return;
        }

        // Instant, not smooth: this is where the tree should have opened, not a place to animate to.
        this.#revealNode(this.$selectedNode(), 'instant');
    });

    constructor() {
        // Call signalMethod with the signal - it will automatically subscribe to changes
        this.handleSelectedNodeFromTable(this.$selectedNode);
        this.revealSelectedNodeOnLoad(this.$loading);
    }

    /**
     * Scrolls a node's row into the middle of the tree's viewport, once the tree has actually
     * rendered it.
     *
     * The wait matters for both callers. A cold load publishes its selection while the loading
     * placeholder is still mounted, and the table's reveal runs straight after
     * `recursiveExpandOneNode`, which only marks ancestors expanded — a branch whose children are
     * still being fetched has no row to scroll to yet either.
     *
     * @param {DotFolderTreeNodeItem | undefined} node - The node to bring into view
     * @param {ScrollBehavior} behavior - How to travel there
     */
    #revealNode(node: DotFolderTreeNodeItem | undefined, behavior: ScrollBehavior): void {
        const data = node?.data;

        if (!data || data.type === LOAD_MORE_NODE_TYPE) {
            return;
        }

        afterNextRender(
            () => {
                this.treeFolder()
                    ?.elementRef.nativeElement.querySelector(`[data-id="${data.id}"]`)
                    ?.scrollIntoView({ behavior, block: 'center' });
            },
            { injector: this.#injector }
        );
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
        const data = node.data;

        if (!data || data.type === LOAD_MORE_NODE_TYPE) {
            return;
        }

        const { hostname, path } = data;

        if ((node.children?.length ?? 0) > 0 || node.leaf) {
            node.expanded = true;
            return;
        }

        node.loading = true;
        this.#store.loadChildFolders(path, hostname).subscribe(({ folders, totalEntries }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
            // First page; append a "Load more" node if the level has more children than this page.
            node.children = appendLoadMoreNodes(folders, totalEntries, path, hostname, 2);
            this.#store.updateFolders([...this.$folders()]);
        });
    }

    /**
     * Loads the next page of children for a folder level when its "Load more" node is clicked,
     * appending them and refreshing (or removing) the "Load more" node.
     *
     * Root-level sentinels are siblings of root folders (not children of ALL_FOLDER), so they
     * update the top-level folders array. Nested sentinels update `parent.children`.
     *
     * @param {DotFolderTreeNodeItem} node - The clicked "Load more" node
     */
    protected onLoadMore(node: DotFolderTreeNodeItem): void {
        const { path, hostname, nextPage } = node.data as TreeNodeLoadMoreData;
        const parentPath = path ?? '/';

        node.loading = true;
        this.#store.updateFolders([...this.$folders()]);

        this.#store
            .loadChildFolders(parentPath, hostname, nextPage)
            .subscribe(({ folders, totalEntries }) => {
                const isRootLevel = parentPath === '/' || parentPath === '';

                if (isRootLevel) {
                    const current = this.$folders();
                    const allFolder =
                        current.find((folder) => folder.key === ALL_FOLDER.key) ?? ALL_FOLDER;
                    const loaded = current.filter(
                        (folder) =>
                            folder.key !== ALL_FOLDER.key &&
                            folder.data?.type !== LOAD_MORE_NODE_TYPE
                    );
                    // Merge rather than concatenate: the hierarchy load can pin a deep-linked folder to
                    // the top of a level out of sort order, and paging far enough returns it again.
                    const combined = mergeFolderNodePage(loaded, folders);

                    this.#store.updateFolders([
                        allFolder,
                        ...appendLoadMoreNodes(
                            combined,
                            totalEntries,
                            parentPath || '/',
                            hostname ?? '',
                            (nextPage ?? 1) + 1
                        )
                    ]);

                    return;
                }

                const parent = this.#findNodeByPath(parentPath, this.$folders());
                if (!parent) {
                    return;
                }

                // Keep the already-loaded folders, drop the old "Load more", append the new page.
                const loaded = (parent.children ?? []).filter(
                    (child) => child.data?.type !== LOAD_MORE_NODE_TYPE
                );
                // Merge rather than concatenate: the hierarchy load can pin a deep-linked folder to
                // the top of a level out of sort order, and paging far enough returns it again.
                const combined = mergeFolderNodePage(loaded, folders);

                parent.children = appendLoadMoreNodes(
                    combined,
                    totalEntries,
                    parentPath,
                    hostname ?? '',
                    (nextPage ?? 1) + 1
                );
                this.#store.updateFolders([...this.$folders()]);
            });
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
            // `data` is optional on the tree node, so it is checked once up front rather than
            // optional-chained on the first read and dereferenced plainly on the second.
            if (node.data && node.data.type !== LOAD_MORE_NODE_TYPE && node.data.path === path) {
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
     * Opens the shared folder context menu for a right-clicked tree node, giving the sidebar the
     * same folder actions the table offers.
     *
     * Every folder node carries its permissions, whichever way it reached the tree: expand,
     * load-more and the deep-link hierarchy load all request them. So this stays synchronous, and a
     * right-click opens the menu immediately.
     *
     * @param {DotContentDriveTreeRightClick} rightClick - The originating event and clicked folder
     */
    protected onNodeRightClick({ event, data }: DotContentDriveTreeRightClick): void {
        this.#openContextMenu(event, data);
    }

    /**
     * Publishes the clicked folder to the store in the shape the shared context menu and the
     * "Edit folder" dialog consume.
     *
     * @param {MouseEvent} event - The originating right-click, used to anchor the menu
     * @param {DotFolderTreeNodeContentData} data - The clicked node's folder data
     */
    #openContextMenu(event: MouseEvent, data: DotFolderTreeNodeContentData): void {
        this.#store.patchContextMenu({
            triggeredEvent: event,
            contentlet: {
                type: 'folder',
                identifier: data.id,
                // The tree labels nodes by full path; `name` comes from the folder-search view.
                name: data.name ?? '',
                path: data.path,
                title: data.title ?? '',
                sortOrder: data.sortOrder ?? 0,
                showOnMenu: data.showOnMenu ?? false,
                filesMasks: data.filesMasks ?? '',
                defaultFileType: data.defaultFileType ?? '',
                defaultBaseType: data.defaultBaseType,
                permissions: data.permissions ?? []
            } satisfies DotContentDriveActionableFolder
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

        const node = nodes.find(
            (candidate) =>
                !!candidate.data &&
                candidate.data.type !== LOAD_MORE_NODE_TYPE &&
                candidate.data.path.includes(segments[0])
        );

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
