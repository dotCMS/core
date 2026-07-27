import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, effect, EffectRef, inject, untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { catchError, take } from 'rxjs/operators';

import { DotContentDriveService } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotContentDriveItem,
    DotContentDriveSearchRequest
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withContextMenu } from './features/context-menu/withContextMenu';
import { withDialog } from './features/dialog/withDialog';
import { withDragging } from './features/dragging/withDragging';
import { withSidebar } from './features/sidebar/withSidebar';

import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    MAP_NUMBERS_TO_BASE_TYPES,
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
    parseWorkflowFilter
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
    pages: [DEFAULT_PAGE],
    userSearchableFields: [],
    userSearchableActive: [],
    userSearchableFieldsLoaded: false
};

export const DotContentDriveStore = signalStore(
    withState<DotContentDriveState>(initialState),
    withComputed(
        ({ path, filters, currentSite, pagination, sort, pages, userSearchableFields }) => {
            return {
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
                            includeSystemHost: true,
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
                            contentCursor: page.contentCursor ?? 0,
                            folderCursor: page.folderCursor ?? 0,
                            maxResults: paginationSignal?.limit,
                            sortBy: sort()?.field + ':' + sort()?.order,
                            archived: false,
                            showFolders:
                                page.hasMoreFolders &&
                                !filters()?.baseType?.length &&
                                !filters()?.contentType?.length &&
                                !filters()?.languageId?.length &&
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
        return {
            initContentDrive({ currentSite, path, filters, isTreeExpanded }: DotContentDriveInit) {
                patchState(store, {
                    currentSite: currentSite ?? SYSTEM_HOST,
                    path,
                    filters,
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
                    filters: {},
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
                        filters: restFilters,
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
                    pagination: { ...store.pagination(), offset: 0, page: 1 },
                    pages: [DEFAULT_PAGE]
                });
            },
            setSelectedItems(items: DotContentDriveItem[]) {
                patchState(store, { selectedItems: items });
            },
            loadItems() {
                const request = store.$request();
                const currentSite = store.currentSite();
                patchState(store, { status: DotContentDriveStatus.LOADING, selectedItems: [] });

                // Avoid fetching content for SYSTEM_HOST sites
                if (currentSite?.identifier == SYSTEM_HOST.identifier) {
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
        let initEffect: EffectRef;
        let searchEffect: EffectRef;

        return {
            onInit() {
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
            }
        };
    }),
    withContextMenu(),
    withDialog(),
    withSidebar(),
    withDragging()
);
