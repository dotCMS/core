import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';

import { BASE_QUERY, DEFAULT_PAGINATION, SYSTEM_HOST } from '../shared/constants';
import {
    DotContentDriveInit,
    DotContentDrivePagination,
    DotContentDriveSort,
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../shared/models';

const initialState: DotContentDriveState = {
    currentSite: SYSTEM_HOST,
    path: '',
    filters: {},
    items: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: DEFAULT_PAGINATION,
    sort: {
        field: 'modDate',
        order: DotContentDriveSortOrder.ASC
    }
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

                if (pathValue) {
                    modifiedQuery = modifiedQuery.field('parentPath').equals(pathValue);
                }

                modifiedQuery = modifiedQuery
                    .field('conhost')
                    .equals(currentSiteValue?.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier);

                if (filtersValue) {
                    // We gotta handle the multiselector (,) but this is enough to pave the path for now
                    Object.entries(filtersValue).forEach(([key, value]) => {
                        modifiedQuery = modifiedQuery.field(key).equals(value);
                    });
                }

                return modifiedQuery.build();
            })
        };
    }),
    withMethods((store) => {
        return {
            initContentDrive({ currentSite, path, filters }: DotContentDriveInit) {
                patchState(store, {
                    currentSite: currentSite ?? SYSTEM_HOST,
                    path,
                    filters,
                    status: DotContentDriveStatus.LOADING
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
            }
        };
    })
);
