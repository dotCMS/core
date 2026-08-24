import { beforeEach, describe, expect, it } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    AddToBundleService,
    DotAlertConfirmService,
    DotContentSearchService,
    DotContentTypeService,
    DotCurrentUserService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotRouterService,
    DotSiteService,
    DotSystemConfigService,
    DotUploadFileService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotContentDriveFolder,
    DotContentDriveItem
} from '@dotcms/dotcms-models';
import {
    DotEditContentSidePanelComponent,
    DotSidePanelNavController,
    EditContentDialogData
} from '@dotcms/edit-content';
import {
    DotFolderTreeNodeData,
    DotFolderTreeNodeItem,
    DotContentDriveMoveItems
} from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';
import { DotFolderListViewComponent, DotUploadTypeSelectorComponent } from '@dotcms/ui';
import { mockLocales } from '@dotcms/utils-testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import {
    ACTION_CENTER_DIALOG_CONTENT_STYLE,
    ACTION_CENTER_DIALOG_STYLE,
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
    DotContentDriveActionExecutionResult,
    DotContentDriveDialogDrillDown,
    DotContentDriveSortOrder,
    DotContentDriveStatus
} from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

// Backs the navigation service mock's readonly `$editPanelRequest`. Typed (not cast) so tests get
// a compile-checked payload; reset in the shared beforeEach for isolation.
const editPanelRequestSignal: WritableSignal<EditContentDialogData | null> = signal(null);
// Module scope: both store mocks in this file read it, and they live in describes that do not
// share a `beforeEach`. Reset per test rather than re-created, so neither mock captures a stale one.
const canAddChildrenSignal: WritableSignal<boolean> = signal(true);

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let messageService: SpyObject<MessageService>;
    let dotMessageService: SpyObject<DotMessageService>;
    let uploadService: SpyObject<DotUploadFileService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;
    let filtersSignal: ReturnType<typeof signal>;
    let statusSignal: ReturnType<typeof signal<DotContentDriveStatus>>;
    // Reactive so the shell's syncDialogEffect reacts (mirrors the real SignalStore signal).
    let dialogSignal: WritableSignal<DotContentDriveDialog | undefined>;
    // Header override published by a dialog body that has drilled into a sub-screen.
    let dialogDrillDownSignal: WritableSignal<DotContentDriveDialogDrillDown | undefined>;
    // Result of a finished workflow action, which the shell turns into a toast.
    let actionExecutionResultSignal: WritableSignal<
        DotContentDriveActionExecutionResult | undefined
    >;
    // Reactive so the shell's $extraColumns computed recomputes when the fields change.
    let showInListFieldsSignal: WritableSignal<DotCMSContentTypeField[]>;

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
            // The folder context menu confirms folder deletes through this.
            mockProvider(DotAlertConfirmService, { confirm: jest.fn() }),
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
            // The panel is behind `@defer`; once it resolves, it mounts the real editor chain,
            // which can make HTTP calls no test here mocks explicitly (e.g. languages). Without
            // this, an unmocked call attempts a real network fetch and fails the test.
            provideHttpClientTesting(),
            // The store composes withFlags, which fetches feature flags on init; stub it.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn(),
                createContent: jest.fn(),
                closeEditPanel: jest.fn(),
                openEditByIdentifier: jest.fn(),
                $editPanelRequest: editPanelRequestSignal
            }),
            LoggerService,
            StringUtils,
            mockProvider(PushPublishService, {
                // The store resolves this on init, and both the Action Center's Push Publish row and
                // the folder context menu's Push Publish item gate on the result. An empty answer
                // disables them, which is all the shell's own tests need.
                getEnvironments: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(AddToBundleService, {
                getBundles: jest.fn().mockReturnValue(of([])),
                addToBundle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSidePanelNavController, {
                shouldCollapse: jest.fn().mockReturnValue(false),
                acquire: jest.fn(),
                release: jest.fn()
            })
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        canAddChildrenSignal.set(true);
        filtersSignal = signal({});
        statusSignal = signal(DotContentDriveStatus.LOADING);
        dialogSignal = signal<DotContentDriveDialog | undefined>(undefined);
        dialogDrillDownSignal = signal<DotContentDriveDialogDrillDown | undefined>(undefined);
        actionExecutionResultSignal = signal<DotContentDriveActionExecutionResult | undefined>(
            undefined
        );
        showInListFieldsSignal = signal<DotCMSContentTypeField[]>([]);
        editPanelRequestSignal.set(null);

        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    // Read by the toolbar (rendered for real here) and the drop zone: both gate
                    // their creation affordances on it.
                    $canAddChildren: canAddChildrenSignal,
                    currentSite: jest.fn().mockReturnValue(MOCK_SITES[0]),
                    // Tree collapsed at start to render the toggle button on toolbar
                    isTreeExpanded: jest.fn().mockReturnValue(false),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn(),
                    $request: jest.fn(),
                    items: jest.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setIsTreeExpanded: jest.fn(),
                    isTreeVisuallyExpanded: jest.fn().mockReturnValue(false),
                    isTreeForceCollapsed: jest.fn().mockReturnValue(false),
                    setTreeForceCollapsed: jest.fn(),
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
                    // Read by the Action Center, which the shell renders for real inside the dialog.
                    currentUserIsAdmin: jest.fn().mockReturnValue(false),
                    // Resolved on portlet init; `false` disables Push Publish everywhere it
                    // is gated, which is all the shell's own tests need.
                    hasPushPublishEnvironments: jest.fn().mockReturnValue(false),
                    patchFilters: jest.fn(),
                    contextMenu: jest.fn().mockReturnValue(null),
                    dialog: dialogSignal,
                    dialogDrillDown: dialogDrillDownSignal,
                    // Read by the toolbar, which the shell renders for real.
                    actionExecution: signal(undefined),
                    // Read by the Locale chip inside that toolbar: the store resolves the languages
                    // once and seeds the environment default into the `languageId` filter.
                    languages: signal(mockLocales),
                    defaultLanguageId: jest.fn().mockReturnValue(1),
                    actionExecutionResult: actionExecutionResultSignal,
                    clearActionExecutionResult: jest.fn(),
                    setDialog: jest.fn(),
                    setDialogDrillDown: jest.fn(),
                    clearDialogDrillDown: jest.fn(),
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
                    reloadContentDrive: jest.fn(),
                    setPath: jest.fn(),
                    setShowAddToBundle: jest.fn(),
                    userSearchableFields: jest.fn().mockReturnValue([]),
                    userSearchableActive: jest.fn().mockReturnValue([]),
                    showInListFields: showInListFieldsSignal,
                    setUserSearchableFields: jest.fn(),
                    setShowInListFields: jest.fn(),
                    addUserSearchableField: jest.fn(),
                    clearUserSearchableFilters: jest.fn()
                }),
                mockProvider(Router, {
                    createUrlTree: jest.fn(
                        (
                            _commands: unknown[],
                            opts: { queryParams?: Record<string, string | null> }
                        ) => ({
                            toString: () => {
                                const params = Object.fromEntries(
                                    Object.entries(opts?.queryParams ?? {}).filter(
                                        ([, value]) => value != null
                                    )
                                ) as Record<string, string>;

                                return '?' + new URLSearchParams(params).toString();
                            }
                        })
                    )
                }),
                mockProvider(Location, {
                    go: jest.fn(),
                    replaceState: jest.fn(),
                    path: jest.fn().mockReturnValue(''),
                    // Return a real subscription so the shell's popstate listener can be captured
                    // and torn down without throwing on destroy.
                    subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
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
                // The Action Center child looks up bulk actions on init, which happens as soon as a
                // selection is present in these tests.
                mockProvider(DotWorkflowsActionsService, {
                    getBulkActions: jest.fn().mockReturnValue(of({ schemes: [] }))
                }),
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
        dotMessageService = spectator.inject(DotMessageService);
        uploadService = spectator.inject(DotUploadFileService);
        navigationService = spectator.inject(DotContentDriveNavigationService);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('workflow action result', () => {
        // The run outlives the Action Center dialog, so the shell is what reports the outcome — it
        // owns <p-toast> and is never destroyed while the portlet is open.
        const settle = (result: DotContentDriveActionExecutionResult) => {
            actionExecutionResultSignal.set(result);
            spectator.detectChanges();
        };

        it('should report a plain success when nothing failed or skipped', () => {
            settle({
                actionName: 'Publish',
                successCount: 3,
                skippedCount: 0,
                failCount: 0
            });

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'success',
                    detail: 'content-drive.action-center.toast.executed-detail'
                })
            );
        });

        it('should downgrade to a warning when items failed', () => {
            // Partial failure is a normal outcome (a lock held by somebody else, a per-contentlet
            // permission) and must not read as an unqualified success.
            settle({
                actionName: 'Publish',
                successCount: 1,
                skippedCount: 0,
                failCount: 1
            });

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'warn',
                    detail: 'content-drive.action-center.toast.executed-partial'
                })
            );
        });

        it('should warn when items were skipped, even though nothing failed', () => {
            // A skip is still a shortfall from what the user asked for: those items did not get the
            // action. A green success toast would overstate the outcome.
            settle({
                actionName: 'Send for Review',
                successCount: 1,
                skippedCount: 1,
                failCount: 0
            });

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'warn',
                    detail: 'content-drive.action-center.toast.executed-partial'
                })
            );
        });

        it('should report both numbers when a run skipped some items and failed others', () => {
            // The bug: the ladder treated these as mutually exclusive, so a mixed result showed the
            // failure copy alone and blamed permissions or locks for the whole shortfall — when part
            // of it was items merely sitting on a step the action does not own. The user's next move
            // (go unlock things) was then wrong.
            settle({
                actionName: 'Send for Review',
                successCount: 3,
                skippedCount: 2,
                failCount: 1
            });

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.action-center.toast.executed-partial',
                'Send for Review',
                '3',
                '1',
                '2'
            );
        });

        it('should not name a cause the result does not carry', () => {
            // Both counts are always passed, so a fails-only run still renders "0 skipped". That is
            // the honest reading — the message names each cause and its number, rather than
            // attributing the whole shortfall to one of them.
            settle({
                actionName: 'Publish',
                successCount: 1,
                skippedCount: 0,
                failCount: 1
            });

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'content-drive.action-center.toast.executed-partial',
                'Publish',
                '1',
                '1',
                '0'
            );
        });

        it('should refresh the grid, close the dialog and consume the result', () => {
            settle({
                actionName: 'Publish',
                successCount: 1,
                skippedCount: 0,
                failCount: 0
            });

            expect(store.loadItems).toHaveBeenCalled();
            expect(store.closeDialog).toHaveBeenCalled();
            expect(store.clearActionExecutionResult).toHaveBeenCalled();
        });

        it('should stay silent while no result is published', () => {
            spectator.detectChanges();

            expect(messageService.add).not.toHaveBeenCalled();
        });
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
                    filters: 'contentType:Blog;baseType:1,2,3',
                    editContent: null,
                    editContentLang: null
                },
                queryParamsHandling: 'merge'
            });

            // A filter write REPLACES rather than pushes. Only opening the panel pushes (AC8), so
            // Back always leaves the portlet instead of walking back through filter URLs.
            expect(location.replaceState).toHaveBeenCalledWith(
                expect.stringContaining('filters=contentType%3ABlog%3BbaseType%3A1%2C2%2C3')
            );
            expect(location.go).not.toHaveBeenCalled();
        });

        it('pushes a history entry when the user navigates to a different folder', () => {
            // Folder navigation is a real user action, so Back must step back up the tree. Only the
            // automatic filter seed is denied an entry.
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/first');
            // `path` is a plain jest.fn, so it is not a tracked dependency. Each phase re-sets the
            // real `filters` signal (a fresh object reference) to drive the effect, which then reads
            // the current path.
            filtersSignal.set({ sharedAssets: 'true' });
            spectator.detectChanges();
            spectator.flushEffects();

            (location.go as jest.Mock).mockClear();
            (location.replaceState as jest.Mock).mockClear();

            store.path.mockReturnValue('/second');
            filtersSignal.set({ sharedAssets: 'true' });
            spectator.detectChanges();
            spectator.flushEffects();

            expect(location.go).toHaveBeenCalledWith(expect.stringContaining('path=%2Fsecond'));
        });

        it('does not push a history entry when the default filters are seeded', () => {
            // The seed is not a user action, and it lands twice on a cold load: once for the
            // sharedAssets default and again when the default language resolves. Pushing either would
            // bury the entry the user arrived on, so Back would take two or three presses to escape
            // the portlet instead of leaving it immediately.
            store.isTreeExpanded.mockReturnValue(false);
            store.path.mockReturnValue('/');
            (location.go as jest.Mock).mockClear();
            (location.replaceState as jest.Mock).mockClear();

            filtersSignal.set({ sharedAssets: 'true' });
            spectator.detectChanges();
            spectator.flushEffects();

            filtersSignal.set({ sharedAssets: 'true', languageId: ['1'] });
            spectator.detectChanges();
            spectator.flushEffects();

            expect(location.go).not.toHaveBeenCalled();
            expect(location.replaceState).toHaveBeenCalled();
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
                    filters: 'contentType:Blog;baseType:1,2,3',
                    editContent: null,
                    editContentLang: null
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
                    filters: null, // With merge, null removes the param
                    editContent: null,
                    editContentLang: null
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

        it('should render the Action Center inside the shared dialog', () => {
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            spectator.flushEffects();
            spectator.detectChanges();

            const dialogComponent = spectator.debugElement.query(By.css('[data-testid="dialog"]'))
                ?.componentInstance as Dialog;

            // One dialog, one visibility path — the Action Center is a case in its content switch.
            expect(dialogComponent.visible).toBe(true);
            expect(spectator.query('[data-testId="dialog-action-center"]')).toBeTruthy();
        });

        it('should make the Action Center content box the only scroll container', () => {
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.component.$dialogContentStyle()).toEqual(
                ACTION_CENTER_DIALOG_CONTENT_STYLE
            );
            expect(spectator.component.$dialogStyle()).toEqual(ACTION_CENTER_DIALOG_STYLE);
        });

        it('should render a sub-header with the selected contentlet count', () => {
            // `selectedItems` is mocked as a plain jest.fn here, so it must be set before the
            // computed is first read — it has no signal dependency to invalidate its cache.
            store.selectedItems.mockReturnValue([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.query('[data-testId="dialog-subheader"]')).toBeTruthy();
            expect(spectator.component.$actionCenterSelectionCount()).toBe(2);
        });

        it('should count folders in the sub-header, since actions now take them', () => {
            store.selectedItems.mockReturnValue([
                MOCK_ITEMS[0],
                { type: 'folder', identifier: 'f1' } as unknown as DotContentDriveItem
            ]);
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.component.$actionCenterSelectionCount()).toBe(2);
        });

        it('should retitle the header to the drilled-into action', () => {
            // The Action Center body publishes this when it opens an action's preview, so the one
            // dialog header names the action instead of the body rendering a second header.
            store.selectedItems.mockReturnValue([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            dialogDrillDownSignal.set({ header: 'Send for Review', itemCount: 1 });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.query('[data-testId="dialog-header"]')?.textContent?.trim()).toBe(
                'Send for Review'
            );
            // The count follows the drill-down, not the full selection of 2.
            expect(spectator.component.$actionCenterCount()).toBe(1);
        });

        it('should restore the dialog title when the drill-down is cleared', () => {
            store.selectedItems.mockReturnValue([MOCK_ITEMS[0], MOCK_ITEMS[1]]);
            dialogSignal.set({ type: DIALOG_TYPE.ACTION_CENTER, header: 'Workflow Center' });
            dialogDrillDownSignal.set({ header: 'Send for Review', itemCount: 1 });
            spectator.flushEffects();
            spectator.detectChanges();

            dialogDrillDownSignal.set(undefined);
            spectator.detectChanges();

            expect(spectator.query('[data-testId="dialog-header"]')?.textContent?.trim()).toBe(
                'Workflow Center'
            );
            expect(spectator.component.$actionCenterCount()).toBe(2);
        });

        it('should not render the sub-header for other dialog types', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
            spectator.detectChanges();

            // The header template is shared, so the default branch must still render the plain title.
            expect(spectator.query('[data-testId="dialog-subheader"]')).toBeNull();
            expect(spectator.query('.p-dialog-title')?.textContent?.trim()).toBe('Folder');
        });

        it('should not apply the Action Center sizing to other dialog types', () => {
            dialogSignal.set({ type: DIALOG_TYPE.FOLDER, header: 'Folder' });
            spectator.flushEffects();
            spectator.detectChanges();

            expect(spectator.query('[data-testId="dialog-action-center"]')).toBeNull();
            expect(spectator.component.$dialogStyle()).toBeUndefined();
            expect(spectator.component.$dialogContentStyle()).toBeUndefined();
            expect(spectator.component.$dialogHeaderClass()).toBe('');
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

        // Dropping a file creates a contentlet in the target folder, which the server refuses
        // without CAN_ADD_CHILDREN. The zone refuses the upload rather than failing after it has
        // started, and carries the reason so it does not just read as a broken drop target.
        it('should disable the dropzone where children cannot be added', () => {
            canAddChildrenSignal.set(false);
            spectator.detectChanges();

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));

            expect(dropzone.componentInstance.$disabled()).toBe(true);
        });

        it('should tell the dropzone why the upload is refused', () => {
            canAddChildrenSignal.set(false);
            spectator.detectChanges();

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));

            expect(dropzone.componentInstance.$disabledMessage()).toBeTruthy();
        });

        it('should leave the dropzone enabled where children can be added', () => {
            canAddChildrenSignal.set(true);
            spectator.detectChanges();

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));

            expect(dropzone.componentInstance.$disabled()).toBe(false);
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

        it('should return exact total when neither content nor folders have more', () => {
            store.pagination.mockReturnValue({ page: 1, limit: 20, offset: 0 });
            store.pages.mockReturnValue([
                { ...DEFAULT_PAGE, hasMoreContent: false, hasMoreFolders: false }
            ]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // no more of either, page=1, limit=20, items=MOCK_ITEMS.length → 20*(1-1) + MOCK_ITEMS.length
            expect(spectator.component.$totalItems()).toBe(MOCK_ITEMS.length);
        });

        it('should account for previous pages when nothing has more on page 2', () => {
            store.pagination.mockReturnValue({ page: 2, limit: 20, offset: 20 });
            store.pages.mockReturnValue([
                { ...DEFAULT_PAGE, hasMoreContent: false, hasMoreFolders: false }
            ]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // no more of either, page=2, limit=20, items=MOCK_ITEMS.length → 20*(2-1) + MOCK_ITEMS.length
            expect(spectator.component.$totalItems()).toBe(20 + MOCK_ITEMS.length);
        });

        it('should offer a next page when only folders have more (hasMoreFolders true, hasMoreContent false)', () => {
            // A folder holding only sub-folders: no more content, but more folders to page through.
            store.pagination.mockReturnValue({ page: 1, limit: 20, offset: 0 });
            store.pages.mockReturnValue([
                { ...DEFAULT_PAGE, hasMoreContent: false, hasMoreFolders: true }
            ]);
            store.items.mockReturnValue(MOCK_ITEMS);
            spectator.detectChanges();

            // hasMoreFolders = true → one page beyond current: 20 * (1 + 1) = 40
            expect(spectator.component.$totalItems()).toBe(40);
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

    // Opens the upload menu (via the button flow when no files are given, or the drag-and-drop
    // flow when they are) and emits the user's choice back to the shell, mirroring the selector's
    // (selectUploadType) output.
    const selectUploadType = (selection: {
        targetFolder?: DotFolderTreeNodeData;
        baseType: string;
        files?: FileList;
    }) => {
        // Drag-and-drop prompts with a modal; the Upload button uses a popover — both funnel
        // through the same code path and render the selector under the same testid.
        if (selection.files) {
            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));
            spectator.triggerEventHandler(dropzone, 'uploadFiles', {
                files: selection.files,
                targetFolder: selection.targetFolder
            });
        } else {
            store.selectedNode.mockReturnValue({
                data: selection.targetFolder
            } as DotFolderTreeNodeItem);
            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));
            spectator.triggerEventHandler(toolbar, 'upload', {
                currentTarget: document.createElement('button'),
                stopPropagation: jest.fn()
            });
        }

        spectator.detectChanges();

        const selector = spectator.debugElement.query(
            By.css('[data-testId="dialog-upload-selector"]')
        );
        spectator.triggerEventHandler(selector, 'selectUploadType', {
            targetFolder: selection.targetFolder,
            baseType: selection.baseType,
            files: selection.files
        });
    };

    describe('upload type selector — opening', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        const openViaButton = (targetFolder?: DotFolderTreeNodeData) => {
            store.selectedNode.mockReturnValue({ data: targetFolder } as DotFolderTreeNodeItem);
            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));
            spectator.triggerEventHandler(toolbar, 'upload', {
                currentTarget: document.createElement('button'),
                stopPropagation: jest.fn()
            });
            spectator.detectChanges();
        };

        it('should open the upload menu with the selected folder when the upload button is clicked', () => {
            openViaButton(TARGET_FOLDER_DATA);

            const selector = spectator.query(DotUploadTypeSelectorComponent);
            expect(selector).toBeTruthy();
            expect(selector.$targetFolder()).toEqual(TARGET_FOLDER_DATA);
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should open the upload menu carrying the files when the dropzone emits uploadFiles', () => {
            const files = createFileList([createFile()]);

            const dropzone = spectator.debugElement.query(By.css('[data-testid="dropzone"]'));
            spectator.triggerEventHandler(dropzone, 'uploadFiles', {
                files,
                targetFolder: TARGET_FOLDER_DATA
            });
            spectator.detectChanges();

            const selector = spectator.query(DotUploadTypeSelectorComponent);
            expect(selector).toBeTruthy();
            expect(selector.$files()).toBe(files);
            expect(selector.$targetFolder()).toEqual(TARGET_FOLDER_DATA);
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should open the upload menu carrying the files when the sidebar emits uploadFiles', () => {
            const files = createFileList([createFile()]);

            const sidebar = spectator.debugElement.query(By.css('[data-testid="sidebar"]'));
            spectator.triggerEventHandler(sidebar, 'uploadFiles', {
                files,
                targetFolder: TARGET_FOLDER_DATA
            });
            spectator.detectChanges();

            const selector = spectator.query(DotUploadTypeSelectorComponent);
            expect(selector).toBeTruthy();
            expect(selector.$files()).toBe(files);
            expect(uploadService.uploadFileByBaseType).not.toHaveBeenCalled();
        });

        it('should render both option menu items when opened', () => {
            openViaButton(TARGET_FOLDER_DATA);

            // The popover overlay is appended to the document body, so query from the root.
            expect(
                spectator.query(byTestId('upload-selector-option-DOTASSET'), { root: true })
            ).toBeTruthy();
            expect(
                spectator.query(byTestId('upload-selector-option-FILEASSET'), { root: true })
            ).toBeTruthy();
        });

        it('should clear the selector payload when the popover is dismissed without a selection', () => {
            openViaButton(TARGET_FOLDER_DATA);
            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeTruthy();

            const popover = spectator.debugElement.query(
                By.css('[data-testId="upload-selector-popover"]')
            );
            spectator.triggerEventHandler(popover, 'onHide', {});
            spectator.detectChanges();

            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeFalsy();
        });

        const dropFiles = () =>
            spectator.triggerEventHandler(
                spectator.debugElement.query(By.css('[data-testid="dropzone"]')),
                'uploadFiles',
                { files: createFileList([createFile()]), targetFolder: TARGET_FOLDER_DATA }
            );

        it('should close the drag-and-drop modal when the Upload button opens the popover', () => {
            dropFiles();
            spectator.detectChanges();
            expect(spectator.component.$uploadModalVisible()).toBe(true);

            openViaButton(TARGET_FOLDER_DATA);

            expect(spectator.component.$uploadModalVisible()).toBe(false);
        });

        it('should hide the button popover when a drag-and-drop opens the modal', () => {
            openViaButton(TARGET_FOLDER_DATA);
            const hideSpy = jest.spyOn(spectator.component.$uploadSelectorPopover(), 'hide');

            dropFiles();
            spectator.detectChanges();

            expect(hideSpy).toHaveBeenCalled();
            expect(spectator.component.$uploadModalVisible()).toBe(true);
        });

        it('should keep the shared payload (modal content) when the popover hides during handoff', () => {
            openViaButton(TARGET_FOLDER_DATA);

            dropFiles();
            spectator.detectChanges();

            // The popover's onHide must NOT clear the shared payload — the modal just opened with it.
            spectator.triggerEventHandler(
                spectator.debugElement.query(By.css('[data-testId="upload-selector-popover"]')),
                'onHide',
                {}
            );
            spectator.detectChanges();

            expect(spectator.component.$uploadSelectorPayload()).toBeTruthy();
            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeTruthy();
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

    describe('upload — folder default preference (prompt skipped)', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        const upload = () =>
            spectator.triggerEventHandler(
                spectator.debugElement.query(By.css('[data-testid="toolbar"]')),
                'upload',
                { currentTarget: document.createElement('button'), stopPropagation: jest.fn() }
            );

        it('should skip the prompt and open the file picker when the folder pins a base type', () => {
            store.selectedNode.mockReturnValue({
                data: { ...TARGET_FOLDER_DATA, defaultBaseType: 'DOTASSET' }
            } as DotFolderTreeNodeItem);
            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            const clickSpy = jest.spyOn(fileInput, 'click');

            upload();
            spectator.detectChanges();

            expect(clickSpy).toHaveBeenCalled();
            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeFalsy();
        });

        it('should upload with the folder base type after the picker returns (button flow)', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            store.selectedNode.mockReturnValue({
                data: { ...TARGET_FOLDER_DATA, defaultBaseType: 'DOTASSET' }
            } as DotFolderTreeNodeItem);
            const file = createFile();
            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;

            upload();
            Object.defineProperty(fileInput, 'files', {
                value: [file],
                writable: true,
                configurable: true
            });
            spectator.triggerEventHandler('input[type="file"]', 'change', { target: fileInput });

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'DOTASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
        });

        it('should upload dropped files directly when the folder pins a base type (drag-and-drop)', () => {
            uploadService.uploadFileByBaseType.mockReturnValue(of({} as DotCMSContentlet));
            const file = createFile();

            spectator.triggerEventHandler(
                spectator.debugElement.query(By.css('[data-testid="dropzone"]')),
                'uploadFiles',
                {
                    files: createFileList([file]),
                    targetFolder: { ...TARGET_FOLDER_DATA, defaultBaseType: 'FILEASSET' }
                }
            );
            spectator.detectChanges();

            expect(uploadService.uploadFileByBaseType).toHaveBeenCalledWith(file, 'FILEASSET', {
                hostFolder: TARGET_FOLDER_DATA.id,
                indexPolicy: 'WAIT_FOR'
            });
            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeFalsy();
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

        it('should carry the folder defaultBaseType into the selected node', () => {
            spectator.detectChanges();

            const folderItem = {
                ...MOCK_ITEMS[0],
                type: 'folder',
                path: '/app/',
                identifier: 'app',
                inode: 'app-inode',
                defaultBaseType: 'DOTASSET'
            };

            store.currentSite.mockReturnValue(MOCK_SITES[0]);
            store.setSelectedNode.mockClear();

            const folderListView = spectator.debugElement.query(
                By.directive(DotFolderListViewComponent)
            );

            spectator.triggerEventHandler(folderListView, 'doubleClick', folderItem);

            expect(store.setSelectedNode).toHaveBeenCalledWith(
                expect.objectContaining({
                    data: expect.objectContaining({ defaultBaseType: 'DOTASSET' })
                })
            );
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
        it('should open the upload menu instead of the file picker directly', () => {
            spectator.detectChanges();

            const fileInput = spectator.query('input[type="file"]') as HTMLInputElement;
            const clickSpy = jest.spyOn(fileInput, 'click');

            const toolbar = spectator.debugElement.query(By.css('[data-testid="toolbar"]'));

            spectator.triggerEventHandler(toolbar, 'upload', {
                currentTarget: document.createElement('button'),
                stopPropagation: jest.fn()
            });
            spectator.detectChanges();

            expect(spectator.query(DotUploadTypeSelectorComponent)).toBeTruthy();
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

    describe('Edit Content side panel', () => {
        let sidePanelNav: SpyObject<DotSidePanelNavController>;

        // Drives the module-scope signal backing the nav service mock's readonly `$editPanelRequest`.
        // Typed at declaration, so no cast is needed and payloads are compile-checked.
        const setPanelRequest = (value: EditContentDialogData | null) =>
            editPanelRequestSignal.set(value);

        const EDIT_REQUEST: EditContentDialogData = {
            mode: 'edit',
            contentletInode: 'inode-1',
            identifier: 'id-1',
            title: 'My content'
        };

        beforeEach(() => {
            sidePanelNav = spectator.inject(DotSidePanelNavController);
        });

        it('delegates the panel `closed` output to the navigation service', async () => {
            setPanelRequest(EDIT_REQUEST);
            spectator.detectChanges();
            // The panel is behind `@defer`; its dynamic import resolves as a microtask, so the
            // element isn't in the DOM until the fixture settles.
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Drive the real template binding `(closed)="onEditPanelClosed()"`, not the handler
            // directly — so a removed/renamed binding would fail the test.
            spectator.triggerEventHandler('dot-edit-content-side-panel', 'closed', undefined);

            expect(navigationService.closeEditPanel).toHaveBeenCalledTimes(1);
        });

        it('reloads the list when the panel `saved` output fires', async () => {
            setPanelRequest(EDIT_REQUEST);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            spectator.triggerEventHandler('dot-edit-content-side-panel', 'saved', undefined);

            expect(store.reloadContentDrive).toHaveBeenCalledTimes(1);
        });

        it('reflects an open edit panel as an editContent identifier in the URL', () => {
            setPanelRequest(EDIT_REQUEST);
            spectator.flushEffects();

            expect(router.createUrlTree).toHaveBeenCalledWith(
                [],
                expect.objectContaining({
                    queryParams: expect.objectContaining({ editContent: 'id-1' })
                })
            );
        });

        it('reflects an open new-mode panel as editContent=new (push), so Back has an entry to pop (AC8)', () => {
            setPanelRequest({ mode: 'new', contentTypeId: 'ct-1', title: 'New content' });
            spectator.flushEffects();

            expect(router.createUrlTree).toHaveBeenCalledWith(
                [],
                expect.objectContaining({
                    queryParams: expect.objectContaining({ editContent: 'new' })
                })
            );
            expect(location.go).toHaveBeenCalled();
        });

        it('uses replaceState (not go) when the panel closes, so Back cannot resurrect the removed param', () => {
            setPanelRequest(EDIT_REQUEST);
            spectator.flushEffects();
            (location.go as jest.Mock).mockClear();
            (location.replaceState as jest.Mock).mockClear();

            setPanelRequest(null);
            spectator.flushEffects();

            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(location.go).not.toHaveBeenCalled();
        });

        describe('browser Back (popstate)', () => {
            const getPopstateHandler = () =>
                (location.subscribe as jest.Mock).mock.calls[0][0] as (event: {
                    url: string;
                }) => void;

            it('routes Back through the panel close guard (does not discard silently)', () => {
                const requestClose = jest.fn();
                jest.spyOn(spectator.component, '$sidePanel').mockReturnValue({
                    requestClose
                } as unknown as DotEditContentSidePanelComponent);
                setPanelRequest(EDIT_REQUEST);

                getPopstateHandler()({ url: '/c/content-drive?path=/foo' });

                // Routes through the guard instead of tearing the panel down directly.
                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(navigationService.closeEditPanel).not.toHaveBeenCalled();
                // Restores the param so the URL matches the still-open panel while the guard decides.
                expect(location.replaceState).toHaveBeenCalledTimes(1);
            });

            it('keeps the panel open when Back preserves the same editContent param', () => {
                const requestClose = jest.fn();
                jest.spyOn(spectator.component, '$sidePanel').mockReturnValue({
                    requestClose
                } as unknown as DotEditContentSidePanelComponent);
                setPanelRequest(EDIT_REQUEST);

                getPopstateHandler()({ url: '/c/content-drive?editContent=id-1' });

                expect(requestClose).not.toHaveBeenCalled();
                expect(navigationService.closeEditPanel).not.toHaveBeenCalled();
            });

            it('routes Back through the guard for an open new-mode panel too (AC8)', () => {
                const requestClose = jest.fn();
                jest.spyOn(spectator.component, '$sidePanel').mockReturnValue({
                    requestClose
                } as unknown as DotEditContentSidePanelComponent);
                setPanelRequest({ mode: 'new', contentTypeId: 'ct-1', title: 'New content' });

                // Back removed the `new` marker entirely — the popstate handler must still close
                // the create panel through the guard, not leave it open with a stale URL.
                getPopstateHandler()({ url: '/c/content-drive?path=/foo' });

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(navigationService.closeEditPanel).not.toHaveBeenCalled();
                expect(location.replaceState).toHaveBeenCalledTimes(1);
            });

            it('keeps a new-mode panel open when Back preserves the editContent=new marker', () => {
                const requestClose = jest.fn();
                jest.spyOn(spectator.component, '$sidePanel').mockReturnValue({
                    requestClose
                } as unknown as DotEditContentSidePanelComponent);
                setPanelRequest({ mode: 'new', contentTypeId: 'ct-1', title: 'New content' });

                getPopstateHandler()({ url: '/c/content-drive?editContent=new' });

                expect(requestClose).not.toHaveBeenCalled();
                expect(navigationService.closeEditPanel).not.toHaveBeenCalled();
            });
        });

        describe('folder tree force-collapse (transient, decoupled from the real preference)', () => {
            it('forces the tree visually collapsed while the panel is open on narrow viewports, and clears the override on close', () => {
                sidePanelNav.shouldCollapse.mockReturnValue(true);
                spectator.flushEffects();

                setPanelRequest(EDIT_REQUEST);
                spectator.flushEffects();
                expect(store.setTreeForceCollapsed).toHaveBeenCalledWith(true);

                setPanelRequest(null);
                spectator.flushEffects();
                expect(store.setTreeForceCollapsed).toHaveBeenCalledWith(false);
            });

            it('does not force a collapse on wide viewports', () => {
                sidePanelNav.shouldCollapse.mockReturnValue(false);
                spectator.flushEffects();

                setPanelRequest(EDIT_REQUEST);
                spectator.flushEffects();

                expect(store.setTreeForceCollapsed).toHaveBeenCalledWith(false);
            });

            // The whole point of the fix (issue: a panel-forced collapse used to leak into the
            // real, shareable `isTreeExpanded` preference — see AC on the tree-collapse bug):
            // the force-collapse override no longer reads `isTreeExpanded()` at all, so it behaves
            // identically regardless of the user's real preference.
            it('is independent of the real tree expanded/collapsed preference', () => {
                store.isTreeExpanded.mockReturnValue(false);
                sidePanelNav.shouldCollapse.mockReturnValue(true);
                spectator.flushEffects();

                setPanelRequest(EDIT_REQUEST);
                spectator.flushEffects();
                expect(store.setTreeForceCollapsed).toHaveBeenCalledWith(true);

                setPanelRequest(null);
                spectator.flushEffects();
                expect(store.setTreeForceCollapsed).toHaveBeenCalledWith(false);

                // Never touches the real preference.
                expect(store.setIsTreeExpanded).not.toHaveBeenCalled();
            });
        });
    });

    describe('extra columns (Show In List)', () => {
        beforeEach(() => spectator.detectChanges());

        it('should map Show In List fields to typed table columns by data/field type', () => {
            showInListFieldsSignal.set([
                { variable: 'summary', name: 'Summary', dataType: 'TEXT', fieldType: 'Text' },
                { variable: 'count', name: 'Count', dataType: 'INTEGER', fieldType: 'Text' },
                { variable: 'active', name: 'Active', dataType: 'BOOL', fieldType: 'Checkbox' },
                { variable: 'pub', name: 'Published', dataType: 'DATE', fieldType: 'Date' },
                { variable: 'evt', name: 'Event', dataType: 'DATE', fieldType: 'Date-and-Time' },
                { variable: 'clock', name: 'Clock', dataType: 'DATE', fieldType: 'Time' }
            ] as DotCMSContentTypeField[]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([
                expect.objectContaining({ field: 'summary', header: 'Summary', type: 'text' }),
                expect.objectContaining({ field: 'count', type: 'number' }),
                expect.objectContaining({ field: 'active', type: 'boolean' }),
                expect.objectContaining({ field: 'pub', type: 'date' }),
                expect.objectContaining({ field: 'evt', type: 'datetime' }),
                expect.objectContaining({ field: 'clock', type: 'time' })
            ]);
        });

        it('should type a True/False Radio or Select field as a boolean column', () => {
            // The reported case: dotCMS's own Radio help text tells users to author a True/False
            // field as `True|1` / `False|0`, and the product ships `Host.runDashboard` in exactly
            // that shape. Only Checkbox was covered before, which is why the boolean column's
            // rendering shipped untested for these two.
            showInListFieldsSignal.set([
                { variable: 'boolRadio', name: 'Bool Radio', dataType: 'BOOL', fieldType: 'Radio' },
                {
                    variable: 'boolSelect',
                    name: 'Bool Select',
                    dataType: 'BOOL',
                    fieldType: 'Select'
                }
            ] as DotCMSContentTypeField[]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([
                expect.objectContaining({ field: 'boolRadio', type: 'boolean' }),
                expect.objectContaining({ field: 'boolSelect', type: 'boolean' })
            ]);
        });

        it('should keep a non-boolean Radio or Select field a text column', () => {
            showInListFieldsSignal.set([
                { variable: 'size', name: 'Size', dataType: 'TEXT', fieldType: 'Radio' },
                { variable: 'colour', name: 'Colour', dataType: 'TEXT', fieldType: 'Select' }
            ] as DotCMSContentTypeField[]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([
                expect.objectContaining({ field: 'size', type: 'text' }),
                expect.objectContaining({ field: 'colour', type: 'text' })
            ]);
        });

        it('should expose no extra columns when there are no Show In List fields', () => {
            showInListFieldsSignal.set([]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([]);
        });

        it('should mark a column sortable only when the field is indexed', () => {
            showInListFieldsSignal.set([
                {
                    variable: 'idx',
                    name: 'Indexed',
                    dataType: 'TEXT',
                    fieldType: 'Text',
                    indexed: true
                },
                {
                    variable: 'noidx',
                    name: 'Not indexed',
                    dataType: 'TEXT',
                    fieldType: 'Text',
                    indexed: false
                }
            ] as DotCMSContentTypeField[]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([
                expect.objectContaining({ field: 'idx', sortable: true }),
                expect.objectContaining({ field: 'noidx', sortable: false })
            ]);
        });

        it('should map Image/Binary/File fields to the image (thumbnail) column type', () => {
            showInListFieldsSignal.set([
                { variable: 'photo', name: 'Photo', dataType: 'SYSTEM', fieldType: 'Image' },
                { variable: 'doc', name: 'Doc', dataType: 'SYSTEM', fieldType: 'Binary' },
                { variable: 'attach', name: 'Attach', dataType: 'SYSTEM', fieldType: 'File' }
            ] as DotCMSContentTypeField[]);
            spectator.detectChanges();

            expect(spectator.component.$extraColumns()).toEqual([
                expect.objectContaining({ field: 'photo', type: 'image' }),
                expect.objectContaining({ field: 'doc', type: 'image' }),
                expect.objectContaining({ field: 'attach', type: 'image' })
            ]);
        });
    });
});

/**
 * Construction-time `?editContent=` deep-link. Own describe + factory so the route param is present
 * BEFORE the component is constructed — the main describe's shared beforeEach always builds against
 * {@link MOCK_ROUTE} (no `editContent`), so it cannot express this path.
 */
describe('DotContentDriveShellComponent — editContent deep link', () => {
    // Mutable so both the identifier and the `new` marker cases share one factory.
    const deepLinkQueryParams: Record<string, string> = {
        path: '/test/path',
        filters: 'contentType:Blog;status:published',
        editContent: 'id-1'
    };
    // Held at describe scope so we can clear it before each mount (mockProvider reuses the same fn).
    const openEditByIdentifier = jest.fn();

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
            mockProvider(ActivatedRoute, {
                snapshot: { queryParams: deepLinkQueryParams }
            }),
            mockProvider(DotSystemConfigService),
            // The folder context menu confirms folder deletes through this.
            mockProvider(DotAlertConfirmService, { confirm: jest.fn() }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                getContentTypes: jest.fn().mockImplementation(() => of([]))
            }),
            mockProvider(DotLanguagesService, { get: jest.fn().mockReturnValue(of()) }),
            mockProvider(DotFolderService, { getFolders: jest.fn().mockReturnValue(of([])) }),
            mockProvider(DotUploadFileService, {
                uploadFileByBaseType: jest.fn().mockReturnValue(of({}))
            }),
            provideHttpClient(),
            // The panel is behind `@defer`; once it resolves, it mounts the real editor chain,
            // which can make HTTP calls no test here mocks explicitly (e.g. languages). Without
            // this, an unmocked call attempts a real network fetch and fails the test.
            provideHttpClientTesting(),
            // The store composes withFlags, which fetches feature flags on init; stub it.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn(),
                createContent: jest.fn(),
                closeEditPanel: jest.fn(),
                openEditByIdentifier,
                $editPanelRequest: signal(null)
            }),
            LoggerService,
            StringUtils,
            mockProvider(PushPublishService, {
                // The store resolves this on init, and both the Action Center's Push Publish row and
                // the folder context menu's Push Publish item gate on the result. An empty answer
                // disables them, which is all the shell's own tests need.
                getEnvironments: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(AddToBundleService, {
                getBundles: jest.fn().mockReturnValue(of([])),
                addToBundle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSidePanelNavController, {
                shouldCollapse: jest.fn().mockReturnValue(false),
                acquire: jest.fn(),
                release: jest.fn()
            })
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        openEditByIdentifier.mockClear();
        // The params object is shared by the factory, so a language set by one test would otherwise
        // leak into the next.
        delete deepLinkQueryParams['editContentLang'];
    });

    /** Mounts with the deps the constructor needs; does not run change detection. */
    const mountShell = () =>
        createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    // Read by the toolbar (rendered for real here) and the drop zone: both gate
                    // their creation affordances on it.
                    $canAddChildren: canAddChildrenSignal,
                    currentSite: jest.fn().mockReturnValue(MOCK_SITES[0]),
                    isTreeExpanded: jest.fn().mockReturnValue(false),
                    items: jest.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: jest.fn().mockReturnValue(DEFAULT_PAGINATION),
                    path: jest.fn().mockReturnValue('/test/path'),
                    filters: signal({}),
                    status: signal(DotContentDriveStatus.LOADING),
                    sort: jest
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    pages: jest.fn().mockReturnValue([DEFAULT_PAGE]),
                    selectedItems: jest.fn().mockReturnValue([]),
                    contextMenu: jest.fn().mockReturnValue(null),
                    dialog: signal(undefined),
                    dragItems: jest.fn().mockReturnValue({ folders: [], contentlets: [] }),
                    userSearchableFields: jest.fn().mockReturnValue([]),
                    userSearchableActive: jest.fn().mockReturnValue([]),
                    showInListFields: signal([]),
                    languages: signal(mockLocales),
                    defaultLanguageId: jest.fn().mockReturnValue(1),
                    setIsTreeExpanded: jest.fn(),
                    isTreeVisuallyExpanded: jest.fn().mockReturnValue(false),
                    isTreeForceCollapsed: jest.fn().mockReturnValue(false),
                    setTreeForceCollapsed: jest.fn(),
                    removeFilter: jest.fn(),
                    getFilterValue: jest.fn(),
                    $request: jest.fn(),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    setPagination: jest.fn(),
                    setSort: jest.fn(),
                    setSelectedItems: jest.fn(),
                    patchFilters: jest.fn(),
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
                    loadItems: jest.fn(),
                    reloadContentDrive: jest.fn(),
                    setPath: jest.fn(),
                    setShowAddToBundle: jest.fn(),
                    setUserSearchableFields: jest.fn(),
                    setShowInListFields: jest.fn(),
                    addUserSearchableField: jest.fn(),
                    clearUserSearchableFilters: jest.fn()
                }),
                mockProvider(Router, {
                    createUrlTree: jest.fn().mockReturnValue({ toString: () => '' })
                }),
                mockProvider(Location, {
                    go: jest.fn(),
                    replaceState: jest.fn(),
                    path: jest.fn().mockReturnValue(''),
                    subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
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

    it('opens the panel by identifier from a shared ?editContent= link on construction', () => {
        deepLinkQueryParams.editContent = 'id-1';
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith('id-1', undefined);
    });

    it('forwards the language from the link so the exact version reopens', () => {
        // An identifier has one version per language, so without this the resolver can only guess —
        // and it runs before the store's languages request has resolved.
        deepLinkQueryParams.editContent = 'id-1';
        deepLinkQueryParams.editContentLang = '2';
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith('id-1', 2);
    });

    it('ignores a non-numeric language on the link', () => {
        deepLinkQueryParams.editContent = 'id-1';
        deepLinkQueryParams.editContentLang = 'nope';
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith('id-1', undefined);
    });

    it('ignores the non-shareable `new` marker on construction', () => {
        deepLinkQueryParams.editContent = 'new';
        mountShell();

        expect(openEditByIdentifier).not.toHaveBeenCalled();
    });
});
