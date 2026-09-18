import { signalMethod } from '@ngrx/signals';

import {
    afterNextRender,
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    Injector,
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';

import type { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import {
    DotContentDriveActionableFolder,
    PERMISSIONS_TYPE,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';
import {
    DotContentDriveMoveItems,
    DotContentDriveTreeRightClick,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeContentData,
    DotFolderTreeNodeItem,
    DotTreeFolderComponent,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { SYSTEM_HOST } from '../../shared/constants';
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
    imports: [DotTreeFolderComponent, DotMessagePipe],
    host: { class: 'flex h-full w-full flex-col' },
    styles: `
        /* The top inset used to come from the site-name header that sat above the tree, then from
           the tree itself once the site was named by its own root row. It now belongs to whatever
           is first in the column, which is the all-site-content row — the amount is what centers
           that first row on the toolbar's search box and tree toggler beside it, so it has to
           travel with the row rather than stay on the tree. */
        :host ::ng-deep .p-tree {
            /* Almost no left inset, so the tree's chevron sits in the same column as the icons on
               the rows above and below it.

               What has to match is the middle of each mark, not the left of its box. The chevron is
               a 10.5px glyph centred in a 24.5px button, while those rows carry a 16px icon, so
               lining the boxes up leaves the chevron looking 4px to the right of everything else.
               Working back from the icon centre through the row's own 0.625rem leaves this much for
               the tree, and it belongs to the sidebar layout rather than to the shared tree, which
               knows nothing about the rows it happens to sit between. */
            padding: 0 0.75rem 0.75rem 1px;
        }

        /* The two rows that are not tree nodes still have to feel like them.

           PrimeNG paints a node's hover state through a rule scoped under the tree root, so the
           class alone buys nothing out here: all-site-content and System Host sit outside the
           tree on purpose, and both were left with no hover at all while every row between them
           had one. Same tokens as the tree node, read with the underlying content token as a
           fallback so a rename of the component-level one degrades instead of going blank.

           Selected rows are excluded: PrimeNG already paints those, and a hover on top of the
           selected background reads as the selection having been lost. */
        :host button.p-tree-node-content:not(.p-tree-node-selected):hover {
            background-color: var(
                --p-tree-node-hover-background,
                var(--p-content-hover-background)
            );
            color: var(--p-tree-node-hover-color, var(--p-text-hover-color));
        }

        /* Drop target, matching what the tree marks its own active row with rather than a second
           look for the same meaning. The all-site-content row deliberately does the opposite and
           dims, because it refuses the drop. */
        :host button.is-drop-target {
            background-color: var(--color-palette-primary-200);
        }
    `
})
export class DotContentDriveSidebarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #injector = inject(Injector);

    readonly $loading = this.#store.sidebarLoading;

    /** Whether the sidebar's first entry, all site content, is the selected one. */
    readonly $allSiteContentSelected = this.#store.$allSiteContentSelected;

    /** Whether the sidebar's last entry, System Host, is the selected one. */
    readonly $systemHostSelected = this.#store.$systemHostSelected;
    readonly $folders = this.#store.folders;
    readonly $selectedNode = this.#store.selectedNode;
    readonly $currentSite = this.#store.currentSite;

    readonly uploadFiles = output<DotContentDriveUploadFiles>();
    readonly moveItems = output<DotContentDriveMoveItems>();

    /** Whether the user may add content to System Host; unknown reads as allowed. */
    readonly $systemHostCanAddChildren = this.#store.systemHostCanAddChildren;

    /**
     * Whether to offer the System Host entry at all.
     *
     * A user who cannot read it gets no entry rather than a disabled one. The scope is still
     * gated in the store for anyone arriving by URL — this only stops the drive advertising a
     * door that opens onto nothing.
     */
    readonly $systemHostVisible = this.#store.systemHostCanRead;

    /**
     * Whether a drag is currently over the all-site-content entry.
     *
     * Held so the row can look refused rather than inert. A gesture that simply does nothing
     * reads as a broken UI, and this row is the one place in the sidebar where a drop is
     * declined by what the row *means* rather than by a permission.
     */
    protected readonly $allSiteContentDragOver = signal(false);

    /**
     * Whether a drag is currently over the System Host row.
     *
     * Unlike the all-site-content row above, this one accepts the drop, so it marks itself as a
     * target rather than as refusing. The tree does the same for its own rows; without it, the one
     * place in the column that would take the item was the only one giving nothing back.
     */
    protected readonly $systemHostDragOver = signal(false);

    /**
     * The drop target that stands for System Host.
     *
     * An empty `path` is what marks it as the host itself rather than a folder on it — the same
     * distinction the upload contract draws, where a folder id with no path is a site. The
     * permission travels with the target so the shell's existing gate answers about System Host
     * instead of about whichever site the switcher happens to show.
     */
    private systemHostTarget(): DotFolderTreeNodeContentData {
        return {
            type: 'folder',
            id: SYSTEM_HOST.identifier,
            path: '',
            hostname: SYSTEM_HOST.hostname,
            permissions: [PERMISSIONS_TYPE.CAN_ADD_CHILDREN]
        } as DotFolderTreeNodeContentData;
    }

    /**
     * Offers the System Host entry as a drop target, but only while the user may add to it.
     *
     * Cancelling the event is what makes a drop possible at all, so declining to cancel is how
     * the row declines the drop — the browser then shows the "no drop" cursor on its own, which
     * is the refusal the spec asks for without inventing a second way to say it.
     */
    protected onSystemHostDragOver(event: DragEvent): void {
        if (this.$systemHostCanAddChildren() === false) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        this.$systemHostDragOver.set(true);
    }

    /** Clears the mark when the drag leaves without dropping. */
    protected onSystemHostDragLeave(): void {
        this.$systemHostDragOver.set(false);
    }

    /** Files land as an upload, anything else as a move — the same fork the tree makes. */
    protected onSystemHostDrop(event: DragEvent): void {
        this.$systemHostDragOver.set(false);

        if (this.$systemHostCanAddChildren() === false) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const targetFolder = this.systemHostTarget();
        const files = event.dataTransfer?.files ?? undefined;

        if (files?.length) {
            this.uploadFiles.emit({ files, targetFolder });

            return;
        }

        this.moveItems.emit({ targetFolder });
    }

    /**
     * All site content is never a destination: it spans every folder, and the site row directly
     * beneath it already means the site root. The event is deliberately left uncancelled so the
     * drop cannot happen; all this does is let the row say so while the drag is over it.
     */
    protected onAllSiteContentDragOver(): void {
        this.$allSiteContentDragOver.set(true);
    }

    protected onAllSiteContentDragLeave(): void {
        this.$allSiteContentDragOver.set(false);
    }

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
    readonly handleSelectedNodeFromTable = signalMethod<DotFolderTreeNodeItem | undefined>(
        (selectedNode) => {
            const data = selectedNode?.data;
            if (!data || data.type === LOAD_MORE_NODE_TYPE || !data.fromTable) {
                return;
            }

            const segments = data.path.split('/').filter(Boolean).slice(0, -1);

            this.recursiveExpandOneNode(segments);

            this.#revealNode(selectedNode, 'smooth');
        }
    );

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
     * Chooses the whole site. The store clears the tree's selection as it does so, because exactly
     * one entry in the sidebar is ever selected and the tree cannot represent this one.
     */
    protected onSelectAllSiteContent(): void {
        this.#store.selectAllSiteContent();
    }

    /**
     * Chooses System Host, which belongs to no site and so clears the tree's selection too.
     */
    protected onSelectSystemHost(): void {
        this.#store.selectSystemHost();
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
     * Root-level sentinels live alongside the root folders, which are the site node's children, so
     * they update that node's children. Nested sentinels update their own `parent.children`.
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
                    const siteNode = this.#siteNode();
                    const siblings = siteNode
                        ? ((siteNode.children as DotFolderTreeNodeItem[]) ?? [])
                        : current;

                    const loaded = siblings.filter(
                        (folder) => folder.data?.type !== LOAD_MORE_NODE_TYPE
                    );
                    // Merge rather than concatenate: the hierarchy load can pin a deep-linked folder to
                    // the top of a level out of sort order, and paging far enough returns it again.
                    const nextSiblings = appendLoadMoreNodes(
                        mergeFolderNodePage(loaded, folders),
                        totalEntries,
                        parentPath || '/',
                        hostname ?? '',
                        (nextPage ?? 1) + 1
                    );

                    // With a site row the root folders are its children; without one they are the
                    // top level itself, which is how a plain folder tree is shaped.
                    if (siteNode) {
                        siteNode.children = nextSiblings;
                        this.#store.updateFolders([...current]);

                        return;
                    }

                    this.#store.updateFolders(nextSiblings);

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
            if (node.data?.type !== LOAD_MORE_NODE_TYPE && node.data.path === path) {
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
     * The site row, when the tree has one. Recognised by having no folder path: every folder node
     * carries one, and the row that stands for a site does not. A tree can be a plain list of
     * folders with no site row at all, which is why this is a lookup rather than an assumption about
     * the first node.
     *
     * @returns {DotFolderTreeNodeItem | undefined} the site row, if the tree has one
     */
    #siteNode(): DotFolderTreeNodeItem | undefined {
        return this.$folders().find((folder) => !folder.data?.path);
    }

    /**
     * The folders at the top of the hierarchy: the site row's children when there is one, otherwise
     * the top level itself. Path walks start here, since the site row's empty path matches no
     * segment.
     *
     * @returns {DotFolderTreeNodeItem[]} the root folders
     */
    #rootFolders(): DotFolderTreeNodeItem[] {
        const siteNode = this.#siteNode();

        return siteNode ? ((siteNode.children as DotFolderTreeNodeItem[]) ?? []) : this.$folders();
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
        nodes: DotFolderTreeNodeItem[] = this.#rootFolders()
    ): void {
        if (segments.length === 0) {
            return;
        }

        const node = nodes.find(
            (candidate) =>
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
