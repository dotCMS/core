import {
    patchState,
    signalStoreFeature,
    withMethods,
    type,
    withState,
    withHooks
} from '@ngrx/signals';
import { forkJoin, Observable } from 'rxjs';

import { inject, effect, EffectRef } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotFolderService, DotFolder } from '@dotcms/data-access';

import { DotContentDriveState } from '../../../shared/models';
import {
    ALL_FOLDER,
    createTreeNode,
    generateAllParentPaths,
    buildTreeFolderNodes,
    TreeNodeItem
} from '../../../utils/tree-folder.utils';

interface WithSidebarState {
    sidebarLoading: boolean;
    folders: TreeNodeItem[];
    selectedNode: TreeNodeItem;
}

export function withSidebar() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithSidebarState>({
            sidebarLoading: true,
            folders: [],
            selectedNode: ALL_FOLDER
        }),
        withMethods((store, dotFolderService = inject(DotFolderService)) => ({
            /**
             * Loads folders for the current site and path
             */
            loadFolders: () => {
                const currentSite = store.currentSite();
                if (!currentSite) {
                    return;
                }

                const urlFolderPath = store.path() || '';
                const fullPath = `${currentSite.hostname}${urlFolderPath}`;

                // patchState(store, { sidebarLoading: true });

                getFolderHierarchyByPath(fullPath, dotFolderService).subscribe((folders) => {
                    const { rootNodes, selectedNode } = buildTreeFolderNodes(
                        folders,
                        urlFolderPath || '/'
                    );

                    patchState(store, {
                        sidebarLoading: false,
                        folders: [ALL_FOLDER, ...rootNodes],
                        selectedNode: selectedNode || ALL_FOLDER
                    });
                });
            },

            /**
             * Loads child folders for a specific path
             */
            loadChildFolders: (
                path: string
            ): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> => {
                return getFolderNodesByPath(path, dotFolderService);
            },

            /**
             * Sets the selected node
             */
            setSelectedNode: (node: TreeNodeItem) => {
                patchState(store, { selectedNode: node });
            },

            /**
             * Updates the folders array
             */
            updateFolders: (folders: TreeNodeItem[]) => {
                patchState(store, { folders: [...folders] });
            }
        })),
        withHooks((store) => {
            let pathEffect: EffectRef;

            return {
                onInit() {
                    /**
                     * Listen to path changes and reload the folders.
                     * This effect is triggered when:
                     * - User performs a search
                     * - Folders are created/updated/deleted (CRUD operations)
                     */
                    pathEffect = effect(() => {
                        store.loadFolders();
                    });
                },
                onDestroy() {
                    pathEffect?.destroy();
                }
            };
        })
    );
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
 * @param {string} path - The full path to generate parent paths from
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<DotFolder[][]>} Observable that emits an array of folder arrays (one for each path level)
 */
function getFolderHierarchyByPath(
    path: string,
    dotFolderService: DotFolderService
): Observable<DotFolder[][]> {
    const paths = generateAllParentPaths(path);
    const folderRequests = paths.map((path) => dotFolderService.getFolders(path));

    return forkJoin(folderRequests);
}

/**
 * Fetches folders and transforms them into tree nodes
 *
 * @param {string} path - The path to fetch folders from
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>}
 */
function getFolderNodesByPath(
    path: string,
    dotFolderService: DotFolderService
): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
    return dotFolderService.getFolders(path).pipe(
        map((folders) => {
            const [parent, ...childFolders] = folders;

            return {
                parent,
                folders: childFolders.map((folder) => createTreeNode(folder))
            };
        })
    );
}
