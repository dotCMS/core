import { describe } from '@jest/globals';
import { mockProvider } from '@ngneat/spectator';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { ActivatedRoute } from '@angular/router';

import { DotContentDriveItem, SiteEntity } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveStore } from './dot-content-drive.store';

import { DEFAULT_PATH, DEFAULT_TREE_EXPANDED, SYSTEM_HOST } from '../shared/constants';
import { mockItems, mockSites } from '../shared/mocks';
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
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should have the correct initial state', () => {
            expect(store.currentSite()).toEqual(SYSTEM_HOST);
            expect(store.path()).toBe(DEFAULT_PATH);
            expect(store.filters()).toEqual({});
            expect(store.items()).toEqual([]);
            expect(store.status()).toBe(DotContentDriveStatus.LOADING);
            expect(store.isTreeExpanded()).toBe(DEFAULT_TREE_EXPANDED);
        });
    });

    describe('Computed Properties', () => {
        describe('$query', () => {
            it('should build base query when no path or filters are provided', () => {
                const baseQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .field('parentPath')
                    .equals(DEFAULT_PATH)
                    .field('conhost')
                    .equals(SYSTEM_HOST.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier)
                    .build();

                expect(store.$query()).toEqual(baseQuery);
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
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .field('parentPath')
                    .equals(testPath)
                    .field('conhost')
                    .equals(SYSTEM_HOST.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier)
                    .build();

                expect(store.$query()).toEqual(expectedQuery);
            });

            it('should include custom site in query when provided', () => {
                const customSite = mockSites[0] as SiteEntity;

                store.initContentDrive({
                    currentSite: customSite,
                    path: DEFAULT_PATH,
                    filters: {},
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .field('parentPath')
                    .equals(DEFAULT_PATH)
                    .field('conhost')
                    .equals(customSite.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier)
                    .build();

                expect(store.$query()).toEqual(expectedQuery);
            });

            it('should include filters in query when provided', () => {
                const filters = {
                    contentType: 'Blog',
                    status: 'published'
                };

                store.initContentDrive({
                    currentSite: SYSTEM_HOST,
                    path: DEFAULT_PATH,
                    filters,
                    isTreeExpanded: false
                });

                const expectedQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .field('parentPath')
                    .equals(DEFAULT_PATH)
                    .field('conhost')
                    .equals(SYSTEM_HOST.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier)
                    .field('contentType')
                    .equals('Blog')
                    .field('status')
                    .equals('published')
                    .build();

                expect(store.$query()).toEqual(expectedQuery);
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
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
                    .field('parentPath')
                    .equals(DEFAULT_PATH)
                    .field('conhost')
                    .equals(SYSTEM_HOST.identifier)
                    .or()
                    .equals(SYSTEM_HOST.identifier)
                    .field('title_dotraw')
                    .equals('*Blog*')
                    .build();

                expect(store.$query()).toEqual(expectedQuery);
            });
        });
    });

    describe('Methods', () => {
        describe('initContentDrive', () => {
            it('should update state with provided values and set status to LOADING', () => {
                const testSite = mockSites[0];
                const testPath = '/some/path';
                const testFilters = { contentType: 'Blog' };

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
                store.setItems(mockItems, mockItems.length);

                expect(store.items()).toEqual(mockItems);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);
            });

            it('should update items with empty array', () => {
                // First set some items
                store.setItems(mockItems, mockItems.length);
                expect(store.items()).toEqual(mockItems);

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

        describe('setFilters', () => {
            it('should update filters with provided values', () => {
                store.setFilters({ contentType: 'Blog' });
                expect(store.filters()).toEqual({ contentType: 'Blog' });
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
                siteDetails: jest.fn().mockReturnValue(mockSites[2])
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should use default path if not provided in query params', () => {
        expect(store.path()).toBe('/initial/test/path');
        expect(store.filters()).toEqual({
            contentType: 'InitialTestContentType'
        });
        expect(store.isTreeExpanded()).toBe(true);
        expect(store.currentSite()).toEqual(mockSites[2]);
    });
});
