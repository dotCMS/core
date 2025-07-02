import { describe } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { mockSites } from '@dotcms/dotcms-js';
import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { QueryBuilder } from '@dotcms/query-builder';

import { DotContentDriveStore } from './dot-content-drive.store';

import { mockItems } from '../shared/mocks';
import { DotContentDriveStatus, SYSTEM_HOST } from '../shared/models';

describe('DotContentDriveStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotContentDriveStore>>;
    let store: InstanceType<typeof DotContentDriveStore>;

    const createService = createServiceFactory({
        service: DotContentDriveStore,
        providers: []
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should have the correct initial state', () => {
            expect(store.currentSite()).toEqual(SYSTEM_HOST);
            expect(store.path()).toBe('');
            expect(store.filters()).toEqual({});
            expect(store.items()).toEqual([]);
            expect(store.status()).toBe(DotContentDriveStatus.LOADING);
        });
    });

    describe('Computed Properties', () => {
        describe('$query', () => {
            it('should build base query when no path or filters are provided', () => {
                const baseQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
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
                    filters: {}
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
                const customSite = mockSites[0];

                store.initContentDrive({
                    currentSite: customSite,
                    path: '',
                    filters: {}
                });

                const expectedQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
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
                    path: '',
                    filters
                });

                const expectedQuery = new QueryBuilder()
                    .raw('+systemType:false -contentType:forms -contentType:Host +deleted:false')
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
                    filters: testFilters
                });

                expect(store.currentSite()).toEqual(testSite);
                expect(store.path()).toBe(testPath);
                expect(store.filters()).toEqual(testFilters);
                expect(store.status()).toBe(DotContentDriveStatus.LOADING);
            });
        });

        describe('setItems', () => {
            it('should update items and set status to LOADED', () => {
                store.setItems(mockItems);

                expect(store.items()).toEqual(mockItems);
                expect(store.status()).toBe(DotContentDriveStatus.LOADED);
            });

            it('should update items with empty array', () => {
                // First set some items
                store.setItems(mockItems);
                expect(store.items()).toEqual(mockItems);

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
    });
});
