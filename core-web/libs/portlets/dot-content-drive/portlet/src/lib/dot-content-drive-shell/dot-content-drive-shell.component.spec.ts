import { it, describe, expect, beforeEach } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DotContentSearchService, DotSiteService } from '@dotcms/data-access';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { DEFAULT_PAGINATION, DEFAULT_PATH, SYSTEM_HOST } from '../shared/constants';
import { mockItems, mockRoute, mockSearchResponse, mockSites } from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let contentSearchService: jest.Mocked<DotContentSearchService>;
    let activatedRoute: SpyObject<ActivatedRoute>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        providers: [
            GlobalStore,
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(mockSites[0]))
            }),
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(mockSearchResponse))
            }),
            mockProvider(ActivatedRoute, mockRoute),
            provideHttpClient()
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    currentSite: jest.fn(),
                    treeExpanded: jest.fn().mockReturnValue(true),
                    $query: jest.fn(),
                    items: jest.fn().mockReturnValue(mockItems),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setTreeExpanded: jest.fn(),
                    path: jest.fn().mockReturnValue('/test/path'),
                    filters: jest.fn().mockReturnValue({}),
                    status: jest.fn().mockReturnValue(DotContentDriveStatus.LOADING),
                    sort: jest
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    totalItems: jest.fn().mockReturnValue(mockItems.length),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    setFilters: jest.fn()
                })
            ]
        });
        contentSearchService = spectator.inject(DotContentSearchService);
        activatedRoute = spectator.inject(ActivatedRoute);
        store = spectator.inject(DotContentDriveStore, true);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Initialization', () => {
        it('should initialize the store with current site and route params', () => {
            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith({
                currentSite: mockSites[0],
                path: '/test/path',
                filters: {
                    contentType: 'Blog',
                    status: 'published'
                },
                treeExpanded: true
            });
        });

        it('should use default path if not provided in query params', () => {
            Object.defineProperty(activatedRoute, 'snapshot', {
                get: jest.fn().mockReturnValue({
                    queryParams: {
                        filters: 'contentType:Blog',
                        treeExpanded: 'false'
                    }
                })
            });

            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith(
                expect.objectContaining({
                    currentSite: mockSites[0],
                    path: DEFAULT_PATH,
                    filters: {
                        contentType: 'Blog'
                    },
                    treeExpanded: false
                })
            );
        });

        it('should use empty object for filters if not provided in query params', () => {
            Object.defineProperty(activatedRoute, 'snapshot', {
                get: jest.fn().mockReturnValue({
                    queryParams: {
                        path: '/test/path/'
                    }
                })
            });

            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith(
                expect.objectContaining({
                    filters: {},
                    path: '/test/path/',
                    treeExpanded: true
                })
            );
        });
    });

    describe('Content Loading Effect', () => {
        beforeEach(() => {
            jest.restoreAllMocks();
        });

        it('should fetch content when store has a non-SYSTEM_HOST site', () => {
            // Setup store mock to simulate the effect running
            store.currentSite.mockReturnValue(mockSites[0]);
            store.$query.mockReturnValue('+testField:testValue');

            spectator.detectChanges();

            expect(contentSearchService.get).toHaveBeenCalledWith({
                query: '+testField:testValue',
                limit: DEFAULT_PAGINATION.limit,
                offset: DEFAULT_PAGINATION.offset,
                sort: 'score,modDate asc'
            });

            expect(store.setItems).toHaveBeenCalledWith(mockItems, mockItems.length);
        });

        it('should not fetch content when current site is SYSTEM_HOST', () => {
            // Setup store to simulate the effect with SYSTEM_HOST
            store.currentSite.mockReturnValue(SYSTEM_HOST);

            spectator.detectChanges();

            expect(contentSearchService.get).not.toHaveBeenCalled();
        });

        it('should handle errors from content search service', () => {
            // Setup store mock
            store.currentSite.mockReturnValue(mockSites[0]);
            store.$query.mockReturnValue('test query');

            // Mock error from content search
            jest.spyOn(contentSearchService, 'get').mockReturnValue(
                throwError(() => new Error('Failed to get content'))
            );

            spectator.detectChanges();

            expect(store.setStatus).toHaveBeenCalledWith(DotContentDriveStatus.ERROR);
            expect(store.setItems).not.toHaveBeenCalled();
        });

        it('should handle sorting', () => {
            // Setup store mock
            store.currentSite.mockReturnValue(mockSites[1]);
            store.sort.mockReturnValue({ field: 'baseType', order: DotContentDriveSortOrder.DESC });
            store.$query.mockReturnValue('+testField:testValue');
            spectator.detectChanges();

            expect(contentSearchService.get).toHaveBeenCalledWith({
                query: '+testField:testValue',
                limit: DEFAULT_PAGINATION.limit,
                offset: DEFAULT_PAGINATION.offset,
                sort: 'score,baseType desc'
            });
        });

        it('should handle pagination', () => {
            // Setup store mock
            store.currentSite.mockReturnValue(mockSites[0]);
            store.pagination.mockReturnValue({ limit: 10, offset: 0 });
            store.$query.mockReturnValue('+testField:testValue');
            spectator.detectChanges();

            expect(contentSearchService.get).toHaveBeenCalledWith({
                query: '+testField:testValue',
                limit: 10,
                offset: 0,
                sort: 'score,modDate asc'
            });
        });
    });

    describe('DOM', () => {
        it('should have a dot-folder-list-view with items from store', () => {
            spectator.detectChanges();

            const folderListView = spectator.query(DotFolderListViewComponent);

            expect(folderListView).toBeTruthy();
            expect(folderListView?.$items()).toEqual(mockItems);
        });
    });

    describe('onPaginate', () => {
        it('should set pagination with provided values', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'paginate', { rows: 10, first: 0 });

            expect(store.setPagination).toHaveBeenCalledWith({ limit: 10, offset: 0 });
        });

        it('should not set pagination if rows are not provided', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'paginate', { rows: 10 });

            expect(store.setPagination).not.toHaveBeenCalled();
        });

        it('should not set pagination if first are not provided', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'paginate', { first: 0 });

            expect(store.setPagination).not.toHaveBeenCalled();
        });
    });

    describe('onSort', () => {
        it('should set sort with provided values', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'sort', { field: 'modDate', order: 1 });

            expect(store.setSort).toHaveBeenCalledWith({
                field: 'modDate',
                order: DotContentDriveSortOrder.ASC
            });
        });

        it('should not set sort if order is not provided', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'sort', { field: 'modDate' });

            expect(store.setSort).not.toHaveBeenCalled();
        });

        it('should not set sort if field is not provided', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'sort', { order: 1 });

            expect(store.setSort).not.toHaveBeenCalled();
        });

        it('should set sort with default order if order is 0', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'sort', { field: 'modDate', order: 0 });

            expect(store.setSort).toHaveBeenCalledWith({
                field: 'modDate',
                order: DotContentDriveSortOrder.ASC
            });
        });
    });
});
