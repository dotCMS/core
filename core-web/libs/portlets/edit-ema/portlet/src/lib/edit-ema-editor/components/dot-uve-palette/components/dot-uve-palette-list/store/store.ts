import { signalStore, withMethods, withState, patchState, withComputed } from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { DotESContentService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';

import {
    DotContentTypeParams,
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../../service/dot-page-favorite-contentType.service';
import { DotESContentParams, SortOption } from '../model';

/**
 * Available view states for the palette list
 */
export enum DotUVEPaletteListView {
    LOADING = 'loading',
    CONTENT_TYPES = 'contenttypes',
    CONTENTLETS = 'contentlets'
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
    currentView: DotUVEPaletteListView.LOADING
};

export const DotPaletteListStore = signalStore(
    withState<DotPaletteListState>(DEFAULT_STATE),
    withComputed((store) => {
        return {
            $start: computed(() => {
                return (store.pagination().currentPage - 1) * store.pagination().perPage;
            }),
            $rowsPerPage: computed(() => store.pagination().perPage)
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

            setContenttypes(contenttypes: DotCMSContentType[]) {
                patchState(store, {
                    contenttypes,
                    currentView: DotUVEPaletteListView.CONTENT_TYPES
                });
            },

            setContentlets(contentlets: DotCMSContentlet[]) {
                patchState(store, {
                    contentlets,
                    currentView: DotUVEPaletteListView.CONTENTLETS
                });
            },

            setView(view: DotUVEPaletteListView) {
                patchState(store, {
                    currentView: view
                });
            },

            getContentTypes(params: DotPageContentTypeParams) {
                const { orderby, direction } = params;

                patchState(store, {
                    currentView: DotUVEPaletteListView.LOADING,
                    sort: { orderby, direction }
                });

                pageContentTypeService.get(params).subscribe(({ contenttypes, pagination }) => {
                    patchState(store, {
                        contenttypes,
                        pagination,
                        currentView: DotUVEPaletteListView.CONTENT_TYPES
                    });
                });
            },
            getWidgets(params: DotPageContentTypeParams) {
                pageContentTypeService
                    .getAllContentTypes(params)
                    .subscribe(({ contenttypes, pagination }) => {
                        patchState(store, {
                            contenttypes,
                            pagination,
                            currentView: DotUVEPaletteListView.CONTENT_TYPES
                        });
                    });
            },
            getAllContentTypes(params: DotContentTypeParams) {
                return pageContentTypeService.getAllContentTypes({
                    ...params,
                    types: [DotCMSBaseTypesContentTypes.CONTENT, DotCMSBaseTypesContentTypes.WIDGET]
                });
            },
            getContentlets(params: DotESContentParams) {
                const { itemsPerPage, lang, filter, offset, query } = params;
                patchState(store, {
                    currentView: DotUVEPaletteListView.LOADING
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
                            currentView: DotUVEPaletteListView.CONTENTLETS
                        });
                    });
            },
            getFavoriteContentTypes(pagePathOrId: string, filter: string) {
                patchState(store, {
                    currentView: DotUVEPaletteListView.LOADING
                });

                const response = dotPageFavoriteContentTypeService.get(pagePathOrId, {
                    orderby: 'name',
                    direction: store.sort().direction,
                    filter
                });

                patchState(store, {
                    contenttypes: response.contenttypes,
                    pagination: response.pagination,
                    currentView: DotUVEPaletteListView.CONTENT_TYPES
                });
            },
            getAllFavoriteContentTypes(pagePathOrId: string, filter: string) {
                return dotPageFavoriteContentTypeService.get(pagePathOrId, {
                    filter
                });
            },
            getIsFavoriteContentType(pagePathOrId: string, contentTypeId: string) {
                return dotPageFavoriteContentTypeService.isFavorite(pagePathOrId, contentTypeId);
            },
            addFavoriteContentType(pagePathOrId: string, contentType: DotCMSContentType) {
                dotPageFavoriteContentTypeService.add(pagePathOrId, contentType);
            },
            saveFavoriteContentTypes(pagePathOrId: string, contentTypes: DotCMSContentType[]) {
                dotPageFavoriteContentTypeService.save(pagePathOrId, contentTypes);

                patchState(store, {
                    contenttypes: contentTypes
                });
            },
            removeFavoriteContentType(pagePathOrId: string, contentTypeId: string) {
                dotPageFavoriteContentTypeService.remove(pagePathOrId, contentTypeId);
            }
        };
    })
);
