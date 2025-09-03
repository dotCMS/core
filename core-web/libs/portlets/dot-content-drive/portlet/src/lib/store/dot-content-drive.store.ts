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

import { DotContentSearchService } from '@dotcms/data-access';
import { DotContentDriveItem, ESContent } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';
import { GlobalStore } from '@dotcms/store';

import { withContextMenu } from './features/withContextMenu';

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
    withComputed(({ path, filters, currentSite, pagination, sort }) => {
        return {
            $query: computed(() => {
                const query = new QueryBuilder();

                const baseQuery = query.raw(BASE_QUERY);

                let modifiedQuery = baseQuery;

                const pathValue = path();
                const currentSiteValue = currentSite();
                const filtersValue = filters();
                // console.log('called computed $query', filtersValue);
                const filtersEntries = Object.entries(filtersValue ?? {});

                if (pathValue) {
                    modifiedQuery = modifiedQuery.field('parentPath').equals(pathValue);
                }

                modifiedQuery = modifiedQuery
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
                            // This is a indexed field, so we need to search by boosting terms https://dev.dotcms.com/docs/content-search-syntax#Boost
                            // We search by catchall, title_dotraw boosting 5 and title boosting 15, giving more weight to the title
                            modifiedQuery = modifiedQuery.raw(
                                `+catchall:*${value}* title_dotraw:*${value}*^5 title:'${value}'^15`
                            );

                            // If the value has multiple words, we need to search for each word and boost them by 5
                            value
                                .split(' ')
                                .filter((word) => word.trim().length > 0)
                                .forEach((word) => {
                                    modifiedQuery = modifiedQuery.raw(`title:${word}^5`);
                                });

                            return;
                        }

                        modifiedQuery = modifiedQuery.field(key).equals(value);
                    });

                return modifiedQuery.build();
            }),
            $searchParams: computed(() => ({
                query: (() => {
                    const query = new QueryBuilder();
                    const baseQuery = query.raw(BASE_QUERY);
                    let modifiedQuery = baseQuery;

                    const pathValue = path();
                    const currentSiteValue = currentSite();
                    const filtersValue = filters();
                    const filtersEntries = Object.entries(filtersValue ?? {});

                    if (pathValue) {
                        modifiedQuery = modifiedQuery.field('parentPath').equals(pathValue);
                    }

                    modifiedQuery = modifiedQuery
                        .field('conhost')
                        .equals(currentSiteValue?.identifier)
                        .or()
                        .equals(SYSTEM_HOST.identifier);

                    filtersEntries
                        .filter(([_key, value]) => value !== undefined)
                        .forEach(([key, value]) => {
                            // Handle multiselectors
                            if (Array.isArray(value)) {
                                const orChain = value.join(' OR ');
                                const orQuery =
                                    value.length > 1
                                        ? `+${key}:(${orChain})`
                                        : `+${key}:${orChain}`;
                                modifiedQuery = modifiedQuery.raw(orQuery);
                                return;
                            }

                            // Handle raw search for title
                            if (key === 'title') {
                                modifiedQuery = modifiedQuery.raw(
                                    `+catchall:*${value}* title_dotraw:*${value}*^5 title:'${value}'^15`
                                );
                                value
                                    .split(' ')
                                    .filter((word) => word.trim().length > 0)
                                    .forEach((word) => {
                                        modifiedQuery = modifiedQuery.raw(`title:${word}^5`);
                                    });
                                return;
                            }

                            modifiedQuery = modifiedQuery.field(key).equals(value);
                        });

                    return modifiedQuery.build();
                })(),
                pagination: pagination(),
                sort: sort(),
                currentSite: currentSite()
            }))
        };
    }),
    withMethods((store) => {
        const contentSearchService = inject(DotContentSearchService);

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
            },
            async loadItems() {
                const { query, pagination, sort, currentSite } = store.$searchParams();
                const { limit, offset } = pagination;
                const { field, order } = sort;

                patchState(store, { status: DotContentDriveStatus.LOADING });

                // Avoid fetching content for SYSTEM_HOST sites
                if (currentSite?.identifier === SYSTEM_HOST.identifier) {
                    return;
                }

                contentSearchService
                    .get<ESContent>({
                        query,
                        limit,
                        offset,
                        sort: `score,${field} ${order}`
                    })
                    .pipe(
                        take(1),
                        catchError(() => {
                            patchState(store, { status: DotContentDriveStatus.ERROR });
                            return EMPTY;
                        })
                    )
                    .subscribe((response) => {
                        patchState(store, {
                            items: response.jsonObjectView.contentlets,
                            totalItems: response.resultsSize,
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

                searchEffect = effect(() => {
                    // const searchParams = store.$searchParams();
                    // console.log('searchEffect triggered', searchParams);
                    store.loadItems();
                });
            },
            onDestroy() {
                initEffect?.destroy();
                searchEffect?.destroy();
            }
        };
    }),
    withContextMenu()
);
