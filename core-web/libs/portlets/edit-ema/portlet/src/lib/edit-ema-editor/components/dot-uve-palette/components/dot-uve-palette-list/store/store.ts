import { signalStore, withMethods, withState, patchState, withComputed } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotESContentService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentType } from '@dotcms/dotcms-models';

import {
    BASETYPES_FOR_CONTENT,
    BASETYPES_FOR_WIDGET,
    DEFAULT_PER_PAGE,
    DotESContentParams,
    DotPageContentTypeQueryParams,
    DotPaletteListState,
    DotPaletteListStatus,
    DotPaletteSearchParams,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../../models';
import { DotPageContentTypeService } from '../../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../../service/dot-page-favorite-contentType.service';
import { buildContentletsResponse, buildFavoriteResponse, getPaletteState } from '../../../utils';

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
            $currentSort: computed(() => {
                return {
                    orderby: store.searchParams.orderby(),
                    direction: store.searchParams.direction()
                };
            }),
            $isLoading: computed(() => store.status() === DotPaletteListStatus.LOADING),
            $isContentTypesView: computed(
                () => store.currentView() === DotUVEPaletteListView.CONTENT_TYPES
            ),
            $isContentletsView: computed(
                () => store.currentView() === DotUVEPaletteListView.CONTENTLETS
            )
        };
    }),
    withMethods((store) => {
        const pageContentTypeService = inject(DotPageContentTypeService);
        const dotESContentService = inject(DotESContentService);
        const dotPageFavoriteContentTypeService = inject(DotPageFavoriteContentTypeService);

        const getData = (
            type: DotUVEPaletteListTypes,
            extraParams: Partial<DotPageContentTypeQueryParams> = {}
        ) => {
            const params = { ...store.searchParams(), ...extraParams };
            switch (type) {
                case DotUVEPaletteListTypes.CONTENT:
                    return pageContentTypeService.get({ ...params, types: BASETYPES_FOR_CONTENT });
                case DotUVEPaletteListTypes.WIDGET:
                    return pageContentTypeService.getAllContentTypes({
                        ...params,
                        types: BASETYPES_FOR_WIDGET
                    });
                case DotUVEPaletteListTypes.FAVORITES:
                    return of(dotPageFavoriteContentTypeService.getAll()).pipe(
                        map((contentTypes) =>
                            buildFavoriteResponse(contentTypes, params.filter || '')
                        )
                    );
            }
        };

        return {
            setSearchParams(searchParams: Partial<DotPaletteSearchParams>) {
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
                type: DotUVEPaletteListTypes,
                params: Partial<DotPageContentTypeQueryParams> = {}
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
