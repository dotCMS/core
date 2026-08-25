import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    DOT_FOLDER_TREE_PAGE_SIZE,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';

import {
    DotBrowsingService,
    TREE_ROOT_NODE_KEY
} from '../../../../services/dot-browsing/dot-browsing.service';
import {
    findFolderParent,
    findNodeByKey,
    findSiteIdByHostname,
    hasMorePages,
    resolveSiteId,
    stripLoadMore,
    withLoadMore
} from '../../../dot-folder-tree/site-tree.utils';
import { ASSET_PICKER_ERROR_KEYS, MIN_TREE_SEARCH_LENGTH } from '../constants';
import { DotAssetPickerFolderTreeState, DotAssetPickerSite, DotAssetPickerState } from '../models';

const initialState: DotAssetPickerFolderTreeState = {
    folders: [],
    selectedNode: null,
    foldersStatus: ComponentStatus.INIT,
    folderSearch: '',
    searchResults: null,
    searchStatus: ComponentStatus.INIT,
    searchHasMore: false
};

/**
 * Icons for the one root the picker renders.
 *
 * `mapSiteToTreeNode` gives site-typed nodes a globe, which is right for Content Drive — its root
 * *is* the site indicator. Here the globe already sits in the site selector above the tree, so
 * repeating it on the root would show the same idea twice and read as a second site control. The
 * root is reassigned the folder affordance of the nodes beneath it instead.
 */
const ROOT_ICONS = {
    expandedIcon: 'pi pi-folder-open',
    collapsedIcon: 'pi pi-folder'
} as const;

/**
 * What a tree load resolves to: the roots to render, and the node to highlight.
 *
 * An absent `selectedNode` means "leave the highlight alone" — distinct from `null`, which clears it.
 * A sidebar search needs the former: it changes what the tree *shows* without moving where the list
 * and uploads are pointed.
 */
type TreeLoadResult = { folders: TreeNodeItem[]; selectedNode?: TreeNodeItem | null };

/**
 * The one tree root: the browsed site, wearing folder icons and rendered as `All` by the sidebar.
 *
 * It stays typed as a `site` node on purpose. `resolveSiteId` walks up to the site root to find the
 * identifier a folder node does not carry, and `selectNode` branches on `type === 'site'` to scope
 * the list to the whole site — both keep working untouched as long as this stays a site node. That
 * is what makes `All` a *presentation* change rather than a structural one.
 */
function buildSiteRoot(
    site: DotAssetPickerSite,
    browsingService: DotBrowsingService
): TreeNodeItem {
    return { ...browsingService.mapSiteToTreeNode(site), ...ROOT_ICONS };
}

/**
 * Appends a "Load more" sentinel to every level of a pre-resolved branch that still has pages left,
 * so a tree opened deep on a remembered folder pages exactly like one browsed by hand.
 *
 * `buildTreeByPaths` returns per-level `{ page, hasMore }` keyed by node key (`'root'` for the site
 * root), which is the only place that information exists — the nodes themselves don't carry it.
 */
function injectLoadMoreSentinels(
    folders: TreeNodeItem[],
    pagination: Record<string, { page: number; hasMore: boolean }>,
    hostname: string
): TreeNodeItem[] {
    const walk = (nodes: TreeNodeItem[], levelKey: string, parentPath: string): TreeNodeItem[] => {
        const cleaned = stripLoadMore(nodes).map((node) =>
            Array.isArray(node.children)
                ? {
                      ...node,
                      children: walk(
                          node.children as TreeNodeItem[],
                          node.key ?? '',
                          node.data?.path ?? '/'
                      )
                  }
                : node
        );

        const level = pagination[levelKey];

        return level?.hasMore
            ? withLoadMore(cleaned, true, levelKey, level.page + 1, parentPath, hostname)
            : cleaned;
    };

    return walk(folders, TREE_ROOT_NODE_KEY, '/');
}

/**
 * Expands the site being browsed down to `path`, so a picker reopening on a remembered folder shows
 * that folder in context instead of collapsed at the root.
 *
 * When that site is not on the first page of the sites list the tree is left collapsed — the user can
 * still page or search to it — rather than issuing a lookup nobody asked for.
 */
function hydrateBrowsingSite(
    site: DotAssetPickerSite,
    path: string | undefined,
    browsingService: DotBrowsingService
): Observable<TreeLoadResult> {
    const { identifier, hostname } = site;
    const root = buildSiteRoot(site, browsingService);
    const roots = [root];

    const targetPath = path ?? '';

    return browsingService.buildTreeByPaths(identifier, hostname, targetPath).pipe(
        take(1),
        map(({ node, tree, pagination }) => {
            const children = injectLoadMoreSentinels(
                tree?.folders ?? [],
                pagination ?? {},
                hostname
            );

            root.children = children;
            root.expanded = true;
            root.leaf = children.length === 0;

            return { folders: roots, selectedNode: node ?? root };
        })
    );
}

