import { tapResponse } from '@ngrx/operators';
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { exhaustMap, switchMap, tap, map, filter } from 'rxjs/operators';

import {
    CustomTreeNode,
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../services/dot-edit-content.service';

type HostFolderFiledState = {
    nodeSelected: TreeNodeItem | null;
    nodeExpaned: TreeNodeSelectItem['node'] | null;
    tree: TreeNodeItem[];
    status: 'idle' | 'pending' | 'fulfilled' | { error: string };
};

const initialState: HostFolderFiledState = {
    nodeSelected: null,
    nodeExpaned: null,
    tree: [],
    status: 'idle'
};

export const HostFolderFiledStore = signalStore(
    withState(initialState),
    withComputed(({ status }) => ({
        iconClasses: computed(() => {
            const currentStatus = status();

            return {
                'pi-spin pi-spinner': currentStatus === 'pending',
                'pi-chevron-down': currentStatus !== 'pending'
            };
        })
    })),
    withMethods((store, dotEditContentService = inject(DotEditContentService)) => ({
        loadSites: rxMethod<string | null>(
            pipe(
                tap(() => patchState(store, { status: 'pending' })),
                switchMap((path) => {
                    return dotEditContentService
                        .getSitesTreePath({ perPage: 7000, filter: '*' })
                        .pipe(
                            tapResponse({
                                next: (sites) => patchState(store, { tree: sites }),
                                error: () => patchState(store, { status: { error: 'fail' } }),
                                finalize: () => patchState(store, { status: 'fulfilled' })
                            }),
                            map((sites) => ({
                                path,
                                sites
                            }))
                        );
                }),
                filter(({ path }) => !!path),
                switchMap(({ path, sites }) => {
                    const hasPaths = path.includes('/');
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
        loadChildren: rxMethod<TreeNodeSelectItem>(
            pipe(
                exhaustMap((event: TreeNodeSelectItem) => {
                    const { node } = event;
                    const { hostname, path } = node.data;

                    return dotEditContentService.getFoldersTreeNode(hostname, path).pipe(
                        tap((children) => {
                            node.leaf = true;
                            node.icon = 'pi pi-folder-open';
                            node.children = children;
                            node.expanded = true;
                            patchState(store, { nodeExpaned: node });
                        })
                    );
                })
            )
        )
    }))
);
