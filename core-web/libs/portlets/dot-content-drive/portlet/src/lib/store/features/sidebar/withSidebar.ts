import {
    patchState,
    signalStoreFeature,
    withMethods,
    type,
    withState,
    withHooks
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { Observable, of, pipe, switchMap, tap } from 'rxjs';

import { inject } from '@angular/core';

import { catchError } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { SYSTEM_HOST } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import {
    applyLoadMoreToHierarchy,
    FolderTreeHierarchyLevel,
    getFolderHierarchyByPath,
    getFolderNodesByPath
} from '../../../utils/functions';
import { buildTreeFolderNodes, createSiteNode } from '../../../utils/tree-folder.utils';

interface WithSidebarState {
    sidebarLoading: boolean;
    folders: DotFolderTreeNodeItem[];
    /** Undefined until a site resolves and its tree is built: there is no node to select before then. */
    selectedNode: DotFolderTreeNodeItem | undefined;
}

export function withSidebar() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithSidebarState>({
            sidebarLoading: true,
            folders: [],
            selectedNode: undefined
        }),
        withMethods((store, dotFolderService = inject(DotFolderService)) => ({
            /**
             * Loads the folder tree for the current site and path.
             *
             * An `rxMethod` rather than a plain method so a newer load **cancels** the one in
             * flight. Two triggers call this on a cold load — this feature's own `onInit` and the
             * sidebar component's `currentSite` effect — and while it was a bare `.subscribe()`
             * both writes landed, so whichever request *resolved* last won regardless of which
             * *started* last. A slower earlier response then overwrote a newer complete one and the
             * tree kept the wrong folders until the next reload, intermittently and only on a cold
             * load. `switchMap` makes the newest call the only one that can still write.
             */
            loadFolders: rxMethod<void>(
                pipe(
                    // Read here, not in a closure over the call: the newest emission decides which
                    // site and path the write belongs to.
                    switchMap(() => {
                        const currentSite = store.currentSite();

                        // SYSTEM_HOST is the pre-resolution seed, not a site anyone browses.
                        if (!currentSite || currentSite.identifier === SYSTEM_HOST.identifier) {
                            return of(null);
                        }

                        const siteNode = createSiteNode(currentSite);
                        const urlFolderPath = store.path() || '';

                        // Only the initial state used to set this, so every later cold load (a site
                        // change) left the previous site's tree on screen while its replacement was
                        // fetched, with no indication anything was happening. It also gives
                        // consumers the loaded edge they need to reveal the folder the drive opened
                        // on. Inside `switchMap` so a cancelled load never leaves it stuck on.
                        patchState(store, { sidebarLoading: true });

                        return getFolderHierarchyByPath(
                            urlFolderPath,
                            currentSite,
                            dotFolderService
                        ).pipe(
                            // Inside the inner pipe: an outer `catchError` would end the whole
                            // `rxMethod` subscription, so the first failed load would be the last
                            // one this store ever ran.
                            catchError((response) => {
                                const error = response.error;
                                if (error?.message) {
                                    console.error('Error loading folders:', error.message);
                                } else {
                                    console.error('Error loading folders:', response);
                                }

                                return of([] as FolderTreeHierarchyLevel[]);
                            }),
                            tap((levels) => {
                                const { rootNodes, selectedNode } = buildTreeFolderNodes({
                                    folderHierarchyLevels: levels.map((level) => level.folders),
                                    targetPath: urlFolderPath || '/',
                                    rootNode: siteNode
                                });

                                const rootsWithLoadMore = applyLoadMoreToHierarchy(
                                    rootNodes,
                                    levels,
                                    currentSite.hostname
                                );

                                patchState(store, {
                                    sidebarLoading: false,
                                    // The site's folders are the site node's children, not its
                                    // siblings, so its chevron collapses the whole site the way any
                                    // folder's collapses its own subtree. As siblings they sat at
                                    // the same level as the site while its chevron controlled
                                    // nothing, and expanding it fetched them a second time — the
                                    // tree showed every root folder twice.
                                    folders: [{ ...siteNode, children: rootsWithLoadMore }],
                                    selectedNode: selectedNode
                                });
                            })
                        );
                    })
                )
            ),

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
             * Sets the selected node
             */
            setSelectedNode: (selectedNode: DotFolderTreeNodeItem) => {
                patchState(store, {
                    selectedNode
                });
            },

            /**
             * Selects the tree's root row, the one that stands for the site rather than a folder.
             *
             * Used when a search spans the whole site, where no single folder is the selected one.
             * A tree of plain folders has no such row, and then nothing is selected, which says the
             * same thing.
             */
            selectRootNode: () => {
                patchState(store, {
                    selectedNode: store.folders().find((folder) => !folder.data?.path)
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
