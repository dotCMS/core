import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';

import { BASE_QUERY, SYSTEM_HOST } from '../shared/constants';
import { DotContentDriveInit, DotContentDriveState, DotContentDriveStatus } from '../shared/models';

const initialState: DotContentDriveState = {
    currentSite: SYSTEM_HOST,
    path: '',
    filters: {},
    items: [],
    status: DotContentDriveStatus.LOADING
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
                    currentSite,
                    path,
                    filters,
                    status: DotContentDriveStatus.LOADING
                });
            },
            setItems(items: DotContentDriveItem[]) {
                patchState(store, { items, status: DotContentDriveStatus.LOADED });
            },
            setStatus(status: DotContentDriveStatus) {
                patchState(store, { status });
            }
        };
    })
);
