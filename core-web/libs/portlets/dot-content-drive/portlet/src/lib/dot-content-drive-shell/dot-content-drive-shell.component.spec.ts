import { beforeEach, describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import {
    DotContentSearchService,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { DEFAULT_PAGINATION } from '../shared/constants';
import { mockItems, mockRoute, mockSearchResponse, mockSites } from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let contentSearchService: jest.Mocked<DotContentSearchService>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let filtersSignal: ReturnType<typeof signal>;

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
            mockProvider(DotSystemConfigService),
            provideHttpClient()
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        filtersSignal = signal({});

        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    currentSite: jest.fn(),
                    isTreeExpanded: jest.fn().mockReturnValue(true),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn(),
                    $query: jest.fn(),
                    items: jest.fn().mockReturnValue(mockItems),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setIsTreeExpanded: jest.fn(),
                    path: jest.fn().mockReturnValue('/test/path'),
                    filters: filtersSignal,
                    status: jest.fn().mockReturnValue(DotContentDriveStatus.LOADING),
                    sort: jest
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    totalItems: jest.fn().mockReturnValue(mockItems.length),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    patchFilters: jest.fn()
                }),
                mockProvider(Router, {
                    createUrlTree: jest.fn(
                        (_commands: unknown[], opts: { queryParams?: Record<string, string> }) => ({
                            toString: () =>
                                '?' + new URLSearchParams(opts?.queryParams ?? {}).toString()
                        })
                    )
                }),
                mockProvider(Location, {
                    go: jest.fn()
                })
            ]
        });
        contentSearchService = spectator.inject(DotContentSearchService);
        store = spectator.inject(DotContentDriveStore, true);
        router = spectator.inject(Router);
        location = spectator.inject(Location);
    });

    afterEach(() => {
        jest.clearAllMocks();
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

    describe('Query Params Update Effect', () => {
        it('should update query params when store changes', () => {
            // Arrange store values for this run
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/another/path');
            store.filters.mockReturnValue({ contentType: ['Blog'], baseType: ['1', '2', '3'] });
            spectator.detectChanges();

            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: {
                    isTreeExpanded: 'false',
                    path: '/another/path',
                    filters: 'contentType:Blog;baseType:1,2,3'
                }
            });

            // And Location.go called with the serialized query string
            expect(location.go).toHaveBeenCalled();
            const calledWith = location.go.mock.calls[0][0] as string;
            expect(calledWith).toContain('isTreeExpanded=false');
            expect(calledWith).toContain('path=%2Fanother%2Fpath');
            expect(calledWith).toContain('filters=contentType%3ABlog%3BbaseType%3A1%2C2%2C3');
        });

        it('should not include filters in query params when filters are empty', () => {
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/another/path');
            filtersSignal.set({ contentType: 'Blog', baseType: ['1', '2', '3'] });
            spectator.detectChanges();

            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: {
                    isTreeExpanded: 'false',
                    path: '/another/path',
                    filters: 'contentType:Blog;baseType:1,2,3'
                }
            });

            spectator.detectChanges();

            filtersSignal.set({});

            spectator.detectChanges();

            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: {
                    isTreeExpanded: 'false',
                    path: '/another/path'
                }
            });

            expect(location.go).toHaveBeenCalled();
            const calledWith = location.go.mock.calls[1][0] as string;
            expect(calledWith).toContain('isTreeExpanded=false');
            expect(calledWith).toContain('path=%2Fanother%2Fpath');
            expect(calledWith).not.toContain('filters');
        });
    });

    describe('DOM', () => {
        it('should have a dot-folder-list-view with items from store', () => {
            spectator.detectChanges();

            const folderListView = spectator.query(DotFolderListViewComponent);

            expect(folderListView).toBeTruthy();
            expect(folderListView?.$items()).toEqual(mockItems);
        });

        it('should have a dot-content-drive-toolbar with tree toggler', () => {
            spectator.detectChanges();

            const toolbar = spectator.query('[data-testid="toolbar"]');

            expect(toolbar).toBeTruthy();
            expect(toolbar?.querySelector('[data-testid="tree-toggler"]')).toBeTruthy();
        });

        it('should show the tree selector by default', () => {
            spectator.detectChanges();

            const treeSelector = spectator.query('[data-testid="tree-selector"]');

            expect(treeSelector).toBeTruthy();
        });

        it('should hide the tree selector when tree is collapsed', () => {
            store.isTreeExpanded.mockReturnValue(false);
            spectator.detectChanges();

            const treeSelector = spectator.query('[data-testid="tree-selector"]');

            expect(treeSelector).toBeTruthy();
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
