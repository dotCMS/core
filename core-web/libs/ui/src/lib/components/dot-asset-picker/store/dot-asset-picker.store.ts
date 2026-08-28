import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';

import { LOAD_MORE_NODE_TYPE, TreeNodeItem } from '@dotcms/dotcms-models';

import {
    DEFAULT_ASSET_PICKER_PAGE,
    DEFAULT_ASSET_PICKER_PAGINATION,
    DEFAULT_ASSET_PICKER_SORT
} from './constants';
import { withAssetBrowse } from './features/with-asset-browse.feature';
import { withAssetFolderTree } from './features/with-asset-folder-tree.feature';
import { withAssetSelection } from './features/with-asset-selection.feature';
import { DotAssetPickerConfig, DotAssetPickerFilters, DotAssetPickerState } from './models';

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
    withMethods((store) => {
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
                patchState(store, {
                    config,
                    // Starting point only — the sidebar can move the picker to another site. Opens
                    // on the remembered site when there is one, otherwise on the editor's own.
                    browsingSite: config.browseSite ?? {
                        identifier: config.site.identifier,
                        hostname: config.site.hostname
                    },
                    path: config.path,
                    filters: {
                        ...(config.languageId ? { languageId: [config.languageId] } : {}),
                        ...(config.baseTypes?.length ? { baseType: config.baseTypes } : {})
                    },
                    pagination: DEFAULT_ASSET_PICKER_PAGINATION,
                    // Seeded, not pinned: `sortByDesc` sets the direction the picker opens with,
                    // and the user can still re-sort from the table header afterwards.
                    sort: config.browse
                        ? {
                              ...DEFAULT_ASSET_PICKER_SORT,
                              order: config.browse.sortByDesc === false ? 'asc' : 'desc'
                          }
                        : DEFAULT_ASSET_PICKER_SORT,
                    pages: [DEFAULT_ASSET_PICKER_PAGE],
                    items: [],
                    selectedAsset: null
                });

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

            patchFilters: (filters: DotAssetPickerFilters): void => {
                patchState(store, {
                    filters: { ...store.filters(), ...filters },
                    ...resetPaging()
                });
            },

            removeFilter: (filter: keyof DotAssetPickerFilters): void => {
                const filters = { ...store.filters() };

                if (!(filter in filters)) {
                    return;
                }

                delete filters[filter];
                patchState(store, { filters, ...resetPaging() });
            },

            /**
             * Clears every filter the editor can see. The host's mimetype restriction survives —
             * it is not a filter, it is part of what the picker is.
             */
            clearFilters: (): void => {
                patchState(store, { filters: {}, ...resetPaging() });
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
