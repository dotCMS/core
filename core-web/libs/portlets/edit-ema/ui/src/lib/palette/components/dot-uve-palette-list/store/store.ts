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
    DotUVEPaletteListView,
    LIST_TYPE_BASE_TYPES
} from '../../../models';
import {
    buildPaletteContent,
    buildESContentParams,
    DEFAULT_SORT_OPTIONS,
    DOT_PALETTE_LAYOUT_MODE_STORAGE_KEY,
    DOT_PALETTE_SORT_OPTIONS_STORAGE_KEY,
    EMPTY_CONTENTLET_RESPONSE,
    EMPTY_CONTENTTYPE_RESPONSE,
    buildPaletteFavorite,
    getPaletteState,
    EMPTY_PAGINATION
} from '../../../utils';

/**
 * Favorite Page content type variables (current + legacy). It's a single system content type
 * with the CONTENT base type, so it leaks into the Content / All Content Types lists and the
 * global endpoint can't exclude it. We drop it from the visible page and adjust the total by one.
 */
const FAVORITE_PAGE_VARIABLES = new Set(['dotfavoritepage', 'favoritepage']);

function isFavoritePageContentType(variable: string | undefined): boolean {
    return !!variable && FAVORITE_PAGE_VARIABLES.has(variable.toLowerCase());
}

/** List types whose base types include CONTENT, where the Favorite Page type can appear. */
const LIST_TYPES_WITH_FAVORITE_PAGE = new Set<DotUVEPaletteListTypes>([
    DotUVEPaletteListTypes.ALL_CONTENT_TYPES,
    DotUVEPaletteListTypes.ALL_CONTENT
]);