/**
 * Sidebar tree: the folders of **one** site — the one chosen in the sidebar's site selector —
 * rooted at a single node the sidebar renders as `All`.
 *
 * The picker used to make every browsable site a root, so changing site meant expanding a different
 * one. That coupled two unrelated queries: a single sidebar term filtered the sites list *and*
 * searched folders, which dropped the browsed site out of the roots whenever the term matched a
 * folder name but not a hostname — the normal case — and forced the folder search to re-synthesise
 * the root it had just lost. Making the site an explicit input removes the cause rather than
 * patching the symptom, and moves the picker onto Content Drive's one-site-at-a-time model.
 *
 * The sites query now lives in the selector (`DotSiteComponent`), not here.
 *
 * System Host is deliberately absent: it is not addressable as a drive `assetPath`, and its shared
 * assets already surface in every site's listing through `includeSystemHost`.
 *
 * Deliberately has no `withHooks` of its own — the tree loads when the host calls `initPicker`, not
 * when the store is constructed, because until then there is no configuration to open on.
 */
export function withAssetFolderTree() {
    return signalStoreFeature(
        { state: type<DotAssetPickerState>() },
        withState<DotAssetPickerFolderTreeState>(initialState),
        withComputed((store) => ({
            /**
             * Whether a term is long enough to be a search at all. Below the minimum the sidebar
             * shows the tree and nothing is requested — `/folder/search` rejects a shorter `name`.
             */
            isSearchingFolders: computed(
                () => store.folderSearch().trim().length >= MIN_TREE_SEARCH_LENGTH
            ),
            displayedResults: computed(() => store.searchResults() ?? []),
            /**
             * Only once a search has actually resolved with nothing. Guarding on LOADED is what
             * keeps the empty state from flashing mid-request or standing in for a failure.
             */
            showResultsEmptyState: computed(
                () =>
                    store.folderSearch().trim().length >= MIN_TREE_SEARCH_LENGTH &&
                    store.searchStatus() === ComponentStatus.LOADED &&
                    (store.searchResults()?.length ?? 0) === 0
            ),
            showRefineHint: computed(
                () =>
                    store.folderSearch().trim().length >= MIN_TREE_SEARCH_LENGTH &&
                    store.searchHasMore()
            ),
            selectedResultKey: computed(() => store.selectedNode()?.key ?? null)
        })),
        withMethods((store, browsingService = inject(DotBrowsingService)) => {
            /**
             * Publishes a tree, keeping the highlight on whatever node it was on.
             *
             * `selectedNode` is compared by reference by `p-tree`, and every publish hands it a
             * new object graph, so the selection has to be re-pointed by key or the highlight
             * silently disappears the first time a branch loads.
             */
            const publish = (folders: TreeNodeItem[]): void => {
                const selectedKey = store.selectedNode()?.key;

                patchState(store, {
                    folders,
                    ...(selectedKey
                        ? { selectedNode: findNodeByKey(folders, selectedKey) ?? null }
                        : {})
                });
            };

            /**
             * Applies `change` to the node with `key` in a **fresh clone** of the tree.
             *
             * Both halves matter, and they pull against each other. The clone is what makes the
             * change visible: `p-tree` and its `p-treeNode`s are `OnPush` and their `*ngFor`
             * tracks by object identity, so a node mutated in place is never re-rendered — the
             * spinner would only clear the next time something else forced change detection.
             * Addressing the node by key rather than by reference is what makes the clone safe:
             * lazy loading spans a request, and a reference taken before it would point at an
             * object the tree has since replaced.
             */
            const mutateNode = (key: string, change: (node: TreeNodeItem) => void): void => {
                const folders = structuredClone(store.folders());
                const target = findNodeByKey(folders, key);

                if (!target) {
                    return;
                }

                change(target);
                publish(folders);
            };

            /**
             * `switchMap`, not `mergeMap`: each keystroke supersedes the last, and a slow earlier
             * response landing after a faster later one would show results for a term the editor
             * has already moved past.
             *
             * Capped at one page on purpose. The results are a recursive match, while the tree's
             * paging is non-recursive — a "load more" here would page a *different* query and
             * quietly return the wrong folders. `searchHasMore` drives a "narrow your search" hint
             * instead.
             */
            const runFolderSearch = rxMethod<{ term: string; site: DotAssetPickerSite }>(
                pipe(
                    tap(() => patchState(store, { searchStatus: ComponentStatus.LOADING })),
                    switchMap(({ term, site }) =>
                        browsingService
                            .searchFolders(
                                {
                                    siteId: site.identifier,
                                    path: '/',
                                    recursive: true,
                                    name: term,
                                    page: 1,
                                    per_page: DOT_FOLDER_TREE_PAGE_SIZE
                                },
                                site.hostname
                            )
                            .pipe(
                                tap(({ folders, pagination }) =>
                                    patchState(store, {
                                        searchResults: folders,
                                        searchHasMore: hasMorePages(pagination),
                                        searchStatus: ComponentStatus.LOADED
                                    })
                                ),
                                catchError(() => {
                                    // ERROR, and `searchResults` left as it was: an empty array
                                    // here would render the empty state and call a failure "no
                                    // matches".
                                    patchState(store, {
                                        searchStatus: ComponentStatus.ERROR,
                                        requestError: {
                                            messageKey: ASSET_PICKER_ERROR_KEYS.folders
                                        }
                                    });

                                    return EMPTY;
                                })
                            )
                    )
                )
            );

            const methods = {
                /**
                 * Loads the tree: the browsed site's root, expanded down to `path`.
                 *
                 * No sites request any more — the site is handed in, not discovered — so this is
                 * one query where it used to be two coupled ones.
                 */
                loadFolders: (): void => {
                    const site = store.browsingSite();

                    if (!site) {
                        return;
                    }

                    patchState(store, { foldersStatus: ComponentStatus.LOADING });

                    // The success work lives in `tap`, BEFORE `catchError`, so a failure can
                    // never reach it. Handling it in `subscribe` instead would run it for the
                    // error fallback too, patching LOADED back over ERROR and making a failed
                    // load indistinguishable from a successfully loaded empty tree.
                    hydrateBrowsingSite(site, store.path(), browsingService)
                        .pipe(
                            take(1),
                            tap(({ folders, selectedNode }) =>
                                patchState(store, {
                                    folders,
                                    // Spread only when the loader actually decided on a node.
                                    // `patchState` merges with `{...current, ...partial}`, so an
                                    // absent key destructured to `undefined` would still be
                                    // present in the partial and would wipe the highlight —
                                    // exactly what `TreeLoadResult` says must not happen.
                                    ...(selectedNode !== undefined ? { selectedNode } : {}),
                                    foldersStatus: ComponentStatus.LOADED
                                })
                            ),
                            catchError(() => {
                                patchState(store, {
                                    foldersStatus: ComponentStatus.ERROR,
                                    requestError: {
                                        messageKey: ASSET_PICKER_ERROR_KEYS.folders
                                    }
                                });

                                // EMPTY, not `of([])`: there is nothing meaningful to emit, and
                                // a fallback value would only give the success path something
                                // to run on.
                                return EMPTY;
                            })
                        )
                        .subscribe();
                },

                /**
                 * Runs the folder term against the browsed site, flat and recursive.
                 *
                 * Below the minimum length this clears back to the tree without issuing anything —
                 * `/folder/search` rejects a shorter `name`, so a single letter is "no search"
                 * rather than a request that fails.
                 *
                 * The highlight is deliberately untouched: searching changes what the sidebar
                 * *shows*, not where the asset list or an upload is pointed.
                 */
                setFolderSearch: (folderSearch: string): void => {
                    if (folderSearch === store.folderSearch()) {
                        return;
                    }

                    patchState(store, { folderSearch });

                    const site = store.browsingSite();
                    const term = folderSearch.trim();

                    if (!site || term.length < MIN_TREE_SEARCH_LENGTH) {
                        patchState(store, {
                            searchResults: null,
                            searchStatus: ComponentStatus.INIT,
                            searchHasMore: false
                        });

                        return;
                    }

                    runFolderSearch({ term, site });
                },

                /**
                 * Lazily loads a node's first page of children the first time it is expanded.
                 *
                 * `mergeMap`, not `exhaustMap`: expanding a second node while the first is still
                 * loading has to work — they are independent branches, not a repeated request.
                 */
                expandNode: rxMethod<TreeNodeItem>(
                    pipe(
                        mergeMap((node) => {
                            const data = node.data;
                            const key = node.key;

                            if (!key || !data || data.type === LOAD_MORE_NODE_TYPE) {
                                return EMPTY;
                            }

                            // Already populated, or known to have nothing to populate.
                            if ((node.children?.length ?? 0) > 0 || node.leaf) {
                                mutateNode(key, (target) => (target.expanded = true));

                                return EMPTY;
                            }

                            const siteId = resolveSiteId(node, store.folders());

                            if (!siteId) {
                                return EMPTY;
                            }

                            const path = data.path || '/';

                            mutateNode(key, (target) => (target.loading = true));

                            return browsingService
                                .searchFolders(
                                    {
                                        siteId,
                                        path,
                                        recursive: false,
                                        page: 1,
                                        per_page: DOT_FOLDER_TREE_PAGE_SIZE
                                    },
                                    data.hostname
                                )
                                .pipe(
                                    tap(({ folders, pagination }) =>
                                        mutateNode(key, (target) => {
                                            target.loading = false;
                                            target.expanded = true;
                                            target.leaf = folders.length === 0;
                                            target.children = withLoadMore(
                                                folders,
                                                hasMorePages(pagination),
                                                key,
                                                2,
                                                path,
                                                data.hostname
                                            );
                                        })
                                    ),
                                    catchError(() => {
                                        mutateNode(key, (target) => (target.loading = false));
                                        patchState(store, {
                                            requestError: {
                                                messageKey: ASSET_PICKER_ERROR_KEYS.folders
                                            }
                                        });

                                        // EMPTY keeps the rxMethod alive for the next expand.
                                        return EMPTY;
                                    })
                                );
                        })
                    )
                ),

                /**
                 * Loads the next page of a level when its "Load more" sentinel is clicked.
                 *
                 * The sites level is identified by having neither a hostname nor a parent path —
                 * its sentinel is a sibling of the site roots, not a child of anything.
                 */
                loadMore: rxMethod<TreeNodeItem>(
                    pipe(
                        mergeMap((node) => {
                            const data = node.data as TreeNodeLoadMoreData | undefined;
                            const key = node.key;

                            if (!key || !data || data.type !== LOAD_MORE_NODE_TYPE) {
                                return EMPTY;
                            }

                            const nextPage = data.nextPage ?? 2;
                            const parentPath = data.path ?? '';
                            const hostname = data.hostname ?? '';

                            // A sentinel with no hostname and no parent path is a *sites*-level
                            // one — the shape the tree produced back when sites were the roots.
                            // There is no such level any more, and paging it would query the site
                            // catalog the selector now owns. Ignore it rather than act on it.
                            if (!hostname && parentPath === '') {
                                return EMPTY;
                            }

                            mutateNode(key, (target) => (target.loading = true));

                            const parentKey = findFolderParent(
                                store.folders(),
                                parentPath || '/',
                                hostname
                            )?.key;
                            const siteId = findSiteIdByHostname(hostname, store.folders());

                            if (!parentKey || !siteId) {
                                mutateNode(key, (target) => (target.loading = false));

                                return EMPTY;
                            }

                            return browsingService
                                .searchFolders(
                                    {
                                        siteId,
                                        path: parentPath || '/',
                                        recursive: false,
                                        page: nextPage,
                                        per_page: DOT_FOLDER_TREE_PAGE_SIZE
                                    },
                                    hostname
                                )
                                .pipe(
                                    tap(({ folders, pagination }) =>
                                        // Keep what is already loaded, drop the old sentinel,
                                        // append the new page.
                                        mutateNode(parentKey, (parent) => {
                                            parent.children = withLoadMore(
                                                [
                                                    ...stripLoadMore(
                                                        parent.children as TreeNodeItem[]
                                                    ),
                                                    ...folders
                                                ],
                                                hasMorePages(pagination),
                                                parentKey,
                                                nextPage + 1,
                                                parentPath || '/',
                                                hostname
                                            );
                                        })
                                    ),
                                    catchError(() => {
                                        mutateNode(key, (target) => (target.loading = false));
                                        patchState(store, {
                                            requestError: {
                                                messageKey: ASSET_PICKER_ERROR_KEYS.folders
                                            }
                                        });

                                        return EMPTY;
                                    })
                                );
                        })
                    )
                ),

                setSelectedNode: (selectedNode: TreeNodeItem | null): void => {
                    patchState(store, { selectedNode });
                },

                /**
                 * Snaps the highlight back to the root of the site being browsed, for whatever
                 * widens the list back to site-wide (today: free-text asset search).
                 *
                 * The highlight is not cosmetic — `$targetFolder` derives the upload destination
                 * from it — so leaving it on a folder the list is no longer scoped to would send
                 * uploads somewhere the user is not looking.
                 */
                selectRootNode: (): void => {
                    // With one root there is nothing to search for: it either exists or the tree
                    // has not resolved yet.
                    patchState(store, { selectedNode: store.folders()[0] ?? null });
                }
            };

            return methods;
        })
    );
}
