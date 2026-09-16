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

import { effect, EffectRef, inject, untracked } from '@angular/core';

import { catchError } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import {
    DEFAULT_PAGE,
    DEFAULT_PATH,
    ROOT_PATH,
    SYSTEM_HOST,
    SYSTEM_HOST_PATH
} from '../../../shared/constants';
import { DotContentDriveState, FolderTreeHierarchyLevel } from '../../../shared/models';
import {
    applyLoadMoreToHierarchy,
    getFolderHierarchyByPath,
    getFolderNodesByPath
} from '../../../utils/functions';
import {
    buildTreeFolderNodes,
    createSiteNode,
    findNodeByPath
} from '../../../utils/tree-folder.utils';

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

                        // Only a folder path names a place inside this site's hierarchy. The other
                        // two locations do not: all site content is the absence of one, and System
                        // Host is a host rather than a folder. Both were resolved as folder paths
                        // anyway, so System Host was queried as `/SYSTEM_HOST/` — a folder nobody
                        // has — and the empty result fell back to selecting the site row. That left
                        // the site root and System Host both looking selected, and since the shell
                        // syncs the location *from* the selected node, the site row then rewrote the
                        // location back to the site root and bounced the user out of System Host.
                        const location = store.path() || '';
                        const urlFolderPath = location.startsWith(ROOT_PATH) ? location : '';

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
                                    // No location means all site content, which is not a place in
                                    // the hierarchy. Preselecting the site row there would have the
                                    // sidebar claiming the root is what you are looking at, and the
                                    // root and the flat whole-site view are different things.
                                    selectedNode: urlFolderPath ? selectedNode : undefined
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
             * Selects all site content: the whole current site at any depth, which is the one
             * sidebar entry that names no place inside the hierarchy.
             *
             * Clearing the selected node is half the job. Exactly one thing in the sidebar is ever
             * selected, and the tree cannot represent this entry, so leaving a node selected would
             * have the sidebar claiming the user is in two places at once.
             *
             * The location is cleared rather than set to the root: absent is what all site content
             * looks like in the URL, which is also what links made before this feature carry.
             */
            selectAllSiteContent: () => {
                patchState(store, {
                    path: DEFAULT_PATH,
                    selectedNode: undefined,
                    pagination: { ...store.pagination(), page: 1, offset: 0 },
                    pages: [DEFAULT_PAGE]
                });
            },

            /**
             * Selects System Host: shared content on its own, which belongs to no site and so has
             * no place in the hierarchy either. Same shape as choosing all site content — one
             * entry selected, the tree's own selection cleared.
             */
            selectSystemHost: () => {
                patchState(store, {
                    path: SYSTEM_HOST_PATH,
                    selectedNode: undefined,
                    pagination: { ...store.pagination(), page: 1, offset: 0 },
                    pages: [DEFAULT_PAGE]
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
            let selectionSync: EffectRef | undefined;

            return {
                onInit() {
                    store.loadFolders();

                    // Keeps the tree's selection honest as the location moves.
                    //
                    // Folders reload on a site change, not on a Back, so returning to a folder
                    // restored the URL and the listing while the tree showed nothing selected.
                    // Everything else in the sidebar derives its selected state from the location
                    // and was therefore already right; this is the one stored piece, so it has to
                    // be pushed back in line rather than left holding whatever the previous
                    // location put there.
                    //
                    // Reads `folders()` as well as `path()` on purpose: on a cold start the tree
                    // is empty when the location is already known, and this has to run again once
                    // the folders arrive.
                    selectionSync = effect(() => {
                        const path = store.path();
                        const folders = store.folders();
                        // The tree marks its site row with an empty path, while the site root as a
                        // *location* is `/` — the same translation the shell makes in the other
                        // direction. Looking `/` up literally matches no node, so without this the
                        // sync read the site root as "nowhere" and cleared the selection every time
                        // the user was standing on it.
                        const match = path?.startsWith(ROOT_PATH)
                            ? findNodeByPath(folders, path === ROOT_PATH ? '' : path)
                            : undefined;

                        // Only when it actually differs: a folder click already sets the node, and
                        // rewriting the same one on every location change churns the tree.
                        untracked(() => {
                            if (store.selectedNode()?.data?.path !== match?.data?.path) {
                                patchState(store, { selectedNode: match });
                            }
                        });
                    });
                },
                onDestroy() {
                    selectionSync?.destroy();
                }
            };
        })
    );
}
