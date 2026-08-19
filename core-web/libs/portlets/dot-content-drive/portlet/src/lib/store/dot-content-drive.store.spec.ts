import { describe, expect } from '@jest/globals';
import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@openng/spectator/jest';
import { NEVER, of, Subject, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import {
    AddToBundleService,
    PushPublishService,
    DotContentDriveService,
    DotCurrentUserService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotPropertiesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    DotAjaxActionResponseView,
    DotContentDriveItem,
    DotContentDriveSearchResponse,
    DotCurrentUser,
    DotFireDefaultActionResult,
    DotSite,
    DotWorkflowPushPublishValue
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { createFakeTagField, createFakeTextField } from '@dotcms/utils-testing';

import { DotContentDriveStore } from './dot-content-drive.store';

import {
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED,
    SYSTEM_HOST
} from '../shared/constants';
import { MOCK_ITEMS, MOCK_SEARCH_RESPONSE, MOCK_SITES } from '../shared/mocks';
import {
    DotContentDriveFilters,
    DotContentDriveSortOrder,
    DotContentDriveStatus
} from '../shared/models';

describe('DotContentDriveStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;
    /** Feeds the store's one-shot current-user fetch; re-created per test so emissions don't leak. */
    let currentUser$: Subject<DotCurrentUser>;

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
            // Fetched once on init to resolve the CMS Administrator role. Answers through a subject
            // rather than a fixed `of(...)` so a test can control *when* — the store subscribes
            // during construction, and "hasn't answered yet" is a case the flag has to get right.
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn(() => currentUser$)
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            // Required by `withActionExecution`, which fires workflow actions from the store.
            mockProvider(DotWorkflowActionsFireService),
            // Also required by `withActionExecution`, which fires Add to Bundle from the store.
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService),
            mockProvider(DotHttpErrorManagerService),
            // The store subscribes to Location (popstate re-hydration); capture the handler here.
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            // withFlags fetches feature flags on init; stub so no real HTTP fires.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        // Assigned before the store is built: `onInit` subscribes straight away.
        currentUser$ = new Subject<DotCurrentUser>();
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

    describe('currentUserIsAdmin', () => {
        it('should start false, before the request has answered', () => {
            // The unresolved case. Nothing waits on the flag, so consumers read this default — the
            // non-admin behaviour, i.e. the Unlock row keeps warning. Over-warning is the safe way
            // to fail here; the copy already says a foreign lock *may* be refused.
            expect(store.currentUserIsAdmin()).toBe(false);
        });

        it('should resolve to true for a CMS Administrator', () => {
            currentUser$.next({ admin: true } as DotCurrentUser);

            expect(store.currentUserIsAdmin()).toBe(true);
        });

        it('should resolve to false for a non-administrator', () => {
            currentUser$.next({ admin: false } as DotCurrentUser);

            expect(store.currentUserIsAdmin()).toBe(false);
        });

        it('should stay false when the response carries no body', () => {
            // `catchError` sits upstream of `subscribe`, so it only covers observable errors.
            // Destructuring the response inside the subscriber would throw on a 204, a proxy that
            // strips the body, or a session-expired gateway returning no JSON — an unhandled error
            // during store init, for a flag that is explicitly non-essential.
            expect(() => currentUser$.next(null as unknown as DotCurrentUser)).not.toThrow();
            expect(store.currentUserIsAdmin()).toBe(false);
        });

        it('should stay false when the request fails', () => {
            // A portlet that cannot answer "is this an admin?" should still work: the role only
            // softens a warning, so a failure is swallowed rather than surfaced.
            currentUser$.error(new HttpErrorResponse({ status: 500 }));

            expect(store.currentUserIsAdmin()).toBe(false);
        });

        it('should not re-fetch the current user as the store changes', () => {
            // The role is fixed for the session, so state changes that re-run the store's effects
            // must not re-request it. Measured as a delta rather than an absolute count: the spy is
            // shared by the factory, so it carries calls from earlier tests.
            const { getCurrentUser } = spectator.inject(DotCurrentUserService);
            const callsAfterInit = getCurrentUser.mock.calls.length;

            store.initContentDrive({
                currentSite: SYSTEM_HOST,
                path: DEFAULT_PATH,
                filters: {},
                isTreeExpanded: false
            });
            store.setPath('/some/other/path/');

            expect(getCurrentUser.mock.calls.length).toBe(callsAfterInit);
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

            it('should set showFolders to false when a field filter is active', () => {
                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters: { 'us.body': 'hello' },
                    isTreeExpanded: false
                });
                store.setUserSearchableFields([createFakeTextField({ variable: 'body' })]);

                const request = store.$request();

                expect(request.userSearchable).toEqual({ body: 'hello' });
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

                // `DotContentDriveFilters` forbids undefined values, so this input is outside the
                // declared contract — hence the cast. Kept as-is because it pins what happens when
                // a caller builds one anyway: the key survives holding `undefined`, which `toEqual`
                // reports as absent. `removeFilter` is the API that actually deletes a key.
                store.patchFilters({ contentType: undefined } as unknown as DotContentDriveFilters);
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
            // The store resolves the CMS Administrator role on init; stub it so no real HTTP fires.
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false } as DotCurrentUser))
            }),
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            // Required by `withActionExecution`, which fires workflow actions from the store.
            mockProvider(DotWorkflowActionsFireService),
            // Also required by `withActionExecution`, which fires Add to Bundle from the store.
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService),
            mockProvider(DotHttpErrorManagerService),
            // The store subscribes to Location (popstate re-hydration); capture the handler here.
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            // withFlags fetches feature flags on init; stub so no real HTTP fires.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
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

