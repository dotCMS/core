import { describe, expect, it, beforeEach, afterEach, jest } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotLocalstorageService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { DotPaletteListStore } from './store';

import {
    DotPaletteListStatus,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../../models';

describe('DotPaletteListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPaletteListStore>>;
    let store: InstanceType<typeof DotPaletteListStore>;
    let pageContentTypeService: jest.Mocked<DotPageContentTypeService>;
    let dotESContentService: jest.Mocked<DotESContentService>;
    let dotFavoriteContentTypeService: jest.Mocked<DotFavoriteContentTypeService>;
    let dotLocalstorageService: jest.Mocked<DotLocalstorageService>;

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

    const createService = createServiceFactory({
        service: DotPaletteListStore,
        providers: [
            DotPaletteListStore,
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

        // Setup default mock return values
        pageContentTypeService.get.mockReturnValue(
            of({
                contenttypes: mockContentTypes,
                pagination: {
                    currentPage: 1,
                    perPage: 30,
                    totalEntries: 2
                }
            })
        );

        pageContentTypeService.getAllContentTypes.mockReturnValue(
            of({
                contenttypes: mockContentTypes,
                pagination: {
                    currentPage: 1,
                    perPage: 30,
                    totalEntries: 2
                }
            })
        );

        dotESContentService.get.mockReturnValue(of(mockESResponse));

        dotFavoriteContentTypeService.getAll.mockReturnValue(mockContentTypes);
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
                    of({
                        contenttypes: [],
                        pagination: {
                            currentPage: 1,
                            perPage: 30,
                            totalEntries: 0
                        }
                    })
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
                    of({
                        contenttypes: [],
                        pagination: {
                            currentPage: 1,
                            perPage: 30,
                            totalEntries: 0
                        }
                    })
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
                expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENT_TYPES);
                expect(store.status()).toBe(DotPaletteListStatus.LOADED);
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
                expect(store.contenttypes()).toEqual(mockContentTypes);
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
                    of({
                        contenttypes: [],
                        pagination: {
                            currentPage: 1,
                            perPage: 30,
                            totalEntries: 0
                        }
                    })
                );

                store.getContentTypes();

                expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            });

            it('should update pagination from response', () => {
                pageContentTypeService.get.mockReturnValueOnce(
                    of({
                        contenttypes: mockContentTypes,
                        pagination: {
                            currentPage: 2,
                            perPage: 30,
                            totalEntries: 50
                        }
                    })
                );

                store.getContentTypes({ page: 2 });

                expect(store.pagination()).toEqual({
                    currentPage: 2,
                    perPage: 30,
                    totalEntries: 50
                });
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
                expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENTLETS);
                expect(store.status()).toBe(DotPaletteListStatus.LOADED);
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
                dotESContentService.get.mockReturnValueOnce(
                    of({
                        contentTook: 10,
                        queryTook: 5,
                        jsonObjectView: {
                            contentlets: []
                        },
                        resultsSize: 0
                    })
                );

                store.getContentlets({ selectedContentType: 'Blog' });

                expect(store.status()).toBe(DotPaletteListStatus.EMPTY);
            });

            it('should update pagination from ES response', () => {
                dotESContentService.get.mockReturnValueOnce(
                    of({
                        contentTook: 10,
                        queryTook: 5,
                        jsonObjectView: {
                            contentlets: mockContentlets
                        },
                        resultsSize: 100
                    })
                );

                store.getContentlets({ selectedContentType: 'Blog', page: 2 });

                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().totalEntries).toBe(100);
            });

            it('should preserve existing search params when not overridden', () => {
                store.getContentTypes({
                    pagePathOrId: 'test-page',
                    language: 2,
                    orderby: 'usage'
                });

                store.getContentlets({ selectedContentType: 'Blog' });

                const searchParams = store.searchParams();

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
            expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENT_TYPES);
            expect(store.$isContentTypesView()).toBe(true);

            // Navigate to contentlets
            store.getContentlets({ selectedContentType: 'Blog' });
            expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENTLETS);
            expect(store.$isContentletsView()).toBe(true);

            // Back to content types
            store.getContentTypes();
            expect(store.currentView()).toBe(DotUVEPaletteListView.CONTENT_TYPES);
            expect(store.$isContentTypesView()).toBe(true);
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
                of({
                    contenttypes: mockContentTypes,
                    pagination: {
                        currentPage: 2,
                        perPage: 30,
                        totalEntries: 100
                    }
                })
            );

            store.getContentTypes({ page: 2 });
            expect(store.pagination().currentPage).toBe(2);

            pageContentTypeService.get.mockReturnValue(
                of({
                    contenttypes: mockContentTypes,
                    pagination: {
                        currentPage: 3,
                        perPage: 30,
                        totalEntries: 100
                    }
                })
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
            expect(store.pagination()).toEqual({
                currentPage: 0,
                perPage: 0,
                totalEntries: 0
            });
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
            expect(store.pagination()).toEqual({
                currentPage: 0,
                perPage: 0,
                totalEntries: 0
            });
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
