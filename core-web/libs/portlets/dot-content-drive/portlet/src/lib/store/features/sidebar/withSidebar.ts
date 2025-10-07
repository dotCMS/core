import {
    patchState,
    signalStoreFeature,
    withMethods,
    type,
    withState,
    withHooks
} from '@ngrx/signals';
import { Observable } from 'rxjs';

import { inject, effect, EffectRef } from '@angular/core';

import { DotFolderService } from '@dotcms/data-access';
import { DotFolder } from '@dotcms/dotcms-models';

import { DotContentDriveState } from '../../../shared/models';
import { getFolderHierarchyByPath, getFolderNodesByPath } from '../../../utils/functions';
import { ALL_FOLDER, buildTreeFolderNodes, TreeNodeItem } from '../../../utils/tree-folder.utils';

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
