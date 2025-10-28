import { describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItemCommandEvent, MessageService } from 'primeng/api';
import { ContextMenu } from 'primeng/contextmenu';

import {
    DotContentletService,
    DotFolderService,
    DotMessageService,
    DotRenderMode,
    DotSystemConfigService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotContentDriveItem } from '@dotcms/dotcms-models';
import { createFakeContentlet, mockWorkflowsActionsWithMove } from '@dotcms/utils-testing';

import { DotFolderListViewContextMenuComponent } from './dot-folder-list-context-menu.component';

import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotFolderListViewContextMenuComponent', () => {
    let spectator: Spectator<DotFolderListViewContextMenuComponent>;
    let component: DotFolderListViewContextMenuComponent;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
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
        componentProviders: [DotContentDriveStore],
        providers: [
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
            mockProvider(Router),
            mockProvider(DotWorkflowActionsFireService, {
                fireTo: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotWorkflowEventHandlerService, {
                open: jest.fn()
            }),
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {}
                }
            }),
            mockProvider(DotSystemConfigService),
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
            provideHttpClient()
        ]
    });

    beforeEach(() => {
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
            expect(component.$items()).toHaveLength(6); // Edit + Lock/Unlock + 3 workflow actions + Add to Bundle
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
            expect(items[5].label).toBe('contenttypes.content.add_to_bundle');
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

            const items = component.$items();
            items[5].command?.({} as unknown as MenuItemCommandEvent);

            expect(store.contextMenu().showAddToBundle).toBe(true);
        });

        it('should memoize menu items after first load', async () => {
            await component.getMenuItems(mockContextMenuData);

            expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(1);
            expect(component.$memoizedMenuItems()[mockContentlet.inode]).toBeDefined();
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
            expect(component.$items()).toHaveLength(6);
        });

        it('should not include move to folder workflow action', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            expect(items).not.toContain({
                label: 'Move',
                command: expect.any(Function)
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
                expect(items).toHaveLength(5); // Edit + 3 workflow actions + Add to Bundle (no lock/unlock)
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
});
