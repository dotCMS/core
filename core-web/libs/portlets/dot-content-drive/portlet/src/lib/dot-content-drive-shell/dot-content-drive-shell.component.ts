import { EMPTY, Observable } from 'rxjs';

import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    effect,
    inject,
    signal
} from '@angular/core';
import { Router } from '@angular/router';

import { LazyLoadEvent, SortEvent, TreeNode } from 'primeng/api';
import { TreeNodeCollapseEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { catchError, map, pluck, take } from 'rxjs/operators';

import { DotContentSearchService } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { DotTreeFolderComponent } from '@dotcms/ui';

import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { SORT_ORDER, SYSTEM_HOST } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { encodeFilters } from '../utils/functions';

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

@Component({
    selector: 'dot-content-drive-shell',
    imports: [DotFolderListViewComponent, DotContentDriveToolbarComponent, DotTreeFolderComponent],
    providers: [DotContentDriveStore],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #http = inject(HttpClient);
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #cd = inject(ChangeDetectorRef);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;
    $folders = signal<TreeNodeItem[]>([]);
    $loading = signal<boolean>(false);

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;

    readonly itemsEffect = effect(() => {
        const query = this.#store.$query();
        const currentSite = this.#store.currentSite();
        const { limit, offset } = this.#store.pagination();
        const { field, order } = this.#store.sort();

        this.#store.setStatus(DotContentDriveStatus.LOADING);

        // Avoid fetching content for SYSTEM_HOST sites
        if (currentSite?.identifier === SYSTEM_HOST.identifier) {
            return;
        }

        this.#contentSearchService
            .get<ESContent>({
                query,
                limit,
                offset,
                sort: `${field} ${order}`
            })
            .pipe(
                take(1),
                catchError(() => {
                    this.#store.setStatus(DotContentDriveStatus.ERROR);

                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.#store.setItems(response.jsonObjectView.contentlets, response.resultsSize);
            });
    });

    readonly updateQueryParamsEffect = effect(() => {
        const isTreeExpanded = this.#store.isTreeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        const queryParams: Record<string, string> = {};

        queryParams['isTreeExpanded'] = isTreeExpanded.toString();

        if (path && path.length) {
            queryParams['path'] = path;
        }

        if (filters && Object.keys(filters).length) {
            queryParams['filters'] = encodeFilters(filters);
        } else {
            delete queryParams['filters'];
        }

        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
    });

    constructor() {
        this.getFoldersTreeNode(`demo.dotcms.com`).subscribe((folders) =>
            this.$folders.set([...folders.folders])
        );
    }

    onPaginate(event: LazyLoadEvent) {
        // Explicit check because it can potentially be 0
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.#store.setPagination({
            limit: event.rows,
            offset: event.first
        });
    }

    onSort(event: SortEvent) {
        // Explicit check because it can potentially be 0
        if (event.order === undefined || !event.field) {
            return;
        }

        this.#store.setSort({
            field: event.field,
            order: SORT_ORDER[event.order] ?? DotContentDriveSortOrder.ASC
        });
    }

    // MOVE THIS TO A SERVICE

    /**
     *
     *
     * @param {string} path
     * @return {*}  {Observable<DotFolder[]>}
     * @memberof DotEditContentService
     */
    getFolders(path: string): Observable<DotFolder[]> {
        return this.#http.post<DotFolder>('/api/v1/folder/byPath', { path }).pipe(pluck('entity'));
    }

    /**
     * Retrieves folders and transforms them into a tree node structure.
     * The first folder in the response is considered the parent folder.
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>} Observable that emits an object containing the parent folder and child folders as TreeNodeItems
     */
    getFoldersTreeNode(path: string): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
        return this.getFolders(`//${path}`).pipe(
            map((folders) => {
                const parent = folders.shift();

                return {
                    parent,
                    folders: folders.map((folder) => {
                        // Split by sladh and remove empty
                        const arrayPath = folder.path.split('/').filter((path) => path);
                        const label = arrayPath.pop();

                        return {
                            key: folder.id,
                            label,
                            loading: true,
                            data: {
                                id: folder.id,
                                hostname: folder.hostName,
                                path: folder.path,
                                type: 'folder'
                            },
                            leaf: false
                        };
                    })
                };
            })
        );
    }

    // onNodesSelect(event: TreeNodeSelectEvent) {
    //     const { node } = event;
    //     const { path } = node.data;

    //     this.#store.setPath(path);
    // }

    onNodeExpand(event: TreeNodeSelectEvent) {
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

    onNodeCollapse(event: TreeNodeCollapseEvent) {
        const { node } = event;

        if (node.key === 'ALL_FOLDER') {
            node.expanded = true;

            return;
        }
    }
}
