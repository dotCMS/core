import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { exhaustMap, switchMap, tap } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
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
        status: ComponentStatus.INIT
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
            setSelectedContent: (selectedContent: DotCMSContentlet | null) => {
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
                                                status: ComponentStatus.LOADED
                                            }
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            folders: {
                                                data: [],
                                                status: ComponentStatus.ERROR
                                            }
                                        })
                                })
                            );
                    })
                )
            ),
            /**
             * Loads the child folders of the selected tree node.
             *

             */
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const { hostname, path } = node.data;

                        node.loading = true;

                        return dotBrowsingService.getFoldersTreeNode(`${hostname}/${path}`).pipe(
                            tapResponse({
                                next: ({ folders: children }) => {
                                    node.loading = false;
                                    node.expanded = true;
                                    node.leaf = true;
                                    node.icon = 'pi pi-folder-open';
                                    node.children = [...children];

                                    // structuredClone produces a new array reference so PrimeNG
                                    // OnPush detects the change and re-renders the tree.
                                    patchState(store, {
                                        folders: {
                                            ...store.folders(),
                                            data: structuredClone(store.folders().data)
                                        }
                                    });
                                },
                                error: () => {
                                    node.loading = false;
                                    patchState(store, {
                                        folders: {
                                            ...store.folders(),
                                            data: structuredClone(store.folders().data)
                                        }
                                    });
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
    })),
    withMethods((store) => {
        const dotUploadFileService = inject(DotUploadFileService);

        return {
            /**
             * Uploads a file to the given folder and refreshes the content list on success.
             * On error, preserves the existing file list and shows a contextual error message:
             * - 403 → permissions error (user lacks write access to the folder)
             * - other → generic upload error
             */
            uploadFile: rxMethod<{ file: File; folderParams: ContentByFolderParams }>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            content: {
                                ...store.content(),
                                status: ComponentStatus.LOADING,
                                error: null
                            }
                        })
                    ),
                    exhaustMap(({ file, folderParams }) =>
                        dotUploadFileService
                            .uploadDotAsset(file, { hostFolder: folderParams.hostFolderId })
                            .pipe(
                                tapResponse({
                                    next: (uploadedContentlet) => {
                                        store.setSelectedContent(uploadedContentlet);
                                        store.loadContent(folderParams);
                                    },
                                    error: (err: { status?: number }) =>
                                        patchState(store, {
                                            content: {
                                                ...store.content(),
                                                status: ComponentStatus.LOADED,
                                                error:
                                                    err?.status === 403
                                                        ? 'dot.file.field.dialog.upload.file.error.permissions'
                                                        : 'dot.file.field.dialog.upload.file.error'
                                            }
                                        })
                                })
                            )
                    )
                )
            )
        };
    })
);
