import {
    patchState,
    signalMethod,
    signalStore,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCurrentUser,
    DotLanguage,
    DotPagination,
    DotSite
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withFavorites } from './withFavorite/withFavorite';

import { DotPageListService, ListPagesParams } from '../services/dot-page-list.service';

export interface DotCMSPagesPortletState {
    pages: DotCMSContentlet[];
    pagination: DotPagination;
    filters: ListPagesParams;
    languages: DotLanguage[];
    /**
     * `| null`, not optional: the initial state seeds it to `null`, which is what "no user loaded
     * yet" means here. Declared optional, `withState(initialState)` did not type-check — and a
     * failing `withState` collapses the whole store's type to `{ [x: string]: Function }`, which is
     * why every named read of this store, in the store and across its spec, reported an
     * index-signature access.
     */
    currentUser: DotCurrentUser | null;
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

/**
 * The state, computeds, methods and hooks `withFavorites` composes on top of, bundled into one
 * feature.
 *
 * Passed as four separate arguments to `signalStore`, ngrx stopped carrying the earlier features'
 * results forward — `withFavorites()` received `InnerSignalStore<object, object,
 * MethodsDictionary>`, so its `state: DotCMSPagesPortletState` constraint could not be met and the
 * whole store's type collapsed to `{ [x: string]: Function }`. Every named read of this store, here
 * and across its spec, then reported an index-signature access. Composing them with
 * `signalStoreFeature` first keeps the inference intact.
 */
const withPagesBase = () =>
    signalStoreFeature(
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
            const httpErrorManagerService = inject(DotHttpErrorManagerService);

            const fetchPages = (params: Partial<ListPagesParams> = {}) => {
                const nextFilters: ListPagesParams = { ...store.filters(), ...params };
                const limit = nextFilters.limit ?? 40;
                const offset = nextFilters.offset ?? 0;

                patchState(store, {
                    status: 'loading',
                    filters: nextFilters
                });

                dotPageListService.getPages(nextFilters).subscribe({
                    next: ({ jsonObjectView, resultsSize }) => {
                        patchState(store, {
                            status: 'loaded',
                            pages: jsonObjectView.contentlets,
                            pagination: {
                                currentPage: Math.floor(offset / limit) + 1,
                                perPage: limit,
                                totalEntries: resultsSize
                            }
                        });
                    },
                    error: (error) => {
                        patchState(store, { status: 'error' });
                        httpErrorManagerService.handle(error);
                    }
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
                    dotPageListService.getSinglePage(identifier).subscribe({
                        next: (updatedPage) => {
                            const currentPages = store.pages();
                            const nextPages = currentPages.map((page) =>
                                page?.identifier === identifier ? updatedPage : page
                            );
                            patchState(store, { pages: nextPages });
                        },
                        error: (error) => {
                            httpErrorManagerService.handle(error);
                        }
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
                    // `| null` matches `globalStore.siteDetails`, which is `Signal<DotSite | null>`
                    // — and the guard below was already written for it.
                    const handleSwitchSite = signalMethod<DotSite | null>(
                        (site: DotSite | null) => {
                            if (!site) return;
                            const host = site.identifier;
                            store.getPages({ ...initialFilters, host });
                        }
                    );
                    handleSwitchSite(globalStore.siteDetails);
                }
            };
        })
    );

export const DotCMSPagesStore = signalStore(withPagesBase(), withFavorites());
