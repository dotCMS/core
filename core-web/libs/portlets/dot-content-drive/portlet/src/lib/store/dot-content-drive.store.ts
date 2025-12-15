import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, effect, EffectRef, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { catchError, take } from 'rxjs/operators';

import { DotContentDriveService } from '@dotcms/data-access';
import { DotContentDriveItem, DotContentDriveSearchRequest } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withContextMenu } from './features/context-menu/withContextMenu';
import { withDialog } from './features/dialog/withDialog';
import { withDragging } from './features/dragging/withDragging';
import { withSidebar } from './features/sidebar/withSidebar';

import {
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    MAP_NUMBERS_TO_BASE_TYPES,
    SYSTEM_HOST
} from '../shared/constants';
import {
    DotContentDriveFilters,
    DotContentDriveInit,
    DotContentDrivePagination,
    DotContentDriveSort,
    DotContentDriveState,
    DotContentDriveStatus
} from '../shared/models';
import { buildContentDriveQuery, decodeFilters } from '../utils/functions';

const initialState: DotContentDriveState = {
    currentSite: undefined, // So we have the actual site selected on start
    path: DEFAULT_PATH,
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: DEFAULT_PAGINATION,
    sort: DEFAULT_SORT,
    isTreeExpanded: DEFAULT_TREE_EXPANDED
};

export const DotContentDriveStore = signalStore(
    withState<DotContentDriveState>(initialState),
    withComputed(({ path, filters, currentSite, pagination, sort }) => {
        return {
            $request: computed<DotContentDriveSearchRequest>(() => ({
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
                offset: pagination()?.offset,
                maxResults: pagination()?.limit,
                sortBy: sort()?.field + ':' + sort()?.order,
                archived: false,
                showFolders:
                    !filters()?.baseType?.length &&
                    !filters()?.contentType?.length &&
                    !filters()?.languageId?.length
            })),
            // We will need this for the global select all in the future, so I'll leave it here for now
            // https://github.com/dotCMS/core/issues/33338
            $query: computed<string>(() => {
                return buildContentDriveQuery({
                    path: path(),
                    currentSite: currentSite() ?? SYSTEM_HOST,
                    filters: filters()
                });
            })
        };
    }),
    withMethods((store) => {
        const dotContentDriveService = inject(DotContentDriveService);
        return {
            initContentDrive({ currentSite, path, filters, isTreeExpanded }: DotContentDriveInit) {
                patchState(store, {
                    currentSite: currentSite ?? SYSTEM_HOST,
                    path,
                    filters,
                    status: DotContentDriveStatus.LOADING,
                    isTreeExpanded
                });
            },
            setItems(items: DotContentDriveItem[], totalItems: number) {
                patchState(store, { items, status: DotContentDriveStatus.LOADED, totalItems });
            },
            setStatus(status: DotContentDriveStatus) {
                patchState(store, { status });
            },
            setGlobalSearch(searchValue: string) {
                patchState(store, {
                    filters: searchValue
                        ? {
                              title: searchValue
                          }
                        : {},
                    pagination: {
                        ...store.pagination(),
                        offset: 0
                    },
                    path: DEFAULT_PATH
                });
            },
            patchFilters(filters: DotContentDriveFilters) {
                patchState(store, {
                    filters: { ...store.filters(), ...filters },
                    pagination: {
                        ...store.pagination(),
                        offset: 0
                    }
                });
            },
            removeFilter(filter: string) {
                const { [filter]: removedFilter, ...restFilters } = store.filters();
                if (removedFilter) {
                    patchState(store, {
                        filters: restFilters,
                        pagination: { ...store.pagination(), offset: 0 }
                    });
                }
            },
            setPath(path: string) {
                // Only reset the title if the path is changed and its not the default path
                const title = path ? undefined : store.filters().title;

                patchState(store, {
                    path,
                    pagination: { ...store.pagination(), offset: 0 },
                    filters: {
                        ...store.filters(),
                        title
                    }
                });
            },
            setPagination(pagination: DotContentDrivePagination) {
                patchState(store, { pagination });
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
            setSelectedItems(items: DotContentDriveItem[]) {
                patchState(store, { selectedItems: items });
            },
            loadItems() {
                const request = store.$request();
                const currentSite = store.currentSite();
                patchState(store, { status: DotContentDriveStatus.LOADING, selectedItems: [] });

                // Avoid fetching content for SYSTEM_HOST sites
                if (currentSite?.identifier === SYSTEM_HOST.identifier) {
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
                        patchState(store, {
                            items: response.list,
                            totalItems: response.contentTotalCount,
                            status: DotContentDriveStatus.LOADED
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
