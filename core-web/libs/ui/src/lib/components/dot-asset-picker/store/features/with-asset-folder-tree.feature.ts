import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, of, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DOT_FOLDER_TREE_PAGE_SIZE,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';

import {
    DotBrowsingService,
    SITE_PAGE_LIMIT,
    TREE_ROOT_NODE_KEY
} from '../../../../services/dot-browsing/dot-browsing.service';
import {
    findFolderParent,
    hasMorePages,
    resolveSiteId,
    SITES_LOAD_MORE_KEY,
    stripLoadMore,
    withLoadMore
} from '../../../dot-folder-tree/site-tree.utils';
import { MIN_TREE_SEARCH_LENGTH } from '../constants';
import { DotAssetPickerFolderTreeState, DotAssetPickerSite, DotAssetPickerState } from '../models';

const initialState: DotAssetPickerFolderTreeState = {
    folders: [],
    selectedNode: null,
    foldersStatus: ComponentStatus.INIT,
    treeSearch: ''
};

/**
 * What a tree load resolves to: the roots to render, and the node to highlight.
 *
 * An absent `selectedNode` means "leave the highlight alone" — distinct from `null`, which clears it.
 * A sidebar search needs the former: it changes what the tree *shows* without moving where the list
 * and uploads are pointed.
 */
type TreeLoadResult = { folders: TreeNodeItem[]; selectedNode?: TreeNodeItem | null };

/** Locates a site root among the tree roots by identifier. */
function findSiteRoot(roots: TreeNodeItem[], siteId: string): TreeNodeItem | undefined {
    return roots.find((node) => node.data?.type === 'site' && node.data.id === siteId);
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
    roots: TreeNodeItem[],
    site: DotAssetPickerSite,
    path: string | undefined,
    browsingService: DotBrowsingService
): Observable<TreeLoadResult> {
    const { identifier, hostname } = site;
    const root = findSiteRoot(roots, identifier);

    if (!root) {
        return of({ folders: roots, selectedNode: null });
    }

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
 * Folders matching the sidebar search term inside the site being browsed, listed flat under its
 * root — the tree half of "Search sites & folders" (the site half is the filtered roots).
 *
 * Capped at one page on purpose: the results are a flat recursive match, so a "Load more" sentinel
 * would page a *different* (non-recursive) query and silently return the wrong folders.
 *
 * The highlight is left untouched throughout: searching the tree must not silently re-scope the asset
 * list, nor move where an upload would land.
 */
function searchFoldersInBrowsingSite(
    roots: TreeNodeItem[],
    term: string,
    site: DotAssetPickerSite | undefined,
    browsingService: DotBrowsingService
): Observable<TreeLoadResult> {
    const root = site ? findSiteRoot(roots, site.identifier) : undefined;

    if (!site || !root) {
        return of({ folders: roots });
    }

    return browsingService
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
            take(1),
            map(({ folders }) => {
                root.children = folders;
                root.expanded = true;
                root.leaf = folders.length === 0;

                return { folders: roots, selectedNode: root };
            })
        );
}

