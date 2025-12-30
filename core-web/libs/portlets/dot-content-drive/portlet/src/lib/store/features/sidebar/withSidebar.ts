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
import { DotFolder } from '@dotcms/dotcms-models';
import { ALL_FOLDER, DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { SYSTEM_HOST } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import { getFolderHierarchyByPath, getFolderNodesByPath } from '../../../utils/functions';
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
                const fullPath = `${currentSite.hostname}${urlFolderPath}`;

                getFolderHierarchyByPath(fullPath, dotFolderService)
                    .pipe(
                        take(1),
                        catchError((response) => {
                            const error = response.error;
                            if (error?.message) {
                                console.error('Error loading folders:', error.message);
                            } else {
                                console.error('Error loading folders:', response);
                            }

                            return of([[realAllFolder as DotFolder]]);
                        })
                    )
                    .subscribe((folders) => {
                        const { rootNodes, selectedNode } = buildTreeFolderNodes({
                            folderHierarchyLevels: folders,
                            targetPath: urlFolderPath || '/',
                            rootNode: realAllFolder
                        });

                        patchState(store, {
                            sidebarLoading: false,
                            folders: [realAllFolder, ...rootNodes],
                            selectedNode: selectedNode
                        });
                    });
            },

            /**
             * Loads child folders for a specific path
             */
            loadChildFolders: (
                path: string,
                hostname?: string
            ): Observable<{ parent: DotFolder; folders: DotFolderTreeNodeItem[] }> => {
                const host = hostname || store.currentSite()?.hostname;
                const fullPath = `${host}${path}`;

                return getFolderNodesByPath(fullPath, dotFolderService);
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
             * Updates the folders array
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