describe('DotContentDriveStore - Browser Back/Forward (popstate) re-hydration', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, { snapshot: { queryParams: {} } }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(MOCK_SITES[0])
            }),
            // The store resolves the CMS Administrator role on init; stub it so no real HTTP fires.
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false } as DotCurrentUser))
            }),
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            // Required by `withActionExecution`, which fires workflow actions from the store.
            mockProvider(DotWorkflowActionsFireService),
            // Also required by `withActionExecution`, which fires Add to Bundle from the store.
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService),
            mockProvider(DotHttpErrorManagerService),
            // withFlags fetches feature flags on init; stub so no real HTTP fires.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient()
        ]
    });

    /** Invokes the popstate handler the store registered in onInit with the given restored URL. */
    const popstate = (url: string) => {
        const subscribe = spectator.inject(Location).subscribe as jest.Mock;
        const handler = subscribe.mock.lastCall?.[0] as (event: { url: string }) => void;
        handler({ url });
    };

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        spectator.flushEffects();
    });

    it('re-hydrates the store when Back changes the filters param (fixes the stale-list bug)', () => {
        popstate('/c/content-drive?filters=contentType:Blog');

        expect(store.filters()).toEqual({ contentType: ['Blog'] });
        // Reset to LOADING is what the search effect turns into a fresh load.
        expect(store.status()).toBe(DotContentDriveStatus.LOADING);
    });

    it('re-hydrates the store when Back changes the path param', () => {
        popstate('/c/content-drive?path=/foo/bar');

        expect(store.path()).toBe('/foo/bar');
    });

    it('re-hydrates the tree-expanded preference from the URL on Back', () => {
        store.initContentDrive({
            currentSite: MOCK_SITES[0],
            path: DEFAULT_PATH,
            filters: {},
            isTreeExpanded: true
        });

        popstate('/c/content-drive?isTreeExpanded=false');

        expect(store.isTreeExpanded()).toBe(false);
    });

    it('does NOT re-hydrate when only the editContent param changed (closing the panel via Back)', () => {
        store.initContentDrive({
            currentSite: MOCK_SITES[0],
            path: '/keep',
            filters: { contentType: ['Blog'] },
            isTreeExpanded: true
        });
        const initSpy = jest.spyOn(store, 'initContentDrive');

        // Same browsing params — only editContent differs (here, absent). Must be a no-op so the
        // list isn't reset/reloaded just because the side panel closed.
        popstate('/c/content-drive?path=/keep&filters=contentType:Blog&isTreeExpanded=true');

        expect(initSpy).not.toHaveBeenCalled();
        expect(store.path()).toBe('/keep');
        expect(store.filters()).toEqual({ contentType: ['Blog'] });
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
            // The store resolves the CMS Administrator role on init; stub it so no real HTTP fires.
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false } as DotCurrentUser))
            }),
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            // Required by `withActionExecution`, which fires workflow actions from the store.
            mockProvider(DotWorkflowActionsFireService),
            // Also required by `withActionExecution`, which fires Add to Bundle from the store.
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService),
            mockProvider(DotHttpErrorManagerService),
            // The store subscribes to Location (popstate re-hydration); capture the handler here.
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            // withFlags fetches feature flags on init; stub so no real HTTP fires.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        contentDriveService = spectator.inject(DotContentDriveService);
        // Reset the shared ActivatedRoute mock so a test that seeds queryParams doesn't leak
        // into the next (the mock's snapshot object is created once by the factory).
        (
            spectator.inject(ActivatedRoute).snapshot as { queryParams: Record<string, string> }
        ).queryParams = {};
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

    it('should defer the search while a restored us.* filter has no field metadata yet', () => {
        // Cold URL restore: a us.* value is present but the field metadata hasn't loaded.
        // Drive loadItems() directly (the init effect would overwrite state from empty queryParams).
        store.initContentDrive({
            currentSite: MOCK_SITES[0],
            path: DEFAULT_PATH,
            filters: { 'us.body': 'hello' },
            isTreeExpanded: false
        });

        store.loadItems();

        // No search yet — searching now would drop the us.* value from the payload.
        expect(contentDriveService.search).not.toHaveBeenCalled();

        // Once the field metadata arrives, the search fires with the value shaped in.
        store.setUserSearchableFields([createFakeTextField({ variable: 'body' })]);
        store.loadItems();

        expect(contentDriveService.search).toHaveBeenCalledWith(
            expect.objectContaining({ userSearchable: { body: 'hello' } })
        );
    });

    it('should release the deferred search once field metadata loads even when the request is unchanged', () => {
        // Cold restore of a us.* key whose field is NOT among the type's searchable fields
        // (removed / flag turned off / tampered URL). The payload builder drops it, so the
        // request is structurally identical before and after the metadata loads. Without a
        // tracked release, the $request dedupe guard would suppress the effect re-run and the
        // portlet would stay stuck in LOADING forever. The search must still fire.
        const route = spectator.inject(ActivatedRoute);
        (route.snapshot as { queryParams: Record<string, string> }).queryParams = {
            filters: 'us.ghost:x'
        };

        // First cycle: init restores the ghost chip, the search is deferred (no metadata yet).
        spectator.flushEffects();
        expect(store.userSearchableActive()).toEqual(['ghost']);
        expect(contentDriveService.search).not.toHaveBeenCalled();

        // Metadata loads but does NOT include 'ghost' → payload stays undefined (request unchanged).
        store.setUserSearchableFields([createFakeTextField({ variable: 'body' })]);
        spectator.flushEffects();

        // The search fires anyway; the ineligible us.* value is simply not sent.
        expect(contentDriveService.search).toHaveBeenCalledTimes(1);
        expect(contentDriveService.search).toHaveBeenCalledWith(
            expect.not.objectContaining({ userSearchable: expect.anything() })
        );
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
            store.setUserSearchableFields([createFakeTextField({ variable: 'title' })]);
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
                createFakeTextField({ variable: 'title' }),
                createFakeTagField({ variable: 'tags' })
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

    describe('Show In List fields', () => {
        it('should set and expose the Show In List fields', () => {
            const fields = [createFakeTextField({ variable: 'summary' })];

            store.setShowInListFields(fields);

            expect(store.showInListFields()).toEqual(fields);
        });

        it('should clear the Show In List fields when field filters are cleared', () => {
            store.setShowInListFields([createFakeTextField({ variable: 'summary' })]);

            store.clearUserSearchableFilters();

            expect(store.showInListFields()).toEqual([]);
        });
    });
});

