import {
    patchState,
    signalStoreFeature,
    withMethods,
    type,
    withState,
    withHooks
} from '@ngrx/signals';
import { Observable } from 'rxjs';

import { inject, effect, EffectRef, untracked } from '@angular/core';

import { DotFolderService } from '@dotcms/data-access';
import { DotFolder } from '@dotcms/dotcms-models';

import { DotContentDriveState } from '../../../shared/models';
import { getFolderHierarchyByPath, getFolderNodesByPath } from '../../../utils/functions';
import { ALL_FOLDER, mergeTreeFolderNodes, TreeNodeItem } from '../../../utils/tree-folder.utils';

interface WithSidebarState {
    sidebarLoading: boolean;
    folders: TreeNodeItem[];
    selectedNode: TreeNodeItem;
    expandedFolderKeys: string[];
}

export function withSidebar() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithSidebarState>({
            sidebarLoading: true,
            folders: [],
            selectedNode: ALL_FOLDER,
            expandedFolderKeys: []
        }),
        withMethods((store, dotFolderService = inject(DotFolderService)) => ({
            /**
             * Loads folders for the current site and path, preserving existing tree state
             */
            loadFolders: () => {
                const currentSite = store.currentSite();
                if (!currentSite) {
                    return;
                }
            
                const urlFolderPath = store.path() || '';
                const fullPath = `${currentSite.hostname}${urlFolderPath}`;
            
                // Use stored expanded keys instead of extracting from current tree
                const currentExpandedKeys = untracked(() =>store.expandedFolderKeys());
            
                getFolderHierarchyByPath(fullPath, dotFolderService).subscribe((folders) => {
                    const currentFolders = store.folders();
                    const existingRootNodes = currentFolders.filter(node => node.key !== 'ALL_FOLDER');
            
                    const { rootNodes, selectedNode } = mergeTreeFolderNodes(
                        existingRootNodes,
                        folders,
                        urlFolderPath || '/',
                        currentExpandedKeys
                    );

                    // Update state, including expanded keys only if they changed
                    patchState(store, {
                        sidebarLoading: false,
                        folders: [ALL_FOLDER, ...rootNodes],
                        selectedNode: selectedNode || ALL_FOLDER,
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
             * Updates the folders array without changing expanded state tracking
             */
            updateFolders: (folders: TreeNodeItem[]) => {
                patchState(store, { folders: [...folders] });
            },

            /**
             * Updates expanded state when a node is manually expanded/collapsed
             * Uses Set for O(1) operations, then converts back to array
             */
            updateExpandedState: (nodeKey: string, expanded: boolean) => {
                const currentKeys = new Set(store.expandedFolderKeys());

                if (expanded) {
                    currentKeys.add(nodeKey);
                } else {
                    currentKeys.delete(nodeKey);
                }

                patchState(store, { expandedFolderKeys: Array.from(currentKeys) });
            },

            /**
             * Gets the current expanded keys
             */
            getExpandedKeys: () => store.expandedFolderKeys(),

            /**
             * Sets the selected node based on path without reloading folders
             */
            setSelectedNodeByPath: (path: string) => {
                const folders = store.folders();

                // Find the node that matches the given path
                const findNodeByPath = (nodes: TreeNodeItem[], targetPath: string): TreeNodeItem | null => {
                    for (const node of nodes) {
                        if (node.data.path === targetPath) {
                            return node;
                        }
                        if (node.children) {
                            const found = findNodeByPath(node.children, targetPath);
                            if (found) return found;
                        }
                    }
                    return null;
                };

                const targetNode = findNodeByPath(folders, path);
                if (targetNode) {
                    patchState(store, { selectedNode: targetNode });
                }
            }
        })),
        withHooks((store) => {
            let pathEffect: EffectRef;

            return {
                onInit() {
                    /**
                     * Listen to path changes and handle based on trigger type.
                     * This effect is triggered when:
                     * - 'navigation': User navigated to a folder (requires tree reload/expansion)
                     * - 'selection': User clicked on a folder to view contents (no tree reload needed)
                     * - 'crud': CRUD operations that affect folder structure (requires tree reload)
                     * - null: Initial state or search operations
                     */
                    pathEffect = effect(() => {
                        const triggerType = store.lastPathChangeTrigger();
                        const path = store.path();

                        switch (triggerType) {
                            case 'navigation':
                            case 'crud':
                                // Full tree reload for navigation or CRUD operations
                                store.loadFolders();
                                break;

                            case 'selection':
                                if (path) {
                                    // Just update selected node for content loading, preserve tree state
                                    store.setSelectedNodeByPath(path);
                                } else {
                                    // When path is cleared during search
                                    patchState(store, { selectedNode: ALL_FOLDER });
                                }
                                break;

                            case null:
                                // Initial load or search operations
                                if (path) {
                                    store.loadFolders();
                                } else {
                                    patchState(store, { selectedNode: ALL_FOLDER });
                                }
                                break;
                        }
                    });
                },
                onDestroy() {
                    pathEffect?.destroy();
                }
            };
        })
    );
}
