import { tapResponse } from '@ngrx/operators';
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, exhaustMap, switchMap } from 'rxjs/operators';

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
             * Loads the sites tree structure
             * @description Fetches the initial tree structure of sites with pagination
             * @method loadSites
             */
            loadSites: rxMethod<void>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(() => {
                        return dotBrowsingService
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
                            );
                    })
                )
            ),
            /**
             * Loads children nodes for a selected tree node
             * @method loadChildren
             * @param {TreeNodeSelectItem} event - The selected tree node item
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
             * @param {TreeNodeSelectItem} event - The selected tree node item
             */
            chooseNode: (event: TreeNodeSelectItem) => {
                const { node: nodeSelected } = event;
                const data = nodeSelected.data;
                if (!data) {
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
            }
        };
    })
);
