import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import {
    DotCMSContentlet,
    DotCurrentUser,
    DotLanguage,
    DotPagination
} from '@dotcms/dotcms-models';

import { DotPageListService, ListPagesParams } from '../dot-page-list.service';

export interface DotCMSPagesPortletState {
    pages: DotCMSContentlet[];
    favoritePages: DotCMSContentlet[];
    pagination: DotPagination;
    filters: ListPagesParams;
    languages: DotLanguage[];
    currentUser?: DotCurrentUser;
    status: 'loading' | 'loaded' | 'error' | 'idle'; // replaces portletStatus
}

const initialState: DotCMSPagesPortletState = {
    pages: [],
    favoritePages: [],
    filters: {
        search: '',
        sort: 'title ASC',
        limit: 40,
        languageId: null, // null means all languages
        archived: false,
        offset: 0,
        host: '',
        userId: 'dotcms.org.1'
    },
    pagination: {
        currentPage: 1,
        perPage: 40,
        totalEntries: 0
    },
    languages: [],
    currentUser: null,
    status: 'loading'
};

export const DotCMSPagesStore = signalStore(
    withState(initialState),
    withComputed((store) => {
        return {
            $totalRecords: computed<number>(() => store.pagination.totalEntries())
        };
    }),
    withMethods((store) => {
        const dotPageListService = inject(DotPageListService);

        const fetchPages = (params: Partial<ListPagesParams> = {}) => {
            const nextFilters: ListPagesParams = { ...store.filters(), ...params };
            const limit = nextFilters.limit ?? 40;
            const offset = nextFilters.offset ?? 0;

            patchState(store, {
                status: 'loading',
                filters: nextFilters
            });

            dotPageListService
                .getPages(nextFilters)
                .subscribe(({ jsonObjectView, resultsSize }) => {
                    patchState(store, {
                        status: 'loaded',
                        pages: jsonObjectView.contentlets,
                        pagination: {
                            currentPage: Math.floor(offset / limit) + 1,
                            perPage: limit,
                            totalEntries: resultsSize
                        }
                    });
                });
        };
        return {
            getPages: (params: Partial<ListPagesParams> = {}) => fetchPages(params),
            searchPages: (search: string) => {
                fetchPages({ search, offset: 0 });
            },
            filterByLanguage: (languageId: number) => {
                fetchPages({ languageId, offset: 0 });
            },
            filterByArchived: (archived: boolean) => {
                fetchPages({ archived, offset: 0 });
            },
            onLazyLoad: (event: LazyLoadEvent) => {
                const { first, sortField, sortOrder } = event;
                const offset = Math.max(0, first ?? 0);
                const sort = sortField
                    ? `${sortField} ${sortOrder === 1 ? 'ASC' : 'DESC'}`
                    : 'title ASC';
                fetchPages({ offset, sort });
            }
        };
    }),
    // This will be a signal feature
    withMethods((store) => {
        const dotPageListService = inject(DotPageListService);
        const fetchFavoritePages = () => {
            dotPageListService.getFavoritePages(store.filters()).subscribe(({ jsonObjectView }) => {
                patchState(store, {
                    favoritePages: jsonObjectView.contentlets
                });
            });
        };

        return {
            getFavoritePages: () => {
                fetchFavoritePages();
            }
        };
    })
);
