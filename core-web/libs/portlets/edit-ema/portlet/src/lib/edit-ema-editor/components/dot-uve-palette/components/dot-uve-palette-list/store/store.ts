import { signalStore, withMethods, withState, patchState, withComputed } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotESContentService } from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';

import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../../service/dot-page-favorite-contentType.service';
import {
    BASETYPES_FOR_CONTENT,
    BASETYPES_FOR_WIDGET,
    buildContentletsResponse,
    buildFavoriteResponse,
    getPaletteState,
    UVE_PALETTE_LIST_TYPES
} from '../../../utils';
import { DotESContentParams } from '../model';

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

export interface SearchParams {
    pagePathOrId: string;
    language: number;
    variantId: string;
    orderby: 'name' | 'usage';
    direction: 'ASC' | 'DESC';
    page: number;
}

/**
 * Component state interface for palette list
 */
export interface DotPaletteListState {
    searchParams: SearchParams;
    contenttypes: DotCMSContentType[];
    contentlets: DotCMSContentlet[];
    pagination: DotPagination;
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
    searchParams: {
        pagePathOrId: '',
        language: 1,
        variantId: DEFAULT_VARIANT_ID,
        orderby: 'name',
        direction: 'ASC',
        page: 1
    },
    currentView: DotUVEPaletteListView.CONTENT_TYPES,
    status: DotPaletteListStatus.LOADING
};

export const DotPaletteListStore = signalStore(
    withState<DotPaletteListState>(DEFAULT_STATE),
    withComputed((store) => {
        const pagination = store.pagination;
        return {
            $start: computed(() => (pagination().currentPage - 1) * pagination().perPage),
            $status: computed(() => store.status()),
            $rowsPerPage: computed(() => store.pagination().perPage),
            $currentSort: computed(() => {
                return {
                    orderby: store.searchParams.orderby(),
                    direction: store.searchParams.direction()
                };
            }),
            $isLoading: computed(() => store.status() === DotPaletteListStatus.LOADING)
        };
    }),
    withMethods((store) => {
        const pageContentTypeService = inject(DotPageContentTypeService);
        const dotESContentService = inject(DotESContentService);
        const dotPageFavoriteContentTypeService = inject(DotPageFavoriteContentTypeService);

        const getData = (
            type: UVE_PALETTE_LIST_TYPES,
            extraParams: Partial<DotPageContentTypeParams> = {}
        ) => {
            const params = { ...store.searchParams(), ...extraParams };
            switch (type) {
                case UVE_PALETTE_LIST_TYPES.CONTENT:
                    return pageContentTypeService.get({ ...params, types: BASETYPES_FOR_CONTENT });
                case UVE_PALETTE_LIST_TYPES.WIDGET:
                    return pageContentTypeService.getAllContentTypes({
                        ...params,
                        types: BASETYPES_FOR_WIDGET
                    });
                case UVE_PALETTE_LIST_TYPES.FAVORITES:
                    return of(dotPageFavoriteContentTypeService.getAll()).pipe(
                        map((contentTypes) =>
                            buildFavoriteResponse(contentTypes, params.filter || '')
                        )
                    );
            }
        };

        return {
            setSearchParams(searchParams: Partial<SearchParams>) {
                patchState(store, { searchParams: { ...store.searchParams(), ...searchParams } });
            },
            setContentTypesFromFavorite(contentTypes: DotCMSContentType[]) {
                const { contenttypes, pagination } = buildFavoriteResponse(contentTypes);
                patchState(store, {
                    contenttypes,
                    pagination,
                    status: getPaletteState(contenttypes)
                });
            },
            getContentTypes(
                type: UVE_PALETTE_LIST_TYPES,
                params: Partial<DotPageContentTypeParams> = {}
            ) {
                patchState(store, {
                    status: DotPaletteListStatus.LOADING
                });

                return getData(type, params).subscribe(({ contenttypes, pagination }) => {
                    patchState(store, {
                        contenttypes,
                        pagination,
                        currentView: DotUVEPaletteListView.CONTENT_TYPES,
                        status: getPaletteState(contenttypes)
                    });
                });
            },
            getContentlets(params: Partial<DotESContentParams>) {
                patchState(store, { status: DotPaletteListStatus.LOADING });
                const { query, offset = '0', filter } = params;

                dotESContentService
                    .get({
                        itemsPerPage: DEFAULT_PER_PAGE,
                        lang: store.searchParams().language.toString(),
                        query,
                        offset,
                        filter
                    })
                    .pipe(map((response) => buildContentletsResponse(response, Number(offset))))
                    .subscribe(({ contentlets, pagination }) => {
                        patchState(store, {
                            contentlets,
                            pagination,
                            currentView: DotUVEPaletteListView.CONTENTLETS,
                            status: getPaletteState(contentlets)
                        });
                    });
            }
        };
    })
);
