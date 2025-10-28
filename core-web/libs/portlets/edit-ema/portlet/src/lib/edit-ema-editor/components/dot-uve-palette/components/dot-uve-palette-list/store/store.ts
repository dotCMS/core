import { signalStore, withMethods, withState, patchState, withComputed } from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { DotESContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType, DotPagination } from '@dotcms/dotcms-models';

import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../../service/dot-page-favorite-contentType.service';
import { DotESContentParams, SortOption } from '../model';

/**
 * Available view states for the palette list
 */
export enum DotUVEPaletteListView {
    CONTENT_TYPES = 'contenttypes',
    CONTENTLETS = 'contentlets'
}

/**
 * Status states for the palette list
 */
export enum DotPaletteListStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    EMPTY = 'empty'
}

/** Default number of items per page */
export const DEFAULT_PER_PAGE = 30;

/**
 * Component state interface for palette list
 */
export interface DotPaletteListState {
    currentContentType: string;
    contenttypes: DotCMSContentType[];
    contentlets: DotCMSContentlet[];
    pagination: DotPagination;
    sort: SortOption;
    currentView: DotUVEPaletteListView;
    status: DotPaletteListStatus;
}

export const DEFAULT_STATE: DotPaletteListState = {
    contenttypes: [],
    contentlets: [],
    pagination: {
        currentPage: 1,
        perPage: DEFAULT_PER_PAGE,
        totalEntries: 0
    },
    sort: {
        orderby: 'name',
        direction: 'ASC'
    },
    currentContentType: '',
    currentView: DotUVEPaletteListView.CONTENT_TYPES,
    status: DotPaletteListStatus.LOADING
};

export const DotPaletteListStore = signalStore(
    withState<DotPaletteListState>(DEFAULT_STATE),
    withComputed((store) => {
        const pagination = store.pagination;
        return {
            $start: computed(() => pagination().currentPage - 1 * pagination().perPage),
            $status: computed(() => store.status()),
            $rowsPerPage: computed(() => store.pagination().perPage),
            $showPaginator: computed(() => pagination().totalEntries > pagination().perPage)
        };
    }),
    withMethods((store) => {
        const pageContentTypeService = inject(DotPageContentTypeService);
        const dotESContentService = inject(DotESContentService);
        const dotPageFavoriteContentTypeService = inject(DotPageFavoriteContentTypeService);
        return {
            setCurrentContentType(contentTypeName: string) {
                patchState(store, {
                    currentContentType: contentTypeName
                });
            },
            getContentTypes(params: DotPageContentTypeParams) {
                const { orderby, direction } = params;

                patchState(store, {
                    status: DotPaletteListStatus.LOADING,
                    sort: { orderby, direction }
                });

                pageContentTypeService.get(params).subscribe(({ contenttypes, pagination }) => {
                    patchState(store, {
                        contenttypes,
                        pagination,
                        currentView: DotUVEPaletteListView.CONTENT_TYPES,
                        status:
                            contenttypes.length > 0
                                ? DotPaletteListStatus.LOADED
                                : DotPaletteListStatus.EMPTY
                    });
                });
            },
            getWidgets(params: DotPageContentTypeParams) {
                patchState(store, {
                    status: DotPaletteListStatus.LOADING
                });

                pageContentTypeService
                    .getAllContentTypes(params)
                    .subscribe(({ contenttypes, pagination }) => {
                        patchState(store, {
                            contenttypes,
                            pagination,
                            currentView: DotUVEPaletteListView.CONTENT_TYPES,
                            status:
                                contenttypes.length > 0
                                    ? DotPaletteListStatus.LOADED
                                    : DotPaletteListStatus.EMPTY
                        });
                    });
            },
            getContentlets(params: DotESContentParams) {
                const { itemsPerPage, lang, filter, offset, query } = params;
                patchState(store, {
                    status: DotPaletteListStatus.LOADING
                });

                dotESContentService
                    .get({
                        itemsPerPage,
                        lang,
                        filter,
                        offset,
                        query
                    })
                    .subscribe((response) => {
                        const contentlets = response.jsonObjectView.contentlets;
                        patchState(store, {
                            contentlets,
                            pagination: {
                                currentPage: Math.floor(Number(offset) / itemsPerPage) + 1,
                                perPage: contentlets.length,
                                totalEntries: response.resultsSize
                            },
                            currentView: DotUVEPaletteListView.CONTENTLETS,
                            status:
                                contentlets.length > 0
                                    ? DotPaletteListStatus.LOADED
                                    : DotPaletteListStatus.EMPTY
                        });
                    });
            },
            getFavoriteContentTypes(filter: string) {
                patchState(store, {
                    status: DotPaletteListStatus.LOADING
                });

                let contenttypes = dotPageFavoriteContentTypeService.getAll();
                const totalEntries = contenttypes.length;

                // Apply filter
                if (filter) {
                    const filterLower = filter.toLowerCase();
                    contenttypes = contenttypes.filter((ct) =>
                        ct.name.toLowerCase().includes(filterLower)
                    );
                }

                // Apply sorting by name
                contenttypes.sort((a, b) => a.name.localeCompare(b.name));

                patchState(store, {
                    contenttypes,
                    pagination: {
                        currentPage: 1,
                        perPage: contenttypes.length,
                        totalEntries
                    },
                    currentView: DotUVEPaletteListView.CONTENT_TYPES,
                    status:
                        contenttypes.length > 0
                            ? DotPaletteListStatus.LOADED
                            : DotPaletteListStatus.EMPTY
                });
            },
            isFavoriteContentType(contentTypeId: string) {
                return dotPageFavoriteContentTypeService.isFavorite(contentTypeId);
            },
            addFavoriteContentType(contentType: DotCMSContentType) {
                const updatedFavorites = dotPageFavoriteContentTypeService.add(contentType);

                // Update store state with current favorites
                patchState(store, {
                    contenttypes: updatedFavorites
                });
            },
            saveFavoriteContentTypes(contentTypes: DotCMSContentType[]) {
                // Replace entire favorites list with the new selection
                const updatedFavorites = dotPageFavoriteContentTypeService.set(contentTypes);

                patchState(store, {
                    contenttypes: updatedFavorites,
                    status:
                        updatedFavorites.length > 0
                            ? DotPaletteListStatus.LOADED
                            : DotPaletteListStatus.EMPTY
                });
            },
            removeFavoriteContentType(contentTypeId: string) {
                const updatedFavorites = dotPageFavoriteContentTypeService.remove(contentTypeId);

                // Update store state with current favorites
                patchState(store, {
                    contenttypes: updatedFavorites,
                    status:
                        updatedFavorites.length > 0
                            ? DotPaletteListStatus.LOADED
                            : DotPaletteListStatus.EMPTY
                });
            }
        };
    })
);
