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

import { catchError, switchMap, take, tap } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { ComponentStatus, LOAD_MORE_NODE_TYPE, TreeNodeItem } from '@dotcms/dotcms-models';

import {
    DEFAULT_ASSET_PICKER_PAGE,
    DEFAULT_ASSET_PICKER_PAGINATION,
    DEFAULT_ASSET_PICKER_SORT
} from './constants';
import { withAssetBrowse } from './features/with-asset-browse.feature';
import { withAssetFolderTree } from './features/with-asset-folder-tree.feature';
import { withAssetSelection } from './features/with-asset-selection.feature';
import { buildPickerFilterDefaults, hasNonDefaultPickerFilters } from './filter-defaults';
import {
    DotAssetPickerConfig,
    DotAssetPickerFilters,
    DotAssetPickerSite,
    DotAssetPickerState
} from './models';

import { resolveSiteId } from '../../dot-folder-tree/site-tree.utils';

const initialState: DotAssetPickerState = {
    config: null,
    requestError: null,
    browsingSite: undefined,
    path: undefined,
    filters: {},
    isFullscreen: false
};

/**
 * Headless browse store for the AssetPicker.
 *
 * Builds the same drive search request Content Drive does, but knows nothing about the router: it is
 * configured explicitly through {@link DotAssetPickerStore.initPicker} instead of hydrating from
 * query params, and it never writes to the URL. That matters because the picker opens as a dialog on
 * top of Edit Contentlet, whose URL state belongs to the editor — a store that wrote query params
 * would corrupt the host's history and could navigate the editor away mid-edit.
 *
 * It also does not navigate to the content or page editors, and carries no context-menu, drag, or
 * content-creation state.
 *
 * Provide it per picker instance (`providers: [DotAssetPickerStore]`), not application-wide, so each
 * opening starts clean.
 */
