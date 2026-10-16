import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { Mock, afterAll, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Location } from '@angular/common';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

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
    DotFolderBulkDeleteRefusalKind,
    DotSystemConfigService,
    DotUploadFileService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    PushPublishService,
    DotPermissionsService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotSite, PERMISSIONS_TYPE } from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';
import { DotSidePanelConfig, DotSidePanelService } from '@dotcms/ui';
import { DOT_SYSTEM_CONFIG_SERVICE_MOCK, mockLocales } from '@dotcms/utils-testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { DEFAULT_PAGE, DEFAULT_PAGINATION, DIALOG_TYPE, SYSTEM_HOST } from '../shared/constants';
import { MOCK_ITEMS, MOCK_SEARCH_RESPONSE, MOCK_SITES, MOCK_BASE_TYPES } from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { provideContentDriveFieldFilterHost } from '../store/content-drive-field-filter-host';
import { provideContentDriveFilterFacade } from '../store/content-drive-filter-facade';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

/** Stands in for the root side-panel outlet: records each panel opened, renders none. */
const createSidePanelsFake = () => {
    const opened: { component: unknown; config: DotSidePanelConfig }[] = [];

    return {
        opened,
        // Wide by default: no panel collapses the main navigation or the tree.
        collapsesNavigation: vi.fn().mockReturnValue(false),
        open: vi.fn((component: unknown, config: DotSidePanelConfig) => {
            const closed = new Subject<void>();
            opened.push({ component, config });

            return {
                closed,
                close: vi.fn(() => {
                    closed.next();
                    closed.complete();
                }),
                requestClose: vi.fn()
            };
        })
    };
};

/**
 * Construction-time `?editContent=` deep-link. Own describe + factory so the route param is present
 * BEFORE the component is constructed — the main shell spec's shared beforeEach always builds
 * against a route with no `editContent`, so it cannot express this path.
 *
 * In its own file, as the grid view's tests are, because the main shell spec sits at the worker's
 * memory ceiling: with these tests in it, it ran out of heap.
 */
