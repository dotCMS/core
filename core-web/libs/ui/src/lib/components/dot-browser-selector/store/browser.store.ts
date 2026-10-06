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
import { EMPTY, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { exhaustMap, switchMap, tap } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import {
    ComponentStatus,
    ContentByFolderParams,
    DotCMSContentlet,
    DOT_FOLDER_TREE_PAGE_SIZE,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem,
    TreeNodeLoadMoreData,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';

import {
    DotBrowsingService,
    SITE_PAGE_LIMIT
} from '../../../services/dot-browsing/dot-browsing.service';
import { SYSTEM_HOST_ID } from '../../dot-folder-tree/constants';
import {
    findFolderParent,
    findSiteIdByHostname,
    hasMorePages,
    SITES_LOAD_MORE_KEY,
    stripLoadMore,
    withLoadMore
} from '../../dot-folder-tree/site-tree.utils';

/** Re-exports so consumers/tests keep a single import site for these. */
export { SITE_PAGE_LIMIT };
export { SYSTEM_HOST_ID };

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

function refreshFolders(store: { folders: () => BrowserSelectorState['folders'] }) {
    return {
        folders: {
            ...store.folders(),
            data: structuredClone(store.folders().data)
        }
    };
}

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
                            .getSitesPage({ perPage: SITE_PAGE_LIMIT, filter: '*', page: 1 })
                            .pipe(
                                tapResponse({
                                    next: ({ sites, pagination }) =>
                                        patchState(store, {
                                            folders: {
                                                data: withLoadMore(
                                                    sites,
                                                    hasMorePages(pagination),
                                                    SITES_LOAD_MORE_KEY,
                                                    2,
                                                    '',
                                                    ''
                                                ),
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
             * Loads the first page of child folders for a site/folder node via paginated search.
             */
            loadChildren: rxMethod<TreeNodeSelectItem>(
                pipe(
                    exhaustMap((event: TreeNodeSelectItem) => {
                        const { node } = event;
                        const data = node.data;

                        if (!data || data.type === LOAD_MORE_NODE_TYPE) {
                            return EMPTY;
                        }

                        if ((node.children?.length ?? 0) > 0 || node.leaf) {
                            node.expanded = true;

                            return EMPTY;
                        }

                        const { hostname, path, id, type } = data;
                        const siteId =
                            type === 'site'
                                ? id
                                : findSiteIdByHostname(hostname, store.folders().data);

                        if (!siteId) {
                            return EMPTY;
                        }

                        const folderPath = path || '/';

                        node.loading = true;

                        return dotBrowsingService
                            .searchFolders(
                                {
                                    siteId,
                                    path: folderPath,
                                    recursive: false,
                                    page: 1,
                                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                                },
                                hostname
                            )
                            .pipe(
                                tapResponse({
                                    next: ({ folders, pagination }) => {
                                        node.loading = false;
                                        node.expanded = true;
                                        node.leaf = folders.length === 0;
                                        node.children = withLoadMore(
                                            folders,
                                            hasMorePages(pagination),
                                            node.key ?? id,
                                            2,
                                            folderPath,
                                            hostname
                                        );

                                        patchState(store, refreshFolders(store));
                                    },
                                    error: () => {
                                        node.loading = false;
                                        patchState(store, refreshFolders(store));
                                    }
                                })
                            );
                    })
                )
            ),
            /**
             * Loads the next page for a site root or folder level when its "Load more" node is clicked.
             */
            loadMore: rxMethod<TreeNodeItem>(
                pipe(
                    exhaustMap((node) => {
                        const data = node.data as TreeNodeLoadMoreData | undefined;

                        if (!data || data.type !== LOAD_MORE_NODE_TYPE) {
                            return EMPTY;
                        }

                        const nextPage = data.nextPage ?? 2;
                        const parentPath = data.path ?? '';
                        const hostname = data.hostname ?? '';
                        const isSitesLevel = !hostname && parentPath === '';

                        node.loading = true;
                        patchState(store, refreshFolders(store));

                        if (isSitesLevel) {
                            return dotBrowsingService
                                .getSitesPage({
                                    filter: '*',
                                    perPage: SITE_PAGE_LIMIT,
                                    page: nextPage
                                })
                                .pipe(
                                    tapResponse({
                                        next: ({ sites, pagination }) => {
                                            const combined = [
                                                ...stripLoadMore(store.folders().data),
                                                ...sites
                                            ];

                                            patchState(store, {
                                                folders: {
                                                    ...store.folders(),
                                                    data: withLoadMore(
                                                        combined,
                                                        hasMorePages(pagination),
                                                        SITES_LOAD_MORE_KEY,
                                                        nextPage + 1,
                                                        '',
                                                        ''
                                                    )
                                                }
                                            });
                                        },
                                        error: () => {
                                            node.loading = false;
                                            patchState(store, refreshFolders(store));
                                        }
                                    })
                                );
                        }

                        const siteId = findSiteIdByHostname(hostname, store.folders().data);

                        if (!siteId) {
                            node.loading = false;

                            return EMPTY;
                        }

                        const folderPath = parentPath || '/';

                        return dotBrowsingService
                            .searchFolders(
                                {
                                    siteId,
                                    path: folderPath,
                                    recursive: false,
                                    page: nextPage,
                                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                                },
                                hostname
                            )
                            .pipe(
                                tapResponse({
                                    next: ({ folders, pagination }) => {
                                        const parent = findFolderParent(
                                            store.folders().data,
                                            folderPath,
                                            hostname
                                        );

                                        if (!parent) {
                                            node.loading = false;
                                            patchState(store, refreshFolders(store));

                                            return;
                                        }

                                        const combined = [
                                            ...stripLoadMore(parent.children as TreeNodeItem[]),
                                            ...folders
                                        ];

                                        parent.children = withLoadMore(
                                            combined,
                                            hasMorePages(pagination),
                                            parent.key ?? siteId,
                                            nextPage + 1,
                                            folderPath,
                                            hostname
                                        );

                                        patchState(store, refreshFolders(store));
                                    },
                                    error: () => {
                                        node.loading = false;
                                        patchState(store, refreshFolders(store));
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
