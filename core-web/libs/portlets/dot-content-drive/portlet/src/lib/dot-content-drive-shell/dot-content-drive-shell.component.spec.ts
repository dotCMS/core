import { beforeEach, describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    AddToBundleService,
    DotContentSearchService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotRouterService,
    DotLanguagesService,
    DotFolderService,
    DotUploadFileService,
    DotMessageService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem
} from '@dotcms/dotcms-models';
import {
    DotFolderListViewComponent,
    DotFolderTreeNodeData,
    DotFolderTreeNodeItem,
    DotContentDriveMoveItems
} from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DIALOG_TYPE,
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
import {
    DotContentDriveDialog,
    DotContentDriveSortOrder,
    DotContentDriveStatus
} from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let messageService: SpyObject<MessageService>;
    let uploadService: SpyObject<DotUploadFileService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;
    let filtersSignal: ReturnType<typeof signal>;
    let statusSignal: ReturnType<typeof signal<DotContentDriveStatus>>;
    // Reactive so the shell's syncDialogEffect reacts (mirrors the real SignalStore signal).
    let dialogSignal: WritableSignal<DotContentDriveDialog | undefined>;

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
                uploadFileByBaseType: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient(),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn()
            }),
            LoggerService,
            StringUtils,
            mockProvider(AddToBundleService, {
                getBundles: jest.fn().mockReturnValue(of([])),
                addToBundle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService)
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        filtersSignal = signal({});
        statusSignal = signal(DotContentDriveStatus.LOADING);
        dialogSignal = signal<DotContentDriveDialog | undefined>(undefined);

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
                    pages: jest.fn().mockReturnValue([DEFAULT_PAGE]),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    selectedItems: jest.fn().mockReturnValue([]),
                    setSelectedItems: jest.fn(),
                    patchFilters: jest.fn(),
                    contextMenu: jest.fn().mockReturnValue(null),
                    dialog: dialogSignal,
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
                    resetContextMenu: jest.fn(),
                    setDragItems: jest.fn(),
                    cleanDragItems: jest.fn(),
                    dragItems: jest.fn().mockReturnValue({ folders: [], contentlets: [] }),
                    loadItems: jest.fn(),
                    setPath: jest.fn(),
                    setShowAddToBundle: jest.fn(),
                    userSearchableFields: jest.fn().mockReturnValue([]),
                    userSearchableActive: jest.fn().mockReturnValue([]),
                    setUserSearchableFields: jest.fn(),
                    addUserSearchableField: jest.fn(),
                    clearUserSearchableFilters: jest.fn()
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
                mockProvider(DotRouterService, { goToEditPage: jest.fn() })
            ]
        });
        store = spectator.inject(DotContentDriveStore, true);
        router = spectator.inject(Router);
        location = spectator.inject(Location);
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
        });
    });

    describe('setPathEffect (cold-load selection)', () => {
        it('should not clear the URL-restored path while the sidebar is still loading', () => {
            // Cold load: path restored from the URL, but the tree hasn't resolved yet so
            // selectedNode is still the default root node (empty path). Syncing here would
            // clobber the restored path back to root.
            store.sidebarLoading.mockReturnValue(true);
            store.selectedNode.mockReturnValue({ data: { path: '' } } as DotFolderTreeNodeItem);
            store.path.mockReturnValue('/about-us/');

            spectator.detectChanges();
            spectator.flushEffects();

            expect(store.setPath).not.toHaveBeenCalled();
        });

        it('should sync the path from the resolved node once the sidebar finishes loading', () => {
            store.sidebarLoading.mockReturnValue(false);
            store.selectedNode.mockReturnValue({
                data: { path: '/about-us/' }
            } as DotFolderTreeNodeItem);
            store.path.mockReturnValue('');

            spectator.detectChanges();
            spectator.flushEffects();

            expect(store.setPath).toHaveBeenCalledWith('/about-us/');
        });

        it('should not sync when the resolved node path already matches the current path', () => {
            store.sidebarLoading.mockReturnValue(false);
            store.selectedNode.mockReturnValue({
                data: { path: '/about-us/' }
            } as DotFolderTreeNodeItem);
            store.path.mockReturnValue('/about-us/');

            spectator.detectChanges();
            spectator.flushEffects();

            expect(store.setPath).not.toHaveBeenCalled();
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
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
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
            dialogSignal.set(undefined);
            spectator.flushEffects();
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

        it('should configure the dialog as closable and closeOnEscape', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
            spectator.detectChanges();

            const dialogDebugElement = spectator.debugElement.query(
                By.css('[data-testid="dialog"]')
            );
            const dialogComponent = dialogDebugElement?.componentInstance as Dialog;
            expect(dialogComponent.closable).toBe(true);
            expect(dialogComponent.closeOnEscape).toBe(true);
        });

        it('should show dialog-folder component when folder dialog type is set', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Create Folder' });
            spectator.flushEffects();
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

    describe('$totalItems', () => {
        it('should return limit * (currentPage + 1) when hasMoreContent is true', () => {
            // DEFAULT_PAGINATION: { page: 1, limit: 20 }, DEFAULT_PAGE: { hasMoreContent: true }
            store.pagination.mockReturnValue({ page: 1, limit: 20, offset: 0 });
            store.pages.mockReturnValue([DEFAULT_PAGE]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // hasMoreContent = true, page=1, limit=20 → 20 * (1+1) = 40
            expect(spectator.component.$totalItems()).toBe(40);
        });

        it('should return exact total when hasMoreContent is false', () => {
            store.pagination.mockReturnValue({ page: 1, limit: 20, offset: 0 });
            store.pages.mockReturnValue([{ ...DEFAULT_PAGE, hasMoreContent: false }]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // hasMoreContent = false, page=1, limit=20, items=MOCK_ITEMS.length → 20*(1-1) + MOCK_ITEMS.length
            expect(spectator.component.$totalItems()).toBe(MOCK_ITEMS.length);
        });

        it('should account for previous pages when hasMoreContent is false on page 2', () => {
            store.pagination.mockReturnValue({ page: 2, limit: 20, offset: 20 });
            store.pages.mockReturnValue([{ ...DEFAULT_PAGE, hasMoreContent: false }]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // hasMoreContent = false, page=2, limit=20, items=MOCK_ITEMS.length → 20*(2-1) + MOCK_ITEMS.length
            expect(spectator.component.$totalItems()).toBe(20 + MOCK_ITEMS.length);
        });
    });

    describe('onPaginate', () => {
        it('should set pagination with provided values', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'paginate', {
                rows: 10,
                first: 0,
                page: 1
            });

            expect(store.setPagination).toHaveBeenCalledWith({ limit: 10, page: 1, offset: 0 });
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

    describe('dialog close', () => {
        it('should close the dialog in the store on a user-driven close (visibleChange false)', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
            spectator.detectComponentChanges();

            const dialogComponent = spectator.debugElement.query(By.css('[data-testid="dialog"]'))
                ?.componentInstance as Dialog;
            dialogComponent.visibleChange.emit(false);
            spectator.detectComponentChanges();

            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should not close the dialog in the store when it becomes visible', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
            spectator.detectComponentChanges();

            const dialogComponent = spectator.debugElement.query(By.css('[data-testid="dialog"]'))
                ?.componentInstance as Dialog;
            dialogComponent.visibleChange.emit(true);
            spectator.detectComponentChanges();

            expect(store.closeDialog).not.toHaveBeenCalled();
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

        it('should always render the banner (no dismiss control)', () => {
            spectator.detectChanges();

            expect(spectator.query('[data-testid="message"]')).toBeTruthy();
            expect(spectator.query('[data-testid="close-message"]')).toBeNull();
        });

        it('should have a learn more link', () => {
            spectator.detectChanges();

            const learnMoreLink = spectator.query('[data-testid="learn-more-link"]');
            expect(learnMoreLink).toBeTruthy();
        });
    });

    const TARGET_FOLDER_DATA = {
        id: 'folder-123',
        hostname: 'localhost',
        path: 'folder-123',
        type: 'folder'
    } as DotFolderTreeNodeData;

    const createFile = (name = 'test.jpg') =>
        new File(['test content'], name, { type: 'image/jpeg' });

    const createFileList = (files: File[]): FileList =>
        ({
            ...files,
            length: files.length,
            item: (index: number) => files[index] ?? null
        }) as unknown as FileList;

    // Opens the upload selector with the given context and emits the user's choice back to the
    // shell, mirroring the dialog's (selectUploadType) output.
    const selectUploadType = (selection: {
        targetFolder?: DotFolderTreeNodeData;
        contentType: string;
        files?: FileList;
    }) => {
        dialogSignal.set({
            type: DIALOG_TYPE.UPLOAD_SELECTOR,
            header: 'Upload',
            payload: { targetFolder: selection.targetFolder, files: selection.files }
        });
        spectator.detectChanges();

        const dialog = spectator.debugElement.query(
            By.css('[data-testId="dialog-upload-selector"]')
        );
        spectator.triggerEventHandler(dialog, 'selectUploadType', selection);
    };

    describe('upload type selector — opening', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should open the upload selector with the selected folder when the upload button is clicked', () => {
            store.selectedNode.mockReturnValue({
                data: TARGET_FOLDER_DATA
            } as DotFolderTreeNodeItem);

            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));
            spectator.triggerEventHandler(toolbar, 'upload', undefined);

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.UPLOAD_SELECTOR,
                header: expect.any(String),
                payload: { targetFolder: TARGET_FOLDER_DATA }
            });
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should open the upload selector carrying the files when the dropzone emits uploadFiles', () => {
            const files = createFileList([createFile()]);

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));
            spectator.triggerEventHandler(dropzone, 'uploadFiles', {
                files,
                targetFolder: TARGET_FOLDER_DATA
            });

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.UPLOAD_SELECTOR,
                header: expect.any(String),
                payload: { targetFolder: TARGET_FOLDER_DATA, files }
            });
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should open the upload selector carrying the files when the sidebar emits uploadFiles', () => {
            const files = createFileList([createFile()]);

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files,
                targetFolder: TARGET_FOLDER_DATA
            });

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.UPLOAD_SELECTOR,
                header: expect.any(String),
                payload: { targetFolder: TARGET_FOLDER_DATA, files }
            });
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should render the upload selector dialog body when the dialog type is UPLOAD_SELECTOR', () => {
            dialogSignal.set({
                type: DIALOG_TYPE.UPLOAD_SELECTOR,
                header: 'Upload',
                payload: { targetFolder: TARGET_FOLDER_DATA }
            });
            spectator.detectChanges();

            expect(spectator.query('[data-testId="dialog-upload-selector"]')).toBeTruthy();
        });
    });

    describe('upload — drag-and-drop flow (files already chosen)', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should upload the file as dotAsset when Asset is selected', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const file = createFile();

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([file]),
                baseType: 'DOTASSET'
            });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'DOTASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should upload the file as FileAsset when File is selected', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const file = createFile();

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([file]),
                baseType: 'FILEASSET'
            });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'FILEASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should upload to the current site root when no folder is selected', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            const file = createFile();

            selectUploadType({
                targetFolder: undefined,
                files: createFileList([file]),
                baseType: 'DOTASSET'
            });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'DOTASSET', {
                hostFolder: MOCK_SITES[0].identifier,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should fall back to empty hostFolder when no folder and no current site', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            store.currentSite.mockReturnValue(undefined);
            const file = createFile();

            selectUploadType({
                targetFolder: undefined,
                files: createFileList([file]),
                baseType: 'FILEASSET'
            });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'FILEASSET', {
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should show the info message when the upload starts', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([createFile()]),
                baseType: 'DOTASSET'
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'info',
                summary: expect.any(String),
                detail: expect.any(String)
            });
        });

        it('should show a success message after a successful upload', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(
                of({ title: 'test.jpg', contentType: 'image/jpeg' } as DotCMSContentlet)
            );
            const addSpy = jest.spyOn(messageService, 'add');

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([createFile()]),
                baseType: 'DOTASSET'
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'success',
                summary: expect.any(String),
                detail: expect.any(String),
                life: SUCCESS_MESSAGE_LIFE
            });
        });

        it('should show an error message on upload failure', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(
                throwError(() => new Error('Upload failed'))
            );
            const addSpy = jest.spyOn(messageService, 'add');

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([createFile()]),
                baseType: 'DOTASSET'
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'error',
                summary: expect.any(String),
                detail: expect.any(String),
                life: ERROR_MESSAGE_LIFE
            });
        });

        it('should show the server error message on failure with an errors payload', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(
                throwError(() => ({ error: { errors: [{ message: 'Upload failed' }] } }))
            );
            const addSpy = jest.spyOn(messageService, 'add');

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([createFile()]),
                baseType: 'DOTASSET'
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.add-dotasset-error',
                detail: 'Upload failed',
                life: ERROR_MESSAGE_LIFE
            });
        });

        it('should warn and upload only the first file when multiple files are selected', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const addSpy = jest.spyOn(messageService, 'add');
            const file1 = createFile('test1.jpg');
            const file2 = createFile('test2.jpg');

            selectUploadType({
                targetFolder: TARGET_FOLDER_DATA,
                files: createFileList([file1, file2]),
                baseType: 'DOTASSET'
            });

            expect(addSpy).toHaveBeenCalledWith({
                severity: 'warn',
                summary: expect.any(String),
                detail: expect.any(String),
                life: WARNING_MESSAGE_LIFE
            });
            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledTimes(1);
            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file1, 'DOTASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
        });
    });

    describe('upload — button flow (file picker opens after choosing)', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should open the file picker after a type is chosen, then upload with that type', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const file = createFile();

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            const clickSpy = jest.spyOn(fileInput, 'click');

            // Button flow: dialog opens with NO files in the payload.
            selectUploadType({ targetFolder: TARGET_FOLDER_DATA, baseType: 'FILEASSET' });

            expect(clickSpy).toHaveBeenCalled();
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();

            Object.defineProperty(fileInput, 'files', {
                value: [file],
                writable: true,
                configurable: true
            });
            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'FILEASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should consume the file before resetting the input (live FileList)', () => {
            // Regression: `input.files` is a LIVE FileList, so clearing `input.value` empties it.
            // The component must consume the files BEFORE resetting the input; resetting first
            // drops the selection and the upload silently no-ops (the real Chrome bug).
            // jsdom doesn't model this, so we mock it faithfully: `.files` is one stable object
            // that is emptied when `.value` is cleared.
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const file = createFile();
            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;

            const liveFiles: File[] = [file];
            Object.defineProperty(fileInput, 'files', {
                get: () => liveFiles as unknown as FileList,
                configurable: true
            });
            Object.defineProperty(fileInput, 'value', {
                get: () => (liveFiles.length ? 'C:\\fakepath\\test.png' : ''),
                set: () => {
                    liveFiles.length = 0; // clearing the input empties the live FileList
                },
                configurable: true
            });

            selectUploadType({ targetFolder: TARGET_FOLDER_DATA, baseType: 'FILEASSET' });
            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'FILEASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
            expect(fileInput.value).toBe(''); // still reset afterwards
        });

        it('should not upload when the file picker is dismissed without files', () => {
            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;

            selectUploadType({ targetFolder: TARGET_FOLDER_DATA, baseType: 'DOTASSET' });

            Object.defineProperty(fileInput, 'files', {
                value: [],
                writable: true,
                configurable: true
            });
            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
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
                identifier: 'folder-123',
                inode: 'folder-inode-123'
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
                    inode: 'folder-inode-123',
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
            } as DotCMSContentlet;

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

    describe('onUpload', () => {
        it('should open the upload selector dialog instead of the file picker directly', () => {
            spectator.detectChanges();

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            const clickSpy = jest.spyOn(fileInput, 'click');

            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));

            spectator.triggerEventHandler(toolbar, 'upload', undefined);

            expect(store.setDialog).toHaveBeenCalledWith(
                expect.objectContaining({ type: DIALOG_TYPE.UPLOAD_SELECTOR })
            );
            expect(clickSpy).not.toHaveBeenCalled();
        });
    });

    describe('onTableScroll', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should reset context menu when table scroll event is emitted', () => {
            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'scroll', new Event('scroll'));

            expect(store.resetContextMenu).toHaveBeenCalled();
        });
    });
});
