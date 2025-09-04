import { forkJoin, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { map, pluck, tap } from 'rxjs/operators';

import { GlobalStore } from '@dotcms/store';
import { DotTreeFolderComponent } from '@dotcms/ui';

import {
    ALL_FOLDER,
    createTreeNode,
    DotFolder,
    generateAllParentPaths,
    TreeNodeItem
} from './utils';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

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

    constructor() {
        this.testFetchAllParentFolders();
    }

    /**
     * Test method to demonstrate fetchAllParentFolders functionality
     * Uncomment the call in getSiteFoldersEffect to test
     */
    private testFetchAllParentFolders(): void {
        const testPath = 'demo.dotcms.com/application/apivtl/';

        this.fetchAllParentFolders(testPath).subscribe({
            next: (_allFolders) => {
                // console.log(_allFolders);
            }
        });
    }

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

        if (node.children?.length > 0) {
            node.loading = false;
            node.expanded = true;
            return;
        }

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
     * Fetches all parent folders from a given path using parallel API calls
     *
     * Example: '/main/sub-folder/inner-folder/child-folder' will make calls to:
     * - /main/sub-folder/inner-folder/child-folder
     * - /main/sub-folder/inner-folder
     * - /main/sub-folder
     * - /main
     * - /
     *
     * @param {string} hostname - The site hostname
     * @param {string} targetPath - The full path to generate parent paths from
     * @returns {Observable<DotFolder[][]>} Observable that emits an array of folder arrays (one for each path level)
     */
    fetchAllParentFolders(path: string): Observable<DotFolder[][]> {
        const paths = generateAllParentPaths(path);

        // Create API calls for each path, prefixed with hostname
        const folderRequests = paths.map((path) => {
            const fullPath = `/${path}`;
            return this.getFolders(fullPath);
        });

        return forkJoin(folderRequests);
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
                    folders: childFolders.map((folder) => createTreeNode(folder))
                };
            })
        );
    }
}
