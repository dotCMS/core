import {
    patchState,
    signalStoreFeature,
    withMethods,
    type,
    withState,
    withHooks
} from '@ngrx/signals';
import { Observable, of } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import { PermissionType } from '@dotcms/dotcms-models';
import { ALL_FOLDER, DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { SYSTEM_HOST } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import {
    applyLoadMoreToHierarchy,
    FolderTreeHierarchyLevel,
    getFolderHierarchyByPath,
    getFolderNodesByPath,
    getFolderPermissionsByPath
} from '../../../utils/functions';
import { buildTreeFolderNodes } from '../../../utils/tree-folder.utils';

interface WithSidebarState {
    sidebarLoading: boolean;
    folders: DotFolderTreeNodeItem[];
    selectedNode: DotFolderTreeNodeItem;
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
                if (!currentSite || currentSite.identifier === SYSTEM_HOST.identifier) {
                    return;
                }

                const realAllFolder: DotFolderTreeNodeItem = {
                    ...ALL_FOLDER,
                    data: {
                        hostname: currentSite.hostname,
                        path: '',
                        type: 'folder',
                        id: currentSite.identifier
                    }
                };

                const urlFolderPath = store.path() || '';

                getFolderHierarchyByPath(urlFolderPath, currentSite, dotFolderService)
                    .pipe(
                        take(1),
                        catchError((response) => {
                            const error = response.error;
                            if (error?.message) {
                                console.error('Error loading folders:', error.message);
                            } else {
                                console.error('Error loading folders:', response);
                            }

                            return of([] as FolderTreeHierarchyLevel[]);
                        })
                    )
                    .subscribe((levels) => {
                        const { rootNodes, selectedNode } = buildTreeFolderNodes({
                            folderHierarchyLevels: levels.map((level) => level.folders),
                            targetPath: urlFolderPath || '/',
                            rootNode: realAllFolder
                        });

                        const rootsWithLoadMore = applyLoadMoreToHierarchy(
                            rootNodes,
                            levels,
                            currentSite.hostname
                        );

                        patchState(store, {
                            sidebarLoading: false,
                            folders: [realAllFolder, ...rootsWithLoadMore],
                            selectedNode: selectedNode
                        });
                    });
            },

            /**
             * Loads child folders for a specific path
             */
            loadChildFolders: (
                path: string,
                hostname?: string,
                page = 1
            ): Observable<{ folders: DotFolderTreeNodeItem[]; totalEntries: number }> => {
                const currentSite = store.currentSite();

                if (!currentSite) {
                    return of({ folders: [], totalEntries: 0 });
                }

                const host = hostname || currentSite.hostname;

                return getFolderNodesByPath(
                    path,
                    { ...currentSite, hostname: host },
                    dotFolderService,
                    page
                );
            },
            /**
             * Resolves the permission types the current user holds on a single folder.
             *
             * Only needed for nodes hydrated by {@link loadFolders}: that call resolves the whole
             * deep-link hierarchy in one large page, which exceeds the backend's cap for
             * `includePermissions`, so those nodes arrive without permissions. Nodes loaded by
             * expanding a folder already carry them and never reach this method.
             *
             * Emits `undefined` when the lookup fails or the folder is not found, so the caller can
             * tell that apart from "the user holds no permissions" (`[]`).
             */
            loadFolderPermissions: (
                folderPath: string,
                folderId: string,
                folderName: string
            ): Observable<PermissionType[] | undefined> => {
                const currentSite = store.currentSite();

                if (!currentSite) {
                    return of(undefined);
                }

                return getFolderPermissionsByPath(
                    folderPath,
                    folderId,
                    folderName,
                    currentSite,
                    dotFolderService
                ).pipe(
                    take(1),
                    catchError((response) => {
                        console.error('Error loading folder permissions:', response);

                        return of(undefined);
                    })
                );
            },

            /**
             * Sets the selected node
             */
            setSelectedNode: (selectedNode: DotFolderTreeNodeItem) => {
                patchState(store, {
                    selectedNode
                });
            },

            /**
             * Updates the folders array.
             * Uses structuredClone to create a deep copy of the folders array.
             * This is necessary because TreeNode objects have nested properties (children, data)
             * and a shallow copy would maintain references to the original objects,
             * preventing Angular's change detection from detecting updates.
             */
            updateFolders: (folders: DotFolderTreeNodeItem[]) => {
                patchState(store, { folders: structuredClone(folders) });
            }
        })),
        withHooks((store) => {
            return {
                onInit() {
                    store.loadFolders();
                }
            };
        })
    );
}
