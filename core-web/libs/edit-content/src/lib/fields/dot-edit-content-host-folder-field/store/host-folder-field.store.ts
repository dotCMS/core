import { tapResponse } from '@ngrx/operators';
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, exhaustMap, switchMap, map, filter } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../models/dot-edit-content-host-folder-field.interface';
import { HostFieldService } from '../services/host-field.service';

export const PEER_PAGE_LIMIT = 7000;

export const SYSTEM_HOST_NAME = 'System Host';

export type HostFolderFiledState = {
    nodeSelected: TreeNodeItem | null;
    nodeExpaned: TreeNodeSelectItem['node'] | null;
    tree: TreeNodeItem[];
    status: ComponentStatus;
    error: string | null;
};

export const initialState: HostFolderFiledState = {
    nodeSelected: null,
    nodeExpaned: null,
    tree: [],
    status: ComponentStatus.INIT,
    error: null
};

export const HostFolderFiledStore = signalStore(
    withState(initialState),
    withComputed(({ status, nodeSelected }) => ({
        iconClasses: computed(() => {
            const currentStatus = status();

            return {
                'pi-spin pi-spinner': currentStatus === ComponentStatus.LOADING,
                'pi-chevron-down': currentStatus !== ComponentStatus.LOADING
            };
        }),
        pathToSave: computed(() => {
            const node = nodeSelected();

            if (node?.data) {
                const { data } = node;
                const newHostname = data.hostname.replace('//', '');

                return `${newHostname}:${data.path ? data.path : '/'}`;
            }

            return null;
        })
    })),
    withMethods((store) => {
        const hostFieldService = inject(HostFieldService);

        return {
            /**
             * Load the sites tree
             */
            loadSites: rxMethod<{ path: string | null; isRequired: boolean }>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(({ path, isRequired }) => {
                        return hostFieldService
                            .getSites({
                                perPage: PEER_PAGE_LIMIT,
                                filter: '*',
                                page: 1,
                                isRequired
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
                                }),
                                map((sites) => ({
                                    path,
                                    sites,
                                    isRequired
                                }))
                            );
                    }),
                    switchMap(({ path, sites, isRequired }) => {
                        if (path) {
                            return of({ path, sites });
                        }

                        if (isRequired) {
                            return hostFieldService.getCurrentSiteAsTreeNodeItem().pipe(
                                switchMap((currentSite) => {
                                    const node = sites.find(
                                        (item) => item.label === currentSite.label
                                    );

                                    return of({
                                        path: node?.label,
                                        sites
                                    });
                                })
                            );
                        }

                        const node = sites.find((item) => item.label === SYSTEM_HOST_NAME);

                        return of({
                            path: node?.label,
                            sites
                        });
                    }),
                    filter(({ path }) => !!path),
                    switchMap(({ path, sites }) => {
                        const hasPaths = path.includes('/');

                        if (!hasPaths) {
                            const response = {
                                node: sites.find((item) => item.data.hostname === path),
                                tree: null
                            };

                            return of(response);
                        }

                        return hostFieldService.buildTreeByPaths(path);
                    }),
                    tap(({ node, tree }) => {

                        const changes: Partial<HostFolderFiledState> = {};
                        if (node) {
                            changes.nodeSelected = node;
                        }

                        if (tree) {
                            const currentTree = store.tree();

                            const newTree = currentTree.map((item) => {
                                if (item.data.hostname === tree.parent.hostName) {

                                    return {
                                        ...item,
                                        children: [...tree.folders]
                                    };
                                }

                                return item;
                            });
                            changes.tree = newTree;
                        }

                        patchState(store, changes);
                    })
                )
            ),
            /**
             *  Load children of a node
             */
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const { hostname, path } = node.data;

                        const fullPath = `${hostname}/${path}`;

                        return hostFieldService.getFoldersTreeNode(fullPath).pipe(
                            tap(({ folders }) => {
                                node.leaf = true;
                                node.icon = 'pi pi-folder-open';
                                node.children = [...folders];
                                patchState(store, { nodeExpaned: node });
                            })
                        );
                    })
                )
            ),
            /**
             * Choose a node from the tree
             */
            chooseNode: (event: TreeNodeSelectItem) => {
                const { node: nodeSelected } = event;
                const data = nodeSelected.data;
                if (!data) {
                    return;
                }

                patchState(store, { nodeSelected });
            }
        };
    })
);
