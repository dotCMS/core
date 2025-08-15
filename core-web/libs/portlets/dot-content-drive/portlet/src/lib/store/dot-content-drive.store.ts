import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';
import { GlobalStore } from '@dotcms/store';

import {
    BASE_QUERY,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_TREE_EXPANDED,
    SYSTEM_HOST
} from '../shared/constants';
import {
    DotContentDriveInit,
    DotContentDrivePagination,
    DotContentDriveSort,
    DotContentDriveSortOrder,
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
    sort: {
        field: 'modDate',
        order: DotContentDriveSortOrder.ASC
    },
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

                // Add the path to the query, the default is "/"
                if (pathValue) {
                    modifiedQuery = modifiedQuery.field('parentPath').equals(pathValue);
                }

                modifiedQuery = modifiedQuery
                    .field('conhost')
                    .equals(currentSiteValue?.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier);

                if (filtersValue) {
                    Object.entries(filtersValue).forEach(([key, value]) => {
                        // Handle multiselectors
                        if (Array.isArray(value)) {
                            // Chain with OR
                            const orChain = value.join(' OR ');

                            // Build the query
                            const orQuery = `+${key}: (${orChain})`;

                            // Add the query to the modified query
                            modifiedQuery = modifiedQuery.raw(orQuery);
                        } else {
                            modifiedQuery = modifiedQuery.field(key).equals(`${value}*`);
                        }
                    });
                }

                return modifiedQuery.build();
            })
        };
    }),
    withMethods((store) => {
        return {
            initContentDrive({
                currentSite,
                path,
                filters,
                isTreeExpanded: treeExpanded
            }: DotContentDriveInit) {
                patchState(store, {
                    currentSite: currentSite ?? SYSTEM_HOST,
                    path,
                    filters,
                    status: DotContentDriveStatus.LOADING,
                    isTreeExpanded: treeExpanded
                });
            },
            setItems(items: DotContentDriveItem[], totalItems: number) {
                patchState(store, { items, status: DotContentDriveStatus.LOADED, totalItems });
            },
            setStatus(status: DotContentDriveStatus) {
                patchState(store, { status });
            },
            setFilters(filters: Record<string, string>) {
                patchState(store, { filters: { ...store.filters(), ...filters } });
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
            removeFilter(filter: string) {
                const { [filter]: removedFilter, ...restFilters } = store.filters();
                if (removedFilter) {
                    patchState(store, { filters: restFilters });
                }
            },
            getFilterValue(filter: string) {
                return store.filters()[filter];
            }
        };
    }),
    withHooks((store) => {
        const route = inject(ActivatedRoute);
        const globalStore = inject(GlobalStore);

        return {
            onInit() {
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
            }
        };
    })
);
