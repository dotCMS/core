import { tapResponse } from '@ngrx/operators';
import { patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { of, pipe } from 'rxjs';

import { switchMap, tap, map, filter } from 'rxjs/operators';

import type { CustomTreeNode } from './../../../../models/dot-edit-content-host-folder-field.interface';
import type { DotEditContentService } from './../../../../services/dot-edit-content.service';
import type { HostFolderFiledState } from './../host-folder-field.store';

const PEER_PAGE_LIMIT = 7000;

export const loadSites = (store, dotEditContentService: DotEditContentService) => {
    return rxMethod<string | null>(
        pipe(
            tap(() => patchState(store, { status: 'LOADING' })),
            switchMap((path) => {
                return dotEditContentService
                    .getSitesTreePath({ perPage: PEER_PAGE_LIMIT, filter: '*' })
                    .pipe(
                        tapResponse({
                            next: (sites) => patchState(store, { tree: sites }),
                            error: () => patchState(store, { status: 'FAILED', error: '' }),
                            finalize: () => patchState(store, { status: 'LOADED' })
                        }),
                        map((sites) => ({
                            path,
                            sites
                        }))
                    );
            }),
            switchMap(({ path, sites }) => {
                if (path) {
                    return of({ path, sites });
                }

                const isRequired = false;

                if (isRequired) {
                    return dotEditContentService.getCurrentSite().pipe(
                        switchMap((currentSite) => {
                            const node = sites.find((item) => item.label === currentSite.label);

                            return of({
                                path: node?.label,
                                sites
                            });
                        })
                    );
                }

                const node = sites.find((item) => item.label === 'System Host');

                return of({
                    path: node?.label,
                    sites
                });
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
    );
};
