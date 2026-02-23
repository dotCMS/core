import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withMethods,
    withState,
    withHooks
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { exhaustMap, switchMap, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    ContentByFolderParams,
    DotCMSContentlet,
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';

import { DotBrowsingService } from '../../../services/dot-browsing/dot-browsing.service';

export const PEER_PAGE_LIMIT = 1000;
export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

export interface Content {
    id: string;
    image: string;
    title: string;
    modifiedBy: string;
    lastModified: Date;
}

export interface BrowserSelectorState {
    folders: {
        data: TreeNodeItem[];
        status: ComponentStatus;
        nodeExpaned: TreeNodeSelectItem['node'] | null;
    };
    content: {
        data: DotCMSContentlet[];
        status: ComponentStatus;
        error: string | null;
    };
    selectedContent: DotCMSContentlet | null;
    searchQuery: string;
    viewMode: 'list' | 'grid grid-cols-12 gap-4';
}

const initialState: BrowserSelectorState = {
    folders: {
        data: [],
        status: ComponentStatus.INIT,
        nodeExpaned: null
    },
    content: {
        data: [],
        status: ComponentStatus.INIT,
        error: null
    },
    selectedContent: null,
    searchQuery: '',
    viewMode: 'list'
};

export const DotBrowserSelectorStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        foldersIsLoading: computed(() => state.folders().status === ComponentStatus.LOADING),
        contentIsLoading: computed(() => state.content().status === ComponentStatus.LOADING)
    })),
    withMethods((store) => {
        const dotBrowsingService = inject(DotBrowsingService);

        return {
            setSelectedContent: (selectedContent: DotCMSContentlet) => {
                patchState(store, {
                    selectedContent
                });
            },
            loadContent: rxMethod<ContentByFolderParams>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            content: { ...store.content(), status: ComponentStatus.LOADING }
                        })
                    ),
                    switchMap((params) => {
                        return dotBrowsingService.getContentByFolder(params).pipe(
                            tapResponse({
                                next: (data) => {
                                    patchState(store, {
                                        content: {
                                            data,
                                            status: ComponentStatus.LOADED,
                                            error: null
                                        }
                                    });
                                },
                                error: () =>
                                    patchState(store, {
                                        content: {
                                            data: [],
                                            status: ComponentStatus.ERROR,
                                            error: 'dot.file.field.dialog.select.existing.file.table.error.content'
                                        }
                                    })
                            })
                        );
                    })
                )
            ),
            loadFolders: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            folders: { ...store.folders(), status: ComponentStatus.LOADING }
                        })
                    ),
                    switchMap(() => {
                        return dotBrowsingService
                            .getSitesTreePath({ perPage: PEER_PAGE_LIMIT, filter: '*' })
                            .pipe(
                                tapResponse({
                                    next: (data) =>
                                        patchState(store, {
                                            folders: {
                                                data,
                                                status: ComponentStatus.LOADED,
                                                nodeExpaned: null
                                            }
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            folders: {
                                                data: [],
                                                status: ComponentStatus.ERROR,
                                                nodeExpaned: null
                                            }
                                        })
                                })
                            );
                    })
                )
            ),
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const { hostname, path } = node.data;

                        node.loading = true;

                        const fullPath = `${hostname}/${path}`;

                        return dotBrowsingService.getFoldersTreeNode(fullPath).pipe(
                            tapResponse({
                                next: ({ folders: children }) => {
                                    node.loading = false;
                                    node.leaf = true;
                                    node.icon = 'pi pi-folder-open';
                                    node.children = [...children];

                                    const folders = store.folders();
                                    patchState(store, {
                                        folders: { ...folders, nodeExpaned: node }
                                    });
                                },
                                error: () => {
                                    node.loading = false;
                                }
                            })
                        );
                    })
                )
            )
        };
    }),
    withHooks((store) => ({
        onInit: () => {
            store.loadFolders();
        }
    }))
);
