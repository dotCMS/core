import { beforeEach, describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotRouterService,
    DotLanguagesService,
    DotFolderService,
    DotUploadFileService,
    DotLocalstorageService
} from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import {
    DEFAULT_PAGINATION,
    DIALOG_TYPE,
    HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY
} from '../shared/constants';
import {
    MOCK_ITEMS,
    MOCK_ROUTE,
    MOCK_SEARCH_RESPONSE,
    MOCK_SITES,
    MOCK_BASE_TYPES
} from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let localStorageService: SpyObject<DotLocalstorageService>;
    let filtersSignal: ReturnType<typeof signal>;

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        providers: [
            GlobalStore,
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(MOCK_SITES[0]))
            }),
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(ActivatedRoute, MOCK_ROUTE),
            mockProvider(DotSystemConfigService),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                getContentTypes: jest.fn().mockImplementation(() => of([]))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of())
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotUploadFileService, {
                uploadDotAsset: jest.fn().mockReturnValue(of({}))
            }),
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
                    currentSite: jest.fn().mockReturnValue(MOCK_SITES[0]),
                    // Tree collapsed at start to render the toggle button on toolbar
                    isTreeExpanded: jest.fn().mockReturnValue(false),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn(),
                    $searchParams: jest.fn(),
                    items: jest.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setIsTreeExpanded: jest.fn(),
                    path: jest.fn().mockReturnValue('/test/path'),
                    filters: filtersSignal,
                    status: jest.fn().mockReturnValue(DotContentDriveStatus.LOADING),
                    sort: jest
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    totalItems: jest.fn().mockReturnValue(MOCK_ITEMS.length),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    patchFilters: jest.fn(),
                    contextMenu: jest.fn().mockReturnValue(null),
                    dialog: jest.fn().mockReturnValue(undefined),
                    setDialog: jest.fn(),
                    loadFolders: jest.fn(),
                    loadChildFolders: jest.fn(),
                    updateFolders: jest.fn(),
                    folders: jest.fn(),
                    selectedNode: jest.fn(),
                    sidebarLoading: jest.fn(),
                    closeDialog: jest.fn()
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
                }),
                mockProvider(DotContentTypeService, {
                    getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypesWithPagination: jest.fn().mockReturnValue(
                        of({
                            contentTypes: MOCK_BASE_TYPES,
                            pagination: {
                                currentPage: MOCK_BASE_TYPES.length,
                                totalEntries: MOCK_BASE_TYPES.length * 2,
                                totalPages: 1
                            }
                        })
                    )
                }),
                mockProvider(DotWorkflowsActionsService),
                mockProvider(DotWorkflowActionsFireService),
                mockProvider(DotWorkflowEventHandlerService),
                mockProvider(MessageService, {
                    messageObserver: of({}),
                    clearObserver: of({})
                }),
                mockProvider(DotRouterService, { goToEditPage: jest.fn() }),
                mockProvider(DotLocalstorageService, {
                    getItem: jest.fn().mockReturnValue(undefined),
                    setItem: jest.fn()
                })
            ]
        });
        store = spectator.inject(DotContentDriveStore, true);
        router = spectator.inject(Router);
        location = spectator.inject(Location);
        localStorageService = spectator.inject(DotLocalstorageService);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Query Params Update Effect', () => {
        it('should update query params when store changes', () => {
            // Arrange store values for this run
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/another/path');
            filtersSignal.set({ contentType: ['Blog'], baseType: ['1', '2', '3'] });
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
            expect(folderListView?.$items()).toEqual(MOCK_ITEMS);
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

        it('should have a dialog when dialog is set', () => {
            store.dialog.mockReturnValue({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.detectChanges();

            const dialog = spectator.query('[data-testid="dialog"]');
            expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
        });

        it('should not have a dialog when dialog is not set', () => {
            store.dialog.mockReturnValue(undefined);
            spectator.detectChanges();

            const dialog = spectator.query('[data-testid="dialog"]');
            expect(dialog.getAttribute('ng-reflect-visible')).toBe('false');
        });

        it('should show dialog-folder component when folder dialog type is set', () => {
            store.dialog.mockReturnValue({ type: DIALOG_TYPE.FOLDER, header: 'Create Folder' });
            spectator.detectChanges();

            const dialogFolder = spectator.query('[data-testId="dialog-folder"]');
            expect(dialogFolder).toBeTruthy();
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

    describe('onHideDialog', () => {
        it('should reset the dialog state', () => {
            store.dialog.mockReturnValue({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.detectComponentChanges();

            const dialog = spectator.query('[data-testid="dialog"]');
            dialog.dispatchEvent(new Event('visibleChange'));
            spectator.detectComponentChanges();

            expect(store.closeDialog).toHaveBeenCalled();
        });
    });

    describe('message', () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should show the message', () => {
            spectator.detectChanges();

            const message = spectator.query('[data-testid="message"]');
            expect(message).toBeTruthy();
        });

        it('should show the message content', () => {
            spectator.detectChanges();

            const messageContent = spectator.query('[data-testid="message-content"]');
            expect(messageContent).toBeTruthy();
        });

        it('should set $showMessage to false when close button is clicked', () => {
            spectator.detectChanges();

            const closeButton = spectator.query('[data-testid="close-message"]');
            closeButton.dispatchEvent(new Event('click'));
            spectator.detectChanges();

            expect(spectator.component.$showMessage()).toBe(false);
        });

        it('should return true if the hide message banner key is not set', () => {
            localStorageService.getItem.mockReturnValue(undefined);
            spectator.detectChanges();

            expect(spectator.component.$showMessage()).toBe(true);
        });

        it('should return false if the hide message banner key is set', () => {
            localStorageService.getItem.mockReturnValue('true');
            spectator.detectComponentChanges();

            expect(spectator.component.$showMessage()).toBe(false);
        });

        it('should call the localStorage service to set the hide message banner key', () => {
            spectator.detectChanges();

            const closeButton = spectator.query('[data-testid="close-message"]');
            closeButton.dispatchEvent(new Event('click'));
            spectator.detectChanges();

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY,
                true
            );
        });
    });

    describe('file upload integration', () => {
        let mockFile: File;
        let uploadService: SpyObject<DotUploadFileService>;

        beforeEach(() => {
            mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            uploadService = spectator.inject(DotUploadFileService);
            spectator.detectChanges();
        });

        it('should upload file when file input changes', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            store.selectedNode.mockReturnValue({ key: 'folder-123' });

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(store.setStatus).toHaveBeenCalledWith(DotContentDriveStatus.LOADING);
            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                baseType: 'dotAsset',
                hostFolder: 'folder-123',
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should show success message on successful upload', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const messageService = spectator.inject(MessageService);
            const addSpy = jest.spyOn(messageService, 'add');

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'success',
                summary: expect.any(String)
            });
        });

        it('should show error message on upload failure', () => {
            const error = new Error('Upload failed');
            uploadService.uploadDotAsset.mockReturnValue(throwError(() => error));
            const messageService = spectator.inject(MessageService);
            const addSpy = jest.spyOn(messageService, 'add');

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'error',
                summary: expect.any(String),
                detail: expect.any(String)
            });
            expect(store.setStatus).toHaveBeenCalledWith(DotContentDriveStatus.LOADED);
        });

        it('should not upload when no files are selected', () => {
            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadDotAsset).not.toHaveBeenCalled();
            expect(store.setStatus).not.toHaveBeenCalled();
        });
    });
});
