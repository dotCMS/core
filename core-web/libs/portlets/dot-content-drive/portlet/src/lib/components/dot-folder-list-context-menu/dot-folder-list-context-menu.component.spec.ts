import { describe, expect, it } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItemCommandEvent, MessageService } from 'primeng/api';
import { ContextMenu } from 'primeng/contextmenu';

import {
    DotContentSearchService,
    DotMessageService,
    DotRenderMode,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotContentDriveItem, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotFolderListViewContextMenuComponent } from './dot-folder-list-context-menu.component';

import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotFolderListViewContextMenuComponent', () => {
    let spectator: Spectator<DotFolderListViewContextMenuComponent>;
    let component: DotFolderListViewContextMenuComponent;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let workflowsActionsService: jest.Mocked<DotWorkflowsActionsService>;
    let navigationService: jest.Mocked<DotContentDriveNavigationService>;

    const mockContentlet = {
        contentType: 'blog',
        inode: 'test-inode-123',
        identifier: 'test-id',
        title: 'Test Blog',
        modDate: '2023-01-01',
        modUser: 'test-user',
        modUserName: 'Test User',
        baseType: 'CONTENT'
    } as unknown as DotContentDriveItem;

    const mockWorkflowActions = [
        {
            id: 'action-1',
            name: 'Publish',
            actionInputs: []
        },
        {
            id: 'action-2',
            name: 'Save/Assign',
            actionInputs: [{ id: 'input-1' }]
        }
    ] as DotCMSWorkflowAction[];

    const createComponent = createComponentFactory({
        component: DotFolderListViewContextMenuComponent,
        componentProviders: [DotContentDriveStore],
        providers: [
            // mockProvider(DotContentDriveStore, {
            //     contextMenu: jest.fn().mockReturnValue(null),
            //     status: jest.fn().mockReturnValue(DotContentDriveStatus.LOADED),
            //     patchContextMenu: jest.fn(),
            //     setStatus: jest.fn(),
            //     setShowAddToBundle: jest.fn(),
            //     reloadContentDrive: jest.fn()
            // }),
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
            // TODO: Can remove this mocking the store
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(ActivatedRoute, {
                snapshot: {
                    queryParams: {}
                }
            }),
            mockProvider(DotSystemConfigService),
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        store = spectator.inject(DotContentDriveStore, true) as jest.Mocked<
            InstanceType<typeof DotContentDriveStore>
        >;
        workflowsActionsService = spectator.inject(
            DotWorkflowsActionsService
        ) as jest.Mocked<DotWorkflowsActionsService>;
        navigationService = spectator.inject(
            DotContentDriveNavigationService
        ) as jest.Mocked<DotContentDriveNavigationService>;
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
            expect(component.$items()).toHaveLength(4); // Edit + 2 workflow actions + Add to Bundle
        });

        it('should build correct menu items for contentlet', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            expect(items[0].label).toBe('content-drive.context-menu.edit-contentlet');
            expect(items[1].label).toBe('Publish');
            expect(items[2].label).toBe('Save/Assign');
            expect(items[3].label).toBe('contenttypes.content.add_to_bundle');
        });

        it('should build correct menu items for htmlpageasset', async () => {
            const pageContentlet = {
                ...mockContentlet,
                contentType: 'htmlpageasset'
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
            items[3].command?.({} as unknown as MenuItemCommandEvent);

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
            expect(component.$items()).toHaveLength(4);
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
});
