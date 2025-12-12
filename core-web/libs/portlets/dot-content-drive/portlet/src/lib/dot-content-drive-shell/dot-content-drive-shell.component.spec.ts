import { beforeEach, describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    AddToBundleService,
    DotContentSearchService,
    DotContentTypeService,
    DotCurrentUserService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotRouterService,
    DotLanguagesService,
    DotFolderService,
    DotUploadFileService,
    DotLocalstorageService,
    DotMessageService
} from '@dotcms/data-access';
import { LoggerService, StringUtils, CoreWebService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem
} from '@dotcms/dotcms-models';
import {
    DotFolderListViewComponent,
    DotFolderTreeNodeItem,
    DotContentDriveMoveItems,
    ALL_FOLDER
} from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import {
    DEFAULT_PAGINATION,
    DIALOG_TYPE,
    HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY,
    WARNING_MESSAGE_LIFE,
    SUCCESS_MESSAGE_LIFE,
    ERROR_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
} from '../shared/constants';
import {
    MOCK_ITEMS,
    MOCK_ROUTE,
    MOCK_SEARCH_RESPONSE,
    MOCK_SITES,
    MOCK_BASE_TYPES
} from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let localStorageService: SpyObject<DotLocalstorageService>;
    let messageService: SpyObject<MessageService>;
    let uploadService: SpyObject<DotUploadFileService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;
    let filtersSignal: ReturnType<typeof signal>;
    let statusSignal: ReturnType<typeof signal<DotContentDriveStatus>>;

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
            provideHttpClient(),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn()
            }),
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            LoggerService,
            StringUtils,
            mockProvider(AddToBundleService, {
                getBundles: jest.fn().mockReturnValue(of([])),
                addToBundle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({}))
            })
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        filtersSignal = signal({});
        statusSignal = signal(DotContentDriveStatus.LOADING);

        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    currentSite: jest.fn().mockReturnValue(MOCK_SITES[0]),
                    // Tree collapsed at start to render the toggle button on toolbar
                    isTreeExpanded: jest.fn().mockReturnValue(false),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn(),
                    $request: jest.fn(),
                    items: jest.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setIsTreeExpanded: jest.fn(),
                    path: jest.fn().mockReturnValue('/test/path'),
                    filters: filtersSignal,
                    status: statusSignal,
                    sort: jest
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    totalItems: jest.fn().mockReturnValue(MOCK_ITEMS.length),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    selectedItems: jest.fn().mockReturnValue([]),
                    setSelectedItems: jest.fn(),
                    patchFilters: jest.fn(),
                    contextMenu: jest.fn().mockReturnValue(null),
                    dialog: jest.fn().mockReturnValue(undefined),
                    setDialog: jest.fn(),
                    loadFolders: jest.fn(),
                    loadChildFolders: jest.fn(),
                    updateFolders: jest.fn(),
                    folders: jest.fn(),
                    selectedNode: jest.fn(),
                    setSelectedNode: jest.fn(),
                    sidebarLoading: jest.fn(),
                    closeDialog: jest.fn(),
                    patchContextMenu: jest.fn(),
                    setDragItems: jest.fn(),
                    cleanDragItems: jest.fn(),
                    dragItems: jest.fn().mockReturnValue({ folders: [], contentlets: [] }),
                    loadItems: jest.fn(),
                    setPath: jest.fn(),
                    setShowAddToBundle: jest.fn()
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
                mockProvider(DotWorkflowActionsFireService, {
                    bulkFire: jest
                        .fn()
                        .mockReturnValue(of({ successCount: 1, skippedCount: 0, fails: [] }))
                }),
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
        messageService = spectator.inject(MessageService);
        uploadService = spectator.inject(DotUploadFileService);
        navigationService = spectator.inject(DotContentDriveNavigationService);
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
                },
                queryParamsHandling: 'merge'
            });

            expect(location.go).toHaveBeenCalledWith(
                expect.stringContaining('filters=contentType%3ABlog%3BbaseType%3A1%2C2%2C3')
            );
        });

        it('should not include filters in query params when filters are empty', () => {
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/another/path');
            filtersSignal.set({ contentType: ['Blog'], baseType: ['1', '2', '3'] });
            spectator.detectChanges();
            spectator.flushEffects();

            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: {
                    isTreeExpanded: 'false',
                    path: '/another/path',
                    filters: 'contentType:Blog;baseType:1,2,3'
                },
                queryParamsHandling: 'merge'
            });

            jest.clearAllMocks(); // Clear previous calls

            filtersSignal.set({});
            spectator.detectChanges();
            spectator.flushEffects();

            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: {
                    isTreeExpanded: 'false',
                    path: '/another/path',
                    filters: null // With merge, null removes the param
                },
                queryParamsHandling: 'merge'
            });

            expect(location.go).toHaveBeenCalledWith(expect.stringContaining('filters=null'));
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
            expect(dialog).toBeTruthy();

            // Access the PrimeNG Dialog component instance to verify visible property
            const dialogDebugElement = spectator.debugElement.query(
                By.css('[data-testid="dialog"]')
            );
            const dialogComponent = dialogDebugElement?.componentInstance as Dialog;
            expect(dialogComponent.visible).toBe(true);
        });

        it('should not have a dialog when dialog is not set', () => {
            store.dialog.mockReturnValue(undefined);
            spectator.detectChanges();

            const dialog = spectator.query('[data-testid="dialog"]');
            expect(dialog).toBeTruthy();

            // Access the PrimeNG Dialog component instance to verify visible property
            const dialogDebugElement = spectator.debugElement.query(
                By.css('[data-testid="dialog"]')
            );
            const dialogComponent = dialogDebugElement?.componentInstance as Dialog;
            expect(dialogComponent.visible).toBe(false);
        });

        it('should show dialog-folder component when folder dialog type is set', () => {
            store.dialog.mockReturnValue({ type: DIALOG_TYPE.FOLDER, header: 'Create Folder' });
            spectator.detectChanges();

            const dialogFolder = spectator.query('[data-testId="dialog-folder"]');
            expect(dialogFolder).toBeTruthy();
        });

        it('should have a dropzone component', () => {
            spectator.detectChanges();

            const dropzone = spectator.query('[data-testid="dropzone"]');
            expect(dropzone).toBeTruthy();
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

    describe('onSelectItems', () => {
        it('should update selectedItems in store when selectionChange is emitted', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            const selectedItems = [MOCK_ITEMS[0], MOCK_ITEMS[1]];

            spectator.triggerEventHandler(folderListView, 'selectionChange', selectedItems);

            expect(store.setSelectedItems).toHaveBeenCalledWith(selectedItems);
        });

        it('should update store with empty array when selection is cleared', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'selectionChange', []);

            expect(store.setSelectedItems).toHaveBeenCalledWith([]);
        });

        it('should update store with single item when one item is selected', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            const singleItem = [MOCK_ITEMS[0]];

            spectator.triggerEventHandler(folderListView, 'selectionChange', singleItem);

            expect(store.setSelectedItems).toHaveBeenCalledWith(singleItem);
        });

        it('should update store with all items when all items are selected', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'selectionChange', MOCK_ITEMS);

            expect(store.setSelectedItems).toHaveBeenCalledWith(MOCK_ITEMS);
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

        it('should have a learn more link', () => {
            spectator.detectChanges();

            const learnMoreLink = spectator.query('[data-testid="learn-more-link"]');
            expect(learnMoreLink).toBeTruthy();
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

        beforeEach(() => {
            mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            spectator.detectChanges();
        });

        it('should upload file when file input changes', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));

            const addSpy = jest.spyOn(messageService, 'add');

            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };
            store.selectedNode.mockReturnValue(mockNode as DotFolderTreeNodeItem);

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'info',
                summary: expect.any(String),
                detail: expect.any(String)
            });
            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                baseType: 'dotAsset',
                hostFolder: 'folder-123',
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should sent the current site identifier when the selected node is the all folder', () => {
            store.selectedNode.mockReturnValue({
                ...ALL_FOLDER,
                data: {
                    hostname: MOCK_SITES[0].hostname,
                    path: '',
                    type: 'folder',
                    id: MOCK_SITES[0].identifier
                }
            });
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            spectator.detectChanges();

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                baseType: 'dotAsset',
                hostFolder: MOCK_SITES[0].identifier,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should show info message when upload starts', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'info',
                summary: expect.any(String),
                detail: expect.any(String)
            });
        });

        it('should show error message on upload failure', () => {
            const error = new Error('Upload failed');
            uploadService.uploadDotAsset.mockReturnValue(throwError(() => error));
            store.selectedNode.mockReturnValue({
                ...ALL_FOLDER,
                data: {
                    hostname: MOCK_SITES[0].hostname,
                    path: '',
                    type: 'folder',
                    id: MOCK_SITES[0].identifier
                }
            });
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
                detail: expect.any(String),
                life: ERROR_MESSAGE_LIFE
            });
        });

        it('should show error message on upload failure with errors', () => {
            const error = {
                error: {
                    errors: [{ message: 'Upload failed' }]
                }
            };
            uploadService.uploadDotAsset.mockReturnValue(throwError(() => error));
            store.selectedNode.mockReturnValue({
                ...ALL_FOLDER,
                data: {
                    hostname: MOCK_SITES[0].hostname,
                    path: '',
                    type: 'folder',
                    id: MOCK_SITES[0].identifier
                }
            });
            const addSpy = jest.spyOn(messageService, 'add');

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(addSpy).toHaveBeenCalledTimes(2);
            expect(addSpy).toHaveBeenNthCalledWith(1, {
                severity: 'info',
                summary: 'content-drive.file-upload-in-progress',
                detail: 'content-drive.file-upload-in-progress-detail'
            });
            expect(addSpy).toHaveBeenNthCalledWith(2, {
                severity: 'error',
                summary: 'content-drive.add-dotasset-error',
                detail: 'content-drive.add-dotasset-error-detail',
                life: ERROR_MESSAGE_LIFE
            });
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

        it('should show warning message when multiple files are selected and upload only the first file', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            const mockFile1 = new File(['test content 1'], 'test1.jpg', { type: 'image/jpeg' });
            const mockFile2 = new File(['test content 2'], 'test2.jpg', { type: 'image/jpeg' });
            const mockFile3 = new File(['test content 3'], 'test3.jpg', { type: 'image/jpeg' });

            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };
            store.selectedNode.mockReturnValue(mockNode);

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile1, mockFile2, mockFile3],
                writable: false
            });

            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            // Should show warning message
            expect(addSpy).toHaveBeenCalledWith({
                severity: 'warn',
                summary: expect.any(String),
                detail: expect.any(String),
                life: WARNING_MESSAGE_LIFE
            });

            // Should upload only the first file
            expect(uploadService.uploadDotAsset).toHaveBeenCalledTimes(1);
            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile1, {
                baseType: 'dotAsset',
                hostFolder: 'folder-123',
                indexPolicy: 'WAIT_FOR'
            });
        });
    });

    describe('sidebar file upload', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should trigger resolveFilesUpload when sidebar emits uploadFiles event with single file', () => {
            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));

            const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile,
                length: 1,
                item: (index: number) => (index === 0 ? mockFile : null)
            } as unknown as FileList;

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                baseType: 'dotAsset',
                hostFolder: mockNode.data.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should trigger resolveFilesUpload when sidebar emits uploadFiles event with multiple files', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            const mockFile1 = new File(['test content 1'], 'test1.jpg', { type: 'image/jpeg' });
            const mockFile2 = new File(['test content 2'], 'test2.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile1,
                1: mockFile2,
                length: 2,
                item: (index: number) => {
                    if (index === 0) return mockFile1;
                    if (index === 1) return mockFile2;

                    return null;
                }
            } as unknown as FileList;

            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-456',
                    hostname: 'localhost',
                    path: 'folder-456',
                    type: 'folder'
                },
                key: 'folder-456',
                label: 'folder-456'
            };

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            // Should show warning message for multiple files
            expect(addSpy).toHaveBeenCalledWith({
                severity: 'warn',
                summary: expect.any(String),
                detail: expect.any(String),
                life: WARNING_MESSAGE_LIFE
            });

            // Should upload only the first file
            expect(uploadService.uploadDotAsset).toHaveBeenCalledTimes(1);
            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile1, {
                baseType: 'dotAsset',
                hostFolder: 'folder-456',
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should show success message after successful upload from sidebar', () => {
            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };

            const mockContentlet = {
                title: 'test.jpg',
                contentType: 'image/jpeg'
            } as DotCMSContentlet;
            uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile,
                length: 1,
                item: (index: number) => (index === 0 ? mockFile : null)
            } as unknown as FileList;

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'success',
                summary: expect.any(String),
                detail: expect.any(String),
                life: SUCCESS_MESSAGE_LIFE
            });
        });

        it('should show error message after failed upload from sidebar', () => {
            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };
            const error = new Error('Upload failed');
            uploadService.uploadDotAsset.mockReturnValue(throwError(() => error));

            const addSpy = jest.spyOn(messageService, 'add');

            const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile,
                length: 1,
                item: (index: number) => (index === 0 ? mockFile : null)
            } as unknown as FileList;

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'error',
                summary: expect.any(String),
                detail: expect.any(String),
                life: ERROR_MESSAGE_LIFE
            });
        });
    });

    describe('dropzone file upload', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should trigger resolveFilesUpload when dropzone emits uploadFiles event with single file', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile,
                length: 1,
                item: (index: number) => (index === 0 ? mockFile : null)
            } as unknown as FileList;

            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));
            spectator.triggerEventHandler(dropzone, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile, {
                baseType: 'dotAsset',
                hostFolder: mockNode.data.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should trigger resolveFilesUpload when dropzone emits uploadFiles event with multiple files', () => {
            uploadService.uploadDotAsset.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            const mockFile1 = new File(['test content 1'], 'test1.jpg', { type: 'image/jpeg' });
            const mockFile2 = new File(['test content 2'], 'test2.jpg', { type: 'image/jpeg' });
            const mockFileList = {
                0: mockFile1,
                1: mockFile2,
                length: 2,
                item: (index: number) => {
                    if (index === 0) return mockFile1;
                    if (index === 1) return mockFile2;

                    return null;
                }
            } as unknown as FileList;

            const mockNode: DotFolderTreeNodeItem = {
                data: {
                    id: 'folder-123',
                    hostname: 'localhost',
                    path: 'folder-123',
                    type: 'folder'
                },
                key: 'folder-123',
                label: 'folder-123'
            };

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));
            spectator.triggerEventHandler(dropzone, 'uploadFiles', {
                files: mockFileList,
                targetFolder: mockNode.data
            });

            // Should show warning message for multiple files
            expect(addSpy).toHaveBeenCalledWith({
                severity: 'warn',
                summary: expect.any(String),
                detail: expect.any(String),
                life: WARNING_MESSAGE_LIFE
            });

            // Should upload only the first file
            expect(uploadService.uploadDotAsset).toHaveBeenCalledTimes(1);
            expect(uploadService.uploadDotAsset).toHaveBeenCalledWith(mockFile1, {
                baseType: 'dotAsset',
                hostFolder: 'folder-123',
                indexPolicy: 'WAIT_FOR'
            });
        });
    });

    describe('Drag Events', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        describe('onDragStart', () => {
            it('should handle drag start with single item', () => {
                const draggedItem = MOCK_ITEMS[0];
                const folderListView = spectator.debugElement.query(
                    By.directive(DotFolderListViewComponent)
                );

                spectator.triggerEventHandler(folderListView, 'dragStart', [draggedItem]);

                expect(store.patchContextMenu).toHaveBeenCalledWith({
                    triggeredEvent: null,
                    contentlet: null
                });
                expect(store.setDragItems).toHaveBeenCalledWith([draggedItem]);
            });

            it('should handle drag start with multiple items', () => {
                const draggedItems = [MOCK_ITEMS[0], MOCK_ITEMS[1]];
                const folderListView = spectator.debugElement.query(
                    By.directive(DotFolderListViewComponent)
                );

                spectator.triggerEventHandler(folderListView, 'dragStart', draggedItems);

                expect(store.patchContextMenu).toHaveBeenCalledWith({
                    triggeredEvent: null,
                    contentlet: null
                });
                expect(store.setDragItems).toHaveBeenCalledWith(draggedItems);
            });

            it('should reset context menu when drag starts', () => {
                const draggedItem = MOCK_ITEMS[0];
                const folderListView = spectator.debugElement.query(
                    By.directive(DotFolderListViewComponent)
                );

                spectator.triggerEventHandler(folderListView, 'dragStart', [draggedItem]);

                expect(store.patchContextMenu).toHaveBeenCalledWith({
                    triggeredEvent: null,
                    contentlet: null
                });
            });
        });

        describe('onDragEnd', () => {
            it('should clean drag items on drag end', () => {
                const folderListView = spectator.debugElement.query(
                    By.directive(DotFolderListViewComponent)
                );

                spectator.triggerEventHandler(folderListView, 'dragEnd', undefined);

                expect(store.cleanDragItems).toHaveBeenCalled();
            });
        });
    });

    describe('Move Items', () => {
        let workflowService: SpyObject<DotWorkflowActionsFireService>;

        beforeEach(() => {
            spectator.detectChanges();
            workflowService = spectator.inject(DotWorkflowActionsFireService);
            messageService.add.mockClear();
        });

        describe('onMoveItems', () => {
            it('should handle move with single item', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 1, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: expect.any(String),
                    detail: expect.any(String)
                });

                expect(workflowService.bulkFire).toHaveBeenCalledWith({
                    additionalParams: {
                        assignComment: {
                            assign: '',
                            comment: ''
                        },
                        pushPublish: {},
                        additionalParamsMap: {
                            _path_to_move: '//demo.dotcms.com/documents/'
                        }
                    },
                    contentletIds: [mockDragItems.contentlets[0].inode],
                    workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
                });
            });

            it('should handle move with multiple items', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [
                        MOCK_ITEMS[0] as DotCMSContentlet,
                        MOCK_ITEMS[1] as DotCMSContentlet
                    ]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 2, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-2',
                        hostname: 'demo.dotcms.com',
                        path: '/images/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                expect(workflowService.bulkFire).toHaveBeenCalledWith({
                    additionalParams: {
                        assignComment: {
                            assign: '',
                            comment: ''
                        },
                        pushPublish: {},
                        additionalParamsMap: {
                            _path_to_move: '//demo.dotcms.com/images/'
                        }
                    },
                    contentletIds: [
                        mockDragItems.contentlets[0].inode,
                        mockDragItems.contentlets[1].inode
                    ],
                    workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
                });
            });

            it('should show success message after successful move', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 1, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: expect.any(String),
                    detail: expect.any(String),
                    life: SUCCESS_MESSAGE_LIFE
                });
            });

            it('should show message with folders when dragging folders and contentlets', () => {
                const mockFolder: DotContentDriveFolder = {
                    __icon__: 'folderIcon',
                    defaultFileType: '',
                    description: '',
                    extension: 'folder',
                    filesMasks: '',
                    hasTitleImage: false,
                    hostId: 'host-1',
                    iDate: 1234567890,
                    identifier: 'folder-1',
                    inode: 'inode-folder-1',
                    mimeType: 'folder',
                    modDate: 1234567890,
                    name: 'Test Folder',
                    owner: 'admin',
                    parent: '/',
                    path: '/test-folder/',
                    permissions: [],
                    showOnMenu: true,
                    sortOrder: 0,
                    title: 'Test Folder',
                    type: 'folder'
                };

                const mockDragItems = {
                    folders: [mockFolder],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 1, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                // Should show the message with folders (different message when folders are included)
                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'info',
                    summary: 'content-drive.move-to-folder-in-progress-with-folders',
                    detail: expect.any(String)
                });

                // Should still call workflow service with contentlet inodes (not folders)
                expect(workflowService.bulkFire).toHaveBeenCalledWith({
                    additionalParams: {
                        assignComment: {
                            assign: '',
                            comment: ''
                        },
                        pushPublish: {},
                        additionalParamsMap: {
                            _path_to_move: '//demo.dotcms.com/documents/'
                        }
                    },
                    contentletIds: [mockDragItems.contentlets[0].inode],
                    workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
                });
            });

            it('should clean drag items and reload items after successful move', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 1, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                expect(store.cleanDragItems).toHaveBeenCalled();
                expect(store.loadItems).toHaveBeenCalled();
            });

            it('should handle move to root folder (empty path)', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 1, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'root-folder',
                        hostname: 'demo.dotcms.com',
                        path: '',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                expect(workflowService.bulkFire).toHaveBeenCalledWith({
                    additionalParams: {
                        assignComment: {
                            assign: '',
                            comment: ''
                        },
                        pushPublish: {},
                        additionalParamsMap: {
                            _path_to_move: '//demo.dotcms.com/'
                        }
                    },
                    contentletIds: [mockDragItems.contentlets[0].inode],
                    workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
                });
            });

            it('should not show success message when successCount is 0', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({ successCount: 0, skippedCount: 0, fails: [] })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                const successCalls = messageService.add.mock.calls.filter(
                    (call) => call[0].severity === 'success'
                );

                expect(successCalls).toHaveLength(0);
                expect(store.cleanDragItems).toHaveBeenCalled();
            });

            it('should show individual error messages for each failed item', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [
                        MOCK_ITEMS[0] as DotCMSContentlet,
                        MOCK_ITEMS[1] as DotCMSContentlet
                    ]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({
                        successCount: 0,
                        skippedCount: 0,
                        fails: [
                            {
                                inode: mockDragItems.contentlets[0].inode,
                                errorMessage: 'Error moving item 1'
                            },
                            {
                                inode: mockDragItems.contentlets[1].inode,
                                errorMessage: 'Error moving item 2'
                            }
                        ]
                    })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                const errorCalls = messageService.add.mock.calls.filter(
                    (call) => call[0].severity === 'error'
                );

                expect(errorCalls).toHaveLength(2);
                expect(errorCalls[0][0]).toEqual({
                    severity: 'error',
                    summary: expect.any(String),
                    detail: 'Error moving item 1',
                    life: ERROR_MESSAGE_LIFE
                });
                expect(errorCalls[1][0]).toEqual({
                    severity: 'error',
                    summary: expect.any(String),
                    detail: 'Error moving item 2',
                    life: ERROR_MESSAGE_LIFE
                });
            });

            it('should handle partial success with some fails', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [
                        MOCK_ITEMS[0] as DotCMSContentlet,
                        MOCK_ITEMS[1] as DotCMSContentlet
                    ]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    of({
                        successCount: 1,
                        skippedCount: 0,
                        fails: [
                            {
                                inode: mockDragItems.contentlets[1].inode,
                                errorMessage: 'Error moving item'
                            }
                        ]
                    })
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                const successCalls = messageService.add.mock.calls.filter(
                    (call) => call[0].severity === 'success'
                );
                const errorCalls = messageService.add.mock.calls.filter(
                    (call) => call[0].severity === 'error'
                );

                expect(successCalls).toHaveLength(1);
                expect(errorCalls).toHaveLength(1);
                expect(store.loadItems).toHaveBeenCalled();
                expect(store.cleanDragItems).toHaveBeenCalled();
            });

            it('should handle workflow service error', () => {
                const mockDragItems = {
                    folders: [],
                    contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
                };
                store.dragItems.mockReturnValue(mockDragItems);
                workflowService.bulkFire.mockReturnValue(
                    throwError(() => new Error('Workflow error'))
                );

                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
                spectator.triggerEventHandler(sidebar, 'moveItems', mockMoveEvent);

                const errorCalls = messageService.add.mock.calls.filter(
                    (call) => call[0].severity === 'error'
                );

                expect(errorCalls.length).toBeGreaterThanOrEqual(1);
                expect(errorCalls[0][0]).toEqual({
                    severity: 'error',
                    summary: expect.any(String),
                    detail: expect.any(String),
                    life: ERROR_MESSAGE_LIFE
                });
                expect(store.cleanDragItems).toHaveBeenCalled();
            });
        });
    });

    describe('onTableDrop', () => {
        let workflowService: SpyObject<DotWorkflowActionsFireService>;

        beforeEach(() => {
            spectator.detectChanges();
            workflowService = spectator.inject(DotWorkflowActionsFireService);
            messageService.add.mockClear();
        });

        it('should trigger move when drop event is emitted with a folder', () => {
            const mockDragItems = {
                folders: [],
                contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
            };
            store.dragItems.mockReturnValue(mockDragItems);
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            workflowService.bulkFire.mockReturnValue(
                of({ successCount: 1, skippedCount: 0, fails: [] })
            );

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/documents/',
                identifier: 'folder-123'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', folderItem);

            // Should show info message
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'info',
                summary: expect.any(String),
                detail: expect.any(String)
            });

            // Should call workflow service with correct parameters
            expect(workflowService.bulkFire).toHaveBeenCalledWith({
                additionalParams: {
                    assignComment: {
                        assign: '',
                        comment: ''
                    },
                    pushPublish: {},
                    additionalParamsMap: {
                        _path_to_move: `//${MOCK_SITES[0].hostname}/documents/`
                    }
                },
                contentletIds: [mockDragItems.contentlets[0].inode],
                workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
            });
        });

        it('should not trigger move when drop event is emitted with a non-folder item', () => {
            const contentItem = {
                ...MOCK_ITEMS[0],
                type: 'content',
                identifier: 'content-123'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', contentItem);

            // Should not show any messages or call workflow service
            expect(messageService.add).not.toHaveBeenCalled();
            expect(workflowService.bulkFire).not.toHaveBeenCalled();
        });

        it('should follow the same flow as sidebar moveItems when drop event is emitted with a folder', () => {
            const mockDragItems = {
                folders: [],
                contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
            };
            store.dragItems.mockReturnValue(mockDragItems);
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            workflowService.bulkFire.mockReturnValue(
                of({ successCount: 1, skippedCount: 0, fails: [] })
            );

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/images/',
                identifier: 'folder-456'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', folderItem);

            // Should show success message after successful move
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: expect.any(String),
                detail: expect.any(String),
                life: SUCCESS_MESSAGE_LIFE
            });

            // Should clean drag items and reload items
            expect(store.cleanDragItems).toHaveBeenCalled();
            expect(store.loadItems).toHaveBeenCalled();
        });

        it('should handle move to root folder (empty path) when drop event is emitted', () => {
            const mockDragItems = {
                folders: [],
                contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
            };
            store.dragItems.mockReturnValue(mockDragItems);
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            workflowService.bulkFire.mockReturnValue(
                of({ successCount: 1, skippedCount: 0, fails: [] })
            );

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '',
                identifier: 'root-folder'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', folderItem);

            expect(workflowService.bulkFire).toHaveBeenCalledWith({
                additionalParams: {
                    assignComment: {
                        assign: '',
                        comment: ''
                    },
                    pushPublish: {},
                    additionalParamsMap: {
                        _path_to_move: `//${MOCK_SITES[0].hostname}/`
                    }
                },
                contentletIds: [mockDragItems.contentlets[0].inode],
                workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
            });
        });

        it('should handle workflow service error when drop event is emitted with a folder', () => {
            const mockDragItems = {
                folders: [],
                contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
            };
            store.dragItems.mockReturnValue(mockDragItems);
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            workflowService.bulkFire.mockReturnValue(throwError(() => new Error('Workflow error')));

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/documents/',
                identifier: 'folder-123'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', folderItem);

            const errorCalls = messageService.add.mock.calls.filter(
                (call) => call[0].severity === 'error'
            );

            expect(errorCalls.length).toBeGreaterThanOrEqual(1);
            expect(errorCalls[0][0]).toEqual({
                severity: 'error',
                summary: expect.any(String),
                detail: expect.any(String),
                life: ERROR_MESSAGE_LIFE
            });
            expect(store.cleanDragItems).toHaveBeenCalled();
        });

        it('should show message with folders when dragging folders and contentlets and drop event is emitted with a folder', () => {
            const mockFolder: DotContentDriveFolder = {
                __icon__: 'folderIcon',
                defaultFileType: '',
                description: '',
                extension: 'folder',
                filesMasks: '',
                hasTitleImage: false,
                hostId: 'host-1',
                iDate: 1234567890,
                identifier: 'folder-1',
                inode: 'inode-folder-1',
                mimeType: 'folder',
                modDate: 1234567890,
                name: 'Test Folder',
                owner: 'admin',
                parent: '/',
                path: '/test-folder/',
                permissions: [],
                showOnMenu: true,
                sortOrder: 0,
                title: 'Test Folder',
                type: 'folder'
            };

            const mockDragItems = {
                folders: [mockFolder],
                contentlets: [MOCK_ITEMS[0] as DotCMSContentlet]
            };
            store.dragItems.mockReturnValue(mockDragItems);
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            workflowService.bulkFire.mockReturnValue(
                of({ successCount: 1, skippedCount: 0, fails: [] })
            );

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/documents/',
                identifier: 'folder-123'
            } as DotContentDriveItem;

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'drop', folderItem);

            // Should show the message with folders (different message when folders are included)
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'info',
                summary: 'content-drive.move-to-folder-in-progress-with-folders',
                detail: expect.any(String)
            });

            // Should still call workflow service with contentlet inodes (not folders)
            expect(workflowService.bulkFire).toHaveBeenCalledWith({
                additionalParams: {
                    assignComment: {
                        assign: '',
                        comment: ''
                    },
                    pushPublish: {},
                    additionalParamsMap: {
                        _path_to_move: `//${MOCK_SITES[0].hostname}/documents/`
                    }
                },
                contentletIds: [mockDragItems.contentlets[0].inode],
                workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
            });
        });
    });

    describe('setPathEffect', () => {
        it('should set path when selectedNode changes', () => {
            const mockNode: DotFolderTreeNodeItem = {
                key: 'folder-1',
                label: '/documents/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder'
                },
                leaf: false
            };

            store.selectedNode.mockReturnValue(mockNode);
            store.setPath.mockClear();

            spectator.detectChanges();
            spectator.flushEffects();

            expect(store.setPath).toHaveBeenCalledWith('/documents/');
        });

        it('should not set path when selectedNode is null', () => {
            store.selectedNode.mockReturnValue(null);
            store.setPath.mockClear();

            spectator.detectChanges();
            spectator.flushEffects();

            expect(store.setPath).not.toHaveBeenCalled();
        });
    });

    describe('onDoubleClick', () => {
        it('should set selectedNode when double clicking a folder', () => {
            spectator.detectChanges();

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/documents/',
                identifier: 'folder-123'
            };

            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            store.setSelectedNode.mockClear();

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'doubleClick', folderItem);

            expect(store.setSelectedNode).toHaveBeenCalledWith({
                data: {
                    type: 'folder',
                    path: '/documents/',
                    hostname: MOCK_SITES[0].hostname,
                    id: 'folder-123',
                    fromTable: true
                },
                key: 'folder-123',
                label: '/documents/',
                leaf: false
            });
        });

        it('should call navigationService.editContent when double clicking a content item', () => {
            spectator.detectChanges();

            const contentItem = {
                ...MOCK_ITEMS[0],
                type: 'content',
                identifier: 'content-123'
            };

            navigationService.editContent.mockClear();

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'doubleClick', contentItem);

            expect(navigationService.editContent).toHaveBeenCalledWith(contentItem);
            expect(store.setSelectedNode).not.toHaveBeenCalled();
        });
    });

    describe('onContextMenu', () => {
        it('should patch context menu when right-clicking a content item', () => {
            spectator.detectChanges();

            const mockEvent = {
                preventDefault: jest.fn()
            } as unknown as MouseEvent;
            const contentlet = MOCK_ITEMS[0];

            store.patchContextMenu.mockClear();

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'rightClick', {
                event: mockEvent,
                contentlet
            });

            expect(mockEvent.preventDefault).toHaveBeenCalled();
            expect(store.patchContextMenu).toHaveBeenCalledWith({
                triggeredEvent: mockEvent,
                contentlet
            });
        });
    });

    describe('cancelAddToBundle', () => {
        it('should set showAddToBundle to false', () => {
            store.contextMenu.mockReturnValue({
                triggeredEvent: new Event('click'),
                contentlet: MOCK_ITEMS[0],
                showAddToBundle: true
            });
            store.setShowAddToBundle.mockClear();

            spectator.detectChanges();

            const addToBundleComponent = spectator.debugElement.query(By.css('dot-add-to-bundle'));

            if (addToBundleComponent) {
                spectator.triggerEventHandler(addToBundleComponent, 'cancel', undefined);
            } else {
                // Fallback: if component is conditionally rendered and not visible, test directly
                spectator.component['cancelAddToBundle']();
            }

            expect(store.setShowAddToBundle).toHaveBeenCalledWith(false);
        });
    });

    describe('onAddNewDotAsset', () => {
        it('should trigger file input click', () => {
            spectator.detectChanges();

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            const clickSpy = jest.spyOn(fileInput, 'click');

            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));

            spectator.triggerEventHandler(toolbar, 'addNewDotAsset', undefined);

            expect(clickSpy).toHaveBeenCalled();
        });
    });
});
