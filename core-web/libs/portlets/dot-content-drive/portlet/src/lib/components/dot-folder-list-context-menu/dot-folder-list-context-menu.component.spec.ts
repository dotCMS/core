import { describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItemCommandEvent, MessageService } from 'primeng/api';
import { ContextMenu } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotContentDriveService,
    DotContentletService,
    DotCurrentUserService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotSiteService,
    DotSystemConfigService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    AddToBundleService,
    DotAlertConfirmService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveFolder,
    DotContentDriveItem,
    PERMISSIONS_TYPE
} from '@dotcms/dotcms-models';
import {
    DotPermissionsIframeDialogComponent,
    DotPushHistoryIframeDialogComponent
} from '@dotcms/ui';
import { createFakeContentlet, mockWorkflowsActionsWithMove } from '@dotcms/utils-testing';

import { DotFolderListViewContextMenuComponent } from './dot-folder-list-context-menu.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotFolderListViewContextMenuComponent', () => {
    let spectator: Spectator<DotFolderListViewContextMenuComponent>;
    let component: DotFolderListViewContextMenuComponent;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    /** What the reachable-environments lookup answers; see the `mockProvider` below. */
    let pushPublishEnvironments: { id: string; name: string }[] = [];
    let workflowsActionsService: SpyObject<DotWorkflowsActionsService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;
    let dotWizardService: SpyObject<DotWizardService>;
    let workflowsActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let dotContentletService: SpyObject<DotContentletService>;
    let messageService: SpyObject<MessageService>;

    const mockContentlet = createFakeContentlet();

    const mockWorkflowActions = mockWorkflowsActionsWithMove; // 3 mocked workflow actions + 1 Move workflow action

    const createMockCanLock = (canLock: boolean, locked: boolean) => ({
        canLock,
        id: mockContentlet.identifier,
        inode: mockContentlet.inode,
        locked,
        lockedBy: locked ? 'admin@dotcms.com' : ''
    });

    const createComponent = createComponentFactory({
        component: DotFolderListViewContextMenuComponent,
        componentProviders: [DotContentDriveStore, DialogService],
        providers: [
            mockProvider(DotContentDriveService, {
                search: jest
                    .fn()
                    .mockReturnValue(
                        of({ list: [], contentTotalCount: 0, folderCount: 0, contentCount: 0 })
                    )
            }),
            mockProvider(DotWorkflowsActionsService, {
                getByInode: jest.fn().mockReturnValue(of(mockWorkflowActions))
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn()
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            mockProvider(DotWorkflowActionsFireService, {
                fireTo: jest.fn().mockReturnValue(of({}))
            }),
            // Required by the store's `withActionExecution`, which routes fire failures through it.
            mockProvider(DotHttpErrorManagerService),
            // Also required by `withActionExecution`, which fires Add to Bundle from the store.
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService, {
                // Reads the mutable on every call rather than being re-programmed per test:
                // `mockProvider` builds this `jest.fn` once for the whole file, and the `afterEach`
                // `clearAllMocks` drops any `mockReturnValue` set on it.
                getEnvironments: jest.fn(() => of(pushPublishEnvironments))
            }),
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotAlertConfirmService, { confirm: jest.fn() }),
            mockProvider(DotWorkflowEventHandlerService, {
                open: jest.fn()
            }),
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {}
                }
            }),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ userId: 'user-123', admin: true }))
            }),
            mockProvider(DotWizardService, {
                open: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotContentletService, {
                canLock: jest.fn().mockReturnValue(of(createMockCanLock(true, false))),
                lockContent: jest.fn().mockReturnValue(of(mockContentlet)),
                unlockContent: jest.fn().mockReturnValue(of(mockContentlet))
            }),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        pushPublishEnvironments = [];
        spectator = createComponent();
        component = spectator.component;
        store = spectator.inject(DotContentDriveStore, true);
        workflowsActionsService = spectator.inject(DotWorkflowsActionsService);
        navigationService = spectator.inject(DotContentDriveNavigationService);
        dotWizardService = spectator.inject(DotWizardService);
        workflowsActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotContentletService = spectator.inject(DotContentletService);
        messageService = spectator.inject(MessageService);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render context menu', () => {
            const contextMenu = spectator.query(ContextMenu);
            expect(contextMenu).toBeTruthy();
        });
    });

    describe('hideContextMenu', () => {
        it('should patch context menu with null triggered event', () => {
            component.hideContextMenu();

            expect(store.contextMenu().triggeredEvent).toBeNull();
        });
    });

    describe('getMenuItems', () => {
        const mockEvent = new MouseEvent('contextmenu');
        const mockContextMenuData: DotContentDriveContextMenu = {
            triggeredEvent: mockEvent,
            contentlet: mockContentlet,
            showAddToBundle: false
        };

        beforeEach(() => {
            component.$memoizedMenuItems.set({});
        });

        it('should return early if no triggered event', async () => {
            await component.getMenuItems({
                triggeredEvent: null,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            expect(workflowsActionsService.getByInode).not.toHaveBeenCalled();
        });

        it('should return early if no contentlet', async () => {
            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: null,
                showAddToBundle: false
            });

            expect(workflowsActionsService.getByInode).not.toHaveBeenCalled();
        });

        it('should fetch workflow actions and build menu items', async () => {
            await component.getMenuItems(mockContextMenuData);

            expect(workflowsActionsService.getByInode).toHaveBeenCalledWith(
                mockContentlet.inode,
                DotRenderMode.LISTING
            );
            // Edit + Lock/Unlock + 3 workflow actions + Push Publish + Add to Bundle
            expect(component.$items()).toHaveLength(7);
        });

        it('should fetch canLock data when building menu items', async () => {
            await component.getMenuItems(mockContextMenuData);

            expect(dotContentletService.canLock).toHaveBeenCalledWith(mockContentlet.inode);
        });

        it('should build correct menu items for contentlet', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            expect(items[0].label).toBe('content-drive.context-menu.edit-content');
            expect(items[1].label).toBe('content-drive.context-menu.lock');
            expect(items[2].label).toBe('Assign Workflow');
            expect(items[3].label).toBe('Save');
            expect(items[4].label).toBe('Save / Publish');
            expect(items[5].label).toBe('contenttypes.content.push_publish');
            expect(items[6].label).toBe('contenttypes.content.add_to_bundle');
        });

        it('should build correct menu items for Pages contentlet', async () => {
            const pageContentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE
            } as DotContentDriveItem;
            const pageContextMenuData = { ...mockContextMenuData, contentlet: pageContentlet };

            await component.getMenuItems(pageContextMenuData);

            const items = component.$items();
            expect(items[0].label).toBe('content-drive.context-menu.edit-page');
        });

        it('should call navigation service when edit action is triggered', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            items[0].command?.({} as unknown as MenuItemCommandEvent);

            expect(navigationService.editContent).toHaveBeenCalledWith(mockContentlet);
        });

        it('should call setShowAddToBundle when add to bundle is triggered', async () => {
            await component.getMenuItems(mockContextMenuData);

            // Found by label rather than index: the push group grew, and an index here would
            // silently point at Push Publish instead.
            component
                .$items()
                .find((item) => item.label === 'contenttypes.content.add_to_bundle')
                ?.command?.({} as unknown as MenuItemCommandEvent);

            expect(store.contextMenu().showAddToBundle).toBe(true);
        });

        it('should memoize menu items after first load using inode as key for contentlets', async () => {
            await component.getMenuItems(mockContextMenuData);

            expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(1);
            expect(component.$memoizedMenuItems()[mockContentlet.inode]).toBeDefined();
            expect(component.$memoizedMenuItems()[mockContentlet.identifier]).toBeUndefined();
        });

        it('should use memoized items on second call without fetching', async () => {
            // First call
            // Mock the contextMenu viewChild
            const mockContextMenu = {
                show: jest.fn(),
                visible: jest.fn().mockReturnValue(false)
            } as unknown as ContextMenu;

            jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

            await component.getMenuItems(mockContextMenuData);
            const firstCallCount = workflowsActionsService.getByInode.mock.calls.length;

            // Second call
            await component.getMenuItems(mockContextMenuData);

            expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(firstCallCount);
            expect(component.$items()).toHaveLength(7);
        });

        it('should not include move to folder workflow action', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            expect(items).not.toContain({
                label: 'Move',
                command: expect.any(Function)
            });
        });

        describe('folder handling', () => {
            const mockFolder: DotContentDriveFolder = {
                __icon__: 'folderIcon',
                defaultFileType: 'FileAsset',
                description: 'Test folder',
                extension: 'folder',
                filesMasks: '*',
                hasTitleImage: false,
                hostId: 'host-123',
                iDate: Date.now(),
                identifier: 'folder-123',
                inode: 'folder-inode-123',
                mimeType: 'folder',
                modDate: Date.now(),
                name: 'Test Folder',
                owner: 'admin',
                parent: '/',
                path: '/documents/',
                permissions: [PERMISSIONS_TYPE.EDIT],
                showOnMenu: true,
                sortOrder: 0,
                title: 'Test Folder',
                type: 'folder'
            };

            const mockFolderContextMenuData: DotContentDriveContextMenu = {
                triggeredEvent: mockEvent,
                contentlet: mockFolder,
                showAddToBundle: false
            };

            it('should build the EDIT-gated items for a folder with only EDIT', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                // Both entries behind EDIT: Folder Settings, and Delete last.
                expect(component.$items().map((item) => item.label)).toEqual([
                    'content-drive.context-menu.edit-folder',
                    'content-drive.context-menu.delete-folder'
                ]);
            });

            it('should not call workflowsActionsService for folders', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                expect(workflowsActionsService.getByInode).not.toHaveBeenCalled();
            });

            it('should not call dotContentletService.canLock for folders', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                expect(dotContentletService.canLock).not.toHaveBeenCalled();
            });

            it('should open folder dialog when edit folder action is triggered', async () => {
                jest.spyOn(store, 'setDialog');
                await component.getMenuItems(mockFolderContextMenuData);

                const items = component.$items();
                items[0].command?.({} as unknown as MenuItemCommandEvent);

                expect(store.setDialog).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.FOLDER,
                    header: 'content-drive.dialog.folder.header.edit',
                    payload: mockFolder
                });
            });

            it('should show context menu for folders', async () => {
                const mockContextMenu = {
                    show: jest.fn(),
                    visible: jest.fn().mockReturnValue(false)
                } as unknown as ContextMenu;

                jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

                await component.getMenuItems(mockFolderContextMenuData);

                expect(mockContextMenu.show).toHaveBeenCalledWith(mockEvent);
            });

            it('should memoize folder menu items using identifier as key', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                expect(component.$memoizedMenuItems()[mockFolder.identifier]).toBeDefined();
                expect(component.$memoizedMenuItems()[mockFolder.identifier]).toHaveLength(2);
            });

            it('should use memoized folder menu items on second call', async () => {
                const mockContextMenu = {
                    show: jest.fn(),
                    visible: jest.fn().mockReturnValue(false)
                } as unknown as ContextMenu;

                jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

                // First call
                await component.getMenuItems(mockFolderContextMenuData);
                const firstCallCount = workflowsActionsService.getByInode.mock.calls.length;

                // Second call
                await component.getMenuItems(mockFolderContextMenuData);

                expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(firstCallCount);
                expect(component.$items()).toHaveLength(2);
            });

            it('should build empty menu when folder has no permissions', async () => {
                const folderNoPermissions: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: []
                };
                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: folderNoPermissions,
                    showAddToBundle: false
                });

                expect(component.$items()).toHaveLength(0);
            });

            it('should not show context menu when folder has no applicable permissions', async () => {
                const mockContextMenu = {
                    show: jest.fn(),
                    visible: jest.fn().mockReturnValue(false)
                } as unknown as ContextMenu;

                jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

                const folderNoPermissions: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: []
                };

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: folderNoPermissions,
                    showAddToBundle: false
                });

                expect(mockContextMenu.show).not.toHaveBeenCalled();
            });

            it('should use identifier as memoization key for folders, not inode', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                expect(component.$memoizedMenuItems()[mockFolder.identifier]).toBeDefined();
                expect(component.$memoizedMenuItems()[mockFolder.inode]).toBeUndefined();
            });

            describe('permissions dialog', () => {
                const folderWithEditPermissions: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: [PERMISSIONS_TYPE.EDIT, PERMISSIONS_TYPE.EDIT_PERMISSIONS]
                };

                const folderContextMenuWithEditPermissions: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: folderWithEditPermissions,
                    showAddToBundle: false
                };

                let dialogService: SpyObject<DialogService>;

                beforeEach(() => {
                    dialogService = spectator.inject(DialogService, true);
                    jest.spyOn(dialogService, 'open').mockReturnValue(null as never);
                    component.$memoizedMenuItems.set({});
                });

                it('should show Edit-Permissions item when folder has EDIT_PERMISSIONS permission', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    expect(
                        component.$items().find((item) => item.label === 'Edit-Permissions')
                    ).toBeDefined();
                });

                it('should not show Edit-Permissions item when folder lacks EDIT_PERMISSIONS permission', async () => {
                    await component.getMenuItems(mockFolderContextMenuData);

                    expect(
                        component.$items().find((item) => item.label === 'Edit-Permissions')
                    ).toBeUndefined();
                });

                it('should open DotPermissionsIframeDialogComponent with correct config when triggered', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    component
                        .$items()
                        .find((item) => item.label === 'Edit-Permissions')
                        ?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(dialogService.open).toHaveBeenCalledWith(
                        DotPermissionsIframeDialogComponent,
                        expect.objectContaining({
                            width: 'min(92vw, 75rem)',
                            closable: true,
                            closeOnEscape: true,
                            data: {
                                url: `/html/portlet/ext/folders/permissions.jsp?folderIdentifier=${folderWithEditPermissions.identifier}&popup=true`
                            }
                        })
                    );
                });
            });

            describe('push history dialog', () => {
                const folderWithEditPermissions: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: [PERMISSIONS_TYPE.EDIT, PERMISSIONS_TYPE.EDIT_PERMISSIONS]
                };

                const folderContextMenuWithEditPermissions: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: folderWithEditPermissions,
                    showAddToBundle: false
                };

                let dialogService: SpyObject<DialogService>;

                beforeEach(() => {
                    dialogService = spectator.inject(DialogService, true);
                    jest.spyOn(dialogService, 'open').mockReturnValue(null as never);
                    component.$memoizedMenuItems.set({});
                });

                it('should show Push History item when folder has EDIT_PERMISSIONS permission', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    expect(
                        component
                            .$items()
                            .find(
                                (item) => item.label === 'content-drive.context-menu.push-history'
                            )
                    ).toBeDefined();
                });

                it('should not show Push History item when folder lacks EDIT_PERMISSIONS permission', async () => {
                    await component.getMenuItems(mockFolderContextMenuData);

                    expect(
                        component
                            .$items()
                            .find(
                                (item) => item.label === 'content-drive.context-menu.push-history'
                            )
                    ).toBeUndefined();
                });

                it('should open DotPushHistoryIframeDialogComponent with correct config when triggered', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    component
                        .$items()
                        .find((item) => item.label === 'content-drive.context-menu.push-history')
                        ?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(dialogService.open).toHaveBeenCalledWith(
                        DotPushHistoryIframeDialogComponent,
                        expect.objectContaining({
                            width: 'min(92vw, 75rem)',
                            closable: true,
                            closeOnEscape: true,
                            data: {
                                url: `/html/portlet/ext/folders/push_history.jsp?folderIdentifier=${folderWithEditPermissions.identifier}&popup=true`
                            }
                        })
                    );
                });
            });

            describe('push publish', () => {
                const ENVIRONMENT = { id: 'env-1', name: 'Production' };

                const folderWithPublish: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: [PERMISSIONS_TYPE.EDIT, PERMISSIONS_TYPE.PUBLISH]
                };

                const folderContextMenuWithPublish: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: folderWithPublish,
                    showAddToBundle: false
                };

                let pushPublishDialogService: SpyObject<DotPushPublishDialogService>;

                /** Re-runs the real store lookup so the gate settles on "reachable". */
                const withEnvironments = (): void => {
                    pushPublishEnvironments = [ENVIRONMENT];
                    store.loadPushPublishEnvironments();
                };

                const pushPublishItem = () =>
                    component
                        .$items()
                        .find((item) => item.label === 'contenttypes.content.push_publish');

                beforeEach(() => {
                    pushPublishDialogService = spectator.inject(DotPushPublishDialogService);
                    component.$memoizedMenuItems.set({});
                });

                it('should show Push Publish when the folder has PUBLISH permission', async () => {
                    withEnvironments();

                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()).toBeDefined();
                });

                it('should not show Push Publish when the folder lacks PUBLISH permission', async () => {
                    withEnvironments();

                    await component.getMenuItems(mockFolderContextMenuData);

                    expect(pushPublishItem()).toBeUndefined();
                });

                it('should open the push publish dialog with the folder identifier', async () => {
                    withEnvironments();

                    await component.getMenuItems(folderContextMenuWithPublish);
                    pushPublishItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(pushPublishDialogService.open).toHaveBeenCalledWith({
                        assetIdentifier: folderWithPublish.identifier,
                        title: 'contenttypes.content.push_publish'
                    });
                });

                it('should enable the item once an environment is reachable', async () => {
                    withEnvironments();

                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()?.disabled).toBe(false);
                    expect(pushPublishItem()?.tooltip).toBeUndefined();
                });

                // Offered but disabled rather than hidden: nothing is missing from dotCMS, something
                // is missing from the configuration, and the fix is an administrator's. The tooltip
                // is what says so.
                it('should disable the item with a tooltip when no environment is reachable', async () => {
                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()?.disabled).toBe(true);
                    expect(pushPublishItem()?.tooltip).toBe(
                        'content-drive.action-center.no-environments'
                    );
                });

                it('should not open the dialog while the item is disabled', async () => {
                    await component.getMenuItems(folderContextMenuWithPublish);
                    pushPublishItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(pushPublishDialogService.open).not.toHaveBeenCalled();
                });
            });

            describe('add to bundle', () => {
                const folderWithPublish: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: [PERMISSIONS_TYPE.EDIT, PERMISSIONS_TYPE.PUBLISH]
                };

                const folderContextMenuWithPublish: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: folderWithPublish,
                    showAddToBundle: false
                };

                const addToBundleItem = () =>
                    component
                        .$items()
                        .find((item) => item.label === 'contenttypes.content.add_to_bundle');

                beforeEach(() => {
                    component.$memoizedMenuItems.set({});
                });

                it('should show Add to Bundle when the folder has PUBLISH permission', async () => {
                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(addToBundleItem()).toBeDefined();
                });

                it('should not show Add to Bundle when the folder lacks PUBLISH permission', async () => {
                    await component.getMenuItems(mockFolderContextMenuData);

                    expect(addToBundleItem()).toBeUndefined();
                });

                // The shell renders the bundle dialog off the context menu's own target, keyed on
                // identifier, which is the one id a folder from the sidebar tree always carries.
                it('should flag the shell to show the bundle dialog', async () => {
                    jest.spyOn(store, 'setShowAddToBundle');

                    await component.getMenuItems(folderContextMenuWithPublish);
                    addToBundleItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(store.setShowAddToBundle).toHaveBeenCalledWith(true);
                });
            });

            describe('delete', () => {
                const folderWithEdit: DotContentDriveFolder = {
                    ...mockFolder,
                    permissions: [PERMISSIONS_TYPE.EDIT]
                };

                const folderContextMenuWithEdit: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: folderWithEdit,
                    showAddToBundle: false
                };

                let alertConfirmService: SpyObject<DotAlertConfirmService>;
                let folderService: SpyObject<DotFolderService>;

                const deleteItem = () =>
                    component
                        .$items()
                        .find((item) => item.label === 'content-drive.context-menu.delete-folder');

                beforeEach(() => {
                    alertConfirmService = spectator.inject(DotAlertConfirmService);
                    folderService = spectator.inject(DotFolderService);
                    folderService.deleteFolder = jest.fn().mockReturnValue(of(true));
                    // The delete path is built from the browsed site, so it has to be a real one
                    // rather than the store's SYSTEM_HOST default.
                    store.initContentDrive({
                        currentSite: { hostname: 'demo.dotcms.com' } as never,
                        path: '/',
                        filters: {},
                        isTreeExpanded: true
                    });
                    component.$memoizedMenuItems.set({});
                });

                // EDIT, because that is what FolderAPIImpl.delete enforces (`:438`).
                it('should show Delete when the folder has EDIT permission', async () => {
                    await component.getMenuItems(folderContextMenuWithEdit);

                    expect(deleteItem()).toBeDefined();
                });

                it('should not show Delete when the folder lacks EDIT permission', async () => {
                    await component.getMenuItems({
                        triggeredEvent: mockEvent,
                        contentlet: { ...mockFolder, permissions: [PERMISSIONS_TYPE.READ] },
                        showAddToBundle: false
                    });

                    expect(deleteItem()).toBeUndefined();
                });

                // "Accept" is the service default and says nothing about what is about to happen.
                it('should label the confirm button Delete rather than Accept', async () => {
                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(alertConfirmService.confirm).toHaveBeenCalledWith(
                        expect.objectContaining({
                            footerLabel: expect.objectContaining({ accept: 'Delete' })
                        })
                    );
                });

                // Recursive and irreversible on the server, so it must never fire straight from a click.
                it('should ask for confirmation rather than deleting immediately', async () => {
                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(alertConfirmService.confirm).toHaveBeenCalled();
                    expect(folderService.deleteFolder).not.toHaveBeenCalled();
                });

                // The sidebar tree listed the folder too, and `reloadContentDrive` only reloads the
                // grid, so the tree keeps showing a folder that no longer exists without this.
                it('should refetch the folder tree once the delete succeeds', async () => {
                    jest.spyOn(store, 'loadFolders');

                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                    (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                    expect(store.loadFolders).toHaveBeenCalled();
                });

                it('should not refetch the folder tree when the delete fails', async () => {
                    folderService.deleteFolder = jest
                        .fn()
                        .mockReturnValue(throwError(() => new Error('nope')));
                    jest.spyOn(store, 'loadFolders');

                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                    (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                    expect(store.loadFolders).not.toHaveBeenCalled();
                });

                it('should delete by path once confirmed, and reload the drive', async () => {
                    jest.spyOn(store, 'reloadContentDrive');
                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);

                    // Run whatever the confirm was armed with, as accepting the dialog would.
                    const confirmArgs = (alertConfirmService.confirm as jest.Mock).mock.lastCall[0];
                    confirmArgs.accept();

                    expect(folderService.deleteFolder).toHaveBeenCalledWith(
                        `//demo.dotcms.com${folderWithEdit.path}`
                    );
                    expect(store.reloadContentDrive).toHaveBeenCalled();
                });

                it('should not reload the drive when the delete fails', async () => {
                    folderService.deleteFolder = jest
                        .fn()
                        .mockReturnValue(throwError(() => new Error('nope')));
                    jest.spyOn(store, 'reloadContentDrive');

                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                    (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                    expect(store.reloadContentDrive).not.toHaveBeenCalled();
                });
            });

            describe('item order', () => {
                it('should order settings, permissions, then the push group', async () => {
                    pushPublishEnvironments = [{ id: 'env-1', name: 'Production' }];
                    store.loadPushPublishEnvironments();
                    component.$memoizedMenuItems.set({});

                    await component.getMenuItems({
                        triggeredEvent: mockEvent,
                        contentlet: {
                            ...mockFolder,
                            permissions: [
                                PERMISSIONS_TYPE.EDIT,
                                PERMISSIONS_TYPE.EDIT_PERMISSIONS,
                                PERMISSIONS_TYPE.PUBLISH
                            ]
                        },
                        showAddToBundle: false
                    });

                    expect(component.$items().map((item) => item.label)).toEqual([
                        'content-drive.context-menu.edit-folder',
                        'Edit-Permissions',
                        'contenttypes.content.push_publish',
                        'contenttypes.content.add_to_bundle',
                        'content-drive.context-menu.push-history',
                        'content-drive.context-menu.delete-folder'
                    ]);
                });
            });
        });

        describe('lock/unlock functionality', () => {
            const mockEvent = new MouseEvent('contextmenu');

            it('should show lock action when content is unlocked and can be locked', async () => {
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, false)));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const lockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.lock'
                );

                expect(lockItem).toBeDefined();
            });

            it('should show unlock action when content is locked and can be unlocked', async () => {
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, true)));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const unlockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.unlock'
                );

                expect(unlockItem).toBeDefined();
            });

            it('should not show lock/unlock action when content cannot be locked', async () => {
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(false, false)));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const lockItem = items.find(
                    (item) =>
                        item.label === 'content-drive.context-menu.lock' ||
                        item.label === 'content-drive.context-menu.unlock'
                );

                expect(lockItem).toBeUndefined();
                // Edit + 3 workflow actions + Push Publish + Add to Bundle (no lock/unlock)
                expect(items).toHaveLength(6);
            });

            it('should call lockContent when lock action is triggered on unlocked content', async () => {
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, false)));
                dotContentletService.lockContent.mockReturnValue(of(mockContentlet));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const lockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.lock'
                );

                lockItem?.command?.({} as unknown as MenuItemCommandEvent);

                expect(dotContentletService.lockContent).toHaveBeenCalledWith(mockContentlet.inode);
            });

            it('should call unlockContent when unlock action is triggered on locked content', async () => {
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, true)));
                dotContentletService.unlockContent.mockReturnValue(of(mockContentlet));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const unlockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.unlock'
                );

                unlockItem?.command?.({} as unknown as MenuItemCommandEvent);

                expect(dotContentletService.unlockContent).toHaveBeenCalledWith(
                    mockContentlet.inode
                );
            });

            it('should show success message when lock action succeeds', async () => {
                jest.useFakeTimers();
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, false)));
                dotContentletService.lockContent.mockReturnValue(of(mockContentlet));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const lockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.lock'
                );

                lockItem?.command?.({} as unknown as MenuItemCommandEvent);

                jest.advanceTimersByTime(0);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'content-drive.toast.lock-success',
                    detail: 'content-drive.toast.lock-success-detail'
                });

                jest.useRealTimers();
            });

            it('should show success message when unlock action succeeds', async () => {
                jest.useFakeTimers();
                dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, true)));
                dotContentletService.unlockContent.mockReturnValue(of(mockContentlet));

                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                const items = component.$items();
                const unlockItem = items.find(
                    (item) => item.label === 'content-drive.context-menu.unlock'
                );

                unlockItem?.command?.({} as unknown as MenuItemCommandEvent);

                jest.advanceTimersByTime(0);

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'content-drive.toast.unlock-success',
                    detail: 'content-drive.toast.unlock-success-detail'
                });

                jest.useRealTimers();
            });
        });

        describe('push publish for contentlets', () => {
            let pushPublishDialogService: SpyObject<DotPushPublishDialogService>;

            const pushPublishItem = () =>
                component
                    .$items()
                    .find((item) => item.label === 'contenttypes.content.push_publish');

            beforeEach(() => {
                pushPublishDialogService = spectator.inject(DotPushPublishDialogService);
                component.$memoizedMenuItems.set({});
            });

            it('should offer Push Publish for a contentlet', async () => {
                await component.getMenuItems(mockContextMenuData);

                expect(pushPublishItem()).toBeDefined();
            });

            it('should open the push publish dialog with the contentlet identifier', async () => {
                pushPublishEnvironments = [{ id: 'env-1', name: 'Production' }];
                store.loadPushPublishEnvironments();

                await component.getMenuItems(mockContextMenuData);
                pushPublishItem()?.command?.({} as unknown as MenuItemCommandEvent);

                expect(pushPublishDialogService.open).toHaveBeenCalledWith({
                    assetIdentifier: mockContentlet.identifier,
                    title: 'contenttypes.content.push_publish'
                });
            });

            it('should disable the item with a tooltip when no environment is reachable', async () => {
                await component.getMenuItems(mockContextMenuData);

                expect(pushPublishItem()?.disabled).toBe(true);
                expect(pushPublishItem()?.tooltip).toBe(
                    'content-drive.action-center.no-environments'
                );
            });
        });
    });

    describe('statusEffect', () => {
        it('should not clear memoized items when status is not loading', () => {
            const memoizedItems = { 'test-inode': [] };
            component.$memoizedMenuItems.set(memoizedItems);
            // store.status.set(DotContentDriveStatus.LOADED);
            store.setStatus(DotContentDriveStatus.LOADED);

            spectator.detectComponentChanges();

            expect(component.$memoizedMenuItems()).toEqual(memoizedItems);
        });

        it('should clear memoized items when status is loading', async () => {
            component.$memoizedMenuItems.set({ 'test-inode': [] });
            store.setStatus(DotContentDriveStatus.LOADED);

            spectator.detectChanges();

            store.setStatus(DotContentDriveStatus.LOADING);

            spectator.detectChanges();

            expect(component.$memoizedMenuItems()).toEqual({});
        });
    });

    describe('closeOnContextMenuReset', () => {
        it('should hide context menu when contentlet is null and menu is visible', () => {
            const mockContextMenu = {
                hide: jest.fn(),
                visible: jest.fn().mockReturnValue(true)
            } as unknown as ContextMenu;

            jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

            store.patchContextMenu({
                contentlet: null,
                triggeredEvent: null,
                showAddToBundle: false
            });

            spectator.detectChanges();

            expect(mockContextMenu.hide).toHaveBeenCalled();
        });

        it('should not hide context menu when contentlet is null and menu is not visible', () => {
            const mockContextMenu = {
                hide: jest.fn(),
                visible: jest.fn().mockReturnValue(false)
            } as unknown as ContextMenu;

            jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

            store.patchContextMenu({
                contentlet: null,
                triggeredEvent: null,
                showAddToBundle: false
            });

            spectator.detectChanges();

            expect(mockContextMenu.hide).not.toHaveBeenCalled();
        });

        it('should not hide context menu when contentlet exists', () => {
            const mockContextMenu = {
                hide: jest.fn(),
                visible: jest.fn().mockReturnValue(true)
            } as unknown as ContextMenu;

            jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

            store.patchContextMenu({
                contentlet: mockContentlet,
                triggeredEvent: null,
                showAddToBundle: false
            });

            spectator.detectChanges();

            expect(mockContextMenu.hide).not.toHaveBeenCalled();
        });
    });

    describe('wizard', () => {
        const mockEvent = new MouseEvent('contextmenu');
        it('should open the wizard', async () => {
            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();

            // Assign Workflow (now at index 2 because of lock/unlock at index 1)
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            expect(dotWizardService.open).toHaveBeenCalled();
        });

        it('should fire the workflow action after the wizard is closed', async () => {
            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();

            // Assign Workflow (now at index 2 because of lock/unlock at index 1)
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            dotWizardService.open.mockReturnValue(of({}));

            expect(workflowsActionsFireService.fireTo).toHaveBeenCalled();
        });
    });

    describe('workflow actions', () => {
        const mockEvent = new MouseEvent('contextmenu');

        it('should execute workflow action without wizard when action has no inputs', async () => {
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1], // "Save" action
                    actionInputs: []
                }
            ];

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            // Save action is at index 2 (Edit at 0, Lock/Unlock at 1, Save at 2)
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            expect(dotWizardService.open).not.toHaveBeenCalled();
            expect(workflowsActionsFireService.fireTo).toHaveBeenCalledWith({
                actionId: mockWorkflowWithoutInputs[0].id,
                inode: mockContentlet.inode,
                data: undefined
            });
        });

        it('should show success message when workflow action succeeds', async () => {
            jest.useFakeTimers();
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1],
                    actionInputs: []
                }
            ];

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));
            workflowsActionsFireService.fireTo.mockReturnValue(of(mockContentlet));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'content-drive.toast.workflow-executed'
            });

            jest.useRealTimers();
        });

        it('should show error message when workflow action fails', async () => {
            jest.useFakeTimers();
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1],
                    actionInputs: []
                }
            ];
            const mockError = new Error('Workflow action failed');

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));
            workflowsActionsFireService.fireTo.mockReturnValue(throwError(() => mockError));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.toast.workflow-error',
                life: 4500
            });

            jest.useRealTimers();
        });

        it('should set status to LOADED when workflow action fails', async () => {
            jest.useFakeTimers();
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1],
                    actionInputs: []
                }
            ];
            const mockError = new Error('Workflow action failed');

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));
            workflowsActionsFireService.fireTo.mockReturnValue(throwError(() => mockError));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(store.status()).toBe(DotContentDriveStatus.LOADED);

            jest.useRealTimers();
        });

        it('should reload content drive when workflow action succeeds', async () => {
            jest.useFakeTimers();
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1],
                    actionInputs: []
                }
            ];
            const reloadSpy = jest.spyOn(store, 'reloadContentDrive');

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));
            workflowsActionsFireService.fireTo.mockReturnValue(of(mockContentlet));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(reloadSpy).toHaveBeenCalled();

            jest.useRealTimers();
        });

        it('should set status to LOADING when workflow action is triggered', async () => {
            const mockWorkflowWithoutInputs = [
                {
                    ...mockWorkflowActions[1],
                    actionInputs: []
                }
            ];

            workflowsActionsService.getByInode.mockReturnValue(of(mockWorkflowWithoutInputs));
            workflowsActionsFireService.fireTo.mockReturnValue(of(mockContentlet));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            items[2].command?.({} as unknown as MenuItemCommandEvent);

            expect(store.status()).toBe(DotContentDriveStatus.LOADING);
        });
    });

    describe('lock/unlock error handling', () => {
        const mockEvent = new MouseEvent('contextmenu');

        it('should show error message when lock action fails', async () => {
            jest.useFakeTimers();
            const mockError = new Error('Lock failed');

            dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, false)));
            dotContentletService.lockContent.mockReturnValue(throwError(() => mockError));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            const lockItem = items.find((item) => item.label === 'content-drive.context-menu.lock');

            lockItem?.command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.toast.lock-error',
                detail: 'content-drive.toast.lock-error-detail',
                life: 4500
            });

            jest.useRealTimers();
        });

        it('should show error message when unlock action fails', async () => {
            jest.useFakeTimers();
            const mockError = new Error('Unlock failed');

            dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, true)));
            dotContentletService.unlockContent.mockReturnValue(throwError(() => mockError));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            const unlockItem = items.find(
                (item) => item.label === 'content-drive.context-menu.unlock'
            );

            unlockItem?.command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.toast.unlock-error',
                detail: 'content-drive.toast.unlock-error-detail',
                life: 4500
            });

            jest.useRealTimers();
        });

        it('should reload content drive when lock succeeds', async () => {
            jest.useFakeTimers();
            const reloadSpy = jest.spyOn(store, 'reloadContentDrive');

            dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, false)));
            dotContentletService.lockContent.mockReturnValue(of(mockContentlet));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            const lockItem = items.find((item) => item.label === 'content-drive.context-menu.lock');

            lockItem?.command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(reloadSpy).toHaveBeenCalled();

            jest.useRealTimers();
        });

        it('should reload content drive when unlock succeeds', async () => {
            jest.useFakeTimers();
            const reloadSpy = jest.spyOn(store, 'reloadContentDrive');

            dotContentletService.canLock.mockReturnValue(of(createMockCanLock(true, true)));
            dotContentletService.unlockContent.mockReturnValue(of(mockContentlet));

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            const items = component.$items();
            const unlockItem = items.find(
                (item) => item.label === 'content-drive.context-menu.unlock'
            );

            unlockItem?.command?.({} as unknown as MenuItemCommandEvent);

            jest.advanceTimersByTime(0);

            expect(reloadSpy).toHaveBeenCalled();

            jest.useRealTimers();
        });
    });
});