export const DotAssetPickerStore = signalStore(
    withState(initialState),
    // Selection comes first: `withAssetBrowse` clears `selectedAsset` when a new result replaces the
    // list, so the slot has to exist before it is composed.
    withAssetSelection(),
    withAssetBrowse(),
    withAssetFolderTree(),
    withComputed(({ filters, config }) => ({
        /**
         * Whether anything differs from what the picker opened with, which is what drives the
         * shared toolbar's "Clear all".
         *
         * Counting filter keys would keep the button on screen permanently: the caller's seeds are
         * always present, so there is always something in the bag. What matters is whether anything
         * is worth clearing.
         */
        $hasNonDefaultFilters: computed(() => hasNonDefaultPickerFilters(filters(), config()))
    })),
    withMethods((store, siteService = inject(DotSiteService)) => {
        /**
         * Resolves the site to open on when the caller did not name one.
         *
         * `DotSiteService` is already in this component's graph (the folder tree reaches it through
         * `DotBrowsingService`) and depends on nothing but `HttpClient` — which is exactly why the
         * picker asks it rather than `GlobalStore`. `@dotcms/ui` is bundled into the legacy Dojo
         * custom elements, which boot without a `Router`, so anything pulling one in would break
         * that bundle at load time even though the picker never opens there.
         */
        const resolveEntrySite = rxMethod<void>(
            pipe(
                switchMap(() =>
                    siteService.getCurrentSite().pipe(
                        take(1),
                        tap((site) => {
                            if (site) {
                                patchState(store, {
                                    browsingSite: {
                                        identifier: site.identifier,
                                        hostname: site.hostname
                                    }
                                });
                            }
                        }),
                        // No site is not an error state: the sidebar lists every site the user can
                        // browse, so the picker opens on the tree with nothing selected and they
                        // pick one. `$isBrowsable` keeps the search from firing until then.
                        catchError(() => EMPTY)
                    )
                )
            )
        );

        /** Any filter change invalidates the cursor bookmarks and sends the user back to page 1. */
        const resetPaging = () => ({
            pagination: { ...store.pagination(), page: 1 },
            pages: [DEFAULT_ASSET_PICKER_PAGE]
        });

        return {
            /**
             * Configures the picker and kicks off the folder tree. Replaces Content Drive's
             * URL-reading init effect. The search follows on its own, driven by `$request`.
             *
             * `config.languageId` and `config.baseTypes` seed the *visible* filters — the editor can
             * clear them. `config.mimeTypes` does not: it lives outside the filter bag on purpose.
             */
            initPicker: (config: DotAssetPickerConfig): void => {
                // Starting point only — the sidebar can move the picker to another site. The
                // remembered location wins, then the caller's own site; with neither, the picker
                // looks the current one up rather than refusing to open.
                const entrySite = config.browseSite ?? config.site;

                patchState(store, {
                    config,
                    browsingSite: entrySite && {
                        identifier: entrySite.identifier,
                        hostname: entrySite.hostname
                    },
                    path: config.path,
                    // Shared with `clearFilters`, so a clear lands on exactly what a fresh open
                    // shows. Keeping the two in one function is what stops one path from quietly
                    // missing a seed.
                    filters: buildPickerFilterDefaults(config),
                    pagination: DEFAULT_ASSET_PICKER_PAGINATION,
                    // Seeded, not pinned: `sortByDesc` sets the direction the picker opens with,
                    // and the user can still re-sort from the table header afterwards.
                    sort: config.browse
                        ? {
                              field: config.browse.sortField ?? DEFAULT_ASSET_PICKER_SORT.field,
                              order: config.browse.sortByDesc === false ? 'asc' : 'desc'
                          }
                        : DEFAULT_ASSET_PICKER_SORT,
                    pages: [DEFAULT_ASSET_PICKER_PAGE],
                    items: [],
                    selectedAsset: null
                });

                if (!entrySite) {
                    resolveEntrySite();
                }

                store.loadFolders();
            },

            /**
             * Scopes the list to whatever the user picked in the sidebar: a site root browses that
             * site's root, a folder browses that folder.
             *
             * This is where a cross-site move happens — the tree is the only way to change site, and
             * a folder node carries its hostname but not its site id, so the id is resolved from the
             * root it hangs from.
             */
            selectNode: (node: TreeNodeItem): void => {
                const data = node.data;

                if (!data || data.type === LOAD_MORE_NODE_TYPE) {
                    return;
                }

                const identifier = resolveSiteId(node, store.folders());

                if (!identifier) {
                    return;
                }

                store.setSelectedNode(node);
                patchState(store, {
                    browsingSite: { identifier, hostname: data.hostname },
                    path: data.type === 'site' ? undefined : data.path || undefined,
                    ...resetPaging()
                });
            },

            /**
             * Moves the picker to another site.
             *
             * Lives here rather than in the folder-tree feature because it touches everything at
             * once — the tree, the folder scope, the search term and the asset list's paging — and
             * `resetPaging` is the store's, not the feature's.
             *
             * The folder term is cleared on the way: a term is only meaningful against the site it
             * was typed for. Carrying it over would leave the editor reading site A's results under
             * a selector that says site B.
             */
            setBrowsingSite: (site: DotAssetPickerSite): void => {
                if (site.identifier === store.browsingSite()?.identifier) {
                    return;
                }

                patchState(store, {
                    browsingSite: site,
                    path: undefined,
                    selectedNode: null,
                    folderSearch: '',
                    searchResults: null,
                    searchStatus: ComponentStatus.INIT,
                    searchHasMore: false,
                    ...resetPaging()
                });

                store.loadFolders();
            },

            /**
             * Scopes the list to a folder picked out of the flat search results.
             *
             * Deliberately **not** `selectNode`: that one resolves the site by walking up to the
             * tree root, and a search result has no parent in the tree to walk. The result already
             * belongs to the browsed site, so the site does not change — only the folder does.
             *
             * The term and the results are left alone. Keeping the list up is the point: the editor
             * can try the next match without retyping.
             */
            selectSearchResult: (node: TreeNodeItem): void => {
                const data = node.data;

                if (!data || data.type === LOAD_MORE_NODE_TYPE) {
                    return;
                }

                store.setSelectedNode(node);
                patchState(store, {
                    path: data.path || undefined,
                    ...resetPaging()
                });
            },

            patchFilters: (filters: DotAssetPickerFilters): void => {
                patchState(store, {
                    filters: { ...store.filters(), ...filters },
                    ...resetPaging()
                });
            },

            removeFilter: (filter: string): void => {
                const filters = { ...store.filters() };

                if (!(filter in filters)) {
                    return;
                }

                delete filters[filter];
                patchState(store, { filters, ...resetPaging() });
            },

            /**
             * One filter's value, or `undefined` when it is not set.
             *
             * The read half of what a shared filter chip needs. `undefined` and `[]` are different
             * states and both are load-bearing: the first is "no filter", the second is "filtered
             * to nothing selected".
             */
            getFilterValue: (filter: string): string | string[] | undefined =>
                store.filters()[filter],

            /**
             * Returns the filters to what the picker opened with — the caller's seeded locale and
             * base types — not to an empty set.
             *
             * It used to clear to `{}`, which dropped the seeds and stranded an Image field's editor
             * in an unfiltered, unlocalized library. `buildPickerFilterDefaults` is shared with
             * `initPicker` so the two paths cannot disagree about what "default" means.
             *
             * Two things deliberately survive a clear: the host's mimetype restriction, which is
             * not a filter but part of what the picker *is*, and the browsed folder, which is not a
             * filter either — an editor who reached the site root by searching stays there rather
             * than being moved somewhere they did not ask for.
             */
            clearFilters: (): void => {
                patchState(store, {
                    filters: buildPickerFilterDefaults(store.config()),
                    ...resetPaging()
                });
            },

            /**
             * Free-text search. Resets the folder scope: results are site-wide, so leaving the tree
             * pinned to a folder would contradict what the list shows.
             */
            setSearch: (title: string): void => {
                const filters = { ...store.filters() };

                if (title) {
                    filters.title = title;
                } else {
                    delete filters.title;
                }

                patchState(store, { filters, path: undefined, ...resetPaging() });

                // The tree highlight has to follow the scope, not just the list: it is also what
                // `$targetFolder` reads to decide where an upload lands.
                store.selectRootNode();
            },

            /**
             * Flips the full-screen flag. The shell reacts to it and resizes the host `.p-dialog`;
             * the store deliberately knows nothing about the DOM.
             */
            toggleFullscreen: (): void => {
                patchState(store, { isFullscreen: !store.isFullscreen() });
            }
        };
    }),
    withHooks((store) => ({
        onInit() {
            // Passing the signal (not its value) makes the rxMethod re-run on every request change
            // and cancel the in-flight call. This is the only reactive wiring in the store — there
            // is no route subscription and no popstate handler.
            store.loadItems(store.$request);
        }
    }))
);
