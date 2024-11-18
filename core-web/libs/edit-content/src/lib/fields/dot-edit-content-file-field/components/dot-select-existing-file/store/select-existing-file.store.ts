import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { exhaustMap, switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/edit-content/models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '@dotcms/edit-content/services/dot-edit-content.service';

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
    };
    currentSite: TreeNodeItem | null;
    selectedContent: DotCMSContentlet | null;
    searchQuery: string;
    viewMode: 'list' | 'grid';
}

const initialState: SelectExisingFileState = {
    folders: {
        data: [],
        status: ComponentStatus.INIT,
        nodeExpaned: null
    },
    content: {
        data: [],
        status: ComponentStatus.INIT
    },
    currentSite: null,
    selectedContent: null,
    searchQuery: '',
    viewMode: 'list'
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
                    switchMap((event) => {
                        const content = store.content();

                        console.log('event', event);

                        let identifier = SYSTEM_HOST_ID;

                        if (event) {
                            identifier = event.node.data.identifier;
                        }

                        return dotEditContentService.getContentByFolder(identifier).pipe(
                            tapResponse({
                                next: (data) => {
                                    patchState(store, {
                                        content: { data, status: ComponentStatus.LOADED }
                                    });
                                },
                                error: () =>
                                    patchState(store, {
                                        content: { ...content, status: ComponentStatus.ERROR }
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

                        return dotEditContentService.getFoldersTreeNode(hostname, path).pipe(
                            tapResponse({
                                next: (children) => {
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
    })
);
