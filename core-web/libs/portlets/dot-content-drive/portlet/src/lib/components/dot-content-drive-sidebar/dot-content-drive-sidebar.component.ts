import { forkJoin, Observable } from 'rxjs';

import { JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { map, tap } from 'rxjs/operators';

import { DotFolderService, DotFolder } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotTreeFolderComponent } from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import {
    ALL_FOLDER,
    createTreeNode,
    generateAllParentPaths,
    buildTreeFolderNodes,
    TreeNodeItem
} from '../../utils/tree-folder.utils';
import { DotContentDriveTreeTogglerComponent } from '../dot-content-drive-toolbar/components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';

@Component({
    selector: 'dot-content-drive-sidebar',
    templateUrl: './dot-content-drive-sidebar.component.html',
    styleUrl: './dot-content-drive-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotTreeFolderComponent, DotContentDriveTreeTogglerComponent, JsonPipe],
    providers: [DotFolderService]
})
export class DotContentDriveSidebarComponent {
    readonly #globalStore = inject(GlobalStore);
    readonly #store = inject(DotContentDriveStore);
    readonly #dotFolderService = inject(DotFolderService);

    readonly $loading = signal<boolean>(true);
    readonly $folders = signal<TreeNodeItem[]>([]);
    readonly $selectedNode = signal<TreeNodeItem>(ALL_FOLDER);
    readonly $currentSite = this.#globalStore.siteDetails;

    readonly getSiteFoldersEffect = effect(() => {
        const currentSite = this.$currentSite();
        if (!currentSite) {
            return;
        }
        const URLForlderPath = this.#store.path() || '';
        const fullPath = untracked(() => `${currentSite.hostname}${URLForlderPath}`);

        this.getFolderHierarchyByPath(fullPath).subscribe((folders) => {
            const { rootNodes, selectedNode } = buildTreeFolderNodes(
                folders,
                URLForlderPath || '/'
            );
            this.$loading.set(false);
            this.$folders.set([ALL_FOLDER, ...rootNodes]);
            this.$selectedNode.set(selectedNode);
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
        this.$selectedNode.set(node);
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
        this.getFolderNodesByPath(fullPath).subscribe(({ folders }) => {
            node.loading = false;
            node.expanded = true;
            node.leaf = folders.length === 0;
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
    protected onNodeCollapse(event: TreeNodeCollapseEvent): void {
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
    private getFolderHierarchyByPath(path: string): Observable<DotFolder[][]> {
        const paths = generateAllParentPaths(path);
        const folderRequests = paths.map((path) => this.#dotFolderService.getFolders(path));

        return forkJoin(folderRequests);
    }

    /**
     * Fetches folders and transforms them into tree nodes
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>}
     */
    private getFolderNodesByPath(
        path: string
    ): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
        return this.#dotFolderService.getFolders(path).pipe(
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
