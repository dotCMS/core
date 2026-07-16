import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    filter,
    groupBy,
    map,
    mergeMap,
    switchMap,
    tap
} from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    CustomTreeNode,
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';
import { DotBrowsingService, normalizeHostFolderBrowsePath, TREE_ROOT_NODE_KEY } from '@dotcms/ui';

export const SITE_PAGE_LIMIT = 40;
export const FOLDER_PAGE_LIMIT = 40;
export const MIN_SEARCH_LENGTH = 2;
export const SITE_SEARCH_THRESHOLD = 5;
/** Re-export of TREE_ROOT_NODE_KEY for existing store consumers/tests. */
export const ROOT_NODE_KEY = TREE_ROOT_NODE_KEY;
export const SEARCH_LOAD_MORE_KEY = 'search';

export const SYSTEM_HOST_NAME = 'System Host';

/**
 * Marks a synthetic tree node used as an in-tree "Load N more" trigger, since `p-tree`
 * has no per-node footer/slot. Injected as the last child of any level that still has
 * more pages (`nodePagination[key].hasMore`).
 */
export const LOAD_MORE_NODE_TYPE = 'load-more';

/**
 * Creates the synthetic "Load more" node appended as the last child of a level.
 * `selectable: false` makes `p-tree` skip selection on click, and `leaf: true` keeps
 * it from rendering a toggler. The key is namespaced per level to avoid collisions.
 */
function createLoadMoreNode(levelKey: string): TreeNodeItem {
    return {
        key: `load-more:${levelKey}`,
        label: '',
        type: LOAD_MORE_NODE_TYPE,
        selectable: false,
        leaf: true
    } as TreeNodeItem;
}

/**
 * Removes any previously-injected "Load more" node from a level's children before
 * appending a fresh page or re-evaluating `hasMore`.
 */
function stripLoadMore(nodes: TreeNodeItem[] | undefined): TreeNodeItem[] {
    return (nodes ?? []).filter((node) => node.type !== LOAD_MORE_NODE_TYPE);
}

export type NodePaginationState = {
    page: number;
    hasMore: boolean;
    loading: boolean;
};

export type SitesPaginationState = {
    page: number;
    hasMore: boolean;
    loading: boolean;
    totalEntries: number;
};

/**
 * Params for loading a level of folders (root or a specific node) through the
 * paginated `searchFolders` endpoint. `append` distinguishes a fresh load
 * (site switch, first expand) from a "Load 40 more" request.
 */
type LoadFoldersParams = {
    key: string;
    path: string;
    siteId: string;
    page: number;
    append: boolean;
    targetNode?: TreeNodeItem;
};

export type HostFolderFiledState = {
    sites: TreeNodeItem[];
    sitesStatus: ComponentStatus;
    sitesPagination: SitesPaginationState;
    sitesCatalogTotal: number;
    sitesLoadedPages: number[];
    pinnedSiteKey: string | null;
    selectedSite: TreeNodeItem | null;
    folders: TreeNodeItem[];
    foldersStatus: ComponentStatus;
    nodePagination: Record<string, NodePaginationState>;
    searchTerm: string;
    siteSearchTerm: string;
    searchResults: TreeNodeItem[] | null;
    searchStatus: ComponentStatus;
    searchPagination: NodePaginationState;
    confirmedNode: TreeNodeItem | null;
    pendingNode: TreeNodeItem | null;
    overlayOpen: boolean;
    queryEpoch: number;
    isRequired: boolean;
    error: string | null;
};

export const initialState: HostFolderFiledState = {
    sites: [],
    sitesStatus: ComponentStatus.INIT,
    sitesPagination: { page: 1, hasMore: false, loading: false, totalEntries: 0 },
    sitesCatalogTotal: 0,
    sitesLoadedPages: [],
    pinnedSiteKey: null,
    selectedSite: null,
    folders: [],
    foldersStatus: ComponentStatus.INIT,
    nodePagination: {},
    searchTerm: '',
    siteSearchTerm: '',
    searchResults: null,
    searchStatus: ComponentStatus.INIT,
    searchPagination: { page: 1, hasMore: false, loading: false },
    confirmedNode: null,
    pendingNode: null,
    overlayOpen: false,
    queryEpoch: 0,
    isRequired: false,
    error: null
};

/**
 * Seeds `nodePagination` from an already-resolved folder tree (e.g. from
 * `buildTreeByPaths`). Uses the optional per-level `pagination` metadata so
 * "Load more" continues from the correct page; levels with loaded children but
 * no entry default to `{ page: 1, hasMore: false }` so `expandNode` skips a
 * re-fetch of already-populated branches.
 */
function buildInitialPaginationMap(
    folders: TreeNodeItem[],
    pagination?: Record<string, { page: number; hasMore: boolean }>
): Record<string, NodePaginationState> {
    const fromMeta = (key: string): NodePaginationState => {
        const entry = pagination?.[key];

        return {
            page: entry?.page ?? 1,
            hasMore: entry?.hasMore ?? false,
            loading: false
        };
    };

    const map: Record<string, NodePaginationState> = {
        [ROOT_NODE_KEY]: fromMeta(ROOT_NODE_KEY)
    };

    const walk = (nodes: TreeNodeItem[]) => {
        nodes.forEach((node) => {
            if (Array.isArray(node.children)) {
                map[node.key] = fromMeta(node.key);
                walk(node.children as TreeNodeItem[]);
            }
        });
    };

    walk(folders);

    return map;
}