describe('DotContentDriveStore - withActionExecution', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;
    let fireService: jest.Mocked<DotWorkflowActionsFireService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: [
            mockProvider(ActivatedRoute, { snapshot: { queryParams: {} } }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(MOCK_SITES[0])
            }),
            // The store resolves the CMS Administrator role on init; stub it so no real HTTP fires.
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false } as DotCurrentUser))
            }),
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotWorkflowActionsFireService, {
                fireDefaultAction: jest.fn(),
                bulkFire: jest.fn()
            }),
            // Add to Bundle leaves the workflow path entirely and posts to the legacy bundle servlet.
            mockProvider(AddToBundleService, { addToBundle: jest.fn() }),
            mockProvider(PushPublishService, { pushPublishAssets: jest.fn() }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            // The store subscribes to Location (popstate re-hydration); stub so it is inert here.
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            // withFlags fetches feature flags on init; stub so no real HTTP fires.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        // The provider mocks live in the factory closure, so call counts would otherwise accumulate
        // across tests in this block.
        jest.clearAllMocks();

        spectator = createService();
        store = spectator.service;
        fireService = spectator.inject(
            DotWorkflowActionsFireService
        ) as jest.Mocked<DotWorkflowActionsFireService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;

        fireService.fireDefaultAction.mockReturnValue(
            of({ results: [], summary: { affected: 2, successCount: 2, failCount: 0, time: 1 } })
        );
        fireService.bulkFire.mockReturnValue(of({ successCount: 2, skippedCount: 0, fails: [] }));
    });

    describe('executeQuickAction', () => {
        it('should publish the running action so the toolbar can report it', () => {
            // Lock, not Publish: Publish is no longer a quick action — it belongs to the
            // Workflow Actions section, where it resolves through the scheme's mapping.
            // Never settles, so the in-flight state is observable.
            fireService.fireDefaultAction.mockReturnValue(NEVER);

            store.executeQuickAction('LOCK', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecution()).toEqual({ actionName: 'Lock', total: 2 });
        });

        it('should fire the default action with the given inodes', () => {
            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);

            expect(fireService.fireDefaultAction).toHaveBeenCalledWith({
                action: 'LOCK',
                inodes: ['inode-1']
            });
        });

        it('should report the counts the endpoint returned, not the number of inodes sent', () => {
            // Per-item failures are an expected outcome (a lock held by another user, a permission
            // the row state cannot see), so the result has to reflect what the server actually did.
            fireService.fireDefaultAction.mockReturnValue(
                of({
                    results: [],
                    summary: { affected: 2, successCount: 1, failCount: 1, time: 1 }
                })
            );

            store.executeQuickAction('LOCK', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Lock',
                successCount: 1,
                skippedCount: 0,
                failCount: 1
            });
        });

        it('should clear the running action once settled', () => {
            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);

            expect(store.actionExecution()).toBeUndefined();
            expect(store.actionExecutionResult()).toBeDefined();
        });

        it('should not fire when there are no inodes', () => {
            store.executeQuickAction('LOCK', 'Lock', []);

            expect(fireService.fireDefaultAction).not.toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });

        it('should refuse to start a second run while one is in flight', () => {
            // Guards the double-fire the old component-owned flag allowed: closing and reopening the
            // dialog used to reset it, letting the same rows be fired twice.
            fireService.fireDefaultAction.mockReturnValue(NEVER);

            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);
            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);

            expect(fireService.fireDefaultAction).toHaveBeenCalledTimes(1);
        });

        it('should hand errors to the error manager and clear the running action', () => {
            const error = new HttpErrorResponse({ status: 403 });
            fireService.fireDefaultAction.mockReturnValue(throwError(() => error));

            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.actionExecution()).toBeUndefined();
            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it('should not report a success when the response arrives without a summary', () => {
            // The endpoint streams `results` then `summary`, and the writer swallows an IOException
            // mid-stream — so a 200 with no summary is reachable. Counting the inodes sent would
            // report every one of them as succeeded, which is the most reassuring possible message
            // for the case where nothing is known to have succeeded.
            fireService.fireDefaultAction.mockReturnValue(
                of({ results: [] } as unknown as DotFireDefaultActionResult)
            );

            store.executeQuickAction('LOCK', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()).toBeUndefined();
            expect(store.actionExecution()).toBeUndefined();
            expect(httpErrorManager.handle).toHaveBeenCalled();
        });

        it('should still report a zeroed summary the endpoint actually sent', () => {
            // A real `successCount: 0` is a fact, not a missing field, so it goes to the toast.
            fireService.fireDefaultAction.mockReturnValue(
                of({
                    results: [],
                    summary: { affected: 2, successCount: 0, failCount: 2, time: 1 }
                })
            );

            store.executeQuickAction('LOCK', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Lock',
                successCount: 0,
                skippedCount: 0,
                failCount: 2
            });
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
        });
    });

    describe('executeWorkflowAction', () => {
        it('should fire the bulk request with the given contentlet ids', () => {
            store.executeWorkflowAction('action-review', 'Send for Review', ['inode-1', 'inode-2']);

            expect(fireService.bulkFire).toHaveBeenCalledWith(
                expect.objectContaining({
                    workflowActionId: 'action-review',
                    contentletIds: ['inode-1', 'inode-2']
                })
            );
        });

        it('should carry skipped items through to the result', () => {
            // A mixed-type selection partially skips by design: contentlets whose scheme does not own
            // the action are skipped server-side.
            fireService.bulkFire.mockReturnValue(
                of({ successCount: 1, skippedCount: 1, fails: [] })
            );

            store.executeWorkflowAction('action-review', 'Send for Review', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Send for Review',
                successCount: 1,
                skippedCount: 1,
                failCount: 0
            });
        });

        it('should count per-item failures from the fails list', () => {
            fireService.bulkFire.mockReturnValue(
                of({
                    successCount: 1,
                    skippedCount: 0,
                    fails: [{ inode: 'inode-2', errorMessage: 'locked' }]
                })
            );

            store.executeWorkflowAction('action-review', 'Send for Review', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()?.failCount).toBe(1);
        });

        it('should hand errors to the error manager and clear the running action', () => {
            fireService.bulkFire.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            store.executeWorkflowAction('action-review', 'Send for Review', ['inode-1']);

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });
    });

    describe('executeAddToBundle', () => {
        const BUNDLE = { id: 'bundle-1', name: 'Release 1' };
        let addToBundleService: SpyObject<AddToBundleService>;

        beforeEach(() => {
            addToBundleService = spectator.inject(AddToBundleService);
            addToBundleService.addToBundle.mockReturnValue(
                // `_body` is required on `DotAjaxActionResponseView` — the raw legacy AJAX
                // payload, which nothing in this flow reads.
                of({ _body: null, total: 2, errors: 0, errorMessages: [], bundleId: 'bundle-1' })
            );
        });

        it('should post the identifiers comma-joined', () => {
            // The servlet splits `assetIdentifier` on "," and has always accepted several ids that
            // way, which is why bulk needs no new endpoint.
            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1', 'id-2']);

            expect(addToBundleService.addToBundle).toHaveBeenCalledWith('id-1,id-2', BUNDLE);
        });

        it('should report the server count of assets queued, not the number sent', () => {
            // The server dedupes by identifier and drops anything already in the bundle, so `total`
            // can be lower than what was posted. Reporting the input would overstate the result.
            addToBundleService.addToBundle.mockReturnValue(
                of({ _body: null, total: 1, errors: 0, errorMessages: [], bundleId: 'bundle-1' })
            );

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1', 'id-2']);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Add to Bundle',
                successCount: 1,
                skippedCount: 0,
                failCount: 0
            });
        });

        it('should split failures out of the total', () => {
            addToBundleService.addToBundle.mockReturnValue(
                of({
                    _body: null,
                    total: 3,
                    errors: 1,
                    errorMessages: ['nope'],
                    bundleId: 'bundle-1'
                })
            );

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1', 'id-2', 'id-3']);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Add to Bundle',
                successCount: 2,
                skippedCount: 0,
                failCount: 1
            });
        });

        it('should never report a negative success count', () => {
            // Defends the subtraction: `errors` exceeding `total` would otherwise read as "-1 added".
            addToBundleService.addToBundle.mockReturnValue(
                of({ _body: null, total: 1, errors: 3, errorMessages: [], bundleId: 'bundle-1' })
            );

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1']);

            expect(store.actionExecutionResult()?.successCount).toBe(0);
        });

        it('should mark the run in progress while it is in flight', () => {
            addToBundleService.addToBundle.mockReturnValue(NEVER);

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1', 'id-2']);

            expect(store.actionExecution()).toEqual({ actionName: 'Add to Bundle', total: 2 });
        });

        it('should refuse a second run while one is in flight', () => {
            addToBundleService.addToBundle.mockReturnValue(NEVER);
            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1']);

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-2']);

            expect(addToBundleService.addToBundle).toHaveBeenCalledTimes(1);
        });

        it('should hand errors to the error manager and clear the running action', () => {
            addToBundleService.addToBundle.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            store.executeAddToBundle('Add to Bundle', BUNDLE, ['id-1']);

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });
    });

    describe('executePushPublish', () => {
        /** In the shape `DotWorkflowPushPublishComponent` emits — already split for the servlet. */
        const SETTINGS: DotWorkflowPushPublishValue = {
            whereToSend: 'env-1,env-2',
            iWantTo: 'publish',
            publishDate: '2026-09-01',
            publishTime: '10-00',
            expireDate: '2026-10-01',
            expireTime: '23-59',
            filterKey: 'default',
            timezoneId: 'America/Costa_Rica'
        };
        let pushPublishService: SpyObject<PushPublishService>;

        beforeEach(() => {
            pushPublishService = spectator.inject(PushPublishService);
            pushPublishService.pushPublishAssets.mockReturnValue(
                of({ _body: null, total: 2, errors: 0, errorMessages: [], bundleId: 'bundle-1' })
            );
        });

        it('should post the identifiers comma-joined', () => {
            // `RemotePublishAjaxAction` splits `assetIdentifier` on "," — bulk needs no new endpoint.
            store.executePushPublish('Push Publish', ['id-1', 'id-2'], SETTINGS);

            expect(pushPublishService.pushPublishAssets).toHaveBeenCalledWith(
                'id-1,id-2',
                SETTINGS
            );
        });

        it('should report the server count, not the number sent', () => {
            pushPublishService.pushPublishAssets.mockReturnValue(
                of({ _body: null, total: 1, errors: 0, errorMessages: [], bundleId: 'bundle-1' })
            );

            store.executePushPublish('Push Publish', ['id-1', 'id-2'], SETTINGS);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Push Publish',
                successCount: 1,
                skippedCount: 0,
                failCount: 0
            });
        });

        it('should split failures out of the total', () => {
            pushPublishService.pushPublishAssets.mockReturnValue(
                of({ _body: null, total: 3, errors: 1, errorMessages: ['nope'], bundleId: 'bundle-1' })
            );

            store.executePushPublish('Push Publish', ['id-1', 'id-2', 'id-3'], SETTINGS);

            expect(store.actionExecutionResult()).toEqual({
                actionName: 'Push Publish',
                successCount: 2,
                skippedCount: 0,
                failCount: 1
            });
        });

        it('should never report a negative success count', () => {
            // Defends the subtraction: `errors` exceeding `total` would read as "-2 pushed".
            pushPublishService.pushPublishAssets.mockReturnValue(
                of({ _body: null, total: 1, errors: 3, errorMessages: [], bundleId: 'bundle-1' })
            );

            store.executePushPublish('Push Publish', ['id-1'], SETTINGS);

            expect(store.actionExecutionResult()?.successCount).toBe(0);
        });

        it('should treat a string `errors` as a failure, not a success', () => {
            // The servlet answers 200 for its own failures, writing `{"errors": "<message>"}` with no
            // `total`. Reported as a result it would produce `NaN` successes on a push that never
            // happened. This guard is the reason the push cannot reuse `bulkFire`.
            pushPublishService.pushPublishAssets.mockReturnValue(
                of({ errors: 'Publisher unreachable' } as unknown as DotAjaxActionResponseView)
            );

            store.executePushPublish('Push Publish', ['id-1'], SETTINGS);

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
            expect(store.actionExecution()).toBeUndefined();
        });

        it('should treat a missing `errors` as a failure, not a success', () => {
            // The other shape the servlet can produce: no body at all when the publisher returns
            // nothing. Zero of everything on a push that may well have worked is not a result.
            pushPublishService.pushPublishAssets.mockReturnValue(
                of(undefined as unknown as DotAjaxActionResponseView)
            );

            store.executePushPublish('Push Publish', ['id-1'], SETTINGS);

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it('should mark the run in progress while it is in flight', () => {
            pushPublishService.pushPublishAssets.mockReturnValue(NEVER);

            store.executePushPublish('Push Publish', ['id-1', 'id-2'], SETTINGS);

            expect(store.actionExecution()).toEqual({ actionName: 'Push Publish', total: 2 });
        });

        it('should refuse a second run while one is in flight', () => {
            pushPublishService.pushPublishAssets.mockReturnValue(NEVER);
            store.executePushPublish('Push Publish', ['id-1'], SETTINGS);

            store.executePushPublish('Push Publish', ['id-2'], SETTINGS);

            expect(pushPublishService.pushPublishAssets).toHaveBeenCalledTimes(1);
        });

        it('should do nothing without identifiers', () => {
            store.executePushPublish('Push Publish', [], SETTINGS);

            expect(pushPublishService.pushPublishAssets).not.toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });

        it('should hand transport errors to the error manager and clear the running action', () => {
            pushPublishService.pushPublishAssets.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            store.executePushPublish('Push Publish', ['id-1'], SETTINGS);

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });
    });

    describe('clearActionExecutionResult', () => {
        it('should drop the result once it has been presented', () => {
            store.executeQuickAction('LOCK', 'Lock', ['inode-1']);
            expect(store.actionExecutionResult()).toBeDefined();

            store.clearActionExecutionResult();

            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });
});
