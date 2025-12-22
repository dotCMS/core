import {
    patchState,
    signalMethod,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import {
    DotCMSContentlet,
    DotCurrentUser,
    DotLanguage,
    DotPagination,
    SiteEntity
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withFavorites } from './withFavorite/withFavorite';

import { DotPageListService, ListPagesParams } from '../services/dot-page-list.service';

export interface DotCMSPagesPortletState {
    pages: DotCMSContentlet[];
    pagination: DotPagination;
    filters: ListPagesParams;
    languages: DotLanguage[];
    currentUser?: DotCurrentUser;
    bundleDialog: {
        show: boolean;
        pageIdentifier: string;
    };
    status: 'loading' | 'loaded' | 'error' | 'idle'; // replaces portletStatus
}

const initialFilters: ListPagesParams = {
    search: '',
    sort: 'modDate DESC',
    limit: 40,
    languageId: null, // null means all languages
    archived: false,
    offset: 0,
    host: ''
};

const initialState: DotCMSPagesPortletState = {
    pages: [],
    filters: initialFilters,
    pagination: {
        currentPage: 1,
        perPage: 40,
        totalEntries: 0
    },
    bundleDialog: {
        show: false,
        pageIdentifier: ''
    },
    languages: [],
    currentUser: null,
    status: 'loading'
};

export const DotCMSPagesStore = signalStore(
    withState(initialState),
    withComputed((store) => {
        return {
            $totalRecords: computed<number>(() => store.pagination.totalEntries()),
            $showBundleDialog: computed<boolean>(() => store.bundleDialog.show()),
            $assetIdentifier: computed<string>(() => store.bundleDialog.pageIdentifier()),
            $isPagesLoading: computed<boolean>(() => store.status() === 'loading')
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
            },
            updatePageNode: (identifier: string) => {
                dotPageListService.getSinglePage(identifier).subscribe((updatedPage) => {
                    const currentPages = store.pages();
                    const nextPages = currentPages.map((page) =>
                        page?.identifier === identifier ? updatedPage : page
                    );
                    patchState(store, { pages: nextPages });
                });
            },
            showBundleDialog: (pageIdentifier: string) => {
                patchState(store, { bundleDialog: { show: true, pageIdentifier } });
            },
            hideBundleDialog: () => {
                patchState(store, { bundleDialog: { show: false, pageIdentifier: '' } });
            }
        };
    }),
    withHooks((store) => {
        const globalStore = inject(GlobalStore);
        return {
            onInit: () => {
                const handleSwitchSite = signalMethod<SiteEntity>((site: SiteEntity) => {
                    const host = site.identifier;
                    store.getPages({ ...initialFilters, host });
                });
                handleSwitchSite(globalStore.siteDetails);
            }
        };
    }),
    withFavorites()
);
