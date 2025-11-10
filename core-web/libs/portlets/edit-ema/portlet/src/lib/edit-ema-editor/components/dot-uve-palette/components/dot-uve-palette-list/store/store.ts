import {
    signalStore,
    withMethods,
    withState,
    patchState,
    withComputed,
    withHooks
} from '@ngrx/signals';
import { of } from 'rxjs';

import { computed, effect, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotLocalstorageService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentType } from '@dotcms/dotcms-models';

import {
    BASETYPES_FOR_CONTENT,
    BASETYPES_FOR_WIDGET,
    DEFAULT_PER_PAGE,
    DotPaletteListState,
    DotPaletteListStatus,
    DotPaletteSearchParams,
    DotPaletteSortOption,
    DotPaletteViewMode,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../../models';
import {
    buildContentletsResponse,
    buildESContentParams,
    DEFAULT_SORT_OPTIONS,
    DOT_PALETTE_LAYOUT_MODE_STORAGE_KEY,
    DOT_PALETTE_SORT_OPTIONS_STORAGE_KEY,
    EMPTY_CONTENTLET_RESPONSE,
    EMPTY_CONTENTTYPE_RESPONSE,
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
    status: DotPaletteListStatus.LOADING,
    layoutMode: 'grid'
};

export const DotPaletteListStore = signalStore(
    withState<DotPaletteListState>(DEFAULT_STATE),
    withComputed((store) => {
        const params = store.searchParams;
        return {
            $isLoading: computed(() => store.status() === DotPaletteListStatus.LOADING),
            $isEmpty: computed(() => store.status() === DotPaletteListStatus.EMPTY),
            $showListLayout: computed(() => {
                const isListLayout = store.layoutMode() === 'list';
                const isContentletsView = store.currentView() === DotUVEPaletteListView.CONTENTLETS;
                return isListLayout || isContentletsView;
            }),
            $isContentTypesView: computed(
                () => store.currentView() === DotUVEPaletteListView.CONTENT_TYPES
            ),
            $isContentletsView: computed(
                () => store.currentView() === DotUVEPaletteListView.CONTENTLETS
            ),
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
        const dotFavoriteContentTypeService = inject(DotFavoriteContentTypeService);

        const getData = () => {
            const { listType, ...params } = store.searchParams();

            switch (listType) {
                case DotUVEPaletteListTypes.CONTENT:
                    return pageContentTypeService.get({
                        ...params,
                        types: BASETYPES_FOR_CONTENT,
                        per_page: DEFAULT_PER_PAGE
                    });
                case DotUVEPaletteListTypes.WIDGET:
                    return pageContentTypeService.getAllContentTypes({
                        ...params,
                        types: BASETYPES_FOR_WIDGET,
                        per_page: DEFAULT_PER_PAGE
                    });
                case DotUVEPaletteListTypes.FAVORITES:
                    return of(dotFavoriteContentTypeService.getAll()).pipe(
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
            setLayoutMode(layoutMode: DotPaletteViewMode) {
                patchState(store, { layoutMode });
            },
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

                return getData()
                    .pipe(
                        catchError((error) => {
                            console.error(
                                `[DotUVEPalette Store]: Error data fetching contenttypes: ${error}`
                            );
                            return of(EMPTY_CONTENTTYPE_RESPONSE);
                        })
                    )
                    .subscribe(({ contenttypes, pagination }) => {
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
                        ),
                        catchError((error) => {
                            console.error(
                                `[DotUVEPalette Store]: Error data fetching contentlets: ${error}`
                            );
                            return of(EMPTY_CONTENTLET_RESPONSE);
                        })
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
    }),
    withHooks((store) => {
        const dotLocalstorageService = inject(DotLocalstorageService);

        return {
            onInit() {
                const layoutMode =
                    dotLocalstorageService.getItem<DotPaletteViewMode>(
                        DOT_PALETTE_LAYOUT_MODE_STORAGE_KEY
                    ) || 'grid';
                const { orderby, direction } =
                    dotLocalstorageService.getItem<DotPaletteSortOption>(
                        DOT_PALETTE_SORT_OPTIONS_STORAGE_KEY
                    ) || DEFAULT_SORT_OPTIONS;
                patchState(store, {
                    layoutMode,
                    searchParams: { ...store.searchParams(), orderby, direction }
                });

                effect(() => {
                    dotLocalstorageService.setItem(
                        DOT_PALETTE_LAYOUT_MODE_STORAGE_KEY,
                        store.layoutMode()
                    );
                });

                effect(() => {
                    dotLocalstorageService.setItem(DOT_PALETTE_SORT_OPTIONS_STORAGE_KEY, {
                        orderby: store.searchParams.orderby(),
                        direction: store.searchParams.direction()
                    });
                });
            }
        };
    })
);
