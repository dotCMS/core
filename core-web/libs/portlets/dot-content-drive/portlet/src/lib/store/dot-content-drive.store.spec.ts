import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { DotContentDriveService, DotFolderService } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotContentDriveItem,
    DotContentDriveSearchResponse,
    DotSite
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveStore } from './dot-content-drive.store';

import {
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
            mockProvider(DotContentDriveService),
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
        describe('$request', () => {
            it('should build request with default values when no path or filters are provided', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.assetPath).toBe(`//${SYSTEM_HOST.hostname}/`);
                expect(request.includeSystemHost).toBe(true);
                expect(request.filters).toEqual({
                    text: '',
                    filterFolders: true
                });
                expect(request.language).toBeUndefined();
                expect(request.contentTypes).toBeUndefined();
                expect(request.baseTypes).toBeUndefined();
                expect(request.contentCursor).toBe(0);
                expect(request.folderCursor).toBe(0);
                expect(request.maxResults).toBe(DEFAULT_PAGINATION.limit);
                expect(request.sortBy).toBe(`${DEFAULT_SORT.field}:${DEFAULT_SORT.order}`);
                expect(request.archived).toBe(false);
                expect(request.showFolders).toBe(true);
            });

            it('should include path in assetPath when provided', () => {
                const testPath = '/test/path/';
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: testPath,
                    filters: {},
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.assetPath).toBe(`//${SYSTEM_HOST.hostname}${testPath}`);
            });

            it('should include custom site hostname in assetPath when provided', () => {
                const customSite = MOCK_SITES[0] as DotSite;
                store.initContentDrive({
                    currentSite: customSite,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.assetPath).toBe(`//${customSite.hostname}/`);
            });

            it('should include title filter in request when provided', () => {
                const filters = {
                    title: 'Blog Post'
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.filters?.text).toBe('Blog Post');
            });

            it('should include contentTypes in request when provided', () => {
                const filters = {
                    contentType: ['Blog', 'News']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.contentTypes).toEqual(['Blog', 'News']);
                expect(request.showFolders).toBe(false);
            });

            it('should include baseTypes in request when provided', () => {
                const filters = {
                    baseType: ['1', '2'] // CONTENT and WIDGET
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.baseTypes).toEqual(['CONTENT', 'WIDGET']);
                expect(request.showFolders).toBe(false);
            });

            it('should include languageId in request when provided', () => {
                const filters = {
                    languageId: ['en']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.language).toEqual(['en']);
                expect(request.showFolders).toBe(false);
            });

            it('should map the workflow filter tokens into request.workflow entries', () => {
                const filters = {
                    workflow: ['a:a2', 'b']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.workflow).toEqual([{ scheme: 'a', step: 'a2' }, { scheme: 'b' }]);
            });

            it('should leave request.workflow undefined when no workflow filter is provided', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.workflow).toBeUndefined();
            });

            it('should include pagination in request', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });
                store.setPagination({ limit: 50, page: 1, offset: 0 });

                const request = store.$request();

                expect(request.maxResults).toBe(50);
            });

            it('should include sort in request', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });
                store.setSort({ field: 'title', order: DotContentDriveSortOrder.ASC });

                const request = store.$request();

                expect(request.sortBy).toBe('title:asc');
            });

            it('should set showFolders to false when contentType filter is provided', () => {
                const filters = {
                    contentType: ['Blog']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.showFolders).toBe(false);
            });

            it('should set showFolders to false when baseType filter is provided', () => {
                const filters = {
                    baseType: ['1']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.showFolders).toBe(false);
            });

            it('should set showFolders to false when languageId filter is provided', () => {
                const filters = {
                    languageId: ['en']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.showFolders).toBe(false);
            });

            it('should set showFolders to false when workflow filter is provided', () => {
                const filters = {
                    workflow: ['a']
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.showFolders).toBe(false);
            });

            it('should set showFolders to true when no filters are provided', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const request = store.$request();

                expect(request.showFolders).toBe(true);
            });

            it('should handle multiple filters together', () => {
                const filters = {
                    title: 'Test',
                    contentType: ['Blog'],
                    baseType: ['1'],
                    languageId: ['en']
                };

                store.initContentDrive({
                    currentSite: MOCK_SITES[0],
                    path: '/documents/',
                    filters,
                    isTreeExpanded: false
                });
                store.setPagination({ limit: 30, page: 1, offset: 0 });
                store.setSort({ field: 'modDate', order: DotContentDriveSortOrder.DESC });

                const request = store.$request();

                expect(request.assetPath).toBe(`//${MOCK_SITES[0].hostname}/documents/`);
                expect(request.filters?.text).toBe('Test');
                expect(request.contentTypes).toEqual(['Blog']);
                expect(request.baseTypes).toEqual(['CONTENT']);
                expect(request.language).toEqual(['en']);
                expect(request.maxResults).toBe(30);
                expect(request.sortBy).toBe('modDate:desc');
                expect(request.showFolders).toBe(false);
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
                store.setItems(MOCK_ITEMS);

                expect(store.items()).toEqual(MOCK_ITEMS);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);
            });

            it('should update items with empty array', () => {
                // First set some items
                store.setItems(MOCK_ITEMS);
                expect(store.items()).toEqual(MOCK_ITEMS);

                // Then clear them
                const emptyItems: DotContentDriveItem[] = [];
                store.setItems(emptyItems);

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

        describe('setGlobalSearch', () => {
            it('should update filters with title search value', () => {
                store.setGlobalSearch('test search');
                expect(store.filters()).toEqual({ title: 'test search' });
            });

            it('should preserve other filters when setting a search value', () => {
                store.patchFilters({ contentType: ['Blog'], baseType: ['1'] });

                store.setGlobalSearch('test search');

                expect(store.filters()).toEqual({
                    contentType: ['Blog'],
                    baseType: ['1'],
                    title: 'test search'
                });
            });

            it('should preserve other filters when search is empty', () => {
                store.patchFilters({ contentType: ['Blog'] });
                expect(store.filters()).toEqual({ contentType: ['Blog'] });

                store.setGlobalSearch('');
                expect(store.filters()).toEqual({ contentType: ['Blog'] });
            });

            it('should reset pagination offset when setting global search', () => {
                store.setPagination({ limit: 20, page: 2, offset: 20 });
                expect(store.pagination()).toEqual({ limit: 20, page: 2, offset: 20 });

                store.setGlobalSearch('test');
                expect(store.pagination()).toEqual({ limit: 20, page: 1, offset: 0 });
            });

            it('should reset path to DEFAULT_PATH when setting global search', () => {
                store.setPath('/some/custom/path');
                expect(store.path()).toBe('/some/custom/path');

                store.setGlobalSearch('test');
                expect(store.path()).toBe(DEFAULT_PATH);
            });
        });

        describe('clearFilters', () => {
            it('should remove every filter', () => {
                store.patchFilters({ contentType: ['Blog'], baseType: ['1'] });
                store.setGlobalSearch('hello');

                store.clearFilters();

                expect(store.filters()).toEqual({});
            });

            it('should reset pagination when clearing filters', () => {
                store.setPagination({ limit: 20, page: 3, offset: 40 });

                store.clearFilters();

                expect(store.pagination()).toEqual({ limit: 20, page: 1, offset: 0 });
            });
        });

        describe('removeFilter', () => {
            it('should remove the specified filter', () => {
                store.patchFilters({ contentType: ['Blog'], baseType: ['1'] });
                expect(store.filters()).toEqual({ contentType: ['Blog'], baseType: ['1'] });

                store.removeFilter('contentType');
                expect(store.filters()).toEqual({ baseType: ['1'] });
            });

            it('should reset pagination offset when removing filter', () => {
                store.patchFilters({ contentType: ['Blog'] });
                store.setPagination({ limit: 20, page: 2, offset: 20 });
                expect(store.pagination()).toEqual({ limit: 20, page: 2, offset: 20 });

                store.removeFilter('contentType');
                expect(store.pagination()).toEqual({ limit: 20, page: 1, offset: 0 });
            });

            it('should not change state if filter does not exist', () => {
                const initialFilters = { contentType: ['Blog'] };
                store.patchFilters(initialFilters);
                store.setPagination({ limit: 20, page: 2, offset: 20 });

                store.removeFilter('nonExistentFilter');

                expect(store.filters()).toEqual(initialFilters);
                expect(store.pagination()).toEqual({ limit: 20, page: 2, offset: 20 });
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
                store.setPagination({ limit: 20, page: 2, offset: 20 });
                expect(store.pagination()).toEqual({ limit: 20, page: 2, offset: 20 });

                store.patchFilters({ contentType: ['Blog'] });
                expect(store.pagination()).toEqual({ limit: 20, page: 1, offset: 0 });
                expect(store.filters()).toEqual({ contentType: ['Blog'] });
            });
        });

        describe('setPagination', () => {
            it('should update pagination with provided values', () => {
                store.setPagination({ limit: 10, page: 1, offset: 0 });
                expect(store.pagination()).toEqual({ limit: 10, page: 1, offset: 0 });
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

        describe('setPath', () => {
            it('should reset pagination offset when setting path', () => {
                store.initContentDrive({
                    currentSite: MOCK_SITES[0],
                    path: '/test/',
                    filters: {},
                    isTreeExpanded: false
                });
                store.setPagination({ limit: 20, page: 3, offset: 40 });
                expect(store.pagination()).toEqual({ limit: 20, page: 3, offset: 40 });

                store.setPath('/documents/');

                expect(store.path()).toBe('/documents/');
                expect(store.pagination()).toEqual({ limit: 20, page: 1, offset: 0 });
            });

            it('should update path', () => {
                store.initContentDrive({
                    currentSite: MOCK_SITES[0],
                    path: '/test/',
                    filters: {},
                    isTreeExpanded: false
                });

                store.setPath('/new/path/');

                expect(store.path()).toBe('/new/path/');
            });

            it('should not touch filters when changing path', () => {
                store.patchFilters({ contentType: ['Blog'] });
                store.setGlobalSearch('hello');
                expect(store.filters()).toEqual({ contentType: ['Blog'], title: 'hello' });

                store.setPath('/documents/');

                expect(store.filters()).toEqual({ contentType: ['Blog'], title: 'hello' });
            });

            it('should leave filters empty when entering a folder with no filters set', () => {
                store.setPath('/some/folder/');

                expect(store.filters()).toEqual({});
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
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
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
    let contentDriveService: jest.Mocked<DotContentDriveService>;

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
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
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
        contentDriveService = spectator.inject(DotContentDriveService);
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should fetch content when store has a non-SYSTEM_HOST site', () => {
        spectator.flushEffects();

        expect(contentDriveService.search).toHaveBeenCalled();
        expect(store.items()).toEqual(MOCK_ITEMS);
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

    it('should handle errors from content drive service', () => {
        // Mock error from content drive service
        contentDriveService.search.mockReturnValue(
            throwError(() => new Error('Failed to get content'))
        );

        spectator.flushEffects();

        expect(store.status()).toBe(DotContentDriveStatus.ERROR);
    });

    it('should handle sorting', () => {
        // Set sort in store
        store.setSort({ field: 'baseType', order: DotContentDriveSortOrder.DESC });

        spectator.flushEffects();

        expect(contentDriveService.search).toHaveBeenCalledWith(
            expect.objectContaining({
                sortBy: 'baseType:desc'
            })
        );
    });

    it('should handle title filter in request', () => {
        // Set title filter
        store.patchFilters({ title: 'test' });

        spectator.service.loadItems();

        expect(contentDriveService.search).toHaveBeenCalledWith(
            expect.objectContaining({
                filters: expect.objectContaining({
                    text: 'test'
                })
            })
        );
    });

    it('should handle pagination', () => {
        // Set pagination in store
        store.setPagination({ limit: 10, page: 1, offset: 0 });

        spectator.service.loadItems();

        expect(contentDriveService.search).toHaveBeenCalledWith(
            expect.objectContaining({
                maxResults: 10
            })
        );
    });

    it('should refresh hasMore flags from an empty result that matches an existing page', () => {
        // An empty result returns cursors 0,0 — which match DEFAULT_PAGE (the initial page,
        // optimistically hasMoreContent: true). The matched page's flags must be refreshed
        // from the response so the paginator does not offer a next page on zero items.
        contentDriveService.search.mockReturnValue(
            of({
                list: [],
                contentTotalCount: 0,
                folderCount: 0,
                contentCount: 0,
                hasMoreContent: false,
                hasMoreFolders: false,
                nextContentCursor: 0,
                nextFolderCursor: 0
            } as unknown as DotContentDriveSearchResponse)
        );

        spectator.service.loadItems();

        expect(store.items()).toEqual([]);
        expect(store.pages()).toHaveLength(1);
        const lastPage = store.pages().at(-1);
        expect(lastPage?.hasMoreContent).toBe(false);
        expect(lastPage?.hasMoreFolders).toBe(false);
    });

    describe('User-searchable field filters', () => {
        const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
            ({
                variable: 'aField',
                fieldType: 'Text',
                dataType: 'TEXT',
                values: '',
                ...overrides
            }) as DotCMSContentTypeField;

        it('should add a chip to the active list without touching the filter bag', () => {
            store.addUserSearchableField('title');

            expect(store.userSearchableActive()).toEqual(['title']);
            // No us.* entry until it has a value — so the search request is unchanged.
            expect(store.filters()['us.title']).toBeUndefined();
        });

        it('should not add the same field twice', () => {
            store.addUserSearchableField('title');
            store.addUserSearchableField('title');

            expect(store.userSearchableActive()).toEqual(['title']);
        });

        it('should clear all field filters, the active list and the cached fields', () => {
            store.setUserSearchableFields([field({ variable: 'title' })]);
            store.addUserSearchableField('title');
            store.patchFilters({ 'us.title': 'review', baseType: ['1'] });

            store.clearUserSearchableFilters();

            expect(store.userSearchableActive()).toEqual([]);
            expect(store.userSearchableFields()).toEqual([]);
            expect(store.filters()['us.title']).toBeUndefined();
            // Non us.* filters are preserved.
            expect(store.filters()['baseType']).toEqual(['1']);
        });

        it('should reshape us.* values into the userSearchable payload by field type', () => {
            store.initContentDrive({
                currentSite: MOCK_SITES[0],
                path: DEFAULT_PATH,
                filters: {},
                isTreeExpanded: false
            });
            store.setUserSearchableFields([
                field({ variable: 'title', fieldType: 'Text' }),
                field({ variable: 'tags', fieldType: 'Tag' })
            ]);
            store.patchFilters({ 'us.title': 'review', 'us.tags': 'angular,cms' });

            expect(store.$request().userSearchable).toEqual({
                title: 'review',
                tags: ['angular', 'cms']
            });
        });

        it('should restore the active list from us.* keys in the URL filters on init', () => {
            store.initContentDrive({
                currentSite: MOCK_SITES[0],
                path: DEFAULT_PATH,
                filters: { 'us.title': 'review', 'us.tags': 'angular', contentType: ['Blog'] },
                isTreeExpanded: false
            });

            expect(store.userSearchableActive()).toEqual(['title', 'tags']);
        });
    });
});
