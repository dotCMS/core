import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { computed, inject, untracked } from '@angular/core';

import { catchError, filter, switchMap, tap } from 'rxjs/operators';

import { DotContentDriveService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotContentDriveSearchRequest,
    DotContentDriveSearchResponse
} from '@dotcms/dotcms-models';

import { SYSTEM_HOST_ID } from '../../../dot-folder-tree/constants';
import {
    DEFAULT_ASSET_PICKER_PAGE,
    DEFAULT_ASSET_PICKER_PAGINATION,
    DEFAULT_ASSET_PICKER_SORT
} from '../constants';
import {
    DotAssetPickerBrowseState,
    DotAssetPickerPagination,
    DotAssetPickerSelectionState,
    DotAssetPickerSort,
    DotAssetPickerState
} from '../models';

const initialState: DotAssetPickerBrowseState = {
    items: [],
    status: ComponentStatus.INIT,
    pagination: DEFAULT_ASSET_PICKER_PAGINATION,
    sort: DEFAULT_ASSET_PICKER_SORT,
    pages: [DEFAULT_ASSET_PICKER_PAGE],
    totalItems: 0
};

/**
 * Asset list: the search request, the results, and paging/sorting over them.
 *
 * Mirrors the shape of Content Drive's request builder, minus everything the picker has no use for
 * (workflow filters, user-searchable fields) and plus the two things it needs: a silent mimetype
 * restriction and `showFolders: false` as an invariant.
 */
