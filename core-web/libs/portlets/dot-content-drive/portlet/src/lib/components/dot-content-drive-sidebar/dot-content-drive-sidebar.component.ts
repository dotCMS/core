import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { map, pluck, tap } from 'rxjs/operators';

import { GlobalStore } from '@dotcms/store';
import { DotTreeFolderComponent } from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}

export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

export type TreeNodeItem = TreeNode<TreeNodeData>;

const ALL_FOLDER: TreeNodeItem = {
    key: 'ALL_FOLDER',
    label: 'All',
    loading: false,
    data: {
        type: 'folder',
        path: '',
        hostname: '',
        id: ''
    },
    leaf: false,
    expanded: true
};

@Component({
    selector: 'dot-content-drive-sidebar',
    templateUrl: './dot-content-drive-sidebar.component.html',
    styleUrl: './dot-content-drive-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent]
})
export class DotContentDriveSidebarComponent {
    readonly #globalStore = inject(GlobalStore);
    readonly #store = inject(DotContentDriveStore);
    readonly #http = inject(HttpClient);

    $folders = signal<TreeNodeItem[]>([]);
    $selectedNode = signal<TreeNodeItem>(ALL_FOLDER);
    $loading = signal<boolean>(true);

    readonly getSiteFoldersEffect = effect(() => {
        const currentSite = this.#globalStore.siteDetails();
        if (currentSite) {
            this.getFoldersTreeNode(`${currentSite.hostname}`).subscribe((folders) => {
                this.$folders.set([ALL_FOLDER, ...folders.folders]);
            });
        }
    });

    /**
     * Handles node selection events
     *
     * @param {TreeNodeSelectEvent} event - The tree node select event
     */
    onNodeSelect(event: TreeNodeSelectEvent): void {
        const { node } = event;
        const { path } = node.data;

        this.#store.setPath(path);
    }

    /**
     * Handles node expansion events and loads child folders
     *
     * @param {TreeNodeExpandEvent} event - The tree node expand event
     */
    onNodeExpand(event: TreeNodeExpandEvent): void {
        const { node } = event;
        const { hostname, path } = node.data;

        node.loading = true;
        const fullPath = `${hostname}${path}`;

        this.getFoldersTreeNode(fullPath).subscribe(({ folders }) => {
            node.loading = false;
            node.expanded = true;
            node.children = [...folders];
            this.$folders.set([...this.$folders()]);
        });
    }

    /**
     * Handles node collapse events
     * Prevents collapse of the special 'ALL_FOLDER' node
     *
     * @param {TreeNodeCollapseEvent} event - The tree node collapse event
     */
    onNodeCollapse(event: TreeNodeCollapseEvent): void {
        const { node } = event;

        if (node.key === 'ALL_FOLDER') {
            node.expanded = true;
            return;
        }
    }

    /**
     * Fetches folders by path from the API
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<DotFolder[]>} Observable that emits an array of folders
     */
    private getFolders(path: string): Observable<DotFolder[]> {
        return this.#http
            .post<{ entity: DotFolder[] }>('/api/v1/folder/byPath', { path })
            .pipe(pluck('entity'));
    }

    /**
     * Extracts the folder name from a full path
     *
     * @param {string} path - The full folder path
     * @returns {string} The folder name
     */
    private getFolderLabel(path: string): string {
        return path.split('/').filter(Boolean).pop() || '';
    }

    /**
     * Transforms a DotFolder into a TreeNodeItem
     *
     * @param {DotFolder} folder - The folder to transform
     * @returns {TreeNodeItem} The tree node item
     */
    private createTreeNode(folder: DotFolder): TreeNodeItem {
        return {
            key: folder.id,
            label: this.getFolderLabel(folder.path),
            data: {
                id: folder.id,
                hostname: folder.hostName,
                path: folder.path,
                type: 'folder'
            },
            leaf: false
        };
    }

    /**
     * Fetches folders and transforms them into tree nodes
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>}
     */
    private getFoldersTreeNode(
        path: string
    ): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
        return this.getFolders(`//${path}`).pipe(
            tap(() => this.$loading.set(false)),
            map((folders) => {
                const [parent, ...childFolders] = folders;

                return {
                    parent,
                    folders: childFolders.map((folder) => this.createTreeNode(folder))
                };
            })
        );
    }
}
