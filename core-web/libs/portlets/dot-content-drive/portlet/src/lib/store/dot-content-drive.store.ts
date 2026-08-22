import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, SubscriptionLike } from 'rxjs';

import { Location } from '@angular/common';
import { computed, effect, EffectRef, inject, untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { catchError, take } from 'rxjs/operators';

import {
    DotContentDriveService,
    DotCurrentUserService,
    DotLanguagesService
} from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotContentDriveItem,
    DotContentDriveSearchRequest,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { GlobalStore, withFlags } from '@dotcms/store';

import { withActionExecution } from './features/action-execution/withActionExecution';
import { withContextMenu } from './features/context-menu/withContextMenu';
import { withDialog } from './features/dialog/withDialog';
import { withDragging } from './features/dragging/withDragging';
import { withPushPublishEnvironments } from './features/push-publish-environments/withPushPublishEnvironments';
import { withSidebar } from './features/sidebar/withSidebar';

import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    MAP_NUMBERS_TO_BASE_TYPES,
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY,
    SYSTEM_HOST,
    USER_SEARCHABLE_PREFIX
} from '../shared/constants';
import {
    DotContentDriveFilters,
    DotContentDriveInit,
    DotContentDrivePagination,
    DotContentDriveSort,
    DotContentDriveState,
    DotContentDriveStatus
} from '../shared/models';
import {
    buildUserSearchablePayload,
    decodeFilters,
    getUserSearchableActive,
    parseWorkflowFilter,
    sortedEncodedFilters,
    withFilterDefaults
} from '../utils/functions';

const initialState: DotContentDriveState = {
    currentSite: undefined, // So we have the actual site selected on start
    path: DEFAULT_PATH,
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    pagination: DEFAULT_PAGINATION,
    sort: DEFAULT_SORT,
    isTreeExpanded: DEFAULT_TREE_EXPANDED,
    isTreeForceCollapsed: false,
    pages: [DEFAULT_PAGE],
    userSearchableFields: [],
    userSearchableActive: [],
    userSearchableFieldsLoaded: false,
    showInListFields: [],
    languages: [],
    defaultLanguageId: undefined,
    defaultLanguageLoaded: false,
    // Pessimistic default: until `getCurrentUser` answers, the user is treated as a non-admin. See
    // `DotContentDriveState.currentUserIsAdmin` for why over-warning is the right way to fail here.
    currentUserIsAdmin: false
};

