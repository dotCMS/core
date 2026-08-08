import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import type { TreeNode } from 'primeng/api';
import type {
    TreeNodeCollapseEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { appendLoadMoreNodes } from '@dotcms/data-access';
import {
    ComponentStatus,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';

import { DotFolderNamePipe } from '../../../../pipes/dot-folder-name/dot-folder-name.pipe';
import { ALL_FOLDER } from '../../../dot-folder-tree/constants';
import { DotFolderTreeComponent } from '../../../dot-folder-tree/dot-folder-tree.component';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * Folder navigation for the AssetPicker.
 *
 * Selecting a node scopes the asset list to that folder. Unlike Content Drive's sidebar this one
 * has no drag-and-drop of rows and no tree toggler — a fixed-width dialog has nothing to collapse
 * into — so it binds the presentational {@link DotFolderTreeComponent} directly.
 */
@Component({
    selector: 'dot-asset-picker-sidebar',
    templateUrl: './dot-asset-picker-sidebar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotFolderTreeComponent, DotFolderNamePipe],
    host: { class: 'block h-full w-full min-h-0' }
})
export class DotAssetPickerSidebarComponent {
    readonly store = inject(DotAssetPickerStore);

    protected readonly $loading = computed(
        () => this.store.foldersStatus() === ComponentStatus.LOADING
    );

    protected readonly treePt = {
        root: { class: 'w-full h-full min-w-0 overflow-x-hidden border-none' },
        wrapper: { class: 'min-w-0 overflow-x-hidden' },
        nodeContent: { class: 'min-w-0' },
        nodeLabel: { class: 'min-w-0 overflow-hidden' }
    };

    /**
     * Scopes the list to the selected folder. Content Drive needs an effect for this to resolve a
     * race with its URL restore; the picker is configured explicitly, so a direct call is enough.
     */
    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const node = event.node as TreeNodeItem;
        const data = node.data;

        if (!data || data.type === LOAD_MORE_NODE_TYPE) {
            return;
        }

        this.store.setSelectedNode(node);
        this.store.setPath(data.path || undefined);
    }

    /** Lazily loads a folder's first page of children the first time it is expanded. */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const node = event.node as TreeNodeItem;
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
        this.store.loadChildFolders(path, hostname).subscribe(({ folders, totalEntries }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
            node.children = appendLoadMoreNodes(folders, totalEntries, path, hostname, 2);
            this.store.updateFolders([...this.store.folders()]);
        });
    }

    /**
     * Loads the next page of a level when its "Load more" sentinel is clicked.
     *
     * Root-level sentinels are siblings of the root folders rather than children of ALL_FOLDER, so
     * that branch rebuilds the top-level array; nested ones rewrite `parent.children`.
     */
    protected onLoadMore(node: TreeNode): void {
        const { path, hostname, nextPage } = node.data as TreeNodeLoadMoreData;
        const parentPath = path ?? '/';

        node.loading = true;
        this.store.updateFolders([...this.store.folders()]);

        this.store
            .loadChildFolders(parentPath, hostname, nextPage)
            .subscribe(({ folders, totalEntries }) => {
                const isRootLevel = parentPath === '/' || parentPath === '';
                const nextPageAfter = (nextPage ?? 1) + 1;

                if (isRootLevel) {
                    const current = this.store.folders();
                    const allFolder =
                        current.find((folder) => folder.key === ALL_FOLDER.key) ?? ALL_FOLDER;
                    const loaded = current.filter(
                        (folder) =>
                            folder.key !== ALL_FOLDER.key &&
                            folder.data?.type !== LOAD_MORE_NODE_TYPE
                    );

                    this.store.updateFolders([
                        allFolder,
                        ...appendLoadMoreNodes(
                            [...loaded, ...folders],
                            totalEntries,
                            parentPath || '/',
                            hostname ?? '',
                            nextPageAfter
                        )
                    ]);

                    return;
                }

                const parent = this.#findNodeByPath(parentPath, this.store.folders());

                if (!parent) {
                    return;
                }

                // Keep what is already loaded, drop the old sentinel, append the new page.
                const loaded = ((parent.children as TreeNodeItem[]) ?? []).filter(
                    (child) => child.data?.type !== LOAD_MORE_NODE_TYPE
                );

                parent.children = appendLoadMoreNodes(
                    [...loaded, ...folders],
                    totalEntries,
                    parentPath,
                    hostname ?? '',
                    nextPageAfter
                );
                this.store.updateFolders([...this.store.folders()]);
            });
    }

    /** ALL_FOLDER is the tree's root scope; collapsing it would hide everything. */
    protected onNodeCollapse(event: TreeNodeCollapseEvent): void {
        if (event.node.key === ALL_FOLDER.key) {
            event.node.expanded = true;
        }
    }

    /** Depth-first lookup of a folder node by path, skipping "Load more" sentinels. */
    #findNodeByPath(path: string, nodes: TreeNodeItem[]): TreeNodeItem | undefined {
        for (const node of nodes) {
            if (node.data?.type !== LOAD_MORE_NODE_TYPE && node.data?.path === path) {
                return node;
            }

            const found = node.children
                ? this.#findNodeByPath(path, node.children as TreeNodeItem[])
                : undefined;

            if (found) {
                return found;
            }
        }

        return undefined;
    }
}
