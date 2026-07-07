import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { debounceTime, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    CustomTreeNode,
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';
import { DotBrowsingService, normalizeHostFolderBrowsePath } from '@dotcms/ui';

export const PEER_PAGE_LIMIT = 7000;
export const FOLDER_PAGE_LIMIT = 40;
export const MIN_SEARCH_LENGTH = 3;
export const ROOT_NODE_KEY = 'root';

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

const HOST_FOLDER_DISPLAY_PATH_SEPARATOR = ' / ';

/**
 * Formats a host/folder label for display in the field trigger.
 * Example: `demo.dotcms.com/application/apivtl/` → `demo.dotcms.com / application / apivtl`
 */
export function formatHostFolderDisplayPath(label: string): string {
    return label.split('/').filter(Boolean).join(HOST_FOLDER_DISPLAY_PATH_SEPARATOR);
}

export type NodePaginationState = {
    page: number;
    hasMore: boolean;
    loading: boolean;
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
    selectedSite: TreeNodeItem | null;
    folders: TreeNodeItem[];
    foldersStatus: ComponentStatus;
    nodePagination: Record<string, NodePaginationState>;
    searchTerm: string;
    searchResults: TreeNodeItem[] | null;
    searchStatus: ComponentStatus;
    confirmedNode: TreeNodeItem | null;
    pendingNode: TreeNodeItem | null;
    overlayOpen: boolean;
    isRequired: boolean;
    error: string | null;
};

export const initialState: HostFolderFiledState = {
    sites: [],
    sitesStatus: ComponentStatus.INIT,
    selectedSite: null,
    folders: [],
    foldersStatus: ComponentStatus.INIT,
    nodePagination: {},
    searchTerm: '',
    searchResults: null,
    searchStatus: ComponentStatus.INIT,
    confirmedNode: null,
    pendingNode: null,
    overlayOpen: false,
    isRequired: false,
    error: null
};

/**
 * Walks an already-resolved folder tree (e.g. from `buildTreeByPaths`) and marks every
 * node that already has a `children` array as fully loaded, so the overlay doesn't
 * re-fetch levels that were populated during initialization from a preselected path.
 */
function buildInitialPaginationMap(folders: TreeNodeItem[]): Record<string, NodePaginationState> {
    const map: Record<string, NodePaginationState> = {
        [ROOT_NODE_KEY]: { page: 1, hasMore: false, loading: false }
    };

    const walk = (nodes: TreeNodeItem[]) => {
        nodes.forEach((node) => {
            if (Array.isArray(node.children)) {
                map[node.key] = { page: 1, hasMore: false, loading: false };
                walk(node.children as TreeNodeItem[]);
            }
        });
    };

    walk(folders);

    return map;
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

export const HostFolderFiledStore = signalStore(
    withState(initialState),
    withComputed(
        ({
            sitesStatus,
            overlayOpen,
            confirmedNode,
            pendingNode,
            searchTerm,
            searchResults,
            folders,
            foldersStatus
        }) => ({
            /**
             * Icon classes for the field trigger: spinner while the initial sites/value
             * resolution is in flight, otherwise a chevron reflecting the overlay state.
             */
            iconClasses: computed(() => {
                const loading = sitesStatus() === ComponentStatus.LOADING;
                const open = overlayOpen();

                return {
                    'pi-spin': loading,
                    'pi-spinner': loading,
                    'pi-chevron-up': !loading && open,
                    'pi-chevron-down': !loading && !open
                };
            }),
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
            copyPath: computed(() => {
                const node = confirmedNode();

                if (!node?.data) {
                    return '';
                }

                const { hostname, path } = node.data;
                const cleanHostname = hostname.replace('//', '');

                return `//${cleanHostname}${path ? path : '/'}`;
            }),
            /**
             * Human-readable full path for the field trigger (hostname and folders separated by ` / `).
             */
            displayPath: computed(() => {
                const node = confirmedNode();

                if (!node?.label) {
                    return '';
                }

                return formatHostFolderDisplayPath(node.label);
            }),
            /**
             * Whether the current search term is long enough to trigger a backend search
             * (the folder search endpoint requires at least `MIN_SEARCH_LENGTH` characters).
             */
            isSearching: computed(() => searchTerm().length >= MIN_SEARCH_LENGTH),
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
            foldersLoading: computed(() => foldersStatus() === ComponentStatus.LOADING),
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
                    searchTerm().length >= MIN_SEARCH_LENGTH ? (searchResults() ?? []) : folders();

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
        })
    ),
    withMethods((store) => {
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
                        patchState(store, {
                            foldersStatus: ComponentStatus.LOADING,
                            nodePagination: {
                                ...store.nodePagination(),
                                [params.key]: { ...current, loading: true }
                            }
                        });
                    }),
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
                                            const target = params.targetNode;
                                            target.leaf = folders.length === 0 && params.page === 1;
                                            target.icon = 'pi pi-folder-open';

                                            const prevChildren = params.append
                                                ? stripLoadMore(target.children as TreeNodeItem[])
                                                : [];
                                            const nextChildren = [...prevChildren, ...folders];
                                            target.children = hasMore
                                                ? [...nextChildren, createLoadMoreNode(params.key)]
                                                : nextChildren;

                                            patchState(store, {
                                                folders: structuredClone(store.folders()),
                                                foldersStatus: ComponentStatus.LOADED,
                                                nodePagination
                                            });
                                        } else {
                                            const prevFolders = params.append
                                                ? stripLoadMore(store.folders())
                                                : [];
                                            const nextFolders = [...prevFolders, ...folders];

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
                                    error: () => {
                                        patchState(store, {
                                            foldersStatus: ComponentStatus.ERROR,
                                            error: '',
                                            nodePagination: {
                                                ...store.nodePagination(),
                                                [params.key]: {
                                                    ...(store.nodePagination()[params.key] ?? {
                                                        page: 1,
                                                        hasMore: false
                                                    }),
                                                    loading: false
                                                }
                                            }
                                        });
                                    }
                                })
                            );
                    })
                )
            ),
            /**
             * Searches folders within the currently selected site (recursive), scoped by
             * name. Clears search results when the term is empty; ignores 1-2 char terms
             * since the backend requires a minimum of 3 characters.
             */
            search: rxMethod<string>(
                pipe(
                    tap((term) => patchState(store, { searchTerm: term })),
                    filter((term) => term.length === 0 || term.length >= MIN_SEARCH_LENGTH),
                    debounceTime(300),
                    distinctUntilChanged(),
                    switchMap((term) => {
                        if (!term) {
                            patchState(store, {
                                searchResults: null,
                                searchStatus: ComponentStatus.IDLE
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
                                    name: term
                                },
                                site.data.hostname
                            )
                            .pipe(
                                tapResponse({
                                    next: ({ folders }) =>
                                        patchState(store, {
                                            searchResults: folders,
                                            searchStatus: ComponentStatus.LOADED
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            searchResults: [],
                                            searchStatus: ComponentStatus.ERROR
                                        })
                                })
                            );
                    })
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
                    pendingNode: store.confirmedNode()
                });
            }
        };
    }),
    withMethods((store) => {
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
                    tap(() => patchState(store, { sitesStatus: ComponentStatus.LOADING })),
                    switchMap(({ path, isRequired }) => {
                        return dotBrowsingService
                            .getSitesTreePath({
                                perPage: PEER_PAGE_LIMIT,
                                filter: '*',
                                page: 1
                            })
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
                                    next: (sites) =>
                                        patchState(store, {
                                            sites,
                                            sitesStatus: ComponentStatus.LOADED,
                                            isRequired
                                        }),
                                    error: () =>
                                        patchState(store, {
                                            sitesStatus: ComponentStatus.ERROR,
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
                            return dotBrowsingService.getCurrentSiteAsTreeNodeItem().pipe(
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

                        const node =
                            sites.find((item) => item.label === SYSTEM_HOST_NAME) ?? sites[0];

                        return of({
                            path: node?.label,
                            sites
                        });
                    }),
                    switchMap(({ path, sites }) => {
                        if (!path) {
                            return of({
                                site: undefined as TreeNodeItem | undefined,
                                node: null as TreeNodeItem | null,
                                tree: null as CustomTreeNode['tree']
                            });
                        }

                        const hasPaths = path.includes('/');

                        if (!hasPaths) {
                            const site = sites.find(
                                (item) => item.data?.hostname === path || item.label === path
                            );

                            return of({
                                site,
                                node: null as TreeNodeItem | null,
                                tree: null as CustomTreeNode['tree']
                            });
                        }

                        return dotBrowsingService.buildTreeByPaths(path).pipe(
                            map(({ node, tree }) => {
                                const hostname = tree?.parent?.hostName;
                                const site = sites.find((item) => item.data?.hostname === hostname);

                                return { site, node, tree };
                            })
                        );
                    }),
                    tap(({ site, node, tree }) => {
                        if (!site) {
                            // The preselected value (or default) couldn't be matched to a site
                            // in the currently loaded list (e.g. an archived/inaccessible site,
                            // or no sites available at all). Surface this deterministically
                            // instead of leaving the store looking successfully-but-silently
                            // uninitialized.
                            patchState(store, {
                                sitesStatus: ComponentStatus.ERROR,
                                error: ''
                            });

                            return;
                        }

                        const changes: Partial<HostFolderFiledState> = {
                            selectedSite: site,
                            confirmedNode: node ?? site,
                            pendingNode: node ?? site
                        };

                        if (tree?.folders) {
                            expandFoldersToTarget(tree.folders, node?.data?.path);
                            changes.folders = tree.folders;
                            changes.nodePagination = buildInitialPaginationMap(tree.folders);
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
             * the already-selected site (e.g. re-clicking it while browsing a preselected
             * nested path) is a no-op, so the resolved folder tree and pending selection
             * aren't lost.
             */
            selectSite: (site: TreeNodeItem) => {
                if (store.selectedSite()?.key === site.key) {
                    return;
                }

                patchState(store, {
                    selectedSite: site,
                    folders: [],
                    nodePagination: {},
                    searchTerm: '',
                    searchResults: null,
                    pendingNode: site
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
            }
        };
    })
);
