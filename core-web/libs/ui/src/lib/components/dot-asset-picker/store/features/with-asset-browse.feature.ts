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

import { DotContentDriveService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotContentDriveSearchRequest,
    DotContentDriveSearchResponse
} from '@dotcms/dotcms-models';

import { buildUserSearchablePayload } from '../../../dot-filter-bar/chips/dot-field-filter/user-searchable.util';
import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '../../../dot-filter-bar/chips/dot-shared-assets-filter/constants';
import { SYSTEM_HOST_ID } from '../../../dot-folder-tree/constants';
import {
    ASSET_PICKER_ERROR_KEYS,
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
 * (workflow filters) and plus the two things it needs: a silent mimetype restriction, and folders
 * and menu links switched off unless the caller opts in through `DotAssetPickerBrowseOptions`.
 */
export function withAssetBrowse() {
    return signalStoreFeature(
        // Depends on the selection slot so a new result can drop a selection that is about to
        // scroll out of existence — see `loadItems`.
        { state: type<DotAssetPickerState & DotAssetPickerSelectionState>() },
        withState<DotAssetPickerBrowseState>(initialState),
        withComputed(
            ({
                config,
                browsingSite,
                path,
                filters,
                pagination,
                sort,
                pages,
                items,
                userSearchableFields
            }) => ({
                /**
                 * Row count the paginator divides into pages.
                 *
                 * NOT `totalItems`: the Drive API is cursor-based and never returns a grand total —
                 * `contentCount` is the size of the page it just returned (`BrowserAPIImpl#getContents`).
                 * Handing that to PrimeNG makes every full page look like the last one, so it computes a
                 * single page and renders "next" disabled — the paging chain below never even runs.
                 *
                 * So while the bookmark for the page on screen reports more of ANY stream, claim one
                 * page beyond to keep the arrow live; once none of them do, the page is the last one and
                 * the exact total is knowable. Mirrors Content Drive's `$totalItems`.
                 *
                 * All three streams matter, not just content: contentlets, folders and menu links page
                 * independently, so a page whose content ran out while folders kept going would
                 * otherwise report the rows already on screen as the grand total. PrimeNG then sees
                 * `first + rows >= totalRecords`, disables Next, and those folders become unreachable.
                 */
                $totalRecords: computed(() => {
                    const { page, limit } = pagination();
                    // `pages[N]` is the bookmark written AFTER loading page N, so its `hasMore*` flags
                    // answer "is there anything past what is on screen".
                    const bookmark = pages()[page];
                    const hasMore = bookmark
                        ? bookmark.hasMoreContent ||
                          bookmark.hasMoreFolders ||
                          bookmark.hasMoreLinks
                        : false;

                    return hasMore ? limit * (page + 1) : limit * (page - 1) + items().length;
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

                        // Read UNTRACKED: the response writes the next page's cursors back into
                        // `pages`, so tracking it here would recompute the request, refire the search,
                        // and loop.
                        const bookmark = untracked(() => pages()[currentPagination.page - 1]);

                        // What the caller opted into. Absent, this is the asset-only picker every entry
                        // point but `openBrowserModal` uses.
                        const browse = pickerConfig?.browse;

                        // Once a stream is exhausted the endpoint asks us to switch it off, so later
                        // pages stop paying for a query with nothing left to return.
                        const showFolders =
                            Boolean(browse?.showFolders) && (bookmark?.hasMoreFolders ?? true);
                        const showLinks =
                            Boolean(browse?.showLinks) && (bookmark?.hasMoreLinks ?? true);

                        return {
                            assetPath: `//${site?.hostname}${path() || '/'}`,
                            // Off only when explicitly turned off. This used to be pinned `true` with
                            // no way for the editor to change it — the chip that drives it now is the
                            // same one Content Drive uses, and reads an absent key as on.
                            includeSystemHost:
                                currentFilters?.[SHARED_ASSETS_FILTER_KEY] !==
                                SHARED_ASSETS_DISABLED_VALUE,
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
                            contentCursor: bookmark?.contentCursor ?? 0,
                            // No longer pinned. It stays 0 while folders are switched off — which is
                            // every entry point but `openBrowserModal` — but a caller that asks for
                            // folders pages through them like any other stream.
                            folderCursor: bookmark?.folderCursor ?? 0,
                            ...(browse?.showLinks
                                ? { showLinks, linkCursor: bookmark?.linkCursor ?? 0 }
                                : {}),
                            maxResults: currentPagination.limit,
                            sortBy: `${sort().field}:${sort().order}`,
                            // No `archived` key at all. Content condition has exactly one
                            // representation now — the Status chip, through `status` below — and a pin
                            // here would contradict an Archived selection. The endpoint defaults the
                            // key to false, so an unfiltered picker asks for precisely what it always
                            // did (FR-014b).
                            //
                            // Sent only when non-empty, the same discipline Content Drive applies: an
                            // absent key leaves the request byte-identical to one that never mentioned
                            // status.
                            ...(currentFilters?.status?.length
                                ? { status: currentFilters.status }
                                : {}),
                            // Field-based criteria from the "More" chips, keyed by field variable.
                            // `undefined` when nothing is set, so the key is dropped from the payload
                            // rather than sent empty.
                            userSearchable: buildUserSearchablePayload(
                                currentFilters ?? {},
                                userSearchableFields()
                            ),
                            // Version state, a separate axis from content condition (FR-014c).
                            // `live: true` means published-only. The endpoint's own default is `false`
                            // (working included), which is what every entry point but `openBrowserModal`
                            // wants, so the key is omitted unless a caller explicitly asked for
                            // live-only.
                            ...(browse?.showWorking === false ? { live: true } : {}),
                            // Default, not an invariant any more: the picker's list is for assets, and
                            // folders are navigated through the sidebar tree — unless the caller
                            // explicitly asked for them.
                            showFolders
                        };
                    },
                    // Structural dedupe so a no-op recompute doesn't refire the search.
                    { equal: (a, b) => JSON.stringify(a) === JSON.stringify(b) }
                )
            })
        ),
        withMethods((store, contentDriveService = inject(DotContentDriveService)) => ({
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
                                // The bookmark this page was requested with — its cursors are the
                                // "where we asked from" fallback below.
                                const sent = pages[page - 1];

                                // Bookmark where the NEXT page starts, so paging forward can
                                // resume from a cursor instead of replaying from the top.
                                pages[page] = {
                                    contentCursor: response.nextContentCursor,
                                    hasMoreContent: response.hasMoreContent,
                                    folderCursor: response.nextFolderCursor,
                                    hasMoreFolders: response.hasMoreFolders,
                                    // Only a `showLinks` response carries these, and unlike the
                                    // folder pair they are both optional — so a response could
                                    // report more links without saying where to resume. Falling
                                    // back to the cursor we sent keeps the stream from rewinding to
                                    // 0 and re-serving links that were already shown; once there
                                    // are no more, 0 is correct because nothing will read it again.
                                    linkCursor: response.hasMoreLinks
                                        ? (response.nextLinkCursor ?? sent?.linkCursor ?? 0)
                                        : 0,
                                    hasMoreLinks: response.hasMoreLinks ?? false
                                };

                                patchState(store, {
                                    items: response.list,
                                    totalItems: response.contentCount,
                                    status: ComponentStatus.LOADED,
                                    pages
                                });
                            }),
                            catchError(() => {
                                patchState(store, {
                                    status: ComponentStatus.ERROR,
                                    items: [],
                                    requestError: { messageKey: ASSET_PICKER_ERROR_KEYS.assets }
                                });

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
        }))
    );
}
