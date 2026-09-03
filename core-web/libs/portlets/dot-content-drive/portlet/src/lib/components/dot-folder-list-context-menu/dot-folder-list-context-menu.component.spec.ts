import { describe, expect, it } from '@jest/globals';
import { patchState } from '@ngrx/signals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem, MenuItemCommandEvent, MessageService } from 'primeng/api';
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
import { DotJspIframeDialogComponent } from '@dotcms/ui';
import { createFakeContentlet, mockWorkflowsActionsWithMove } from '@dotcms/utils-testing';

import { DotFolderListViewContextMenuComponent } from './dot-folder-list-context-menu.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveContextMenu, DotContentDriveStatus } from '../../shared/models';
import { DotContentDriveNavigationService } from '../../shared/services';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * Finds a menu entry by label at any depth. Index-free and depth-free, so regrouping the workflow
 * actions cannot silently retarget these assertions.
 */
const find = (items: MenuItem[], label: string): MenuItem | undefined => {
    for (const item of items) {
        if (item.label === label) {
            return item;
        }

        const nested = item.items && find(item.items, label);

        if (nested) {
            return nested;
        }
    }

    return undefined;
};

/**
 * The menu as an ordered list of labels, with separators rendered as `SEPARATOR`.
 *
 * Separators carry grouping meaning here — they are what holds the destructive actions apart from
 * the rest — so they belong in the assertion rather than being filtered out, and a bare
 * `.map(item => item.label)` would render them as an unreadable `undefined`.
 */
const SEPARATOR = '\u2014 separator \u2014';
const labels = (items: MenuItem[]): string[] =>
    items.map((item) => (item.separator ? SEPARATOR : item.label));