export const DotContentDriveStore = signalStore(
    withState<DotContentDriveState>(initialState),
    // Side-panel feature flag, fetched once on init and exposed as `flags()`. `as const` narrows the
    // typing to exactly this flag. Consumed by DotContentDriveNavigationService to decide side panel
    // vs full-screen editor.
    withFlags([FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL] as const),
    withComputed(
        ({
            path,
            filters,
            currentSite,
            pagination,
            sort,
            pages,
            userSearchableFields,
            isTreeExpanded,
            isTreeForceCollapsed
        }) => {
            return {
                /**
                 * The tree's VISUAL expanded state — the user's real preference (`isTreeExpanded`,
                 * persisted/shareable via the URL) minus any transient collapse the side panel is
                 * forcing on a narrow viewport (`isTreeForceCollapsed`, never persisted). Kept
                 * separate so the panel's temporary collapse can never leak into the shareable
                 * state — see `setTreeForceCollapsed`.
                 */
                isTreeVisuallyExpanded: computed(() => isTreeExpanded() && !isTreeForceCollapsed()),
                $request: computed<DotContentDriveSearchRequest>(
                    () => {
                        const paginationSignal = pagination();
                        const page = untracked(() => pages()[paginationSignal?.page - 1]);
                        const userSearchable = buildUserSearchablePayload(
                            filters(),
                            userSearchableFields()
                        );

                        return {
                            assetPath: `//${currentSite()?.hostname}${path() || '/'}`,
                            // Off only when explicitly turned off. The key is seeded on every path
                            // that builds filters (see `withFilterDefaults`), so a missing one means
                            // state that predates the seeding, not a deliberate opt-out.
                            includeSystemHost:
                                filters()?.[SHARED_ASSETS_FILTER_KEY] !==
                                SHARED_ASSETS_DISABLED_VALUE,
                            filters: {
                                text: filters()?.title || '',
                                filterFolders: true
                            },
                            language: filters()?.languageId,
                            contentTypes: filters()?.contentType,
                            baseTypes: filters()?.baseType?.map(
                                (baseType) => MAP_NUMBERS_TO_BASE_TYPES[Number(baseType)]
                            ),
                            workflow: filters()?.workflow?.length
                                ? parseWorkflowFilter(filters()?.workflow)
                                : undefined,
                            userSearchable,
                            // NOTE: `languageId` is deliberately absent from `showFolders` below.
                            // Folders have no language, so a locale filter — which selects a
                            // *version* of content — must not remove the structure being navigated.
                            // It also could not stay there once a default language is always
                            // selected: every folder in the drive would disappear.
                            contentCursor: page.contentCursor ?? 0,
                            folderCursor: page.folderCursor ?? 0,
                            maxResults: paginationSignal?.limit,
                            sortBy: sort()?.field + ':' + sort()?.order,
                            archived: false,
                            showFolders:
                                page.hasMoreFolders &&
                                !filters()?.baseType?.length &&
                                !filters()?.contentType?.length &&
                                !filters()?.workflow?.length &&
                                // A field-based filter narrows to content, so hide folders too —
                                // consistent with the other filters above.
                                !userSearchable
                        };
                    },
                    {
                        // Dedupe structurally-identical requests so the search effect doesn't re-fire on
                        // no-op recomputes — e.g. selecting a content type loads its fields
                        // (setUserSearchableFields), which changes `userSearchableFields` but not the
                        // payload when no `us.*` value is set. A real payload change still differs here.
                        equal: (a, b) => JSON.stringify(a) === JSON.stringify(b)
                    }
                )
            };
        }
    ),
    withMethods((store) => {
        const dotContentDriveService = inject(DotContentDriveService);
        const dotCurrentUserService = inject(DotCurrentUserService);
        const dotLanguagesService = inject(DotLanguagesService);
        return {
            initContentDrive({ currentSite, path, filters, isTreeExpanded }: DotContentDriveInit) {
                patchState(store, {
                    currentSite: currentSite ?? SYSTEM_HOST,
                    path,
                    filters: withFilterDefaults(filters, store.defaultLanguageId()),
                    status: DotContentDriveStatus.LOADING,
                    isTreeExpanded,
                    pagination: {
                        limit: DEFAULT_PAGINATION.limit,
                        page: 1,
                        offset: 0
                    },
                    pages: [DEFAULT_PAGE],
                    // Which field-filter chips to show — parsed from the `us.*` value keys at the
                    // decode layer (getUserSearchableActive), keeping this method free of that logic.
                    userSearchableActive: getUserSearchableActive(filters),
                    // Field metadata for the restored type isn't loaded yet; loadItems waits on this
                    // so a restored `us.*` filter isn't dropped from the first search request.
                    userSearchableFields: [],
                    userSearchableFieldsLoaded: false
                });
            },
            setItems(items: DotContentDriveItem[]) {
                patchState(store, { items, status: DotContentDriveStatus.LOADED });
            },
            setStatus(status: DotContentDriveStatus) {
                patchState(store, { status });
            },
            setGlobalSearch(searchValue: string) {
                const filters = { ...store.filters() };
                if (searchValue) {
                    filters.title = searchValue;
                } else {
                    delete filters.title;
                }

                patchState(store, {
                    filters,
                    pagination: {
                        ...store.pagination(),
                        offset: 0,
                        page: 1
                    },
                    path: DEFAULT_PATH
                });
            },
            clearFilters() {
                patchState(store, {
                    // Clearing every filter still leaves the defaults applied: an empty language
                    // filter is not a neutral state, and shared assets stay on (see
                    // `withFilterDefaults`).
                    filters: withFilterDefaults({}, store.defaultLanguageId()),
                    pagination: { ...store.pagination(), offset: 0, page: 1 },
                    pages: [DEFAULT_PAGE]
                });
            },
            patchFilters(filters: DotContentDriveFilters) {
                patchState(store, {
                    filters: { ...store.filters(), ...filters },
                    pagination: {
                        ...store.pagination(),
                        offset: 0,
                        page: 1
                    },
                    pages: [DEFAULT_PAGE]
                });
            },
            removeFilter(filter: string) {
                const { [filter]: removedFilter, ...restFilters } = store.filters();
                if (removedFilter) {
                    patchState(store, {
                        // Re-seeded so dropping a defaulted key — `languageId`, or the shared-assets
                        // toggle — can never leave it unset, whichever caller does it.
                        filters: withFilterDefaults(restFilters, store.defaultLanguageId()),
                        pagination: { ...store.pagination(), page: 1, offset: 0 },
                        pages: [DEFAULT_PAGE]
                    });
                }
            },
            setPath(path: string) {
                patchState(store, {
                    path,
                    pagination: { ...store.pagination(), page: 1, offset: 0 },
                    pages: [DEFAULT_PAGE]
                });
            },
            setPagination(pagination: DotContentDrivePagination) {
                const currentLimit = store.pagination().limit;
                const limit = pagination.limit;
                patchState(store, () => {
                    if (currentLimit == limit) {
                        return {
                            pagination: {
                                ...pagination
                            }
                        };
                    }

                    return {
                        pagination: {
                            ...pagination,
                            page: 1,
                            offset: 0
                        },
                        pages: [DEFAULT_PAGE]
                    };
                });
            },
            setSort(sort: DotContentDriveSort) {
                patchState(store, { sort });
            },
            setIsTreeExpanded(isTreeExpanded: boolean) {
                patchState(store, { isTreeExpanded });
            },
            /**
             * Sets the side panel's transient tree-collapse override (see `isTreeVisuallyExpanded`).
             * Never touches `isTreeExpanded` — the real, shareable preference — so a panel-forced
             * collapse can never be persisted to the URL or survive a refresh as if it were the
             * user's own choice.
             */
            setTreeForceCollapsed(isTreeForceCollapsed: boolean) {
                patchState(store, { isTreeForceCollapsed });
            },
            getFilterValue(filter: string) {
                return store.filters()[filter];
            },
            /**
             * Caches the eligible searchable fields of the active single content type. Consumed by
             * the field-filter chips (to render controls) and by `$request` (to reshape values).
             */
            setUserSearchableFields(fields: DotCMSContentTypeField[]) {
                patchState(store, {
                    userSearchableFields: fields,
                    userSearchableFieldsLoaded: true
                });
            },
            /**
             * Sets the "Show In List" fields of the active content type (empty when 0 or >1 are
             * selected). Consumed by the results table to render extra columns after the Type column.
             */
            setShowInListFields(fields: DotCMSContentTypeField[]) {
                patchState(store, { showInListFields: fields });
            },
            /**
             * Shows a field-filter chip by adding it to the active list only — NOT to `filters`.
             * This keeps the search request unchanged (no reload/flicker); a `us.*` entry lands in
             * `filters` only once the chip has a value.
             */
            addUserSearchableField(variable: string) {
                if (store.userSearchableActive().includes(variable)) {
                    return;
                }

                patchState(store, {
                    userSearchableActive: [...store.userSearchableActive(), variable]
                });
            },
            /**
             * Drops every `us.*` field filter, the active chip list, and the cached field metadata.
             * Called when the active content type changes (removed / another added / switched to a
             * different single type). The reactive URL write-back removes these entries from the URL.
             */
            clearUserSearchableFilters() {
                const restFilters = Object.fromEntries(
                    Object.entries(store.filters()).filter(
                        ([key]) => !key.startsWith(USER_SEARCHABLE_PREFIX)
                    )
                );

                patchState(store, {
                    filters: restFilters,
                    userSearchableFields: [],
                    userSearchableActive: [],
                    userSearchableFieldsLoaded: false,
                    showInListFields: [],
                    pagination: { ...store.pagination(), offset: 0, page: 1 },
                    pages: [DEFAULT_PAGE]
                });
            },
            setSelectedItems(items: DotContentDriveItem[]) {
                patchState(store, { selectedItems: items });
            },
            /**
             * Resolves the logged-in user's CMS Administrator role, once per portlet load.
             *
             * A failure leaves the flag at its `false` default rather than surfacing an error: the
             * role only softens a warning, so a portlet that cannot answer "is this an admin?" should
             * still work — it just keeps warning, which is what it did before the flag existed.
             */
            loadCurrentUserIsAdmin() {
                dotCurrentUserService
                    .getCurrentUser()
                    .pipe(
                        take(1),
                        catchError(() => EMPTY)
                    )
                    // Read defensively rather than destructured: `catchError` is upstream of the
                    // subscriber, so it covers a failed request but not a successful one with no
                    // body (a 204, a proxy that strips it, a gateway answering without JSON). The
                    // documented default — false — should hold for both.
                    .subscribe((user) => patchState(store, { currentUserIsAdmin: !!user?.admin }));
            },
            /**
             * Resolves the environment's languages, once per portlet load, and seeds the default one
             * into the `languageId` filter when nothing is selected.
             *
             * The default is the language flagged `defaultLanguage` — not id 1, and not the first
             * entry returned, which are different languages on plenty of environments. `/api/v2/languages`
             * is the only source that carries the flag: the app-configuration payload behind
             * `GlobalStore.systemLanguages` omits it.
             *
             * The response may land either side of the init effect, so the filters are re-seeded here
             * as well as in `initContentDrive`. A failure still marks the load settled so the portlet
             * searches unseeded — exactly its behaviour before the seed existed — instead of waiting
             * forever in `LOADING`.
             */
            loadDefaultLanguage() {
                dotLanguagesService
                    .get()
                    .pipe(
                        take(1),
                        catchError(() => {
                            patchState(store, { defaultLanguageLoaded: true });
                            return EMPTY;
                        })
                    )
                    .subscribe((response) => {
                        // Coalesce with `??` rather than a default parameter: a default only covers
                        // `undefined`, so a `null` body would reach `.find` and throw HERE, inside the
                        // subscribe body and therefore past the pipe's `catchError`. That would leave
                        // `defaultLanguageLoaded` false forever, and because `loadItems` patches
                        // `LOADING` before its gate, the portlet would sit in LOADING for good.
                        const languages = response ?? [];
                        const defaultLanguageId = languages.find(
                            (language) => language.defaultLanguage
                        )?.id;

                        patchState(store, {
                            languages,
                            defaultLanguageId,
                            defaultLanguageLoaded: true,
                            filters: withFilterDefaults(store.filters(), defaultLanguageId)
                        });
                    });
            },
            loadItems() {
                const request = store.$request();
                const currentSite = store.currentSite();
                patchState(store, { status: DotContentDriveStatus.LOADING, selectedItems: [] });

                // Avoid fetching content for SYSTEM_HOST sites
                if (currentSite?.identifier == SYSTEM_HOST.identifier) {
                    return;
                }

                // Hold the first search until the default language has been resolved. Read TRACKED
                // (like `userSearchableFieldsLoaded` below) so the effect re-runs the moment it
                // settles. Without this the portlet searches once with no language — briefly showing
                // every language version of every row — and again with the seeded default.
                if (!store.defaultLanguageLoaded()) {
                    return;
                }

                // Hold the search while a restored `us.*` filter has no field metadata yet: the
                // payload builder can only shape values it has a field for, so searching now would
                // drop them and briefly show unfiltered results.
                //
                // `userSearchableFieldsLoaded` is read TRACKED so the effect re-runs the moment
                // field metadata arrives — even when the resulting `$request` is structurally
                // identical and its dedupe guard would otherwise suppress the re-run (e.g. a
                // restored `us.*` key for an ineligible/removed field yields no payload either
                // way, which would otherwise leave the portlet stuck in LOADING). It flips
                // false→true exactly once per content-type field load and is never touched by
                // adding a chip. `userSearchableActive` stays untracked so adding an empty chip
                // (which changes it but not `loaded`) does not re-fire a search.
                const fieldsLoaded = store.userSearchableFieldsLoaded();
                const hasActiveFields = untracked(() => store.userSearchableActive().length > 0);
                if (hasActiveFields && !fieldsLoaded) {
                    return;
                }

                // Since we are using scored search for the title we need to sort by score desc
                dotContentDriveService
                    .search(request)
                    .pipe(
                        take(1),
                        catchError(() => {
                            patchState(store, { status: DotContentDriveStatus.ERROR });
                            return EMPTY;
                        })
                    )
                    .subscribe((response) => {
                        patchState(store, (store) => {
                            const samePage = store.pages.find(
                                (page) =>
                                    page.folderCursor === response.nextFolderCursor &&
                                    page.contentCursor === response.nextContentCursor
                            );

                            if (samePage) {
                                return {
                                    // Refresh the matched page's hasMore flags from this
                                    // response (new array ref so dependent computeds
                                    // recompute). Otherwise an emptied result that lands on
                                    // DEFAULT_PAGE's cursors keeps its optimistic
                                    // hasMoreContent: true and the paginator wrongly offers a
                                    // next page when there are zero items.
                                    pages: store.pages.map((page) =>
                                        page === samePage
                                            ? {
                                                  ...page,
                                                  hasMoreContent: response.hasMoreContent,
                                                  hasMoreFolders: response.hasMoreFolders
                                              }
                                            : page
                                    ),
                                    items: response.list,
                                    status: DotContentDriveStatus.LOADED
                                };
                            }

                            return {
                                items: response.list,
                                status: DotContentDriveStatus.LOADED,
                                pages: [
                                    ...store.pages,
                                    {
                                        hasMoreContent: response.hasMoreContent,
                                        hasMoreFolders: response.hasMoreFolders,
                                        folderCursor: response.nextFolderCursor,
                                        contentCursor: response.nextContentCursor,
                                        offset: store.pagination.offset
                                    }
                                ]
                            };
                        });
                    });
            },
            reloadContentDrive() {
                this.loadItems();
            }
        };
    }),
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const globalStore = inject(GlobalStore);
        const location = inject(Location);
        let initEffect: EffectRef;
        let searchEffect: EffectRef;
        let locationSub: SubscriptionLike;

        return {
            onInit() {
                // Fired here, not from an effect: the role is fixed for the session, so one request
                // per portlet load is enough and re-running it on every state change would be pure
                // noise. Nothing waits on it — consumers read the flag's default until it lands.
                store.loadCurrentUserIsAdmin();

                // Same rationale as above: the environment's languages don't change within a
                // session, so one request per portlet load is enough. Unlike the admin role, the
                // first search DOES wait on this — see the gate in `loadItems`.
                store.loadDefaultLanguage();

                initEffect = effect(() => {
                    const queryParams = route.snapshot.queryParams;
                    const currentSite = globalStore.siteDetails();
                    const path = queryParams['path'] || DEFAULT_PATH;
                    const filters = decodeFilters(queryParams['filters'] || '');
                    const queryTreeExpanded =
                        queryParams['isTreeExpanded'] ?? DEFAULT_TREE_EXPANDED.toString();

                    store.initContentDrive({
                        currentSite,
                        path,
                        filters,
                        isTreeExpanded: queryTreeExpanded == 'true'
                    });
                });

                /**
                 * Browser Back/Forward re-hydration. The browsing params (path/filters/tree) are
                 * written to the URL via `Location.go` (bypassing the router, so no content reload
                 * fires on every filter change), and the init effect above hydrates from a one-time
                 * `route.snapshot` read. Together that means a Back/Forward changes the URL but never
                 * re-hydrates the store, leaving the list stale. `Location.subscribe` fires on
                 * popstate (not on our own `go`/`replaceState`), so re-run the same hydration from
                 * the restored URL — `initContentDrive` resets to LOADING, which the search effect
                 * turns into a fresh load.
                 */
                locationSub = location.subscribe((event) => {
                    const params = new URLSearchParams(event.url?.split('?')[1] ?? '');
                    const path = params.get('path') || DEFAULT_PATH;
                    const filtersRaw = params.get('filters') || '';
                    const isTreeExpanded =
                        (params.get('isTreeExpanded') ?? DEFAULT_TREE_EXPANDED.toString()) ===
                        'true';

                    // Seeded the same way `initContentDrive` would, so a restored URL that carries no
                    // language reads as equal to the state it produced rather than as a change. Without
                    // this the seed becomes a history trap: the write-back pushes the seeded URL, Back
                    // returns to the language-less one, the guard sees a difference, re-hydration
                    // re-seeds, and the same entry is pushed again — the user can never Back out.
                    const restoredFilters = withFilterDefaults(
                        decodeFilters(filtersRaw),
                        store.defaultLanguageId()
                    );

                    // Only re-hydrate when a browsing param actually changed. A popstate that only
                    // flips `editContent` (e.g. closing the side panel via Back) must NOT reset and
                    // reload the list — that param is owned by the shell's own popstate handler.
                    // Compared order-insensitively: `encodeFilters` follows insertion order, and the
                    // seed appends `languageId` last, so an equivalent URL can spell the keys in
                    // another order.
                    if (
                        path === store.path() &&
                        sortedEncodedFilters(restoredFilters) ===
                            sortedEncodedFilters(store.filters()) &&
                        isTreeExpanded === store.isTreeExpanded()
                    ) {
                        return;
                    }

                    store.initContentDrive({
                        currentSite: globalStore.siteDetails(),
                        path,
                        filters: restoredFilters,
                        isTreeExpanded
                    });
                });

                /**
                 * Effect that triggers a content reload when search parameters change.
                 * loadItems internally uses $searchParams signal, so it will be triggered
                 * whenever query, pagination or sort changes.
                 */
                searchEffect = effect(() => {
                    store.loadItems();
                });
            },
            onDestroy() {
                initEffect?.destroy();
                searchEffect?.destroy();
                locationSub?.unsubscribe();
            }
        };
    }),
    withContextMenu(),
    withDialog(),
    withSidebar(),
    withDragging(),
    withActionExecution(),
    withPushPublishEnvironments()
);
