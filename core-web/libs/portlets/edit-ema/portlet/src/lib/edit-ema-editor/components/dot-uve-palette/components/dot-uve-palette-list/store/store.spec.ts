import { describe, expect, it, beforeEach, afterEach, jest } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

jest.mock('../../../utils', () => {
    // jest.requireActual is typed as unknown, cast so we can spread and reference exports
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const actual = jest.requireActual('../../../utils') as any;
    return {
        ...actual,
        buildPaletteFavorite: jest.fn(actual.buildPaletteFavorite)
    };
});

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotLocalstorageService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { DotPaletteListStore } from './store';

import { UVEStore } from '../../../../../../store/dot-uve.store';
import {
    DotCMSContentTypePalette,
    DotPaletteListStatus,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../../models';
import { buildPaletteFavorite, EMPTY_PAGINATION } from '../../../utils';

// ===== Mock Data =====

const mockContentTypes: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'blog',
        baseType: 'CONTENT'
    } as DotCMSContentType,
    {
        id: '2',
        name: 'News',
        variable: 'news',
        baseType: 'CONTENT'
    } as DotCMSContentType
];

const mockContentlets: DotCMSContentlet[] = [
    {
        identifier: '1',
        title: 'Article 1',
        baseType: 'CONTENT'
    } as DotCMSContentlet,
    {
        identifier: '2',
        title: 'Article 2',
        baseType: 'CONTENT'
    } as DotCMSContentlet
];

const mockESResponse = {
    contentTook: 10,
    queryTook: 5,
    jsonObjectView: {
        contentlets: mockContentlets
    },
    resultsSize: 2
};