/** Invokes a menu entry by label, wherever it lives in the tree. */
const invoke = (items: MenuItem[], label: string) => {
    const item = find(items, label);

    if (!item?.command) {
        throw new Error(`No command found on menu item "${label}"`);
    }

    item.command({} as unknown as MenuItemCommandEvent);
};

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
    let dotMessageService: SpyObject<DotMessageService>;

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
        dotMessageService = spectator.inject(DotMessageService);
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
            // Actions caption + Edit + Lock/Unlock + Push Publish + Add to Bundle, then a
            // separator, the Workflows caption and the fixture's 3 non-Move actions beneath it.
            expect(component.$items()).toHaveLength(10);
        });

        it('should fetch canLock data when building menu items', async () => {
            await component.getMenuItems(mockContextMenuData);

            expect(dotContentletService.canLock).toHaveBeenCalledWith(mockContentlet.inode);
        });

        it('should build correct menu items for contentlet', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            // Asserted as one ordered list rather than index by index: this test is about the
            // order, so the positions have to stay pinned, but an inserted item then fails once
            // with the whole list in the diff instead of pointing at whichever index shifted.
            //
            // Built-in items first, then the Workflows caption with its actions inline beneath it.
            expect(labels(items)).toEqual([
                'content-drive.context-menu.actions',
                'content-drive.context-menu.edit-content',
                'content-drive.context-menu.lock',
                // Suffixed, because the default fixture has no reachable environment.
                'content-drive.context-menu.push-publish.no-environment',
                'contenttypes.content.add_to_bundle',
                SEPARATOR,
                'content-drive.context-menu.workflows',
                'Assign Workflow',
                'Save',
                'Save / Publish'
            ]);

            const workflows = items.find(
                (item) => item.label === 'content-drive.context-menu.workflows'
            );

            // A caption, not an entry: it names the group, so it must not be invocable and must not
            // open a flyout. `disabled` is what keeps it out of keyboard navigation.
            expect(workflows?.command).toBeUndefined();
            expect(workflows?.items).toBeUndefined();
            expect(workflows?.disabled).toBe(true);
            // PrimeNG's own group-label class, not a local approximation of it. Asserted by name
            // because swapping it for bespoke styling is the regression worth catching here; the
            // utilities alongside it are free to change.
            expect(workflows?.styleClass).toContain('p-menu-submenu-label');
        });

        it('should build correct menu items for Pages contentlet', async () => {
            const pageContentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE
            } as DotContentDriveItem;
            const pageContextMenuData = { ...mockContextMenuData, contentlet: pageContentlet };

            await component.getMenuItems(pageContextMenuData);

            const items = component.$items();
            expect(find(items, 'content-drive.context-menu.edit-page')).toBeTruthy();
        });

        it('should call navigation service when edit action is triggered', async () => {
            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();
            invoke(items, 'content-drive.context-menu.edit-content');

            expect(navigationService.editContent).toHaveBeenCalledWith(mockContentlet);
        });

        it('should call setShowAddToBundle when add to bundle is triggered', async () => {
            await component.getMenuItems(mockContextMenuData);

            // By label, not index: the push group grew AND the workflow actions are nested now,
            // so an index here would silently point at Push Publish instead.
            invoke(component.$items(), 'contenttypes.content.add_to_bundle');

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
                hide: jest.fn(),
                visible: jest.fn().mockReturnValue(false)
            } as unknown as ContextMenu;

            jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

            await component.getMenuItems(mockContextMenuData);
            const firstCallCount = workflowsActionsService.getByInode.mock.calls.length;

            // Second call
            await component.getMenuItems(mockContextMenuData);

            expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(firstCallCount);
            // Both captions + Edit + Lock/Unlock + Push Publish + Add to Bundle + separator + 3
            expect(component.$items()).toHaveLength(10);
        });

        it.each([
            ['hasArchiveActionlet' as const, 'Retire this blog'],
            ['hasDeleteActionlet' as const, 'Send to trash'],
            ['hasDestroyActionlet' as const, 'Purge']
        ])('should hold a %s action apart from the Workflows group', async (flag, actionName) => {
            // Named nothing like "Archive" or "Delete" on purpose: the split reads the action's
            // actual sub-actionlets, so a custom scheme's wording must not decide the grouping.
            // `mockReturnValueOnce`, not `mockReturnValue`: the spy is shared, `getByInode` is
            // called once per `getMenuItems`, and a persistent override leaks this fixture into
            // every test that runs after this one.
            workflowsActionsService.getByInode.mockReturnValueOnce(
                of([
                    { ...mockWorkflowActions[0], id: 'keep', name: 'Save' },
                    {
                        ...mockWorkflowActions[0],
                        id: 'destructive',
                        name: actionName,
                        [flag]: true
                    }
                ])
            );

            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();

            expect(labels(items).slice(-5)).toEqual([
                SEPARATOR,
                'content-drive.context-menu.workflows',
                'Save',
                SEPARATOR,
                actionName
            ]);

            // The separator is the whole point: it is what stops the destructive action being
            // clicked by momentum after the one above it.
            expect(items.at(-2)?.separator).toBe(true);
        });

        it('should not open a separator-led menu when every action is destructive', async () => {
            workflowsActionsService.getByInode.mockReturnValueOnce(
                of([{ ...mockWorkflowActions[0], name: 'Purge', hasDestroyActionlet: true }])
            );

            await component.getMenuItems(mockContextMenuData);

            const items = component.$items();

            // With no non-destructive action there is no Workflows caption, so the separator must
            // still land after the built-in items rather than opening the menu.
            expect(find(items, 'content-drive.context-menu.workflows')).toBeUndefined();
            expect(labels(items)[0]).toBe('content-drive.context-menu.actions');
            expect(labels(items).slice(-2)).toEqual([SEPARATOR, 'Purge']);
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

            it('should build only Folder Settings for a folder with just EDIT', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                // Delete needs EDIT_PERMISSIONS too, matching `FolderAPIImpl.delete`.
                expect(labels(component.$items())).toEqual([
                    'content-drive.context-menu.actions',
                    'content-drive.context-menu.edit-folder'
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
                invoke(items, 'content-drive.context-menu.edit-folder');

                expect(store.setDialog).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.FOLDER,
                    header: 'content-drive.dialog.folder.header.edit',
                    payload: mockFolder
                });
            });

            it('should show context menu for folders', async () => {
                const mockContextMenu = {
                    show: jest.fn(),
                    hide: jest.fn(),
                    visible: jest.fn().mockReturnValue(false)
                } as unknown as ContextMenu;

                jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

                await component.getMenuItems(mockFolderContextMenuData);

                expect(mockContextMenu.show).toHaveBeenCalledWith(mockEvent);
            });

            it('should memoize folder menu items using identifier as key', async () => {
                await component.getMenuItems(mockFolderContextMenuData);

                expect(component.$memoizedMenuItems()[mockFolder.identifier]).toBeDefined();
                // The Actions caption plus the one permitted entry.
                expect(component.$memoizedMenuItems()[mockFolder.identifier]).toHaveLength(2);
            });

            it('should use memoized folder menu items on second call', async () => {
                const mockContextMenu = {
                    show: jest.fn(),
                    hide: jest.fn(),
                    visible: jest.fn().mockReturnValue(false)
                } as unknown as ContextMenu;

                jest.spyOn(component, 'contextMenu').mockReturnValue(mockContextMenu);

                // First call
                await component.getMenuItems(mockFolderContextMenuData);
                const firstCallCount = workflowsActionsService.getByInode.mock.calls.length;

                // Second call
                await component.getMenuItems(mockFolderContextMenuData);

                expect(workflowsActionsService.getByInode).toHaveBeenCalledTimes(firstCallCount);
                // The Actions caption plus the one permitted entry.
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
                    hide: jest.fn(),
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

                // Both dialogs share one component now, so `data` is what distinguishes them —
                // asserting the component alone would pass with the two call sites swapped.
                it('should open the permissions JSP with correct config when triggered', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    component
                        .$items()
                        .find((item) => item.label === 'Edit-Permissions')
                        ?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(dialogService.open).toHaveBeenCalledWith(
                        DotJspIframeDialogComponent,
                        expect.objectContaining({
                            width: 'min(92vw, 75rem)',
                            closable: true,
                            closeOnEscape: true,
                            data: {
                                url: `/html/portlet/ext/folders/permissions.jsp?folderIdentifier=${folderWithEditPermissions.identifier}&popup=true`,
                                titleKey: 'Permissions',
                                emptyKey: 'dot.permissions.iframe.dialog.no-asset',
                                testIdPrefix: 'permissions'
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

                it('should open the push history JSP with correct config when triggered', async () => {
                    await component.getMenuItems(folderContextMenuWithEditPermissions);

                    component
                        .$items()
                        .find((item) => item.label === 'content-drive.context-menu.push-history')
                        ?.command?.({} as unknown as MenuItemCommandEvent);

                    expect(dialogService.open).toHaveBeenCalledWith(
                        DotJspIframeDialogComponent,
                        expect.objectContaining({
                            width: 'min(92vw, 75rem)',
                            closable: true,
                            closeOnEscape: true,
                            data: {
                                url: `/html/portlet/ext/folders/push_history.jsp?folderIdentifier=${folderWithEditPermissions.identifier}&popup=true`,
                                titleKey: 'publisher_push_history',
                                emptyKey: 'dot.push-history.iframe.dialog.no-asset',
                                testIdPrefix: 'push-history'
                            }
                        })
                    );
                });
            });

            // `getMenuItems` clears `$items` before it works out what to show, so bailing out for
            // an item with no actions left an already-open menu on screen with nothing in it. Only
            // reachable by opening one menu and then right-clicking something with no actions.
            it('should close an open menu when the next item has no actions', async () => {
                const menu = {
                    show: jest.fn(),
                    hide: jest.fn(),
                    visible: jest.fn().mockReturnValue(true)
                } as unknown as ContextMenu;
                jest.spyOn(component, 'contextMenu').mockReturnValue(menu);

                // A contentlet first, which does have actions.
                await component.getMenuItems(mockContextMenuData);
                expect(menu.show).toHaveBeenCalled();

                // Then a folder with nothing on offer.
                await component.getMenuItems({
                    triggeredEvent: mockEvent,
                    contentlet: { ...mockFolder, permissions: [] },
                    showAddToBundle: false
                });

                expect(component.$items()).toEqual([]);
                expect(menu.hide).toHaveBeenCalled();
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
                        .find(
                            (item) =>
                                item.label?.includes('push_publish') ||
                                item.label?.includes('push-publish')
                        );

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

                it('should enable the item, plainly labelled, once an environment is reachable', async () => {
                    withEnvironments();

                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()?.disabled).toBe(false);
                    expect(pushPublishItem()?.label).toBe('contenttypes.content.push_publish');
                });

                // Offered but disabled rather than hidden: nothing is missing from dotCMS, something
                // is missing from the configuration, and the fix is an administrator's. The tooltip
                // is what says so.
                // `tooltipOptions`, not `tooltip`: PrimeNG's ContextMenu template binds `pTooltip`
                // with only `[tooltipOptions]`, so a plain `tooltip` on the item is silently ignored
                // and the row explains nothing.
                // Measured in the browser: a disabled context menu item computes
                // `pointer-events: none`, so no hover reaches it and no tooltip can ever fire,
                // whichever of PrimeNG's tooltip inputs it carries. The reason has to sit in the
                // label, which needs neither hover nor click.
                it('should say why in the label when no environment is reachable', async () => {
                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()?.disabled).toBe(true);
                    expect(pushPublishItem()?.label).toBe(
                        'content-drive.context-menu.push-publish.no-environment'
                    );
                });

                // The item's disabled state is computed when the menu is built, and the menu is
                // memoized per folder. If the one-shot lookup lands after a menu was cached, that
                // folder would keep saying "(no environment)" while the Action Center, which reads
                // the signal reactively, already shows it enabled.
                it('should drop the memo when the environments lookup settles', async () => {
                    await component.getMenuItems(folderContextMenuWithPublish);
                    expect(pushPublishItem()?.disabled).toBe(true);

                    withEnvironments();
                    spectator.detectChanges();

                    await component.getMenuItems(folderContextMenuWithPublish);

                    expect(pushPublishItem()?.disabled).toBe(false);
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
                    permissions: [PERMISSIONS_TYPE.EDIT, PERMISSIONS_TYPE.EDIT_PERMISSIONS]
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

                // `FolderAPIImpl.delete` enforces **both**: EDIT at `:438` and EDIT_PERMISSIONS at
                // `:456`. Gating on EDIT alone offered Delete to a contributor who would confirm the
                // destructive dialog and then be refused with a 403.
                it('should show Delete when the folder has EDIT and EDIT_PERMISSIONS', async () => {
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

                it('should not show Delete with EDIT but no EDIT_PERMISSIONS', async () => {
                    await component.getMenuItems({
                        triggeredEvent: mockEvent,
                        contentlet: { ...mockFolder, permissions: [PERMISSIONS_TYPE.EDIT] },
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

                // A confirmed destructive action that does nothing at all, with no message, is worse
                // than an error. Narrow (there is normally a browsed site) but it must not be silent.
                it('should report rather than silently skip when no site is resolved', async () => {
                    patchState(store, { currentSite: undefined } as never);
                    jest.spyOn(messageService, 'add');

                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                    (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                    expect(folderService.deleteFolder).not.toHaveBeenCalled();
                    expect(messageService.add).toHaveBeenCalledWith(
                        expect.objectContaining({ severity: 'error' })
                    );
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

                // The sidebar tree serves this menu too, so a user can right-click an *ancestor* of
                // the folder they are browsing. Reloading the current path would then reload a path
                // that no longer exists, leaving the grid empty and the breadcrumb pointing into a
                // deleted folder.
                describe('when the deleted folder contains the one being browsed', () => {
                    it('should move to the site root', async () => {
                        store.setPath(`${folderWithEdit.path}nested/`);
                        jest.spyOn(store, 'setPath');

                        await component.getMenuItems(folderContextMenuWithEdit);
                        deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                        (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                        expect(store.setPath).toHaveBeenCalledWith('/');
                    });

                    it('should move to the site root when browsing the deleted folder itself', async () => {
                        store.setPath(folderWithEdit.path);
                        jest.spyOn(store, 'setPath');

                        await component.getMenuItems(folderContextMenuWithEdit);
                        deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                        (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                        expect(store.setPath).toHaveBeenCalledWith('/');
                    });

                    // A sibling or a child of the browsed folder leaves the current path valid, so
                    // moving would throw the user out of where they were working for no reason.
                    it('should stay put when the deleted folder is elsewhere', async () => {
                        store.setPath('/somewhere-else/');
                        jest.spyOn(store, 'setPath');

                        await component.getMenuItems(folderContextMenuWithEdit);
                        deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                        (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                        expect(store.setPath).not.toHaveBeenCalled();
                    });
                });

                // The two cases above assert only what does *not* happen, so dropping the
                // `handle(error)` call entirely would keep them green while the delete failed in
                // total silence. This is the AC clause that says a failure reaches the user.
                it('should surface a failed delete through the HTTP error handler', async () => {
                    folderService.deleteFolder = jest
                        .fn()
                        .mockReturnValue(throwError(() => new Error('nope')));

                    await component.getMenuItems(folderContextMenuWithEdit);
                    deleteItem()?.command?.({} as unknown as MenuItemCommandEvent);
                    (alertConfirmService.confirm as jest.Mock).mock.lastCall[0].accept();

                    expect(
                        spectator.inject(DotHttpErrorManagerService, true).handle
                    ).toHaveBeenCalled();
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

                    expect(labels(component.$items())).toEqual([
                        'content-drive.context-menu.actions',
                        'content-drive.context-menu.edit-folder',
                        'Edit-Permissions',
                        'contenttypes.content.push_publish',
                        'contenttypes.content.add_to_bundle',
                        'content-drive.context-menu.push-history',
                        // Delete is held apart, the same way the destructive workflow actions are.
                        SEPARATOR,
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
                // Both captions + Edit + Push Publish + Add to Bundle + separator + 3, no lock
                expect(items).toHaveLength(9);
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
                    .find(
                        (item) =>
                            item.label?.includes('push_publish') ||
                            item.label?.includes('push-publish')
                    );

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
                expect(pushPublishItem()?.label).toBe(
                    'content-drive.context-menu.push-publish.no-environment'
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
            invoke(items, 'Assign Workflow');

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
            invoke(items, 'Assign Workflow');

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
            invoke(items, 'Save');

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
            invoke(items, 'Save');

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'content-drive.toast.workflow-executed',
                detail: 'content-drive.toast.workflow-executed-detail'
            });
            // The mocked `get` echoes the key, so the assertion above cannot tell whether the
            // action and the item reached the copy. This is what actually proves FR-028.
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.toast.workflow-executed-detail',
                'Save',
                mockContentlet.title
            );

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
            invoke(items, 'Save');

            jest.advanceTimersByTime(0);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.toast.workflow-error',
                detail: 'content-drive.toast.workflow-error-detail',
                life: 4500
            });
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.toast.workflow-error-detail',
                'Save',
                mockContentlet.title
            );

            jest.useRealTimers();
        });

        it('should leave the listing status untouched when a workflow action fails', async () => {
            jest.useFakeTimers();
            const statusBefore = store.status();
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
            invoke(items, 'Save');

            jest.advanceTimersByTime(0);

            // It used to set LOADED here only to undo the LOADING it had set itself. Now that the run
            // reports on the toolbar indicator, the listing's status is not this component's
            // business at all, and writing either value would be reaching into an unrelated concern.
            expect(store.status()).toBe(statusBefore);

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
            invoke(items, 'Save');

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
            invoke(items, 'Save');

            expect(store.status()).toBe(DotContentDriveStatus.LOADING);
        });
    });

    describe('in-flight reporting (FR-007, FR-009)', () => {
        const mockEvent = new MouseEvent('contextmenu');

        const fireSaveAction = async () => {
            workflowsActionsService.getByInode.mockReturnValue(
                of([{ ...mockWorkflowActions[1], actionInputs: [] }])
            );

            await component.getMenuItems({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet,
                showAddToBundle: false
            });

            invoke(component.$items(), 'Save');
            jest.advanceTimersByTime(0);
        };

        it('should not blank the listing while a workflow action runs', async () => {
            jest.useFakeTimers();
            const setStatus = jest.spyOn(store, 'setStatus');

            await fireSaveAction();

            // The listing's loading state means "the listing is being fetched" and nothing else
            // (FR-009). Using it to report a one-row action hid the very row the author acted on,
            // and was indistinguishable from an ordinary page load.
            expect(setStatus).not.toHaveBeenCalledWith(DotContentDriveStatus.LOADING);

            jest.useRealTimers();
        });

        it('should report the run on the toolbar indicator, naming the item', async () => {
            jest.useFakeTimers();
            const setActionExecution = jest.spyOn(store, 'setActionExecution');

            await fireSaveAction();

            expect(setActionExecution).toHaveBeenCalledWith({
                actionName: 'Save',
                total: 1,
                targetLabel: mockContentlet.title
            });

            jest.useRealTimers();
        });

        it('should clear the indicator once the run settles', async () => {
            jest.useFakeTimers();
            const setActionExecution = jest.spyOn(store, 'setActionExecution');

            await fireSaveAction();

            expect(setActionExecution).toHaveBeenLastCalledWith(undefined);

            jest.useRealTimers();
        });

        it('should clear the indicator when the run fails', async () => {
            jest.useFakeTimers();
            workflowsActionsFireService.fireTo.mockReturnValue(throwError(() => new Error('boom')));
            const setActionExecution = jest.spyOn(store, 'setActionExecution');

            await fireSaveAction();

            // A failed run that left the indicator up would report work that is not happening.
            expect(setActionExecution).toHaveBeenLastCalledWith(undefined);

            jest.useRealTimers();
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
            // Asymmetry this closes: `lock-success` is "Locked {0}" while the failure said only
            // "The contentlet wasn't locked", leaving the author to work out which one.
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.toast.lock-error-detail',
                mockContentlet.title
            );

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
            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.toast.unlock-error-detail',
                mockContentlet.title
            );

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
