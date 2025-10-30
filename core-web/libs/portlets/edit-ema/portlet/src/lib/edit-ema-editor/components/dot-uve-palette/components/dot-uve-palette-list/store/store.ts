import { signalStore, withMethods, withState, patchState, withComputed } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotESContentService, DotPageContentTypeService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentType } from '@dotcms/dotcms-models';

import {
    BASETYPES_FOR_CONTENT,
    BASETYPES_FOR_WIDGET,
    DEFAULT_PER_PAGE,
    DotPaletteListState,
    DotPaletteListStatus,
    DotPaletteSearchParams,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../../models';
import { DotPageFavoriteContentTypeService } from '../../../service/dot-page-favorite-contentType.service';
import {
    buildContentletsResponse,
    buildESContentParams,
    filterAndBuildFavoriteResponse,
    getPaletteState
} from '../../../utils';

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
        listType: DotUVEPaletteListTypes.CONTENT,
        selectedContentType: '',
        orderby: 'name',
        direction: 'ASC',
        page: 1,
        filter: ''
    },
    currentView: DotUVEPaletteListView.CONTENT_TYPES,
    status: DotPaletteListStatus.LOADING
};

export const DotPaletteListStore = signalStore(
    withState<DotPaletteListState>(DEFAULT_STATE),
    withComputed((store) => {
        const pagination = store.pagination;
        const params = store.searchParams;
        return {
            $start: computed(() => (pagination().currentPage - 1) * pagination().perPage),
            $status: computed(() => store.status()),
            $isLoading: computed(() => store.status() === DotPaletteListStatus.LOADING),
            $isContentTypesView: computed(() => params.selectedContentType() === ''),
            $isContentletsView: computed(() => params.selectedContentType() !== ''),
            $currentSort: computed(() => ({
                orderby: params.orderby(),
                direction: params.direction()
            })),
            $emptyStateMessage: computed(() => {
                const currentView = store.currentView();
                const listType = params.listType();

                if (currentView === DotUVEPaletteListView.CONTENTLETS) {
                    return 'uve.palette.empty.contentlets.message';
                }

                if (listType === DotUVEPaletteListTypes.FAVORITES) {
                    return 'uve.palette.empty.favorites.message';
                }

                return 'uve.palette.empty.content-types.message';
            })
        };
    }),
    withMethods((store) => {
        const pageContentTypeService = inject(DotPageContentTypeService);
        const dotESContentService = inject(DotESContentService);
        const dotPageFavoriteContentTypeService = inject(DotPageFavoriteContentTypeService);

        const getData = () => {
            const { listType, ...params } = store.searchParams();

            switch (listType) {
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
                            filterAndBuildFavoriteResponse({
                                contentTypes,
                                filter: params.filter || '',
                                page: params.page
                            })
                        )
                    );
            }
        };

        return {
            setContentTypesFromFavorite(contentTypes: DotCMSContentType[]) {
                const { contenttypes, pagination } = filterAndBuildFavoriteResponse({
                    contentTypes,
                    filter: store.searchParams().filter,
                    page: store.searchParams().page
                });
                patchState(store, {
                    contenttypes,
                    pagination,
                    status: getPaletteState(contenttypes)
                });
            },
            /**
             * Fetch content types with optional parameter updates.
             * Updates store search params and then fetches data using the updated params.
             * Automatically uses listType from store state.
             */
            getContentTypes(params: Partial<DotPaletteSearchParams> = {}) {
                // Update search params in store
                patchState(store, {
                    searchParams: {
                        ...store.searchParams(),
                        ...params,
                        selectedContentType: '' // Ensure we're in content types view
                    },
                    status: DotPaletteListStatus.LOADING
                });

                return getData().subscribe(({ contenttypes, pagination }) => {
                    patchState(store, {
                        contenttypes,
                        pagination,
                        currentView: DotUVEPaletteListView.CONTENT_TYPES,
                        status: getPaletteState(contenttypes)
                    });
                });
            },
            /**
             * Fetch contentlets with optional parameter updates.
             * Updates store search params and builds ES query automatically.
             * Uses selectedContentType, variantId, language, page, and filter from store.
             */
            getContentlets(params: Partial<DotPaletteSearchParams> = {}) {
                // Update search params in store
                patchState(store, {
                    searchParams: { ...store.searchParams(), ...params },
                    status: DotPaletteListStatus.LOADING
                });

                // Build ES params from updated store state
                const esParams = buildESContentParams(store.searchParams());

                dotESContentService
                    .get(esParams)
                    .pipe(
                        map((response) =>
                            buildContentletsResponse(response, Number(esParams.offset))
                        )
                    )
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