/**
 * Appends a synthetic "Load more" sentinel to any level whose pagination still
 * has more pages, so pre-resolved trees from `buildTreeByPaths` keep the same
 * in-tree load-more UX as lazily browsed levels.
 */
function injectLoadMoreSentinels(
    folders: TreeNodeItem[],
    pagination: Record<string, NodePaginationState>
): TreeNodeItem[] {
    const withSentinel = (nodes: TreeNodeItem[], levelKey: string): TreeNodeItem[] => {
        const cleaned = stripLoadMore(nodes).map((node) => {
            if (!Array.isArray(node.children)) {
                return node;
            }

            return {
                ...node,
                children: withSentinel(node.children as TreeNodeItem[], node.key)
            };
        });

        if (pagination[levelKey]?.hasMore) {
            return [...cleaned, createLoadMoreNode(levelKey)];
        }

        return cleaned;
    };

    return withSentinel(folders, ROOT_NODE_KEY);
}

/**
 * Marks every ancestor folder leading to `targetPath` as expanded, so `p-tree` renders the
 * full branch open instead of requiring a manual expand click. Acts as a safety net on top
 * of `buildTreeByPaths`'s own `expanded` flag, since the tree can be re-created via
 * `structuredClone` elsewhere (e.g. `loadFolders`) which would otherwise silently drop it.
 */
function expandFoldersToTarget(folders: TreeNodeItem[], targetPath: string | undefined): void {
    if (!targetPath) {
        return;
    }

    const walk = (nodes: TreeNodeItem[]) => {
        nodes.forEach((node) => {
            const nodePath = node.data?.path;
            const children = node.children as TreeNodeItem[] | undefined;

            if (nodePath && nodePath !== targetPath && targetPath.startsWith(nodePath)) {
                node.expanded = true;
            }

            if (Array.isArray(children)) {
                walk(children);
            }
        });
    };

    walk(folders);
}

/**
 * Finds a tree node by key in a folder list, including nested children.
 */
function findNodeByKey(nodes: TreeNodeItem[], key: string): TreeNodeItem | null {
    for (const node of nodes) {
        if (node.key === key) {
            return node;
        }

        if (Array.isArray(node.children)) {
            const found = findNodeByKey(node.children as TreeNodeItem[], key);

            if (found) {
                return found;
            }
        }
    }

    return null;
}

function sitesHasMore(pageSites: TreeNodeItem[]): boolean {
    return pageSites.length >= SITE_PAGE_LIMIT;
}

function extractHostnameFromPath(path: string): string {
    if (!path.includes('/')) {
        return path;
    }

    return path.split('/').filter(Boolean)[0];
}

function prependSite(sites: TreeNodeItem[], pinned: TreeNodeItem | null): TreeNodeItem[] {
    if (!pinned) {
        return sites;
    }

    return [pinned, ...sites.filter((site) => site.key !== pinned.key)];
}

function mergeSites(existing: TreeNodeItem[], incoming: TreeNodeItem[]): TreeNodeItem[] {
    const keys = new Set(existing.map((site) => site.key));
    const newSites = incoming.filter((site) => !keys.has(site.key));

    return [...existing, ...newSites];
}

function filterSystemHost(sites: TreeNodeItem[], isRequired: boolean): TreeNodeItem[] {
    if (!isRequired) {
        return sites;
    }

    return sites.filter((site) => site.label !== SYSTEM_HOST_NAME);
}