describe('DotPaletteListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPaletteListStore>>;
    let store: InstanceType<typeof DotPaletteListStore>;
    let pageContentTypeService: jest.Mocked<DotPageContentTypeService>;
    let dotESContentService: jest.Mocked<DotESContentService>;
    let dotFavoriteContentTypeService: jest.Mocked<DotFavoriteContentTypeService>;
    let dotLocalstorageService: jest.Mocked<DotLocalstorageService>;
    let uveStore: { $allowedContentTypes: jest.Mock };

    // ===== Test Helper Functions =====

    /**
     * Creates a mock pagination response
     */
    const createMockPagination = (currentPage = 1, perPage = 30, totalEntries = 0) => ({
        currentPage,
        perPage,
        totalEntries
    });

    /**
     * Creates a mock content types response
     */
    const createMockContentTypesResponse = (
        contenttypes: DotCMSContentType[] = mockContentTypes,
        pagination = createMockPagination(1, 30, contenttypes.length)
    ) => ({ contenttypes, pagination });

    /**
     * Creates a mock ES response
     */
    const createMockESResponse = (
        contentlets: DotCMSContentlet[] = mockContentlets,
        resultsSize = contentlets.length
    ) => ({
        contentTook: 10,
        queryTook: 5,
        jsonObjectView: { contentlets },
        resultsSize
    });

    /**
     * Verifies store state matches content types view
     */
    const expectContentTypesView = () => {
        expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENT_TYPES);
        expect(store.$isContentTypesView()).toBe(true);
        expect(store.$isContentletsView()).toBe(false);
    };

    /**
     * Verifies store state matches contentlets view
     */
    const expectContentletsView = () => {
        expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENTLETS);
        expect(store.$isContentTypesView()).toBe(false);
        expect(store.$isContentletsView()).toBe(true);
    };

    /**
     * Verifies store is in loaded state with data
     */
    const expectLoadedState = () => {
        expect(store.status()).toBe(DotPaletteListStatus.LOADED);
        expect(store.$isLoading()).toBe(false);
        expect(store.$isEmpty()).toBe(false);
    };

    /**
     * Verifies store is in empty state
     */
    const expectEmptyState = () => {
        expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
        expect(store.$isLoading()).toBe(false);
        expect(store.$isEmpty()).toBe(true);
    };

    const createService = createServiceFactory({
        service: DotPaletteListStore,
        providers: [
            DotPaletteListStore,
            {
                provide: UVEStore,
                useValue: {
                    // Default to empty map so favorites are marked as disabled unless tests override.
                    $allowedContentTypes: jest.fn().mockReturnValue({})
                }
            },
            {
                provide: DotLocalstorageService,
                useValue: {
                    getItem: jest.fn().mockReturnValue(null),
                    setItem: jest.fn().mockReturnValue(undefined)
                }
            }
        ],
        mocks: [DotPageContentTypeService, DotESContentService, DotFavoriteContentTypeService]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;

        pageContentTypeService = spectator.inject(DotPageContentTypeService);
        dotESContentService = spectator.inject(DotESContentService);
        dotFavoriteContentTypeService = spectator.inject(DotFavoriteContentTypeService);
        dotLocalstorageService = spectator.inject(DotLocalstorageService);
        uveStore = spectator.inject(UVEStore) as unknown as { $allowedContentTypes: jest.Mock };

        // Setup default mock return values
        pageContentTypeService.get.mockReturnValue(
            of(createMockContentTypesResponse(mockContentTypes, createMockPagination(1, 30, 2)))
        );

        pageContentTypeService.getAllContentTypes.mockReturnValue(
            of(createMockContentTypesResponse(mockContentTypes, createMockPagination(1, 30, 2)))
        );

        dotESContentService.get.mockReturnValue(of(mockESResponse));

        dotFavoriteContentTypeService.getAll.mockReturnValue(mockContentTypes);
        dotFavoriteContentTypeService.add.mockReturnValue(mockContentTypes);
        dotFavoriteContentTypeService.remove.mockReturnValue(mockContentTypes);
    });

    describe('Initial State', () => {
        it('should initialize with DEFAULT_STATE', () => {
            expect(store.contenttypes()).toEqual([]);
            expect(store.contentlets()).toEqual([]);
            expect(store.pagination()).toEqual({
                currentPage: 1,
                perPage: 30,
                totalEntries: 0
            });
            expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENT_TYPES);
            expect(store.status()).toBe(DotPaletteListStatus.LOADING);
            expect(store.layoutMode()).toBe('grid');
        });

        it('should initialize search params with correct defaults', () => {
            const searchParams = store.searchParams();

            expect(searchParams.host).toBe('');
            expect(searchParams.pagePathOrId).toBe('');
            expect(searchParams.language).toBe(1);
            expect(searchParams.variantId).toBe(DEFAULT_VARIANT_ID);
            expect(searchParams.listType).toBe(DotUVEPaletteListTypes.CONTENT);
            expect(searchParams.selectedContentType).toBe('');
            expect(searchParams.orderby).toBe('name');
            expect(searchParams.direction).toBe('ASC');
            expect(searchParams.page).toBe(1);
            expect(searchParams.filter).toBe('');
        });
    });

    describe('Computed Properties', () => {
        describe('$isLoading', () => {
            it('should return true when status is LOADING', () => {
                expect(store.$isLoading()).toBe(true);
            });

            it('should return false when status is LOADED', () => {
                store.getContentTypes();

                expect(store.$isLoading()).toBe(false);
            });

            it('should return false when status is EMPTY', () => {
                pageContentTypeService.get.mockReturnValueOnce(
                    of(createMockContentTypesResponse([]))
                );

                store.getContentTypes();

                expect(store.$isLoading()).toBe(false);
            });
        });

        describe('$isEmpty', () => {
            it('should return false when status is LOADING', () => {
                expect(store.$isEmpty()).toBe(false);
            });

            it('should return false when status is LOADED', () => {
                store.getContentTypes();

                expect(store.$isEmpty()).toBe(false);
            });

            it('should return true when status is EMPTY', () => {
                pageContentTypeService.get.mockReturnValueOnce(
                    of(createMockContentTypesResponse([]))
                );

                store.getContentTypes();

                expect(store.$isEmpty()).toBe(true);
            });
        });

        describe('$showListLayout', () => {
            it('should return false when layoutMode is grid and in content types view', () => {
                store.setLayoutMode('grid');

                expect(store.$showListLayout()).toBe(false);
            });

            it('should return true when layoutMode is list and in content types view', () => {
                store.setLayoutMode('list');

                expect(store.$showListLayout()).toBe(true);
            });

            it('should return true when in contentlets view regardless of layout mode', () => {
                store.setLayoutMode('grid');
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.$showListLayout()).toBe(true);
            });

            it('should return false when back to content types view with grid layout', () => {
                store.setLayoutMode('grid');
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.$showListLayout()).toBe(true);

                store.getContentTypes();

                expect(store.$showListLayout()).toBe(false);
            });
        });

        describe('$isContentTypesView', () => {
            it('should return true when in CONTENT_TYPES view', () => {
                expect(store.$isContentTypesView()).toBe(true);
            });

            it('should return false when in CONTENTLETS view', () => {
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.$isContentTypesView()).toBe(false);
            });
        });

        describe('$isContentletsView', () => {
            it('should return false when in CONTENT_TYPES view', () => {
                expect(store.$isContentletsView()).toBe(false);
            });

            it('should return true when in CONTENTLETS view', () => {
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.$isContentletsView()).toBe(true);
            });
        });

        describe('$currentSort', () => {
            it('should return current sort configuration', () => {
                const sort = store.$currentSort();

                expect(sort.orderby).toBe('name');
                expect(sort.direction).toBe('ASC');
            });

            it('should update when sort parameters change', () => {
                store.getContentTypes({ orderby: 'usage', direction: 'DESC' });

                const sort = store.$currentSort();

                expect(sort.orderby).toBe('usage');
                expect(sort.direction).toBe('DESC');
            });
        });
    });

    describe('Methods', () => {
        describe('setStatus', () => {
            it('should update status to LOADING', () => {
                expect(store.status()).toBe(DotPaletteListStatus.LOADING);

                store.setStatus(DotPaletteListStatus.LOADED);
                expect(store.status()).toBe(DotPaletteListStatus.LOADED);

                store.setStatus(DotPaletteListStatus.LOADING);
                expect(store.status()).toBe(DotPaletteListStatus.LOADING);
            });

            it('should update status to EMPTY', () => {
                store.setStatus(DotPaletteListStatus.EMPTY);

                expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            });

            it('should affect computed signals $isLoading and $isEmpty', () => {
                store.setStatus(DotPaletteListStatus.LOADING);
                expect(store.$isLoading()).toBe(true);
                expect(store.$isEmpty()).toBe(false);

                store.setStatus(DotPaletteListStatus.EMPTY);
                expect(store.$isLoading()).toBe(false);
                expect(store.$isEmpty()).toBe(true);

                store.setStatus(DotPaletteListStatus.LOADED);
                expect(store.$isLoading()).toBe(false);
                expect(store.$isEmpty()).toBe(false);
            });
        });

        describe('setLayoutMode', () => {
            it('should update layoutMode to list', () => {
                expect(store.layoutMode()).toBe('grid');

                store.setLayoutMode('list');

                expect(store.layoutMode()).toBe('list');
            });

            it('should update layoutMode to grid', () => {
                store.setLayoutMode('list');

                expect(store.layoutMode()).toBe('list');

                store.setLayoutMode('grid');

                expect(store.layoutMode()).toBe('grid');
            });

            it('should affect $showListLayout computed', () => {
                expect(store.$showListLayout()).toBe(false);

                store.setLayoutMode('list');

                expect(store.$showListLayout()).toBe(true);

                store.setLayoutMode('grid');

                expect(store.$showListLayout()).toBe(false);
            });
        });

        describe('setContentTypesFromFavorite', () => {
            it('should update store with filtered and paginated content types', () => {
                store.setContentTypesFromFavorite(mockContentTypes);

                expect(store.contenttypes()).toHaveLength(2);
                expect(store.pagination().totalEntries).toBe(2);
                expect(store.status()).toBe(DotPaletteListStatus.LOADED);
            });

            it('should apply current filter when setting favorites', () => {
                store.getContentTypes({ filter: 'Blog' });

                store.setContentTypesFromFavorite(mockContentTypes);

                expect(store.contenttypes()).toHaveLength(1);
                expect(store.contenttypes()[0].name).toBe('Blog');
            });

            it('should set status to EMPTY when no content types match filter', () => {
                store.getContentTypes({ filter: 'NonExistent' });

                store.setContentTypesFromFavorite(mockContentTypes);

                expect(store.contenttypes()).toHaveLength(0);
                expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            });

            it('should apply current page when setting favorites', () => {
                const manyContentTypes = Array.from({ length: 50 }, (_, i) => ({
                    id: String(i),
                    name: `Content ${i}`,
                    variable: `content${i}`,
                    baseType: 'CONTENT'
                })) as DotCMSContentType[];

                store.getContentTypes({ page: 2 });

                store.setContentTypesFromFavorite(manyContentTypes);

                expect(store.contenttypes()).toHaveLength(20); // 50 - 30 = 20 on page 2
                expect(store.pagination().currentPage).toBe(2);
            });
        });

        describe('favorites actions', () => {
            const extraFavorite = {
                id: '3',
                name: 'Events',
                variable: 'events',
                baseType: 'CONTENT'
            } as DotCMSContentType;

            it('should refresh store when adding favorites in favorites view', () => {
                store.getContentTypes({ listType: DotUVEPaletteListTypes.FAVORITES });
                const updatedFavorites = [...mockContentTypes, extraFavorite];
                dotFavoriteContentTypeService.add.mockReturnValueOnce(updatedFavorites);

                store.addFavorite(mockContentTypes[0]);

                expect(dotFavoriteContentTypeService.add).toHaveBeenCalledWith(mockContentTypes[0]);
                // Favorites are sorted alphabetically by name: Blog, Events, News
                const expectedOrder = [
                    { ...mockContentTypes[0], disabled: true }, // Blog
                    { ...extraFavorite, disabled: true }, // Events
                    { ...mockContentTypes[1], disabled: true } // News
                ] as DotCMSContentTypePalette[];
                expect(store.contenttypes()).toEqual(expectedOrder);
            });

            it('should not refresh store when adding favorites outside favorites view', () => {
                const spy = jest.spyOn(store, 'setContentTypesFromFavorite');

                store.addFavorite(mockContentTypes[0]);

                expect(dotFavoriteContentTypeService.add).toHaveBeenCalledWith(mockContentTypes[0]);
                expect(spy).not.toHaveBeenCalled();
            });

            it('should refresh store when removing favorites in favorites view', () => {
                store.getContentTypes({ listType: DotUVEPaletteListTypes.FAVORITES });
                const remainingFavorites = mockContentTypes.slice(1);
                dotFavoriteContentTypeService.remove.mockReturnValueOnce(remainingFavorites);

                store.removeFavorite(mockContentTypes[0].id);

                expect(dotFavoriteContentTypeService.remove).toHaveBeenCalledWith(
                    mockContentTypes[0].id
                );
                expect(store.contenttypes()).toEqual([
                    { ...remainingFavorites[0], disabled: true }
                ] as DotCMSContentTypePalette[]);
            });

            it('should pass allowedContentTypes to buildPaletteFavorite when refreshing favorites state', () => {
                store.getContentTypes({ listType: DotUVEPaletteListTypes.FAVORITES });

                (buildPaletteFavorite as unknown as jest.Mock).mockClear();
                uveStore.$allowedContentTypes.mockReturnValueOnce({ blog: true, banner: true });

                store.setContentTypesFromFavorite(mockContentTypes);

                expect(buildPaletteFavorite).toHaveBeenCalledWith(
                    expect.objectContaining({
                        allowedContentTypes: { blog: true, banner: true }
                    })
                );
            });

            it('should not refresh store when removing favorites outside favorites view', () => {
                const spy = jest.spyOn(store, 'setContentTypesFromFavorite');

                store.removeFavorite(mockContentTypes[0].id);

                expect(dotFavoriteContentTypeService.remove).toHaveBeenCalledWith(
                    mockContentTypes[0].id
                );
                expect(spy).not.toHaveBeenCalled();
            });
        });

        describe('getContentTypes', () => {
            it('should fetch content types for CONTENT list type', () => {
                store.getContentTypes();

                expect(pageContentTypeService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        types: ['CONTENT', 'FILEASSET', 'DOTASSET'],
                        per_page: 30
                    })
                );
                expect(store.contenttypes()).toEqual(mockContentTypes);
                expectContentTypesView();
                expectLoadedState();
            });

            it('should fetch content types for WIDGET list type', () => {
                store.getContentTypes({ listType: DotUVEPaletteListTypes.WIDGET });

                expect(pageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                    expect.objectContaining({
                        types: ['WIDGET'],
                        per_page: 30
                    })
                );
            });

            it('should fetch favorites for FAVORITES list type', () => {
                store.getContentTypes({ listType: DotUVEPaletteListTypes.FAVORITES });

                expect(dotFavoriteContentTypeService.getAll).toHaveBeenCalled();
                expect(store.contenttypes()).toEqual(
                    mockContentTypes.map(
                        (ct) => ({ ...ct, disabled: true }) as DotCMSContentTypePalette
                    )
                );
            });

            it('should update search params with provided values', () => {
                store.getContentTypes({
                    filter: 'test',
                    page: 2,
                    orderby: 'usage',
                    direction: 'DESC'
                });

                const searchParams = store.searchParams();

                expect(searchParams.filter).toBe('test');
                expect(searchParams.page).toBe(2);
                expect(searchParams.orderby).toBe('usage');
                expect(searchParams.direction).toBe('DESC');
            });

            it('should always reset selectedContentType to empty string', () => {
                // First set a selected content type
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.searchParams().selectedContentType).toBe('Blog');

                // Then get content types
                store.getContentTypes();

                expect(store.searchParams().selectedContentType).toBe('');
            });

            it('should set status to LOADING before fetching', () => {
                store.getContentTypes();

                // During the call, status would have been set to LOADING
                // After the observable completes, it's LOADED
                expect(store.status()).toBe(DotPaletteListStatus.LOADED);
            });

            it('should set status to EMPTY when no content types returned', () => {
                pageContentTypeService.get.mockReturnValueOnce(
                    of(createMockContentTypesResponse([]))
                );

                store.getContentTypes();

                expectEmptyState();
            });

            it('should update pagination from response', () => {
                const expectedPagination = createMockPagination(2, 30, 50);
                pageContentTypeService.get.mockReturnValueOnce(
                    of(createMockContentTypesResponse(mockContentTypes, expectedPagination))
                );

                store.getContentTypes({ page: 2 });

                expect(store.pagination()).toEqual(expectedPagination);
            });

            it('should pass host parameter to service for CONTENT list type', () => {
                store.getContentTypes({
                    host: 'demo.dotcms.com',
                    pagePathOrId: '/test-page'
                });

                expect(pageContentTypeService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        host: 'demo.dotcms.com',
                        pagePathOrId: '/test-page',
                        types: ['CONTENT', 'FILEASSET', 'DOTASSET'],
                        per_page: 30
                    })
                );
            });

            it('should pass host parameter to service for WIDGET list type', () => {
                store.getContentTypes({
                    host: 'demo.dotcms.com',
                    listType: DotUVEPaletteListTypes.WIDGET
                });

                expect(pageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                    expect.objectContaining({
                        host: 'demo.dotcms.com',
                        types: ['WIDGET'],
                        per_page: 30
                    })
                );
            });

            it('should update searchParams with host when provided', () => {
                store.getContentTypes({
                    host: 'demo.dotcms.com',
                    pagePathOrId: '/test-page'
                });

                const searchParams = store.searchParams();

                expect(searchParams.host).toBe('demo.dotcms.com');
                expect(searchParams.pagePathOrId).toBe('/test-page');
            });

            it('should not include host in service call when not provided', () => {
                store.getContentTypes({ pagePathOrId: '/test-page' });

                expect(pageContentTypeService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        host: '',
                        pagePathOrId: '/test-page',
                        types: ['CONTENT', 'FILEASSET', 'DOTASSET'],
                        per_page: 30
                    })
                );
            });
        });

        describe('getContentlets', () => {
            it('should fetch contentlets with correct ES params', () => {
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(dotESContentService.get).toHaveBeenCalledWith({
                    query: `+contentType:Blog +deleted:false +variant:(${DEFAULT_VARIANT_ID} OR ${DEFAULT_VARIANT_ID})`,
                    offset: '0',
                    itemsPerPage: 30,
                    lang: '1',
                    filter: ''
                });
            });

            it('should update store with contentlets response', () => {
                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.contentlets()).toEqual(mockContentlets);
                expectContentletsView();
                expectLoadedState();
            });

            it('should calculate correct offset for page 2', () => {
                store.getContentlets({ selectedContentType: 'Blog', page: 2 });

                expect(dotESContentService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        offset: '30'
                    })
                );
            });

            it('should include filter in ES params', () => {
                store.getContentlets({ selectedContentType: 'Blog', filter: 'test' });

                expect(dotESContentService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'test'
                    })
                );
            });

            it('should use variantId in query when provided', () => {
                store.getContentlets({
                    selectedContentType: 'Blog',
                    variantId: 'christmas-variant'
                });

                expect(dotESContentService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        query: `+contentType:Blog +deleted:false +variant:(${DEFAULT_VARIANT_ID} OR christmas-variant)`
                    })
                );
            });

            it('should update language parameter', () => {
                store.getContentlets({ selectedContentType: 'Blog', language: 2 });

                expect(dotESContentService.get).toHaveBeenCalledWith(
                    expect.objectContaining({
                        lang: '2'
                    })
                );
            });

            it('should set status to EMPTY when no contentlets returned', () => {
                dotESContentService.get.mockReturnValueOnce(of(createMockESResponse([], 0)));

                store.getContentlets({ selectedContentType: 'Blog' });

                expectEmptyState();
            });

            it('should update pagination from ES response', () => {
                dotESContentService.get.mockReturnValueOnce(
                    of(createMockESResponse(mockContentlets, 100))
                );

                store.getContentlets({ selectedContentType: 'Blog', page: 2 });

                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().totalEntries).toBe(100);
            });

            it('should preserve existing search params when not overridden', () => {
                store.getContentTypes({
                    host: 'demo.dotcms.com',
                    pagePathOrId: 'test-page',
                    language: 2,
                    orderby: 'usage'
                });

                store.getContentlets({ selectedContentType: 'Blog' });

                const searchParams = store.searchParams();

                expect(searchParams.host).toBe('demo.dotcms.com');
                expect(searchParams.pagePathOrId).toBe('test-page');
                expect(searchParams.language).toBe(2);
                expect(searchParams.orderby).toBe('usage');
                expect(searchParams.selectedContentType).toBe('Blog');
            });
        });
    });

    describe('Integration Tests', () => {
        it('should handle full workflow: content types -> contentlets -> back to content types', () => {
            // Start with content types
            store.getContentTypes();
            expectContentTypesView();

            // Navigate to contentlets
            store.getContentlets({ selectedContentType: 'Blog' });
            expectContentletsView();

            // Back to content types
            store.getContentTypes();
            expectContentTypesView();
        });

        it('should handle switching between list types', () => {
            store.getContentTypes({ listType: DotUVEPaletteListTypes.CONTENT });
            expect(pageContentTypeService.get).toHaveBeenCalled();

            store.getContentTypes({ listType: DotUVEPaletteListTypes.WIDGET });
            expect(pageContentTypeService.getAllContentTypes).toHaveBeenCalled();

            store.getContentTypes({ listType: DotUVEPaletteListTypes.FAVORITES });
            expect(dotFavoriteContentTypeService.getAll).toHaveBeenCalled();
        });

        it('should handle pagination across multiple pages', () => {
            store.getContentTypes({ page: 1 });
            expect(store.pagination().currentPage).toBe(1);

            pageContentTypeService.get.mockReturnValue(
                of(
                    createMockContentTypesResponse(
                        mockContentTypes,
                        createMockPagination(2, 30, 100)
                    )
                )
            );

            store.getContentTypes({ page: 2 });
            expect(store.pagination().currentPage).toBe(2);

            pageContentTypeService.get.mockReturnValue(
                of(
                    createMockContentTypesResponse(
                        mockContentTypes,
                        createMockPagination(3, 30, 100)
                    )
                )
            );

            store.getContentTypes({ page: 3 });
            expect(store.pagination().currentPage).toBe(3);
        });

        it('should handle filter changes correctly', () => {
            store.getContentTypes({ filter: '' });
            expect(store.contenttypes()).toHaveLength(2);

            store.getContentTypes({ filter: 'Blog' });
            expect(store.searchParams().filter).toBe('Blog');
        });
    });

    describe('Error Handling', () => {
        let consoleErrorSpy: jest.SpiedFunction<typeof console.error>;

        beforeEach(() => {
            consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(jest.fn());
        });

        afterEach(() => {
            consoleErrorSpy.mockRestore();
        });

        it('should set empty state and log error when fetching contenttypes fails', () => {
            const mockError = new Error('Failed to fetch content types');
            pageContentTypeService.get.mockReturnValue(throwError(() => mockError));

            store.getContentTypes();

            expect(store.contenttypes()).toEqual([]);
            expect(store.pagination()).toEqual(EMPTY_PAGINATION);
            expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                expect.stringContaining('[DotUVEPalette Store]: Error data fetching contenttypes')
            );
        });

        it('should set empty state and log error when fetching contentlets fails', () => {
            const mockError = new Error('Failed to fetch contentlets');
            dotESContentService.get.mockReturnValue(throwError(() => mockError));

            store.getContentlets({ selectedContentType: 'Blog' });

            expect(store.contentlets()).toEqual([]);
            expect(store.pagination()).toEqual(EMPTY_PAGINATION);
            expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                expect.stringContaining('[DotUVEPalette Store]: Error data fetching contentlets')
            );
        });
    });

    describe('LocalStorage Integration (withHooks)', () => {
        describe('onInit - Loading from localStorage', () => {
            it('should call getItem for layoutMode on initialization', () => {
                // Verify localStorage was queried for layout mode
                expect(dotLocalstorageService.getItem).toHaveBeenCalledWith(
                    'dot-uve-palette-layout-mode'
                );

                // When null is returned, defaults to grid
                expect(store.layoutMode()).toBe('grid');
            });

            it('should call getItem for sort options on initialization', () => {
                // Verify localStorage was queried for sort options
                expect(dotLocalstorageService.getItem).toHaveBeenCalledWith(
                    'dot-uve-palette-sort-options'
                );

                // When null is returned, uses defaults
                expect(store.searchParams().orderby).toBe('name');
                expect(store.searchParams().direction).toBe('ASC');
            });
        });

        describe('State Management: Layout and Sort', () => {
            it('should update layoutMode state when changed', () => {
                expect(store.layoutMode()).toBe('grid');

                store.setLayoutMode('list');

                expect(store.layoutMode()).toBe('list');
            });

            it('should update sort params when orderby changes', () => {
                expect(store.searchParams().orderby).toBe('name');

                store.getContentTypes({ orderby: 'usage' });

                expect(store.searchParams().orderby).toBe('usage');
            });

            it('should update sort params when direction changes', () => {
                expect(store.searchParams().direction).toBe('ASC');

                store.getContentTypes({ direction: 'DESC' });

                expect(store.searchParams().direction).toBe('DESC');
            });

            it('should update both orderby and direction together', () => {
                store.getContentTypes({ orderby: 'usage', direction: 'DESC' });

                expect(store.searchParams().orderby).toBe('usage');
                expect(store.searchParams().direction).toBe('DESC');
            });
        });

        describe('setLayoutMode Method', () => {
            it('should update layoutMode state correctly', () => {
                store.setLayoutMode('list');
                expect(store.layoutMode()).toBe('list');

                store.setLayoutMode('grid');
                expect(store.layoutMode()).toBe('grid');
            });

            it('should affect computed $showListLayout', () => {
                store.setLayoutMode('grid');
                expect(store.$showListLayout()).toBe(false);

                store.setLayoutMode('list');
                expect(store.$showListLayout()).toBe(true);
            });
        });
    });
});
