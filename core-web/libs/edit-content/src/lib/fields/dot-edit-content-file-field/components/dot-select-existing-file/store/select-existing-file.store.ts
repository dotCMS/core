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

import { exhaustMap, switchMap, tap, filter, map } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../../../services/dot-edit-content.service';

export const PEER_PAGE_LIMIT = 1000;

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

export interface Content {
    id: string;
    image: string;
    title: string;
    modifiedBy: string;
    lastModified: Date;
}

export interface SelectExisingFileState {
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
    mimeTypes: string[];
}

const initialState: SelectExisingFileState = {
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
    viewMode: 'list',
    mimeTypes: []
};

export const SelectExisingFileStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        foldersIsLoading: computed(() => state.folders().status === ComponentStatus.LOADING),
        contentIsLoading: computed(() => state.content().status === ComponentStatus.LOADING)
    })),
    withMethods((store) => {
        const dotEditContentService = inject(DotEditContentService);

        return {
            setMimeTypes: (mimeTypes: string[]) => {
                patchState(store, {
                    mimeTypes
                });
            },
            setSelectedContent: (selectedContent: DotCMSContentlet) => {
                patchState(store, {
                    selectedContent
                });
            },
            loadContent: rxMethod<TreeNodeSelectItem | void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            content: { ...store.content(), status: ComponentStatus.LOADING }
                        })
                    ),
                    map((event) => (event ? event?.node?.data?.id : SYSTEM_HOST_ID)),
                    filter((identifier) => {
                        const hasIdentifier = !!identifier;

                        if (!hasIdentifier) {
                            patchState(store, {
                                content: {
                                    data: [],
                                    status: ComponentStatus.ERROR,
                                    error: 'dot.file.field.dialog.select.existing.file.table.error.id'
                                }
                            });
                        }

                        return hasIdentifier;
                    }),
                    switchMap((identifier) => {
                        return dotEditContentService
                            .getContentByFolder({
                                folderId: identifier,
                                mimeTypes: store.mimeTypes()
                            })
                            .pipe(
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
                        return dotEditContentService
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

                        return dotEditContentService.getFoldersTreeNode(fullPath).pipe(
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
