import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { DotContentSearchService, DotFolderService } from '@dotcms/data-access';
import { DotContentDriveItem, SiteEntity } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveStore } from './dot-content-drive.store';

import {
    BASE_QUERY,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    SYSTEM_HOST
} from '../shared/constants';
import { MOCK_ITEMS, MOCK_SEARCH_RESPONSE, MOCK_SITES } from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';

describe('DotContentDriveStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {}
                }
            }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(SYSTEM_HOST)
            }),
            mockProvider(DotContentSearchService),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should have the correct initial state', () => {
            expect(store.currentSite()).toEqual(undefined);
            expect(store.path()).toBe(DEFAULT_PATH);
            expect(store.filters()).toEqual({});
            expect(store.items()).toEqual([]);
            expect(store.selectedItems()).toEqual([]);
            expect(store.status()).toBe(DotContentDriveStatus.LOADING);
            expect(store.isTreeExpanded()).toBe(DEFAULT_TREE_EXPANDED);
            expect(store.sort()).toEqual(DEFAULT_SORT);
        });
    });

    describe('Computed Properties', () => {
        describe('$query', () => {
            it('should build base query when no path or filters are provided', () => {
                const baseQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .raw(`+conhost:${SYSTEM_HOST.identifier} +working:true +variant:default`)
                    .build();

                expect(store.$searchParams().query).toEqual(baseQuery);
            });

            it('should include path in query when provided', () => {
                const testPath = '/test/path/';
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: testPath,
                    filters: {},
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw(BASE_QUERY)
                    .field('parentPath')
                    .equals(testPath)
                    .raw(
                        `+(conhost:${SYSTEM_HOST.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
                    )
                    .build();

                expect(store.$searchParams().query).toEqual(expectedQuery);
            });

            it('should include custom site in query when provided', () => {
                const customSite = MOCK_SITES[0] as SiteEntity;

                store.initContentDrive({
                    currentSite: customSite,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw(BASE_QUERY)
                    .raw(
                        `+(conhost:${customSite.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
                    )
                    .build();

                expect(store.$searchParams().query).toEqual(expectedQuery);
            });

            it('should include filters in query when provided', () => {
                const filters = {
                    contentType: ['Blog'],
                    status: 'published'
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw(BASE_QUERY)
                    .raw(
                        `+(conhost:${SYSTEM_HOST.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
                    )
                    .field('contentType')
                    .equals('Blog')
                    .field('status')
                    .equals('published')
                    .build();

                expect(store.$searchParams().query).toEqual(expectedQuery);
            });

            it('should include title filter in query when provided', () => {
                const filters = {
                    title: 'Blog'
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw(BASE_QUERY)
                    .raw(
                        `+(conhost:${SYSTEM_HOST.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
                    )
                    .raw(`+catchall:*Blog* title_dotraw:*Blog*^5 title:'Blog'^15 title:Blog^5`)
                    .build();

                expect(store.$searchParams().query).toEqual(expectedQuery);
            });

            it('should include title filter in query when provided with multiple words', () => {
                const filters = {
                    title: 'Blog Post'
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw(BASE_QUERY)
                    .raw(
                        `+(conhost:${SYSTEM_HOST.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
                    )
                    .raw(
                        `+catchall:*Blog Post* title_dotraw:*Blog Post*^5 title:'Blog Post'^15 title:Blog^5 title:Post^5`
                    )
                    .build();

                expect(store.$searchParams().query).toEqual(expectedQuery);
            });
        });
    });

    describe('Methods', () => {
        describe('initContentDrive', () => {
            it('should update state with provided values and set status to LOADING', () => {
                const testSite = MOCK_SITES[0];
                const testPath = '/some/path';
                const testFilters = { contentType: ['Blog'] };

                store.initContentDrive({
                    currentSite: testSite,
                    path: testPath,
                    filters: testFilters,
                    isTreeExpanded: true
                });

                expect(store.currentSite()).toEqual(testSite);
                expect(store.path()).toBe(testPath);
                expect(store.filters()).toEqual(testFilters);
                expect(store.status()).toBe(DotContentDriveStatus.LOADING);
                expect(store.isTreeExpanded()).toBe(true);
            });
        });

        describe('setItems', () => {
            it('should update items and set status to LOADED', () => {
                store.setItems(MOCK_ITEMS, MOCK_ITEMS.length);

                expect(store.items()).toEqual(MOCK_ITEMS);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);
            });

            it('should update items with empty array', () => {
                // First set some items
                store.setItems(MOCK_ITEMS, MOCK_ITEMS.length);
                expect(store.items()).toEqual(MOCK_ITEMS);

                // Then clear them
                const emptyItems: DotContentDriveItem[] = [];
                store.setItems(emptyItems, emptyItems.length);

                expect(store.items()).toEqual(emptyItems);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);
            });
        });

        describe('setStatus', () => {
            it('should update status to LOADING', () => {
                // First set to something else
                store.setStatus(DotContentDriveStatus.LOADED);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);

                // Then set to LOADING
                store.setStatus(DotContentDriveStatus.LOADING);
                expect(store.status()).toBe(DotContentDriveStatus.LOADING);
            });

            it('should update status to ERROR', () => {
                store.setStatus(DotContentDriveStatus.ERROR);
                expect(store.status()).toBe(DotContentDriveStatus.ERROR);
            });
        });

        describe('patchFilters', () => {
            it('should update filters with provided values', () => {
                store.patchFilters({ contentType: ['Blog'] });
                expect(store.filters()).toEqual({ contentType: ['Blog'] });
            });

            it('should remove filter if value is undefined', () => {
                store.patchFilters({ contentType: ['Blog'] });
                expect(store.filters()).toEqual({ contentType: ['Blog'] });

                store.patchFilters({ contentType: undefined });
                expect(store.filters()).toEqual({});
            });

            it('should update filters and reset pagination offset', () => {
                store.setPagination({ limit: 10, offset: 10 });
                expect(store.pagination()).toEqual({ limit: 10, offset: 10 });

                store.patchFilters({ contentType: ['Blog'] });
                expect(store.pagination()).toEqual({ limit: 10, offset: 0 });
                expect(store.filters()).toEqual({ contentType: ['Blog'] });
            });
        });

        describe('setPagination', () => {
            it('should update pagination with provided values', () => {
                store.setPagination({ limit: 10, offset: 0 });
                expect(store.pagination()).toEqual({ limit: 10, offset: 0 });
            });
        });

        describe('setSort', () => {
            it('should update sort with provided values', () => {
                store.setSort({ field: 'modDate', order: DotContentDriveSortOrder.ASC });
                expect(store.sort()).toEqual({
                    field: 'modDate',
                    order: DotContentDriveSortOrder.ASC
                });
            });
        });

        describe('setSelectedItems', () => {
            it('should set selected items', () => {
                const selectedItems = [MOCK_ITEMS[0], MOCK_ITEMS[1]];

                store.setSelectedItems(selectedItems);

                expect(store.selectedItems()).toEqual(selectedItems);
                expect(store.selectedItems().length).toBe(2);
            });

            it('should replace existing selected items', () => {
                // First set some items
                const firstSelection = [MOCK_ITEMS[0]];
                store.setSelectedItems(firstSelection);
                expect(store.selectedItems()).toEqual(firstSelection);

                // Then replace with new selection
                const secondSelection = [MOCK_ITEMS[1], MOCK_ITEMS[2]];
                store.setSelectedItems(secondSelection);

                expect(store.selectedItems()).toEqual(secondSelection);
                expect(store.selectedItems().length).toBe(2);
            });

            it('should clear selected items when passed empty array', () => {
                // First set some items
                store.setSelectedItems([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
                expect(store.selectedItems().length).toBe(2);

                // Then clear
                store.setSelectedItems([]);

                expect(store.selectedItems()).toEqual([]);
                expect(store.selectedItems().length).toBe(0);
            });

            it('should handle single item selection', () => {
                const selectedItem = [MOCK_ITEMS[0]];

                store.setSelectedItems(selectedItem);

                expect(store.selectedItems()).toEqual(selectedItem);
                expect(store.selectedItems().length).toBe(1);
            });
        });

        describe('updateSelectedItems', () => {
            it('should add items to existing selection', () => {
                // First set initial selection
                store.setSelectedItems([MOCK_ITEMS[0]]);
                expect(store.selectedItems().length).toBe(1);

                // Then update with additional items
                store.updateSelectedItems([MOCK_ITEMS[1], MOCK_ITEMS[2]]);

                expect(store.selectedItems().length).toBe(3);
                expect(store.selectedItems()).toEqual([
                    MOCK_ITEMS[0],
                    MOCK_ITEMS[1],
                    MOCK_ITEMS[2]
                ]);
            });

            it('should add to empty selection', () => {
                expect(store.selectedItems()).toEqual([]);

                store.updateSelectedItems([MOCK_ITEMS[0]]);

                expect(store.selectedItems().length).toBe(1);
                expect(store.selectedItems()).toEqual([MOCK_ITEMS[0]]);
            });

            it('should handle adding empty array', () => {
                store.setSelectedItems([MOCK_ITEMS[0]]);
                expect(store.selectedItems().length).toBe(1);

                store.updateSelectedItems([]);

                expect(store.selectedItems().length).toBe(1);
                expect(store.selectedItems()).toEqual([MOCK_ITEMS[0]]);
            });

            it('should preserve order when adding items', () => {
                store.setSelectedItems([MOCK_ITEMS[0]]);
                store.updateSelectedItems([MOCK_ITEMS[1]]);
                store.updateSelectedItems([MOCK_ITEMS[2]]);

                expect(store.selectedItems()).toEqual([
                    MOCK_ITEMS[0],
                    MOCK_ITEMS[1],
                    MOCK_ITEMS[2]
                ]);
            });
        });
    });
});
describe('DotContentDriveStore - onInit', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {
                        path: '/initial/test/path',
                        filters: 'contentType:InitialTestContentType',
                        isTreeExpanded: 'true'
                    }
                }
            }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(MOCK_SITES[2])
            }),
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should initialize with provided values', () => {
        spectator.flushEffects();

        expect(store.path()).toBe('/initial/test/path');
        expect(store.filters()).toEqual({
            contentType: ['InitialTestContentType']
        });
        expect(store.isTreeExpanded()).toBe(true);
        expect(store.currentSite()).toBe(MOCK_SITES[2]);
    });
});

describe('DotContentDriveStore - Content Loading Effect', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;
    let contentSearchService: jest.Mocked<DotContentSearchService>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {}
                }
            }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(MOCK_SITES[0])
            }),
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        contentSearchService = spectator.inject(DotContentSearchService);
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should fetch content when store has a non-SYSTEM_HOST site', () => {
        spectator.flushEffects();

        expect(contentSearchService.get).toHaveBeenCalled();
        expect(store.items()).toEqual(MOCK_ITEMS);
        expect(store.totalItems()).toBe(MOCK_ITEMS.length);
        expect(store.status()).toBe(DotContentDriveStatus.LOADED);
    });

    it('should clear selected items when loading items', () => {
        // Set some selected items
        store.setSelectedItems([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
        expect(store.selectedItems().length).toBe(2);

        // Trigger loadItems by flushing effects
        spectator.flushEffects();

        // Selected items should be cleared
        expect(store.selectedItems()).toEqual([]);
        expect(store.selectedItems().length).toBe(0);
    });

    it('should handle errors from content search service', () => {
        // Mock error from content search
        contentSearchService.get.mockReturnValue(
            throwError(() => new Error('Failed to get content'))
        );

        spectator.flushEffects();

        expect(store.status()).toBe(DotContentDriveStatus.ERROR);
    });

    it('should handle sorting', () => {
        // Set sort in store
        store.setSort({ field: 'baseType', order: DotContentDriveSortOrder.DESC });

        spectator.flushEffects();

        expect(contentSearchService.get).toHaveBeenCalledWith({
            query: expect.any(String),
            limit: DEFAULT_PAGINATION.limit,
            offset: DEFAULT_PAGINATION.offset,
            sort: 'baseType desc'
        });
    });

    it('should handle sorting with score when query includes title', () => {
        // Set sort in store
        store.patchFilters({ title: 'test' });

        spectator.service.loadItems();

        expect(contentSearchService.get).toHaveBeenCalledWith({
            query: expect.any(String),
            limit: DEFAULT_PAGINATION.limit,
            offset: DEFAULT_PAGINATION.offset,
            sort: 'score,modDate desc'
        });
    });

    it('should handle pagination', () => {
        // Set pagination in store
        store.setPagination({ limit: 10, offset: 0 });

        spectator.flushEffects();

        expect(contentSearchService.get).toHaveBeenCalledWith({
            query: expect.any(String),
            limit: 10,
            offset: 0,
            sort: expect.any(String)
        });
    });
});