describe('DotContentDriveShellComponent — editContent deep link', () => {
    // Shaped like dotCMS identifiers: the URL parser refuses anything else (#37759, T109).
    const DEEP_LINK_ID = '4b1d7c1e-8a2f-4c3b-9d5e-6f7a8b9c0d1e';
    const FOLDER_ID = '9f8e7d6c5b4a39281706f5e4d3c2b1a0';

    // Mutable so both the identifier and the `new` marker cases share one factory.
    const deepLinkQueryParams: Record<string, string> = {
        path: '/test/path',
        filters: 'contentType:Blog;status:published',
        editContent: DEEP_LINK_ID
    };
    // Held at describe scope so we can clear it before each mount (mockProvider reuses the same fn).
    const openEditByIdentifier = vi.fn();
    const createContent = vi.fn();
    // What a `createContent` link waits for before it opens: the tree's first load, and the
    // default language a create may start in (#37759, FR-024).
    const folders = signal<DotFolderTreeNodeItem[]>([]);
    const sidebarLoading = signal(false);
    const defaultLanguageLoaded = signal(true);
    // A folder named only by its identifier, resolved when a folder-dialog link loads (FR-031).
    const getFolderById = vi.fn();
    const getUserAccess = vi.fn();
    // The store's current site. A folder link checks the folder against it, so a test can hold it
    // at the SYSTEM_HOST placeholder to play a cold load.
    const currentSiteSignal = signal<DotSite>(MOCK_SITES[0]);
    // Whether the folder on show accepts children: New Folder's permission check reads it.
    const canAddChildrenSignal: WritableSignal<boolean> = signal(true);
    // The Edit Permissions dialog the shell opens through `DialogService` (#37759, FR-030).
    const dialogServiceOpen = vi.fn(() => ({ onClose: new Subject<unknown>(), close: vi.fn() }));
    let sidePanels: ReturnType<typeof createSidePanelsFake>;

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        providers: [
            GlobalStore,
            mockProvider(DotSiteService, {
                getCurrentSite: vi.fn().mockReturnValue(of(MOCK_SITES[0]))
            }),
            mockProvider(DotContentSearchService, {
                get: vi.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(ActivatedRoute, {
                snapshot: { queryParams: deepLinkQueryParams }
            }),
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            // The folder context menu confirms folder deletes through this.
            mockProvider(DotAlertConfirmService, { confirm: vi.fn() }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                getContentTypes: vi.fn().mockImplementation(() => of([]))
            }),
            mockProvider(DotLanguagesService, { get: vi.fn().mockReturnValue(of()) }),
            mockProvider(DotFolderService, {
                getFolders: vi.fn().mockReturnValue(of([])),
                getFolderById
            }),
            mockProvider(DotPermissionsService, { getUserAccess }),
            mockProvider(DotUploadFileService, {
                uploadFileByBaseType: vi.fn().mockReturnValue(of({}))
            }),
            provideHttpClient(),
            // The panel is behind `@defer`; once it resolves, it mounts the real editor chain,
            // which can make HTTP calls no test here mocks explicitly (e.g. languages). Without
            // this, an unmocked call attempts a real network fetch and fails the test.
            provideHttpClientTesting(),
            // The store composes withFlags, which fetches feature flags on init; stub it.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService, {
                get: vi.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: vi.fn(),
                createContent,
                closeEditPanel: vi.fn(),
                openEditByIdentifier,
                $editPanelRequest: signal(null),
                $legacyPanelRequest: signal(null),
                $panelLocation: signal(null)
            }),
            LoggerService,
            StringUtils,
            mockProvider(PushPublishService, {
                // The store resolves this on init, and both the Action Center's Push Publish row and
                // the folder context menu's Push Publish item gate on the result. An empty answer
                // disables them, which is all the shell's own tests need.
                getEnvironments: vi.fn().mockReturnValue(of([]))
            }),
            mockProvider(AddToBundleService, {
                getBundles: vi.fn().mockReturnValue(of([])),
                addToBundle: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            { provide: DotSidePanelService, useFactory: () => sidePanels }
        ],
        componentProviders: [
            DotContentDriveStore,
            mockProvider(DialogService, { open: dialogServiceOpen }),
            mockProvider(DotFolderService, {
                getFolders: vi.fn().mockReturnValue(of([])),
                getFolderById
            }),
            provideContentDriveFilterFacade(),
            // The toolbar renders for real here, "More" overflow included, so its own seam has to
            // be provided the same way the shell provides it in production.
            provideContentDriveFieldFilterHost()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        sidePanels = createSidePanelsFake();
        openEditByIdentifier.mockClear();
        createContent.mockClear();
        folders.set([]);
        sidebarLoading.set(false);
        defaultLanguageLoaded.set(true);
        delete deepLinkQueryParams['createContent'];
        getFolderById.mockReset();
        getUserAccess.mockReset();
        currentSiteSignal.set(MOCK_SITES[0]);
        // The params object is shared by the factory, so a language set by one test would otherwise
        // leak into the next.
        delete deepLinkQueryParams['editContentLang'];
    });

    /** Mounts with the deps the constructor needs; does not run change detection. */
    const mountShell = () =>
        createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: vi.fn(),
                    folderDeleteRefusal: signal<DotFolderBulkDeleteRefusalKind | undefined>(
                        undefined
                    ),
                    clearFolderDeleteRefusal: vi.fn(),
                    folderDuplicateRefusal: signal<string | undefined>(undefined),
                    folderDuplicateMaxPaths: signal<number | null>(null),
                    folderDeleteMaxPaths: signal<number | null>(null),
                    clearFolderDuplicateRefusal: vi.fn(),
                    // No advertised ceiling by default, which is the case that leaves the refusing
                    // to the server. The gate's own tests set one.
                    uploadCeilings: vi.fn().mockReturnValue(null),
                    // Read by the toolbar (rendered for real here) and the drop zone: both gate
                    // their creation affordances on it.
                    $canAddChildren: canAddChildrenSignal,
                    siteCanAddChildren: signal<boolean | undefined>(undefined),
                    // The sidebar this shell renders reads it to decide whether to offer the
                    // System Host entry at all.
                    systemHostCanRead: signal(true),
                    // Read by the template once a test runs change detection here, which only the
                    // `createContent` link tests do (#37759).
                    $allSiteContentSelected: signal(false),
                    $canDuplicateHere: canAddChildrenSignal,
                    $newContentHostId: vi.fn().mockReturnValue(MOCK_SITES[0].identifier),
                    $systemHostSelected: vi.fn().mockReturnValue(false),
                    actionExecution: signal(undefined),
                    actionExecutionResult: signal(undefined),
                    blockingRunCount: signal(0),
                    clearActionExecutionResult: vi.fn(),
                    clearDialogDrillDown: vi.fn(),
                    clearFilters: vi.fn(),
                    currentUserIsAdmin: vi.fn().mockReturnValue(false),
                    dialogDrillDown: signal(undefined),
                    hasPushPublishEnvironments: vi.fn().mockReturnValue(false),
                    selectAllSiteContent: vi.fn(),
                    selectSystemHost: vi.fn(),
                    setDialogDrillDown: vi.fn(),
                    toolbarBlockingRunCount: signal(0),
                    trackUploadJob: vi.fn(),
                    currentSite: currentSiteSignal,
                    isTreeExpanded: vi.fn().mockReturnValue(false),
                    items: vi.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: vi.fn().mockReturnValue(DEFAULT_PAGINATION),
                    path: vi.fn().mockReturnValue('/test/path'),
                    filters: signal({}),
                    status: signal(DotContentDriveStatus.LOADING),
                    sort: vi
                        .fn()
                        .mockReturnValue({ field: 'modDate', order: DotContentDriveSortOrder.ASC }),
                    pages: vi.fn().mockReturnValue([DEFAULT_PAGE]),
                    selectedItems: vi.fn().mockReturnValue([]),
                    contextMenu: vi.fn().mockReturnValue(null),
                    dialog: signal(undefined),
                    dragItems: vi.fn().mockReturnValue({ folders: [], contentlets: [] }),
                    userSearchableFields: vi.fn().mockReturnValue([]),
                    userSearchableActive: vi.fn().mockReturnValue([]),
                    showInListFields: signal([]),
                    languages: signal(mockLocales),
                    defaultLanguageId: vi.fn().mockReturnValue(1),
                    setIsTreeExpanded: vi.fn(),
                    isTreeVisuallyExpanded: vi.fn().mockReturnValue(false),
                    isTreeForceCollapsed: vi.fn().mockReturnValue(false),
                    setTreeForceCollapsed: vi.fn(),
                    removeFilter: vi.fn(),
                    getFilterValue: vi.fn(),
                    $request: vi.fn(),
                    setItems: vi.fn(),
                    setStatus: vi.fn(),
                    startExternalRun: vi.fn().mockReturnValue('run-1'),
                    activeRunCount: signal(0),
                    toolbarRun: signal(undefined),
                    toolbarRunCount: signal(0),
                    busyRows: signal<string[]>([]),
                    allBusyRows: signal<string[]>([]),
                    endExternalRun: vi.fn(),
                    setPagination: vi.fn(),
                    setSort: vi.fn(),
                    setSelectedItems: vi.fn(),
                    patchFilters: vi.fn(),
                    setDialog: vi.fn(),
                    loadFolders: vi.fn(),
                    loadChildFolders: vi.fn(),
                    updateFolders: vi.fn(),
                    folders,
                    selectedNode: vi.fn(),
                    setSelectedNode: vi.fn(),
                    sidebarLoading,
                    defaultLanguageLoaded,
                    closeDialog: vi.fn(),
                    sourceEditor: signal(null),
                    closeSourceEditor: vi.fn(),
                    patchContextMenu: vi.fn(),
                    resetContextMenu: vi.fn(),
                    setDragItems: vi.fn(),
                    cleanDragItems: vi.fn(),
                    loadItems: vi.fn(),
                    reloadContentDrive: vi.fn(),
                    setPath: vi.fn(),
                    setShowAddToBundle: vi.fn(),
                    setUserSearchableFields: vi.fn(),
                    setShowInListFields: vi.fn(),
                    addUserSearchableField: vi.fn(),
                    clearUserSearchableFilters: vi.fn(),
                    viewType: signal<'list' | 'card'>('list'),
                    setViewType: vi.fn()
                }),
                mockProvider(Router, {
                    createUrlTree: vi.fn().mockReturnValue({ toString: () => '' })
                }),
                mockProvider(Location, {
                    go: vi.fn(),
                    replaceState: vi.fn(),
                    path: vi.fn().mockReturnValue(''),
                    subscribe: vi.fn().mockReturnValue({ unsubscribe: vi.fn() })
                }),
                mockProvider(DotContentTypeService, {
                    getAllContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypesWithPagination: vi.fn().mockReturnValue(
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
                    bulkFire: vi
                        .fn()
                        .mockReturnValue(of({ successCount: 1, skippedCount: 0, fails: [] }))
                }),
                mockProvider(DotWorkflowEventHandlerService),
                mockProvider(MessageService, {
                    messageObserver: of({}),
                    clearObserver: of({})
                }),
                mockProvider(DotRouterService, {
                    goToEditPage: vi.fn(),
                    forbidRouteDeactivation: vi.fn(),
                    allowRouteDeactivation: vi.fn(),
                    pageLeaveRequest$: NEVER
                })
            ]
        });

    it('opens the panel by identifier from a shared ?editContent= link on construction', () => {
        deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith(DEEP_LINK_ID, undefined);
    });

    it('forwards the language from the link so the exact version reopens', () => {
        // An identifier has one version per language, so without this the resolver can only guess —
        // and it runs before the store's languages request has resolved.
        deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
        deepLinkQueryParams['editContentLang'] = '2';
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith(DEEP_LINK_ID, 2);
    });

    it('ignores a non-numeric language on the link', () => {
        deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
        deepLinkQueryParams['editContentLang'] = 'nope';
        mountShell();

        expect(openEditByIdentifier).toHaveBeenCalledWith(DEEP_LINK_ID, undefined);
    });

    it('ignores the non-shareable `new` marker on construction', () => {
        deepLinkQueryParams['editContent'] = 'new';
        mountShell();

        expect(openEditByIdentifier).not.toHaveBeenCalled();
    });

    describe('createContent link', () => {
        const SITE_NODE = {
            key: 'site',
            data: { type: 'site', path: '' }
        } as DotFolderTreeNodeItem;

        beforeEach(() => {
            delete deepLinkQueryParams['editContent'];
            deepLinkQueryParams['createContent'] = 'Banner';
        });

        afterAll(() => {
            delete deepLinkQueryParams['createContent'];
            deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
        });

        // The folder is the navigation service's call; the shell only waits for the tree.
        it('opens the create form, once the tree has loaded', () => {
            const spectator = mountShell();
            spectator.detectChanges();

            expect(createContent).not.toHaveBeenCalled();

            folders.set([SITE_NODE]);
            spectator.detectChanges();

            expect(createContent).toHaveBeenCalledTimes(1);
            expect(createContent).toHaveBeenCalledWith('Banner');
            expect(openEditByIdentifier).not.toHaveBeenCalled();
        });

        it('waits for a tree load that is still in flight', () => {
            const spectator = mountShell();
            sidebarLoading.set(true);
            folders.set([SITE_NODE]);
            spectator.detectChanges();

            expect(createContent).not.toHaveBeenCalled();

            sidebarLoading.set(false);
            spectator.detectChanges();

            expect(createContent).toHaveBeenCalledTimes(1);
        });

        it('waits for the default language, the one a create starts in without a language filter', () => {
            const spectator = mountShell();
            defaultLanguageLoaded.set(false);
            folders.set([SITE_NODE]);
            spectator.detectChanges();

            expect(createContent).not.toHaveBeenCalled();

            defaultLanguageLoaded.set(true);
            spectator.detectChanges();

            expect(createContent).toHaveBeenCalledTimes(1);
        });

        it('opens it once, however often the tree reloads afterwards', () => {
            const spectator = mountShell();
            folders.set([SITE_NODE]);
            spectator.detectChanges();

            folders.set([{ ...SITE_NODE }]);
            spectator.detectChanges();

            expect(createContent).toHaveBeenCalledTimes(1);
        });
    });

    describe('params that cannot hold together', () => {
        afterEach(() => {
            delete deepLinkQueryParams['createContent'];
            delete deepLinkQueryParams['editFolder'];
            deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
        });

        /** What the shell writes to clear every panel and folder-dialog param, keeping the rest. */
        const ALL_CLEARED = {
            editContent: null,
            editContentLang: null,
            createContent: null,
            createFolder: null,
            editFolder: null,
            folderPermissions: null
        };

        it.each([
            ['an edit and a create', { createContent: 'Blog' }],
            ['an edit and a folder dialog', { editFolder: FOLDER_ID }]
        ])('opens nothing for %s, and removes all of those params', (_label, extra) => {
            Object.assign(deepLinkQueryParams, extra);

            const spectator = mountShell();
            const router = spectator.inject(Router);
            const location = spectator.inject(Location);

            expect(openEditByIdentifier).not.toHaveBeenCalled();
            expect(createContent).not.toHaveBeenCalled();
            expect(router.createUrlTree).toHaveBeenCalledWith([], {
                queryParams: ALL_CLEARED,
                queryParamsHandling: 'merge'
            });
            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(location.go).not.toHaveBeenCalled();
        });
    });

    /**
     * A folder-dialog link reopens its dialog for the folder it names, following the same
     * permission rules as the context menu that opens it (#37759, FR-031, FR-033).
     */
    describe('folder dialog links', () => {
        const FOLDER_BEAN = {
            identifier: FOLDER_ID,
            inode: 'folder-inode-1',
            name: 'blog',
            title: 'Blog',
            path: '/blog/',
            hostId: MOCK_SITES[0].identifier,
            sortOrder: 2,
            showOnMenu: true,
            filesMasks: '*.jpg',
            defaultFileType: 'FileAsset',
            defaultBaseType: null
        };

        beforeEach(() => {
            delete deepLinkQueryParams['editContent'];
        });

        afterEach(() => {
            delete deepLinkQueryParams['createFolder'];
            delete deepLinkQueryParams['editFolder'];
            delete deepLinkQueryParams['folderPermissions'];
            deepLinkQueryParams['editContent'] = DEEP_LINK_ID;
            canAddChildrenSignal.set(true);
        });

        const setDialogOf = (spectator: Spectator<DotContentDriveShellComponent>) =>
            spectator.inject(DotContentDriveStore, true).setDialog as Mock;

        describe('New Folder', () => {
            const SITE_NODE = {
                key: 'site',
                data: { type: 'site', path: '' }
            } as DotFolderTreeNodeItem;

            beforeEach(() => {
                deepLinkQueryParams['createFolder'] = 'true';
            });

            it('opens New Folder once the tree has loaded and the folder accepts children', () => {
                const spectator = mountShell();
                spectator.detectChanges();

                expect(setDialogOf(spectator)).not.toHaveBeenCalled();

                folders.set([SITE_NODE]);
                spectator.detectChanges();

                expect(setDialogOf(spectator)).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.FOLDER,
                    header: 'content-drive.dialog.folder.header'
                });
            });

            // Where the toolbar would not offer New Folder, the link says why (US8/AC5, T114).
            it('shows the standard permission error and opens nothing where New Folder is not allowed', () => {
                canAddChildrenSignal.set(false);
                const spectator = mountShell();
                folders.set([SITE_NODE]);
                spectator.detectChanges();

                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 403 })
                );
            });
        });

        describe('Folder Settings', () => {
            beforeEach(() => {
                deepLinkQueryParams['editFolder'] = FOLDER_ID;
            });

            it('resolves the folder by id and opens its settings when the author can edit it', () => {
                getFolderById.mockReturnValue(of(FOLDER_BEAN));
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: false }));

                const spectator = mountShell();

                expect(getFolderById).toHaveBeenCalledWith(FOLDER_ID);
                expect(getUserAccess).toHaveBeenCalledWith(FOLDER_ID);
                expect(setDialogOf(spectator)).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.FOLDER,
                    header: 'content-drive.dialog.folder.header.edit',
                    payload: {
                        type: 'folder',
                        identifier: FOLDER_ID,
                        inode: 'folder-inode-1',
                        name: 'blog',
                        title: 'Blog',
                        path: '/blog/',
                        sortOrder: 2,
                        showOnMenu: true,
                        filesMasks: '*.jpg',
                        defaultFileType: 'FileAsset',
                        defaultBaseType: null,
                        permissions: [PERMISSIONS_TYPE.EDIT]
                    }
                });
            });

            it('shows the standard permission error and opens nothing when the author cannot edit it', () => {
                getFolderById.mockReturnValue(of(FOLDER_BEAN));
                getUserAccess.mockReturnValue(of({ canEdit: false, canEditPermissions: true }));

                const spectator = mountShell();

                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 403 })
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });

            // A folder that resolves in another site is out of reach from this one (Edge case
            // "folder … moved to another site", T115).
            it('shows the standard error and opens nothing for a folder in another site', () => {
                getFolderById.mockReturnValue(of({ ...FOLDER_BEAN, hostId: 'another-site' }));
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: true }));

                const spectator = mountShell();

                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 404 })
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });

            // On a cold load the folder lookups can answer before the admin's site does. The store
            // holds SYSTEM_HOST until then, so checking against it would refuse a folder in the
            // author's own site.
            it('waits for the admin site before checking the folder on a cold load', () => {
                currentSiteSignal.set(SYSTEM_HOST);
                getFolderById.mockReturnValue(of(FOLDER_BEAN));
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: false }));

                const spectator = mountShell();
                spectator.detectChanges();

                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
                expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();

                currentSiteSignal.set(MOCK_SITES[0]);
                spectator.detectChanges();

                expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
                expect(setDialogOf(spectator)).toHaveBeenCalledWith(
                    expect.objectContaining({ type: DIALOG_TYPE.FOLDER })
                );
            });

            it('shows the standard error and opens nothing when the folder is gone', () => {
                const error = new HttpErrorResponse({ status: 404 });
                getFolderById.mockReturnValue(throwError(() => error));
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: true }));

                const spectator = mountShell();

                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    error
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });
        });

        describe('Edit Permissions', () => {
            beforeEach(() => {
                deepLinkQueryParams['folderPermissions'] = FOLDER_ID;
                getFolderById.mockReturnValue(of(FOLDER_BEAN));
            });

            // Same rule as Folder Settings: a folder in another site is out of reach from this one
            // (Edge case "folder … moved to another site", T120).
            it('shows the standard error and opens nothing for a folder in another site', () => {
                getFolderById.mockReturnValue(of({ ...FOLDER_BEAN, hostId: 'another-site' }));
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: true }));

                const spectator = mountShell();

                expect(getFolderById).toHaveBeenCalledWith(FOLDER_ID);
                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 404 })
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });

            // A bad id or a folder that is gone (T117).
            it('shows the standard error and opens nothing when the lookup fails', () => {
                const error = new HttpErrorResponse({ status: 404 });
                getUserAccess.mockReturnValue(throwError(() => error));

                const spectator = mountShell();

                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    error
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });

            // Nothing opened, so the URL no longer names it (FR-033, T117).
            it('removes the param from the URL when nothing opens', () => {
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: false }));

                const spectator = mountShell();
                spectator.detectChanges();

                expect(spectator.inject(Router).createUrlTree).toHaveBeenLastCalledWith(
                    [],
                    expect.objectContaining({
                        queryParams: expect.objectContaining({ folderPermissions: null })
                    })
                );
            });

            it('opens Edit Permissions when the author may edit the folder permissions', () => {
                getUserAccess.mockReturnValue(of({ canEdit: false, canEditPermissions: true }));

                const spectator = mountShell();

                expect(getUserAccess).toHaveBeenCalledWith(FOLDER_ID);
                expect(setDialogOf(spectator)).toHaveBeenCalledWith({
                    type: 'FOLDER_PERMISSIONS',
                    header: 'Edit-Permissions',
                    payload: { identifier: FOLDER_ID }
                });
            });

            it('shows the standard permission error and opens nothing otherwise', () => {
                getUserAccess.mockReturnValue(of({ canEdit: true, canEditPermissions: false }));

                const spectator = mountShell();

                expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                    expect.objectContaining({ status: 403 })
                );
                expect(setDialogOf(spectator)).not.toHaveBeenCalled();
            });
        });
    });
});