export const HostFolderFiledStore = signalStore(
    withState(initialState),
    withComputed(
        ({
            sites,
            sitesStatus,
            sitesPagination,
            sitesCatalogTotal,
            confirmedNode,
            pendingNode,
            searchTerm,
            siteSearchTerm,
            searchResults,
            folders,
            foldersStatus,
            searchStatus
        }) => {
            /**
             * Full path in `//hostname/path/` format, used by the copy-to-clipboard action.
             */
            const fullPath = computed(() => {
                const node = confirmedNode();

                if (!node?.data) {
                    return '';
                }

                const { hostname, path } = node.data;
                const cleanHostname = hostname.replace('//', '');

                return `//${cleanHostname}${path ? path : '/'}`;
            });

            /**
             * Human-readable path for the field trigger label: hostname only for site root,
             * otherwise hostname and folder segments joined with ` / `.
             */
            const displayPath = computed(() => {
                const node = confirmedNode();

                if (!node?.data) {
                    return '';
                }

                const cleanHostname = node.data.hostname.replace('//', '');
                const path = node.data.path;

                if (!path || path === '/') {
                    return cleanHostname;
                }

                const segments = path
                    .replace(/^\/+|\/+$/g, '')
                    .split('/')
                    .filter(Boolean);

                return [cleanHostname, ...segments].join(' / ');
            });

            const sitesLoading = computed(() => sitesStatus() === ComponentStatus.LOADING);
            const showTriggerLoading = computed(
                () => sitesStatus() === ComponentStatus.LOADING && !confirmedNode()
            );
            const showSitesPanelLoading = computed(() => {
                if (sites().length > 0) {
                    return false;
                }

                return sitesStatus() === ComponentStatus.LOADING || sitesPagination().loading;
            });
            const showFoldersPanelLoading = computed(() => {
                const loading =
                    foldersStatus() === ComponentStatus.LOADING ||
                    searchStatus() === ComponentStatus.LOADING;

                if (!loading) {
                    return false;
                }

                if (searchTerm().length >= MIN_SEARCH_LENGTH) {
                    return (searchResults() ?? []).length === 0;
                }

                return folders().length === 0;
            });

            return {
                /**
                 * Whether the confirmed selection is a site root or a folder, used to pick
                 * between the Material "globe" icon and the folder icon in the field trigger.
                 */
                triggerIconType: computed<'site' | 'folder'>(() => {
                    const node = confirmedNode();

                    return node?.data?.type === 'folder' ? 'folder' : 'site';
                }),
                pathToSave: computed(() => {
                    const node = confirmedNode();

                    if (node?.data) {
                        const { data } = node;
                        const newHostname = data.hostname.replace('//', '');

                        return `${newHostname}:${data.path ? data.path : '/'}`;
                    }

                    return null;
                }),
                /**
                 * Full path in `//hostname/path/` format, used by the copy-to-clipboard action.
                 */
                copyPath: fullPath,
                displayPath,
                /**
                 * Whether the current search term is long enough to trigger a backend search
                 * (the folder search endpoint requires at least `MIN_SEARCH_LENGTH` characters).
                 */
                isSearching: computed(() => searchTerm().length >= MIN_SEARCH_LENGTH),
                /**
                 * Whether the sites panel should show a search input instead of the static label.
                 */
                showSitesSearch: computed(
                    () =>
                        siteSearchTerm().trim().length > 0 ||
                        sitesCatalogTotal() > SITE_SEARCH_THRESHOLD
                ),
                /**
                 * Whether the folders panel should show the search input. Hidden while the panel
                 * loading state is shown and when the selected site has no folders after load.
                 */
                showFolderSearch: computed(() => {
                    if (showFoldersPanelLoading()) {
                        return false;
                    }

                    if (searchTerm().length >= MIN_SEARCH_LENGTH) {
                        return true;
                    }

                    if (foldersStatus() !== ComponentStatus.LOADED) {
                        return true;
                    }

                    return folders().length > 0;
                }),
                /**
                 * Sites currently loaded for the panel (API-backed list; no local filtering).
                 */
                filteredSites: computed(() => sites()),
                /**
                 * Folders to render in the tree: search results while searching, otherwise the
                 * regular (lazily-loaded) folder tree for the selected site.
                 */
                displayedFolders: computed(() => {
                    if (searchTerm().length >= MIN_SEARCH_LENGTH) {
                        return searchResults() ?? [];
                    }

                    return folders();
                }),
                sitesLoading,
                showTriggerLoading,
                showSitesPanelLoading,
                foldersLoading: computed(() => foldersStatus() === ComponentStatus.LOADING),
                searchLoading: computed(() => searchStatus() === ComponentStatus.LOADING),
                showFoldersPanelLoading,
                sitesLoadFailed: computed(() => sitesStatus() === ComponentStatus.ERROR),
                foldersLoadFailed: computed(
                    () =>
                        foldersStatus() === ComponentStatus.ERROR &&
                        searchTerm().length < MIN_SEARCH_LENGTH
                ),
                searchLoadFailed: computed(() => searchStatus() === ComponentStatus.ERROR),
                /**
                 * The staged folder resolved to its matching object reference inside the folders
                 * currently rendered by the tree, so `p-tree`'s `[selection]` binding (which relies
                 * on referential equality against `value`) highlights it correctly. A plain
                 * `pendingNode()` read can go stale after tree mutations that clone the array
                 * (e.g. `loadFolders`'s `structuredClone`), so we re-resolve by `key` on every read.
                 */
                treeSelection: computed<TreeNodeItem | null>(() => {
                    const pending = pendingNode();
                    if (!pending) {
                        return null;
                    }

                    const source =
                        searchTerm().length >= MIN_SEARCH_LENGTH
                            ? (searchResults() ?? [])
                            : folders();

                    const findByKey = (nodes: TreeNodeItem[]): TreeNodeItem | null => {
                        for (const node of nodes) {
                            if (node.key === pending.key) {
                                return node;
                            }

                            if (Array.isArray(node.children)) {
                                const found = findByKey(node.children as TreeNodeItem[]);
                                if (found) {
                                    return found;
                                }
                            }
                        }

                        return null;
                    };

                    return findByKey(source);
                })
            };
        }
    ),
    withMethods((store, dotHttpErrorManagerService = inject(DotHttpErrorManagerService)) => {
        const dotBrowsingService = inject(DotBrowsingService);

        return {
            /**
             * Loads a level of folders (root of the selected site, or a specific node)
             * through the paginated search endpoint. Replaces the level's items unless
             * `append` is set (used for "Load 40 more").
             */
            loadFolders: rxMethod<LoadFoldersParams>(
                pipe(
                    tap((params) => {
                        const current = store.nodePagination()[params.key] ?? {
                            page: 1,
                            hasMore: false,
                            loading: false
                        };
                        const changes: Partial<HostFolderFiledState> = {
                            nodePagination: {
                                ...store.nodePagination(),
                                [params.key]: { ...current, loading: true }
                            }
                        };

                        if (params.key === ROOT_NODE_KEY) {
                            changes.foldersStatus = ComponentStatus.LOADING;
                        }

                        if (params.targetNode) {
                            const isSearching = store.searchTerm().length >= MIN_SEARCH_LENGTH;
                            const currentSearchResults = store.searchResults();

                            if (isSearching && currentSearchResults) {
                                const cloned = structuredClone(currentSearchResults);
                                const node = findNodeByKey(cloned, params.key) ?? params.targetNode;

                                node.loading = true;
                                changes.searchResults = cloned;
                            } else {
                                const cloned = structuredClone(store.folders());
                                const node = findNodeByKey(cloned, params.key) ?? params.targetNode;

                                node.loading = true;
                                changes.folders = cloned;
                            }
                        }

                        patchState(store, changes);
                    }),
                    groupBy((params) => params.key),
                    mergeMap((group$) =>
                        group$.pipe(
                            switchMap((params) => {
                                const hostname = store.selectedSite()?.data?.hostname ?? '';

                                return dotBrowsingService
                                    .searchFolders(
                                        {
                                            siteId: params.siteId,
                                            path: params.path,
                                            recursive: false,
                                            page: params.page,
                                            per_page: FOLDER_PAGE_LIMIT
                                        },
                                        hostname
                                    )
                                    .pipe(
                                        tapResponse({
                                            next: ({ folders, pagination }) => {
                                                if (
                                                    store.selectedSite()?.data?.id !== params.siteId
                                                ) {
                                                    return;
                                                }

                                                const hasMore =
                                                    pagination.currentPage * pagination.perPage <
                                                    pagination.totalEntries;
                                                const nodePagination = {
                                                    ...store.nodePagination(),
                                                    [params.key]: {
                                                        page: params.page,
                                                        hasMore,
                                                        loading: false
                                                    }
                                                };

                                                if (params.targetNode) {
                                                    const isSearching =
                                                        store.searchTerm().length >=
                                                        MIN_SEARCH_LENGTH;
                                                    const currentSearchResults =
                                                        store.searchResults();
                                                    const cloned =
                                                        isSearching && currentSearchResults
                                                            ? structuredClone(currentSearchResults)
                                                            : structuredClone(store.folders());
                                                    const target =
                                                        findNodeByKey(cloned, params.key) ??
                                                        params.targetNode;

                                                    target.loading = false;
                                                    target.leaf =
                                                        folders.length === 0 && params.page === 1;
                                                    target.icon = 'pi pi-folder-open';

                                                    const prevChildren = params.append
                                                        ? stripLoadMore(
                                                              target.children as TreeNodeItem[]
                                                          )
                                                        : [];
                                                    const nextChildren = [
                                                        ...prevChildren,
                                                        ...folders
                                                    ];
                                                    target.children = hasMore
                                                        ? [
                                                              ...nextChildren,
                                                              createLoadMoreNode(params.key)
                                                          ]
                                                        : nextChildren;

                                                    if (isSearching && currentSearchResults) {
                                                        patchState(store, {
                                                            searchResults: cloned,
                                                            nodePagination
                                                        });
                                                    } else {
                                                        patchState(store, {
                                                            folders: cloned,
                                                            nodePagination
                                                        });
                                                    }
                                                } else {
                                                    const prevFolders = params.append
                                                        ? stripLoadMore(store.folders())
                                                        : [];
                                                    const nextFolders = [
                                                        ...prevFolders,
                                                        ...folders
                                                    ];

                                                    patchState(store, {
                                                        folders: hasMore
                                                            ? [
                                                                  ...nextFolders,
                                                                  createLoadMoreNode(params.key)
                                                              ]
                                                            : nextFolders,
                                                        foldersStatus: ComponentStatus.LOADED,
                                                        nodePagination
                                                    });
                                                }
                                            },
                                            error: (error: HttpErrorResponse) => {
                                                if (
                                                    store.selectedSite()?.data?.id !== params.siteId
                                                ) {
                                                    return;
                                                }

                                                dotHttpErrorManagerService.handle(error);

                                                const nodePagination = {
                                                    ...store.nodePagination(),
                                                    [params.key]: {
                                                        ...(store.nodePagination()[params.key] ?? {
                                                            page: 1,
                                                            hasMore: false
                                                        }),
                                                        loading: false
                                                    }
                                                };

                                                if (params.key === ROOT_NODE_KEY) {
                                                    patchState(store, {
                                                        foldersStatus: ComponentStatus.ERROR,
                                                        nodePagination
                                                    });
                                                } else if (params.targetNode) {
                                                    const isSearching =
                                                        store.searchTerm().length >=
                                                        MIN_SEARCH_LENGTH;
                                                    const currentSearchResults =
                                                        store.searchResults();
                                                    const cloned =
                                                        isSearching && currentSearchResults
                                                            ? structuredClone(currentSearchResults)
                                                            : structuredClone(store.folders());
                                                    const node =
                                                        findNodeByKey(cloned, params.key) ??
                                                        params.targetNode;

                                                    node.loading = false;

                                                    if (isSearching && currentSearchResults) {
                                                        patchState(store, {
                                                            searchResults: cloned,
                                                            nodePagination
                                                        });
                                                    } else {
                                                        patchState(store, {
                                                            folders: cloned,
                                                            nodePagination
                                                        });
                                                    }
                                                } else {
                                                    patchState(store, { nodePagination });
                                                }
                                            }
                                        })
                                    );
                            })
                        )
                    )
                )
            ),
            /**
             * Searches folders within the currently selected site (recursive), scoped by
             * name. Clears search results when the term is empty; ignores single-character
             * terms since the backend requires a minimum of 2 characters.
             */
            search: rxMethod<string>(
                pipe(
                    tap((term) => patchState(store, { searchTerm: term })),
                    filter((term) => term.length === 0 || term.length >= MIN_SEARCH_LENGTH),
                    debounceTime(300),
                    filter(() => store.overlayOpen()),
                    map((term) => ({ term, epoch: store.queryEpoch() })),
                    distinctUntilChanged((a, b) => a.term === b.term && a.epoch === b.epoch),
                    switchMap(({ term }) => {
                        if (!term) {
                            patchState(store, {
                                searchResults: null,
                                searchStatus: ComponentStatus.IDLE,
                                searchPagination: { page: 1, hasMore: false, loading: false }
                            });

                            return EMPTY;
                        }

                        const site = store.selectedSite();
                        if (!site) {
                            return EMPTY;
                        }

                        patchState(store, { searchStatus: ComponentStatus.LOADING });

                        return dotBrowsingService
                            .searchFolders(
                                {
                                    siteId: site.data.id,
                                    path: '/',
                                    recursive: true,
                                    name: term,
                                    page: 1,
                                    per_page: FOLDER_PAGE_LIMIT
                                },
                                site.data.hostname
                            )
                            .pipe(
                                tapResponse({
                                    next: ({ folders, pagination }) => {
                                        const hasMore =
                                            pagination.currentPage * pagination.perPage <
                                            pagination.totalEntries;

                                        patchState(store, {
                                            searchResults: hasMore
                                                ? [
                                                      ...folders,
                                                      createLoadMoreNode(SEARCH_LOAD_MORE_KEY)
                                                  ]
                                                : folders,
                                            searchStatus: ComponentStatus.LOADED,
                                            searchPagination: { page: 1, hasMore, loading: false }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            searchResults: [],
                                            searchStatus: ComponentStatus.ERROR,
                                            searchPagination: {
                                                page: 1,
                                                hasMore: false,
                                                loading: false
                                            }
                                        });
                                    }
                                })
                            );
                    })
                )
            ),
            /**
             * Loads the next page of search results for the current search term.
             * Appends to the existing `searchResults` and re-evaluates `hasMore`,
             * mirroring `loadFolders`'s pagination but for a flat (non-tree) list.
             */
            searchMore: rxMethod<{
                term: string;
                siteId: string;
                hostname: string;
                page: number;
            }>(
                pipe(
                    switchMap(({ term, siteId, hostname, page }) =>
                        dotBrowsingService
                            .searchFolders(
                                {
                                    siteId,
                                    path: '/',
                                    recursive: true,
                                    name: term,
                                    page,
                                    per_page: FOLDER_PAGE_LIMIT
                                },
                                hostname
                            )
                            .pipe(
                                tapResponse({
                                    next: ({ folders, pagination }) => {
                                        if (store.searchTerm() !== term) {
                                            return;
                                        }

                                        const hasMore =
                                            pagination.currentPage * pagination.perPage <
                                            pagination.totalEntries;
                                        const nextResults = [
                                            ...stripLoadMore(store.searchResults() ?? []),
                                            ...folders
                                        ];

                                        patchState(store, {
                                            searchResults: hasMore
                                                ? [
                                                      ...nextResults,
                                                      createLoadMoreNode(SEARCH_LOAD_MORE_KEY)
                                                  ]
                                                : nextResults,
                                            searchPagination: { page, hasMore, loading: false }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        if (store.searchTerm() !== term) {
                                            return;
                                        }

                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            searchPagination: {
                                                ...store.searchPagination(),
                                                loading: false
                                            }
                                        });
                                    }
                                })
                            )
                    )
                )
            ),
            /**
             * Stages a folder node as the pending selection (highlighted in the overlay),
             * without persisting it to the field's value until `commit()` is called.
             */
            setPendingNode: (node: TreeNodeItem) => {
                patchState(store, { pendingNode: node });
            },
            /**
             * Persists the pending selection as the confirmed value. Called when the user
             * clicks "Select".
             */
            commit: () => {
                patchState(store, { confirmedNode: store.pendingNode() });
            },
            /**
             * Opens the overlay.
             */
            openOverlay: () => {
                patchState(store, { overlayOpen: true });
            },
            /**
             * Closes the overlay and discards any pending (unconfirmed) selection.
             */
            closeOverlay: () => {
                patchState(store, {
                    overlayOpen: false,
                    pendingNode: store.confirmedNode(),
                    siteSearchTerm: '',
                    searchTerm: '',
                    searchStatus: ComponentStatus.INIT,
                    searchResults: null,
                    searchPagination: { page: 1, hasMore: false, loading: false },
                    queryEpoch: store.queryEpoch() + 1
                });
            },
            /**
             * Debounced site search input handler that queries the sites API.
             */
            filterSites: rxMethod<string>(
                pipe(
                    debounceTime(300),
                    filter(() => store.overlayOpen()),
                    map((term) => ({ term, epoch: store.queryEpoch() })),
                    distinctUntilChanged((a, b) => a.term === b.term && a.epoch === b.epoch),
                    map(({ term }) => term),
                    tap((term: string) =>
                        patchState(store, {
                            siteSearchTerm: term,
                            sitesPagination: {
                                ...store.sitesPagination(),
                                loading: true
                            }
                        })
                    ),
                    switchMap((term) => {
                        patchState(store, {
                            sites: [],
                            sitesLoadedPages: []
                        });

                        const filter = term.trim() || '*';

                        return dotBrowsingService
                            .getSitesPage({
                                filter,
                                perPage: SITE_PAGE_LIMIT,
                                page: 1
                            })
                            .pipe(
                                tapResponse({
                                    next: ({ sites, pagination }) => {
                                        let pageSites = filterSystemHost(sites, store.isRequired());

                                        if (!term.trim()) {
                                            const selected = store.selectedSite();
                                            if (selected) {
                                                pageSites = prependSite(pageSites, selected);
                                            }
                                        }

                                        patchState(store, {
                                            sites: pageSites,
                                            sitesStatus: ComponentStatus.LOADED,
                                            sitesLoadedPages: [1],
                                            ...(filter === '*'
                                                ? { sitesCatalogTotal: pagination.totalEntries }
                                                : {}),
                                            sitesPagination: {
                                                page: 1,
                                                hasMore: sitesHasMore(sites),
                                                loading: false,
                                                totalEntries: pagination.totalEntries
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            sites: [],
                                            sitesPagination: {
                                                page: 1,
                                                hasMore: false,
                                                loading: false,
                                                totalEntries: 0
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
    withMethods((store, dotHttpErrorManagerService = inject(DotHttpErrorManagerService)) => {
        const dotBrowsingService = inject(DotBrowsingService);

        return {
            /**
             * Load the sites tree and resolve the initial selection (site + confirmed/pending
             * node) from an optional preselected path, preserving parity with the previous
             * implementation for backend-provided values (e.g. //demo.dotcms.com/app/folder/).
             */
            loadSites: rxMethod<{ path: string | null; isRequired: boolean }>(
                pipe(
                    map(({ path, isRequired }) => ({
                        path: path ? normalizeHostFolderBrowsePath(path) : path,
                        isRequired
                    })),
                    tap(() =>
                        patchState(store, {
                            sitesStatus: ComponentStatus.LOADING,
                            siteSearchTerm: '',
                            sites: [],
                            sitesPagination: {
                                page: 1,
                                hasMore: false,
                                loading: false,
                                totalEntries: 0
                            },
                            sitesCatalogTotal: 0,
                            sitesLoadedPages: [],
                            pinnedSiteKey: null
                        })
                    ),
                    switchMap(({ path, isRequired }) => {
                        const hostnameFromPath = path ? extractHostnameFromPath(path) : null;
                        const pinned$ = hostnameFromPath
                            ? dotBrowsingService.resolveSiteByHostname(
                                  hostnameFromPath,
                                  SITE_PAGE_LIMIT
                              )
                            : of(null as TreeNodeItem | null);

                        return pinned$.pipe(
                            switchMap((pinnedFromPath) =>
                                dotBrowsingService
                                    .getSitesPage({
                                        filter: '*',
                                        perPage: SITE_PAGE_LIMIT,
                                        page: 1
                                    })
                                    .pipe(
                                        map(({ sites, pagination }) => ({
                                            pinnedFromPath,
                                            sites,
                                            pagination,
                                            path,
                                            isRequired
                                        })),
                                        catchError((error: HttpErrorResponse) => {
                                            dotHttpErrorManagerService.handle(error);
                                            patchState(store, {
                                                sitesStatus: ComponentStatus.ERROR
                                            });

                                            return EMPTY;
                                        })
                                    )
                            )
                        );
                    }),
                    switchMap(({ pinnedFromPath, sites, pagination, path, isRequired }) => {
                        let pageSites = filterSystemHost(sites, isRequired);

                        if (pinnedFromPath) {
                            pageSites = prependSite(pageSites, pinnedFromPath);
                        }

                        patchState(store, {
                            sites: pageSites,
                            isRequired,
                            pinnedSiteKey: pinnedFromPath?.key ?? null,
                            sitesCatalogTotal: pagination.totalEntries,
                            sitesLoadedPages: [1],
                            sitesPagination: {
                                page: 1,
                                hasMore: sitesHasMore(sites),
                                loading: false,
                                totalEntries: pagination.totalEntries
                            }
                        });

                        if (path) {
                            return of({ path, pinnedFromPath });
                        }

                        if (isRequired) {
                            return dotBrowsingService.getCurrentSiteAsTreeNodeItem().pipe(
                                switchMap((currentSite) => {
                                    const updatedSites = store.sites();

                                    if (
                                        !updatedSites.some((site) => site.key === currentSite.key)
                                    ) {
                                        patchState(store, {
                                            sites: prependSite(updatedSites, currentSite),
                                            pinnedSiteKey: currentSite.key
                                        });
                                    }

                                    return of({
                                        path: currentSite.label,
                                        pinnedFromPath: currentSite
                                    });
                                })
                            );
                        }

                        const systemOrFirst =
                            pageSites.find((site) => site.label === SYSTEM_HOST_NAME) ??
                            pageSites[0];

                        if (systemOrFirst) {
                            return of({
                                path: systemOrFirst.label,
                                pinnedFromPath: pinnedFromPath ?? systemOrFirst
                            });
                        }

                        return dotBrowsingService.resolveSiteByHostname(SYSTEM_HOST_NAME).pipe(
                            map((systemSite) => {
                                if (systemSite) {
                                    patchState(store, {
                                        sites: prependSite(store.sites(), systemSite),
                                        pinnedSiteKey: systemSite.key
                                    });
                                }

                                return {
                                    path: systemSite?.label ?? null,
                                    pinnedFromPath: pinnedFromPath ?? systemSite
                                };
                            })
                        );
                    }),
                    switchMap(({ path, pinnedFromPath }) => {
                        if (!path) {
                            return of({
                                site: undefined as TreeNodeItem | undefined,
                                node: null as TreeNodeItem | null,
                                tree: null as CustomTreeNode['tree'],
                                pagination: undefined as CustomTreeNode['pagination']
                            });
                        }

                        const hasPaths = path.includes('/');

                        if (!hasPaths) {
                            const site =
                                pinnedFromPath ??
                                store
                                    .sites()
                                    .find(
                                        (item) =>
                                            item.data?.hostname === path || item.label === path
                                    );

                            return of({
                                site,
                                node: null as TreeNodeItem | null,
                                tree: null as CustomTreeNode['tree'],
                                pagination: undefined as CustomTreeNode['pagination']
                            });
                        }

                        const [hostname, ...folderSegments] = path.split('/').filter(Boolean);
                        const site =
                            pinnedFromPath ??
                            store
                                .sites()
                                .find(
                                    (item) =>
                                        item.data?.hostname === hostname || item.label === hostname
                                );

                        if (!site?.data?.id) {
                            return of({
                                site: undefined as TreeNodeItem | undefined,
                                node: null as TreeNodeItem | null,
                                tree: null as CustomTreeNode['tree'],
                                pagination: undefined as CustomTreeNode['pagination']
                            });
                        }

                        const folderPath = folderSegments.length
                            ? `/${folderSegments.join('/')}/`
                            : '/';

                        return dotBrowsingService
                            .buildTreeByPaths(
                                site.data.id,
                                site.data.hostname ?? hostname,
                                folderPath
                            )
                            .pipe(
                                map(({ node, tree, pagination }) => ({
                                    site,
                                    node,
                                    tree,
                                    pagination
                                })),
                                catchError((error: HttpErrorResponse) => {
                                    dotHttpErrorManagerService.handle(error);

                                    return of({
                                        site: undefined as TreeNodeItem | undefined,
                                        node: null as TreeNodeItem | null,
                                        tree: null as CustomTreeNode['tree'],
                                        pagination: undefined as CustomTreeNode['pagination']
                                    });
                                })
                            );
                    }),
                    tap(({ site, node, tree, pagination }) => {
                        if (!site) {
                            patchState(store, {
                                sitesStatus: ComponentStatus.ERROR
                            });

                            return;
                        }

                        const changes: Partial<HostFolderFiledState> = {
                            selectedSite: site,
                            confirmedNode: node ?? site,
                            pendingNode: node ?? site,
                            sitesStatus: ComponentStatus.LOADED
                        };

                        if (tree?.folders) {
                            expandFoldersToTarget(tree.folders, node?.data?.path);
                            const nodePagination = buildInitialPaginationMap(
                                tree.folders,
                                pagination
                            );
                            changes.folders = injectLoadMoreSentinels(tree.folders, nodePagination);
                            changes.nodePagination = nodePagination;
                        } else {
                            changes.folders = [];
                        }

                        patchState(store, changes);

                        if (!tree?.folders) {
                            store.loadFolders({
                                key: ROOT_NODE_KEY,
                                path: '/',
                                siteId: site.data.id,
                                page: 1,
                                append: false
                            });
                        }
                    })
                )
            ),
            /**
             * Selects a site in the overlay: resets folders/search/pagination and loads the
             * new site's root-level folders. Also stages the site itself as the pending
             * selection, so clicking "Select" right away targets the site root. Re-selecting
             * the already-selected site (e.g. re-clicking it while browsing a nested folder)
             * stages the site root as pending and clears folder selection without reloading
             * the folder tree.
             */
            selectSite: (site: TreeNodeItem) => {
                if (store.selectedSite()?.key === site.key) {
                    patchState(store, { pendingNode: site });
                    return;
                }

                patchState(store, {
                    selectedSite: site,
                    folders: [],
                    nodePagination: {},
                    searchTerm: '',
                    siteSearchTerm: '',
                    searchResults: null,
                    searchStatus: ComponentStatus.INIT,
                    searchPagination: { page: 1, hasMore: false, loading: false },
                    pendingNode: site,
                    queryEpoch: store.queryEpoch() + 1
                });

                store.loadFolders({
                    key: ROOT_NODE_KEY,
                    path: '/',
                    siteId: site.data.id,
                    page: 1,
                    append: false
                });
            },
            /**
             * Lazily loads a node's children the first time it's expanded. Skips the
             * request when the node is a leaf or already has loaded children (e.g. from
             * the initial preselected-path expansion).
             */
            expandNode: (event: TreeNodeSelectItem) => {
                const { node } = event;
                const hasChildrenArray = Array.isArray(node.children);
                const hasLoadedChildren =
                    hasChildrenArray && (node.children as TreeNodeItem[]).length > 0;
                const isLeaf = node.leaf === true;

                if (isLeaf || (hasChildrenArray && hasLoadedChildren)) {
                    return;
                }

                const site = store.selectedSite();
                if (!site) {
                    return;
                }

                store.loadFolders({
                    key: node.key,
                    path: node.data.path,
                    siteId: site.data.id,
                    page: 1,
                    append: false,
                    targetNode: node
                });
            },
            /**
             * Loads the next page (40 more) for the given level. Pass `null` to load
             * more items at the root level of the selected site.
             */
            loadMore: (node: TreeNodeItem | null) => {
                const site = store.selectedSite();
                if (!site) {
                    return;
                }

                const key = node ? node.key : ROOT_NODE_KEY;
                const pagination = store.nodePagination()[key];
                const nextPage = (pagination?.page ?? 1) + 1;

                store.loadFolders({
                    key,
                    path: node ? node.data.path : '/',
                    siteId: site.data.id,
                    page: nextPage,
                    append: true,
                    targetNode: node ?? undefined
                });
            },
            /**
             * Loads the next page (40 more) of search results for the current search term.
             */
            loadMoreSearchResults: () => {
                const site = store.selectedSite();
                const term = store.searchTerm();
                if (!site || !term) {
                    return;
                }

                const nextPage = store.searchPagination().page + 1;

                store.searchMore({
                    term,
                    siteId: site.data.id,
                    hostname: site.data.hostname,
                    page: nextPage
                });
            },
            /**
             * Loads the next page of sites for infinite scroll in the Sites panel.
             */
            loadMoreSites: rxMethod<void>(
                pipe(
                    filter(() => {
                        const pagination = store.sitesPagination();
                        const nextPage = pagination.page + 1;

                        return (
                            pagination.hasMore &&
                            !pagination.loading &&
                            store.sitesStatus() !== ComponentStatus.LOADING &&
                            !store.sitesLoadedPages().includes(nextPage)
                        );
                    }),
                    tap(() =>
                        patchState(store, {
                            sitesPagination: {
                                ...store.sitesPagination(),
                                loading: true
                            }
                        })
                    ),
                    switchMap(() => {
                        const filterTerm = store.siteSearchTerm().trim() || '*';
                        const nextPage = store.sitesPagination().page + 1;

                        return dotBrowsingService
                            .getSitesPage({
                                filter: filterTerm,
                                perPage: SITE_PAGE_LIMIT,
                                page: nextPage
                            })
                            .pipe(
                                tapResponse({
                                    next: ({ sites, pagination }) => {
                                        const pageSites = filterSystemHost(
                                            sites,
                                            store.isRequired()
                                        );

                                        patchState(store, {
                                            sites: mergeSites(store.sites(), pageSites),
                                            sitesLoadedPages: [
                                                ...store.sitesLoadedPages(),
                                                nextPage
                                            ],
                                            sitesPagination: {
                                                page: nextPage,
                                                hasMore: sitesHasMore(sites),
                                                loading: false,
                                                totalEntries: pagination.totalEntries
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            sitesPagination: {
                                                ...store.sitesPagination(),
                                                loading: false
                                            }
                                        });
                                    }
                                })
                            );
                    })
                )
            )
        };
    })
);