export function withAssetBrowse() {
    return signalStoreFeature(
        // Depends on the selection slot so a new result can drop a selection that is about to
        // scroll out of existence — see `loadItems`.
        { state: type<DotAssetPickerState & DotAssetPickerSelectionState>() },
        withState<DotAssetPickerBrowseState>(initialState),
        withComputed(({ config, browsingSite, path, filters, pagination, sort, pages, items }) => ({
            /**
             * Row count the paginator divides into pages.
             *
             * NOT `totalItems`: the Drive API is cursor-based and never returns a grand total —
             * `contentCount` is the size of the page it just returned (`BrowserAPIImpl#getContents`).
             * Handing that to PrimeNG makes every full page look like the last one, so it computes a
             * single page and renders "next" disabled — the paging chain below never even runs.
             *
             * So while the bookmark for the page on screen reports more content, claim one page
             * beyond to keep the arrow live; once it doesn't, the page is the last one and the exact
             * total is knowable. Mirrors Content Drive's `$totalItems`.
             */
            $totalRecords: computed(() => {
                const { page, limit } = pagination();
                // `pages[N]` is the bookmark written AFTER loading page N, so its `hasMoreContent`
                // answers "is there anything past what is on screen".
                const bookmark = pages()[page];

                return bookmark?.hasMoreContent
                    ? limit * (page + 1)
                    : limit * (page - 1) + items().length;
            }),

            /**
             * `false` until the host configures a real site. Guards the search so the store can be
             * constructed by a dialog host before that host knows what to browse — Content Drive
             * never needs this because its URL-driven init runs at construction time.
             *
             * Reads `browsingSite`, not `config.site`: the user can move the picker to another site
             * from the sidebar, and it is the site actually being browsed that has to be addressable.
             */
            $isBrowsable: computed(
                () => !!browsingSite() && browsingSite()?.identifier !== SYSTEM_HOST_ID
            ),

            $request: computed<DotContentDriveSearchRequest>(
                () => {
                    const pickerConfig = config();
                    const site = browsingSite();
                    const currentFilters = filters();
                    const currentPagination = pagination();

                    // `allowedBaseTypes` is a boundary, not a default the editor can clear: with
                    // no chip selected the request still has to carry it, or a File field lists
                    // Pages and every other content type instead of just files. A narrower
                    // selection wins over it.
                    const baseTypes = currentFilters?.baseType?.length
                        ? currentFilters.baseType
                        : pickerConfig?.allowedBaseTypes;

                    // Read UNTRACKED: the response writes the next page's cursor back into `pages`,
                    // so tracking it here would recompute the request, refire the search, and loop.
                    const cursor = untracked(
                        () => pages()[currentPagination.page - 1]?.contentCursor ?? 0
                    );

                    return {
                        assetPath: `//${site?.hostname}${path() || '/'}`,
                        includeSystemHost: true,
                        filters: { text: currentFilters?.title || '', filterFolders: true },
                        language: currentFilters?.languageId,
                        contentTypes: currentFilters?.contentType,
                        // Base-type NAMES. Content Drive maps these to numbers so they survive the
                        // URL; the picker has no URL, so it skips that round-trip entirely.
                        baseTypes: baseTypes?.length ? baseTypes : undefined,
                        // Silent restriction from the host — never part of `filters`, so no chip.
                        mimeTypes: pickerConfig?.mimeTypes?.length
                            ? pickerConfig.mimeTypes
                            : undefined,
                        contentCursor: cursor,
                        // Always 0: with `showFolders: false` the folder cursor never advances.
                        folderCursor: 0,
                        maxResults: currentPagination.limit,
                        sortBy: `${sort().field}:${sort().order}`,
                        archived: false,
                        // Invariant, not a default: the picker's list is for assets. Folders are
                        // navigated through the sidebar tree.
                        showFolders: false
                    };
                },
                // Structural dedupe so a no-op recompute doesn't refire the search.
                { equal: (a, b) => JSON.stringify(a) === JSON.stringify(b) }
            )
        })),
        withMethods(
            (
                store,
                contentDriveService = inject(DotContentDriveService),
                httpErrorManager = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Runs whenever the request changes. Feed it the `$request` signal (not a value) so
                 * it re-runs on every change and cancels the in-flight call — a stale response can
                 * never overwrite a newer one.
                 */
                loadItems: rxMethod<DotContentDriveSearchRequest>(
                    pipe(
                        filter(() => store.$isBrowsable()),
                        tap(() =>
                            patchState(store, {
                                status: ComponentStatus.LOADING,
                                // The list silently forgets its own PrimeNG selection whenever
                                // `items` change (DotFolderListViewComponent's $cleanSelectedItems
                                // effect assigns `selectedItems` without emitting `selectionChange`),
                                // so `onSelect` never fires and the store has to clear itself.
                                // Otherwise Confirm stays enabled for a row that is no longer in the
                                // list and returns that stale asset.
                                selectedAsset: null
                            })
                        ),
                        switchMap((request) =>
                            contentDriveService.search(request).pipe(
                                tap((response: DotContentDriveSearchResponse) => {
                                    const page = store.pagination().page;
                                    const pages = [...store.pages()];

                                    // Bookmark where the NEXT page starts, so paging forward can
                                    // resume from a cursor instead of replaying from the top.
                                    pages[page] = {
                                        contentCursor: response.nextContentCursor,
                                        hasMoreContent: response.hasMoreContent
                                    };

                                    patchState(store, {
                                        items: response.list,
                                        totalItems: response.contentCount,
                                        status: ComponentStatus.LOADED,
                                        pages
                                    });
                                }),
                                catchError((error) => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        items: []
                                    });
                                    httpErrorManager.handle(error);

                                    // EMPTY keeps the rxMethod alive: the next request retries.
                                    return EMPTY;
                                })
                            )
                        )
                    )
                ),

                setPagination: (pagination: DotAssetPickerPagination) => {
                    // Changing the page size invalidates every cursor bookmark.
                    const limitChanged = pagination.limit !== store.pagination().limit;

                    patchState(store, {
                        pagination: limitChanged ? { ...pagination, page: 1 } : pagination,
                        ...(limitChanged ? { pages: [DEFAULT_ASSET_PICKER_PAGE] } : {})
                    });
                },

                setSort: (sort: DotAssetPickerSort) => {
                    // A different order means different results in a different place: cursors from
                    // the previous ordering are meaningless.
                    patchState(store, {
                        sort,
                        pagination: { ...store.pagination(), page: 1 },
                        pages: [DEFAULT_ASSET_PICKER_PAGE]
                    });
                }
            })
        )
    );
}