/**
 * Sidebar tree: every site the user can browse as a root, each expandable into its folders.
 *
 * Unlike Content Drive — pinned to the site chosen in the global site switcher — the picker lets the
 * editor cross sites without leaving the dialog, because the asset they need is often not on the
 * site they happen to be editing. That makes it closer to the legacy Browser Selector, whose
 * paging helpers it shares (`site-tree.utils`).
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
        withMethods(
            (
                store,
                browsingService = inject(DotBrowsingService),
                httpErrorManager = inject(DotHttpErrorManagerService)
            ) => {
                /** Re-emits `folders` as a new object graph so `OnPush` sees in-place node mutations. */
                const refresh = () =>
                    patchState(store, { folders: structuredClone(store.folders()) });

                /** Current sites query: filtered by the sidebar term when one is active. */
                const sitesPage = (page: number) =>
                    browsingService.getSitesPage({
                        // Roots must all be browsable, and System Host is not.
                        system: false,
                        filter: activeSearchTerm() || '*',
                        perPage: SITE_PAGE_LIMIT,
                        page
                    });

                /** The sidebar term, or `''` when it is too short for the folder-name search. */
                const activeSearchTerm = (): string => {
                    const term = store.treeSearch().trim();

                    return term.length >= MIN_TREE_SEARCH_LENGTH ? term : '';
                };

                const methods = {
                    /**
                     * Loads the tree: page 1 of the sites, then either the configured site's branch
                     * or the folder matches for the active search term.
                     */
                    loadFolders: (): void => {
                        const site = store.browsingSite();

                        if (!site) {
                            return;
                        }

                        const term = activeSearchTerm();

                        patchState(store, { foldersStatus: ComponentStatus.LOADING });

                        // The success work lives in `tap`, BEFORE `catchError`, so a failure can
                        // never reach it. Handling it in `subscribe` instead would run it for the
                        // error fallback too, patching LOADED back over ERROR and making a failed
                        // load indistinguishable from a successfully loaded empty tree.
                        sitesPage(1)
                            .pipe(
                                take(1),
                                map(({ sites, pagination }) =>
                                    withLoadMore(
                                        sites,
                                        hasMorePages(pagination),
                                        SITES_LOAD_MORE_KEY,
                                        2,
                                        '',
                                        ''
                                    )
                                ),
                                switchMap((roots) =>
                                    term
                                        ? searchFoldersInBrowsingSite(
                                              roots,
                                              term,
                                              site,
                                              browsingService
                                          )
                                        : hydrateBrowsingSite(
                                              roots,
                                              site,
                                              store.path(),
                                              browsingService
                                          )
                                ),
                                tap(({ folders, selectedNode }) =>
                                    patchState(store, {
                                        folders,
                                        selectedNode,
                                        foldersStatus: ComponentStatus.LOADED
                                    })
                                ),
                                catchError((error) => {
                                    httpErrorManager.handle(error);
                                    patchState(store, { foldersStatus: ComponentStatus.ERROR });

                                    // EMPTY, not `of([])`: there is nothing meaningful to emit, and
                                    // a fallback value would only give the success path something
                                    // to run on.
                                    return EMPTY;
                                })
                            )
                            .subscribe();
                    },

                    /** Narrows the tree. Terms shorter than two characters behave as "no search". */
                    setTreeSearch: (treeSearch: string): void => {
                        if (treeSearch === store.treeSearch()) {
                            return;
                        }

                        patchState(store, { treeSearch });
                        methods.loadFolders();
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

                                if (!data || data.type === LOAD_MORE_NODE_TYPE) {
                                    return EMPTY;
                                }

                                // Already populated, or known to have nothing to populate.
                                if ((node.children?.length ?? 0) > 0 || node.leaf) {
                                    node.expanded = true;
                                    refresh();

                                    return EMPTY;
                                }

                                const siteId = resolveSiteId(node, store.folders());

                                if (!siteId) {
                                    return EMPTY;
                                }

                                const path = data.path || '/';

                                node.loading = true;
                                refresh();

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
                                        tap(({ folders, pagination }) => {
                                            node.loading = false;
                                            node.expanded = true;
                                            node.leaf = folders.length === 0;
                                            node.children = withLoadMore(
                                                folders,
                                                hasMorePages(pagination),
                                                node.key ?? siteId,
                                                2,
                                                path,
                                                data.hostname
                                            );
                                            refresh();
                                        }),
                                        catchError((error) => {
                                            node.loading = false;
                                            refresh();
                                            httpErrorManager.handle(error);

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

                                if (!data || data.type !== LOAD_MORE_NODE_TYPE) {
                                    return EMPTY;
                                }

                                const nextPage = data.nextPage ?? 2;
                                const parentPath = data.path ?? '';
                                const hostname = data.hostname ?? '';
                                const isSitesLevel = !hostname && parentPath === '';

                                node.loading = true;
                                refresh();

                                if (isSitesLevel) {
                                    return sitesPage(nextPage).pipe(
                                        tap(({ sites, pagination }) => {
                                            patchState(store, {
                                                folders: withLoadMore(
                                                    [...stripLoadMore(store.folders()), ...sites],
                                                    hasMorePages(pagination),
                                                    SITES_LOAD_MORE_KEY,
                                                    nextPage + 1,
                                                    '',
                                                    ''
                                                )
                                            });
                                        }),
                                        catchError((error) => {
                                            node.loading = false;
                                            refresh();
                                            httpErrorManager.handle(error);

                                            return EMPTY;
                                        })
                                    );
                                }

                                const parent = findFolderParent(
                                    store.folders(),
                                    parentPath || '/',
                                    hostname
                                );
                                const siteId = parent
                                    ? resolveSiteId(parent, store.folders())
                                    : undefined;

                                if (!parent || !siteId) {
                                    node.loading = false;
                                    refresh();

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
                                        tap(({ folders, pagination }) => {
                                            // Keep what is already loaded, drop the old sentinel,
                                            // append the new page.
                                            parent.children = withLoadMore(
                                                [
                                                    ...stripLoadMore(
                                                        parent.children as TreeNodeItem[]
                                                    ),
                                                    ...folders
                                                ],
                                                hasMorePages(pagination),
                                                parent.key ?? siteId,
                                                nextPage + 1,
                                                parentPath || '/',
                                                hostname
                                            );
                                            refresh();
                                        }),
                                        catchError((error) => {
                                            node.loading = false;
                                            refresh();
                                            httpErrorManager.handle(error);

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
                        const site = store.browsingSite();

                        patchState(store, {
                            selectedNode: site
                                ? (findSiteRoot(store.folders(), site.identifier) ?? null)
                                : null
                        });
                    },

                    /**
                     * `structuredClone` is load-bearing: tree nodes are mutated in place (children,
                     * loading flags), and a shallow copy would keep the same references, so change
                     * detection would never see the update.
                     */
                    updateFolders: (folders: TreeNodeItem[]): void => {
                        patchState(store, { folders: structuredClone(folders) });
                    }
                };

                return methods;
            }
        )
    );
}