export const DEFAULT_STATE: DotPaletteListState = {
    contenttypes: [],
    contentlets: [],
    pagination: EMPTY_PAGINATION,
    searchParams: {
        host: '',
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
        const $isContentTypesView = computed(
            () => store.currentView() === DotUVEPaletteListView.CONTENT_TYPES
        );
        const $isContentletsView = computed(
            () => store.currentView() === DotUVEPaletteListView.CONTENTLETS
        );
        const $isFavoritesList = computed(
            () => params.listType() === DotUVEPaletteListTypes.FAVORITES
        );
        const $isListLayout = computed(() => store.layoutMode() === 'list');

        return {
            $isFavoritesList,
            $isContentletsView,
            $isContentTypesView,
            $isLoading: computed(() => store.status() === DotPaletteListStatus.LOADING),
            $isEmpty: computed(() => store.status() === DotPaletteListStatus.EMPTY),
            $showListLayout: computed(() => $isListLayout() || $isContentletsView()),
            $currentSort: computed(() => ({
                orderby: params.orderby(),
                direction: params.direction()
            }))
        };
    }),
    withMethods((store) => {
        const pageContentTypeService = inject(DotPageContentTypeService);
        const dotESContentService = inject(DotESContentService);
        const dotFavoriteContentTypeService = inject(DotFavoriteContentTypeService);

        const getData = () => {
            const { listType, ...params } = store.searchParams();

            // Page-agnostic list types (e.g. Content Drive "New" menu): fetch content types of
            // the mapped base types from the global endpoint (no page context), with server-side
            // pagination so it scales on large instances.
            const baseTypes = LIST_TYPE_BASE_TYPES[listType];
            if (baseTypes) {
                const request = pageContentTypeService.getAllContentTypes({
                    ...params,
                    types: baseTypes,
                    per_page: DEFAULT_PER_PAGE
                });

                // Favorite Page (system, CONTENT base type) leaks into the Content / All lists and
                // can't be excluded server-side. Drop it from the visible page; when browsing (no
                // filter) it's guaranteed present, so adjust the total by one to keep counts right.
                if (!LIST_TYPES_WITH_FAVORITE_PAGE.has(listType)) {
                    return request;
                }

                return request.pipe(
                    map(({ contenttypes, pagination }) => ({
                        contenttypes: contenttypes.filter(
                            (contentType) => !isFavoritePageContentType(contentType.variable)
                        ),
                        pagination: params.filter
                            ? pagination
                            : {
                                  ...pagination,
                                  totalEntries: Math.max(0, pagination.totalEntries - 1)
                              }
                    }))
                );
            }

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
                            buildPaletteFavorite({
                                contentTypes,
                                filter: params.filter || '',
                                page: params.page,
                                allowedContentTypes: params.allowedContentTypes
                            })
                        )
                    );
                default:
                    // Unhandled list type — return an empty response so every path returns.
                    return of(EMPTY_CONTENTTYPE_RESPONSE);
            }
        };

        return {
            setLayoutMode(layoutMode: DotPaletteViewMode) {
                patchState(store, { layoutMode });
            },
            getContentTypes(params: Partial<DotPaletteSearchParams> = {}) {
                patchState(store, {
                    searchParams: {
                        ...store.searchParams(),
                        ...params,
                        selectedContentType: '' // Ensure we're in content types view
                    },
                    status: DotPaletteListStatus.LOADING,
                    currentView: DotUVEPaletteListView.CONTENT_TYPES
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
                            status: getPaletteState(contenttypes)
                        });
                    });
            },
            getContentlets(params: Partial<DotPaletteSearchParams> = {}) {
                const searchParams = { ...store.searchParams(), ...params };
                const esParams = buildESContentParams(searchParams);
                const offset = Number(esParams.offset);
                patchState(store, {
                    searchParams,
                    status: DotPaletteListStatus.LOADING,
                    currentView: DotUVEPaletteListView.CONTENTLETS
                });

                dotESContentService
                    .get(esParams)
                    .pipe(
                        map((response) => buildPaletteContent(response, offset)),
                        catchError((error) => {
                            console.error(
                                `[DotUVEPalette Store]: Error data fetching contentlets: ${error}`
                            );
                            return of(EMPTY_CONTENTLET_RESPONSE);
                        })
                    )
                    .subscribe((response) => patchState(store, { ...response }));
            }
        };
    }),
    withMethods((store) => {
        const params = store.searchParams;
        const dotFavoriteContentTypeService = inject(DotFavoriteContentTypeService);
        const updateFavoriteState = (contentTypes: DotCMSContentType[]) => {
            const response = buildPaletteFavorite({
                contentTypes,
                filter: params.filter(),
                page: params.page() || 1,
                // allowedContentTypes is optional, so read it from the snapshot (no deep signal).
                allowedContentTypes: params().allowedContentTypes
            });
            patchState(store, response);
        };

        const $isFavoritesList = computed(
            () => params.listType() === DotUVEPaletteListTypes.FAVORITES
        );

        return {
            /**
             * Manually sets the loading status of the palette.
             * Used when transitioning states or showing loading indicators before data fetches.
             *
             * @param status - The status to set (LOADING, LOADED, or EMPTY)
             */
            setStatus(status: DotPaletteListStatus) {
                patchState(store, { status });
            },
            /**
             * Sets the content types from a favorite list.
             * Used when updating the favorite state of a content type.
             *
             * @param contentTypes - The content types to set
             */
            setContentTypesFromFavorite(contentTypes: DotCMSContentType[]) {
                updateFavoriteState(contentTypes);
            },
            /**
             * Adds a content type to the favorite list.
             * If the list is a favorites list, updates the favorite state.
             *
             * @param contentType - The content type to add
             */
            addFavorite(contentType: DotCMSContentType) {
                const response = dotFavoriteContentTypeService.add(contentType);

                // If the list is a favorites list, update the favorite state
                if ($isFavoritesList()) {
                    updateFavoriteState(response);
                }
            },
            /**
             * Removes a content type from the favorite list.
             * If the list is a favorites list, updates the favorite state.
             *
             * @param contentTypeId - The content type ID to remove
             */
            removeFavorite(contentTypeId: string) {
                const response = dotFavoriteContentTypeService.remove(contentTypeId);

                // If the list is a favorites list, update the favorite state
                if ($isFavoritesList()) {
                    updateFavoriteState(response);
                }
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
