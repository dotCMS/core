import { tapResponse } from '@ngrx/operators';
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, tap, exhaustMap, switchMap } from 'rxjs/operators';

import { ComponentStatus, TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

/** Maximum number of items to fetch per page */
export const PEER_PAGE_LIMIT = 7000;

/**
 * Represents the state structure for the Site Field component
 * @interface SiteFieldState
 */
export type SiteFieldState = {
    nodeSelected: TreeNodeItem | null;
    nodeExpanded: TreeNodeSelectItem['node'] | null;
    tree: TreeNodeItem[];
    status: ComponentStatus;
    error: string | null;
};

/**
 * Initial state for the Site Field store
 */
export const initialState: SiteFieldState = {
    nodeSelected: null,
    nodeExpanded: null,
    tree: [],
    status: ComponentStatus.INIT,
    error: null
};

/**
 * Signal store for managing site field state and operations
 * Provides functionality for loading and managing site tree data
 */
export const SiteFieldStore = signalStore(
    withState(initialState),
    withComputed(({ status, nodeSelected }) => ({
        /** Indicates if the store is in a loading state */
        isLoading: computed(() => status() === ComponentStatus.LOADING),
        /** Computed value to be saved, derived from the selected node */
        valueToSave: computed(() => {
            const node = nodeSelected();

            if (node?.data?.id && node?.data?.type) {
                return `${node.data.type}:${node.data.id}`;
            }

            return null;
        })
    })),
    withMethods((store) => {
        const dotBrowsingService = inject(DotBrowsingService);

        return {
            /**
             * Loads the sites tree structure.
             * After loading, if there is a synthetic nodeSelected (from setInitialSelection),
             * automatically resolves it to a real tree node for proper TreeSelect highlighting.
             * @method loadSites
             */
            loadSites: rxMethod<void>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(() =>
                        dotBrowsingService
                            .getSitesTreePath({
                                perPage: PEER_PAGE_LIMIT,
                                filter: '*',
                                page: 1
                            })
                            .pipe(
                                tapResponse({
                                    next: (sites) =>
                                        patchState(store, {
                                            tree: sites,
                                            status: ComponentStatus.LOADED
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            status: ComponentStatus.ERROR,
                                            error: ''
                                        })
                                })
                            )
                    ),
                    // After sites load, resolve synthetic selection to real tree node
                    switchMap(() => {
                        const selected = store.nodeSelected();
                        const tree = store.tree();

                        if (!selected?.data || tree.length === 0) {
                            return EMPTY;
                        }

                        const { id, type, hostname } = selected.data;

                        // For sites: find the real node by id
                        if (type === 'site') {
                            const realNode = tree.find((s) => s.data?.id === id);
                            if (realNode) {
                                patchState(store, { nodeSelected: realNode });
                            }

                            return EMPTY;
                        }

                        // For folders: find the site, load its children, find the folder
                        const siteNode = tree.find((s) => s.data?.hostname === hostname);

                        if (!siteNode) {
                            return EMPTY;
                        }

                        // Load the site's root children (same as manual expand)
                        // so the tree shows the first-level folders under the site.
                        // Known limitation: only root-level folders are loaded, so nested
                        // folders (e.g. /news/2024/) won't be visually highlighted in the
                        // TreeSelect. The chip label and search filter remain correct.
                        return dotBrowsingService.getFoldersTreeNode(`${hostname}/`).pipe(
                            tap(({ folders }) => {
                                const expandedSite: TreeNodeItem = {
                                    ...siteNode,
                                    leaf: true,
                                    icon: 'pi pi-folder-open',
                                    expanded: true,
                                    children: [...folders]
                                };

                                const realFolder = folders.find((f) => f.data?.id === id);

                                // Replace the site node immutably so PrimeNG detects the change
                                patchState(store, {
                                    tree: store
                                        .tree()
                                        .map((node) => (node === siteNode ? expandedSite : node)),
                                    nodeExpanded: expandedSite,
                                    ...(realFolder && {
                                        nodeSelected: realFolder
                                    })
                                });
                            }),
                            // If folder resolution fails, keep the synthetic node — the
                            // chip label and search filter are still correct.
                            catchError(() => EMPTY)
                        );
                    })
                )
            ),
            /**
             * Loads children nodes for a selected tree node
             * @method loadChildren
             */
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const { hostname, path } = node.data;

                        const fullPath = `${hostname}/${path}`;

                        return dotBrowsingService.getFoldersTreeNode(fullPath).pipe(
                            tap(({ folders }) => {
                                node.leaf = true;
                                node.icon = 'pi pi-folder-open';
                                node.children = [...folders];
                                patchState(store, { nodeExpanded: node });
                            })
                        );
                    })
                )
            ),
            /**
             * Updates the store with the selected node
             * @method chooseNode
             */
            chooseNode: (event: TreeNodeSelectItem) => {
                const { node: nodeSelected } = event;
                if (!nodeSelected.data) {
                    return;
                }

                patchState(store, { nodeSelected });
            },
            /**
             * Clears the selected node when a node is deselected
             * @method clearSelection
             */
            clearSelection: () => {
                patchState(store, { nodeSelected: null });
            },
            /**
             * Sets an initial selection from a pre-populated value (e.g., from contentlet context).
             * Creates a synthetic TreeNodeItem for immediate display in the TreeSelect header.
             * The real tree node is resolved automatically when loadSites completes.
             * @method setInitialSelection
             */
            setInitialSelection: (params: {
                id: string;
                type: 'site' | 'folder';
                hostname: string;
                path: string;
            }) => {
                const isSite = params.type === 'site';
                const label = isSite ? params.hostname : `${params.hostname}${params.path}`;

                patchState(store, {
                    nodeSelected: {
                        key: params.id,
                        label,
                        data: {
                            id: params.id,
                            type: params.type,
                            hostname: params.hostname,
                            path: params.path
                        },
                        icon: isSite ? 'pi pi-globe' : 'pi pi-folder',
                        leaf: !isSite,
                        children: []
                    }
                });
            }
        };
    })
);
