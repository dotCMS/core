import { tapResponse } from '@ngrx/operators';
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, exhaustMap, switchMap, map, filter } from 'rxjs/operators';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../services/dot-edit-content.service';

import type { CustomTreeNode } from './../../../models/dot-edit-content-host-folder-field.interface';

export const PEER_PAGE_LIMIT = 7000;

export const SYSTEM_HOST_NAME = 'System Host';

export type ComponentStatus = 'INIT' | 'LOADING' | 'LOADED' | 'SAVING' | 'IDLE' | 'FAILED';

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
    status: 'INIT',
    error: null
};

export const HostFolderFiledStore = signalStore(
    withState(initialState),
    withComputed(({ status, nodeSelected }) => ({
        iconClasses: computed(() => {
            const currentStatus = status();

            return {
                'pi-spin pi-spinner': currentStatus === 'LOADING',
                'pi-chevron-down': currentStatus !== 'LOADING'
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
        const dotEditContentService = inject(DotEditContentService);

        return {
            /**
             * Load the sites tree
             */
            loadSites: rxMethod<{ path: string | null; isRequired: boolean }>(
                pipe(
                    tap(() => patchState(store, { status: 'LOADING' })),
                    switchMap(({ path, isRequired }) => {
                        return dotEditContentService
                            .getSitesTreePath({ perPage: PEER_PAGE_LIMIT, filter: '*' })
                            .pipe(
                                map((sites) => {
                                    if (isRequired) {
                                        return sites.filter(
                                            (site) => site.label !== SYSTEM_HOST_NAME
                                        );
                                    }

                                    return sites;
                                }),
                                tapResponse({
                                    next: (sites) => patchState(store, { tree: sites }),
                                    error: () => patchState(store, { status: 'FAILED', error: '' }),
                                    finalize: () => patchState(store, { status: 'LOADED' })
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
                            return dotEditContentService.getCurrentSiteAsTreeNodeItem().pipe(
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
                        const newPath = path.replace('//', '');
                        const hasPaths = newPath.includes('/');
                        if (!hasPaths) {
                            const response: CustomTreeNode = {
                                node: sites.find((item) => item.key === path),
                                tree: null
                            };

                            return of(response);
                        }

                        return dotEditContentService.buildTreeByPaths(path);
                    }),
                    tap(({ node, tree }) => {
                        const changes: Partial<HostFolderFiledState> = {};
                        if (node) {
                            changes.nodeSelected = node;
                        }

                        if (tree) {
                            const currentTree = store.tree();
                            const newTree = currentTree.map((item) => {
                                if (item.key === tree.path) {
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

                        return dotEditContentService.getFoldersTreeNode(hostname, path).pipe(
                            tap((children) => {
                                node.leaf = true;
                                node.icon = 'pi pi-folder-open';
                                node.children = [...children];
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
