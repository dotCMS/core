import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, effect, EffectRef, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';
import { GlobalStore } from '@dotcms/store';

import {
    BASE_QUERY,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
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
import { decodeFilters } from '../utils/functions';

const initialState: DotContentDriveState = {
    currentSite: SYSTEM_HOST,
    path: DEFAULT_PATH,
    filters: {},
    items: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: DEFAULT_PAGINATION,
    sort: DEFAULT_SORT,
    isTreeExpanded: DEFAULT_TREE_EXPANDED
};

export const DotContentDriveStore = signalStore(
    withState<DotContentDriveState>(initialState),
    withComputed(({ path, filters, currentSite }) => {
        return {
            $query: computed(() => {
                const query = new QueryBuilder();

                const baseQuery = query.raw(BASE_QUERY);

                let modifiedQuery = baseQuery;

                const pathValue = path();
                const currentSiteValue = currentSite();
                const filtersValue = filters();
                const filtersEntries = Object.entries(filtersValue ?? {});

                modifiedQuery = modifiedQuery
                    .field('parentPath')
                    .equals(pathValue ?? DEFAULT_PATH) // Add the path to the query, the default is "/"
                    .field('conhost')
                    .equals(currentSiteValue?.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier);

                filtersEntries
                    // Remove filters that are undefined
                    .filter(([_key, value]) => value !== undefined)
                    .forEach(([key, value]) => {
                        // Handle multiselectors
                        if (Array.isArray(value)) {
                            // Chain with OR
                            const orChain = value.join(' OR ');

                            // Build the query, if the value is a single value, we don't need to wrap it in parentheses
                            const orQuery =
                                value.length > 1 ? `+${key}:(${orChain})` : `+${key}:${orChain}`;

                            // Add the query to the modified query
                            modifiedQuery = modifiedQuery.raw(orQuery);
                            return;
                        }

                        // Handle raw search for title
                        if (key === 'title') {
                            // This is a indexed field, so we need to search for the title_dotraw
                            modifiedQuery = modifiedQuery.raw(`+title_dotraw:*${value}*^15`);
                            return;
                        }

                        modifiedQuery = modifiedQuery.field(key).equals(value);
                    });

                return modifiedQuery.build();
            })
        };
    }),
    withMethods((store) => {
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
            patchFilters(filters: DotContentDriveFilters) {
                patchState(store, { filters: { ...store.filters(), ...filters } });
            },
            removeFilter(filter: string) {
                const { [filter]: removedFilter, ...restFilters } = store.filters();
                if (removedFilter) {
                    patchState(store, { filters: restFilters });
                }
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
            }
        };
    }),
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const globalStore = inject(GlobalStore);
        let initEffect: EffectRef;

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
            },
            onDestroy() {
                initEffect?.destroy();
            }
        };
    })
);
