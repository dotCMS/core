import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/vitest';
import { of, Subject } from 'rxjs';
import { Mocked, beforeEach, describe, expect, it, vi } from 'vitest';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
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
    PushPublishService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotEditContentRequest } from '@dotcms/edit-content';
import { GlobalStore } from '@dotcms/store';
import { DotFolderListViewComponent } from '@dotcms/ui';
import { DOT_SYSTEM_CONFIG_SERVICE_MOCK, mockLocales } from '@dotcms/utils-testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { DotContentDriveGridCardComponent } from '../components/dot-content-drive-grid-card/dot-content-drive-grid-card.component';
import { DEFAULT_PAGE, DEFAULT_PAGINATION } from '../shared/constants';
import {
    MOCK_ITEMS,
    MOCK_ROUTE,
    MOCK_SEARCH_RESPONSE,
    MOCK_SITES,
    MOCK_BASE_TYPES
} from '../shared/mocks';
import {
    DotContentDriveDialog,
    DotContentDriveActionExecution,
    DotContentDriveActionExecutionResult,
    DotContentDriveDialogDrillDown,
    DotContentDrivePanelLocation,
    DotContentDriveSortOrder,
    DotContentDriveStatus
} from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { provideContentDriveFieldFilterHost } from '../store/content-drive-field-filter-host';
import { provideContentDriveFilterFacade } from '../store/content-drive-filter-facade';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

// Backs the navigation service mock's readonly `$editPanelRequest`. Typed (not cast) so tests get
// a compile-checked payload; reset in the shared beforeEach for isolation.
const editPanelRequestSignal: WritableSignal<DotEditContentRequest | null> = signal(null);
// Backs the mock's `$panelLocation`: what the URL must say about the open panel. The shell's URL
// sync and its Back guard read this, not the request (#37759).
const panelLocationSignal: WritableSignal<DotContentDrivePanelLocation | null> = signal(null);
/**
 * The Edit Permissions dialog the shell opens through `DialogService` (#37759, FR-030). `onClose`
 * is a subject so a test can close it the way PrimeNG does.
 */
let permissionsDialogClose$ = new Subject<unknown>();
const permissionsDialogRef = {
    get onClose() {
        return permissionsDialogClose$.asObservable();
    },
    close: vi.fn(() => permissionsDialogClose$.next(undefined))
};
const dialogServiceOpen = vi.fn(() => permissionsDialogRef);

// Module scope: both store mocks in this file read it, and they live in describes that do not
// share a `beforeEach`. Reset per test rather than re-created, so neither mock captures a stale one.
/** Nothing refused by default; the refusal tests set it. Module-scoped like its siblings,
 * because the deep-link describe mounts its own shell without the main block's beforeEach. */
const folderDeleteRefusalSignal: WritableSignal<DotFolderBulkDeleteRefusalKind | undefined> =
    signal(undefined);
/** Why a folder duplication never became a run, as the store holds it (#37062). */
const folderDuplicateRefusalSignal: WritableSignal<string | undefined> = signal(undefined);
// How many folders one duplicate or delete may carry; `null` is no ceiling advertised.
const folderDuplicateMaxPathsSignal: WritableSignal<number | null> = signal(null);
const folderDeleteMaxPathsSignal: WritableSignal<number | null> = signal(null);
const canAddChildrenSignal: WritableSignal<boolean> = signal(true);
// The site-level answer the drop guard falls back to at the root. Module scope for the same reason
// as the one above: two store mocks in this file read it from describes with no shared `beforeEach`.
const siteCanAddChildrenSignal: WritableSignal<boolean | undefined> = signal(undefined);
const systemHostCanReadSignal = signal(true);
const allSiteContentSelectedSignal = signal(false);
// The listing on show (#37930). Module-level and reset per test, like the signals above.
const viewTypeSignal: WritableSignal<'list' | 'card'> = signal('list');
// The folder on show, so a test can play Back across a folder change.
const pathSignal: WritableSignal<string> = signal('/');

/**
 * The shell's wiring of the grid view (#37930). In its own file because the main shell spec sits at
 * the worker's memory ceiling: adding these tests there ran it out of heap.
 */
describe('DotContentDriveShellComponent — grid view', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let store: Mocked<InstanceType<typeof DotContentDriveStore>>;
    let router: SpyObject<Router>;
    let location: SpyObject<Location>;
    let filtersSignal: ReturnType<typeof signal>;
    let statusSignal: ReturnType<typeof signal<DotContentDriveStatus>>;
    // Reactive so the shell's syncDialogEffect reacts (mirrors the real SignalStore signal).
    let dialogSignal: WritableSignal<DotContentDriveDialog | undefined>;
    let pageLeaveRequestSubject: Subject<void>;
    let selectedItemsSignal: WritableSignal<DotContentDriveItem[]>;
    // Header override published by a dialog body that has drilled into a sub-screen.
    let dialogDrillDownSignal: WritableSignal<DotContentDriveDialogDrillDown | undefined>;
    // Result of a finished workflow action, which the shell turns into a toast.
    let actionExecutionResultSignal: WritableSignal<
        DotContentDriveActionExecutionResult | undefined
    >;
    // Reactive so the shell's $extraColumns computed recomputes when the fields change.
    let showInListFieldsSignal: WritableSignal<DotCMSContentTypeField[]>;

    // The run the toolbar surface speaks for, driven from here so the shell's status-toast effect
    // can be exercised. Only unmarked runs reach it; the store's own computed does that filtering.
    const toolbarRunSignal = signal<DotContentDriveActionExecution | undefined>(undefined);
    const toolbarRunCountSignal = signal(0);

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
            mockProvider(ActivatedRoute, MOCK_ROUTE),
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            // The folder context menu confirms folder deletes through this.
            mockProvider(DotAlertConfirmService, { confirm: vi.fn() }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                getContentTypes: vi.fn().mockImplementation(() => of([]))
            }),
            mockProvider(DotLanguagesService, {
                get: vi.fn().mockReturnValue(of())
            }),
            mockProvider(DotFolderService, {
                getFolders: vi.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotUploadFileService, {
                uploadFileByBaseType: vi.fn().mockReturnValue(of({})),
                uploadFilesByBaseType: vi.fn().mockReturnValue(
                    of({
                        kind: 'accepted',
                        handle: { jobId: 'job-1', statusUrl: '/api/v1/jobs/job-1/status' }
                    })
                )
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
                createContent: vi.fn(),
                closeEditPanel: vi.fn(),
                openEditByIdentifier: vi.fn(),
                panelSaved: vi.fn(),
                panelLanguageChanged: vi.fn(),
                $editPanelRequest: editPanelRequestSignal,
                $forceLegacyEditor: signal(false),
                $reopeningPanel: signal(false),
                $panelLocation: panelLocationSignal
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
            mockProvider(DotHttpErrorManagerService)
        ],
        componentProviders: [
            DotContentDriveStore,
            mockProvider(DialogService, { open: dialogServiceOpen }),
            provideContentDriveFilterFacade(),
            // The toolbar renders for real here, "More" overflow included, so its own seam has to
            // be provided the same way the shell provides it in production.
            provideContentDriveFieldFilterHost()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        canAddChildrenSignal.set(true);
        siteCanAddChildrenSignal.set(undefined);
        systemHostCanReadSignal.set(true);
        allSiteContentSelectedSignal.set(false);
        viewTypeSignal.set('list');
        pathSignal.set('/');
        filtersSignal = signal({});
        statusSignal = signal(DotContentDriveStatus.LOADING);
        dialogSignal = signal<DotContentDriveDialog | undefined>(undefined);
        selectedItemsSignal = signal<DotContentDriveItem[]>([]);
        pageLeaveRequestSubject = new Subject<void>();
        dialogDrillDownSignal = signal<DotContentDriveDialogDrillDown | undefined>(undefined);
        folderDeleteRefusalSignal.set(undefined);
        folderDuplicateRefusalSignal.set(undefined);
        folderDuplicateMaxPathsSignal.set(null);
        folderDeleteMaxPathsSignal.set(null);
        actionExecutionResultSignal = signal<DotContentDriveActionExecutionResult | undefined>(
            undefined
        );
        showInListFieldsSignal = signal<DotCMSContentTypeField[]>([]);
        editPanelRequestSignal.set(null);
        panelLocationSignal.set(null);
        permissionsDialogClose$ = new Subject<unknown>();
        dialogServiceOpen.mockClear();
        permissionsDialogRef.close.mockClear();

        const currentSiteMock = vi.fn().mockReturnValue(MOCK_SITES[0]);
        const systemHostSelectedMock = vi.fn().mockReturnValue(false);

        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: vi.fn(),
                    folderDeleteRefusal: folderDeleteRefusalSignal,
                    clearFolderDeleteRefusal: vi.fn(),
                    folderDuplicateRefusal: folderDuplicateRefusalSignal,
                    folderDuplicateMaxPaths: folderDuplicateMaxPathsSignal,
                    folderDeleteMaxPaths: folderDeleteMaxPathsSignal,
                    clearFolderDuplicateRefusal: vi.fn(),
                    // No advertised ceiling by default, which is the case that leaves the refusing
                    // to the server. The gate's own tests set one.
                    uploadCeilings: vi.fn().mockReturnValue(null),
                    // Read by the toolbar (rendered for real here) and the drop zone: both gate
                    // their creation affordances on it.
                    $canAddChildren: canAddChildrenSignal,
                    // Read by the folder menu the shell renders, to gate Duplicate.
                    $canDuplicateHere: canAddChildrenSignal,
                    siteCanAddChildren: siteCanAddChildrenSignal,
                    // The sidebar this shell renders reads it to decide whether to offer the
                    // System Host entry at all.
                    systemHostCanRead: systemHostCanReadSignal,
                    currentSite: currentSiteMock,
                    // Tree collapsed at start to render the toggle button on toolbar
                    isTreeExpanded: vi.fn().mockReturnValue(false),
                    removeFilter: vi.fn(),
                    getFilterValue: vi.fn(),
                    $request: vi.fn(),
                    items: vi.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: vi.fn().mockReturnValue(DEFAULT_PAGINATION),
                    setIsTreeExpanded: vi.fn(),
                    isTreeVisuallyExpanded: vi.fn().mockReturnValue(false),
                    isTreeForceCollapsed: vi.fn().mockReturnValue(false),
                    setTreeForceCollapsed: vi.fn(),
                    path: pathSignal,
                    filters: filtersSignal,
                    clearFilters: vi.fn(),
                    status: statusSignal,
                    sort: vi.fn().mockReturnValue({
                        field: 'title',
                        order: DotContentDriveSortOrder.ASC
                    }),
                    pages: vi.fn().mockReturnValue([DEFAULT_PAGE]),
                    setItems: vi.fn(),
                    setStatus: vi.fn(),
                    startExternalRun: vi.fn().mockReturnValue('run-1'),
                    trackUploadJob: vi.fn(),
                    activeRunCount: signal(0),
                    toolbarRun: toolbarRunSignal,
                    toolbarRunCount: toolbarRunCountSignal,
                    // The Action Center and the toolbar lock only on runs that are not
                    // backgrounded, and both render inside the shell.
                    blockingRunCount: signal(0),
                    toolbarBlockingRunCount: toolbarRunCountSignal,
                    busyRows: signal<string[]>([]),
                    allBusyRows: signal<string[]>([]),
                    endExternalRun: vi.fn(),
                    setPagination: vi.fn(),
                    setSort: vi.fn(),
                    selectedItems: selectedItemsSignal,
                    setSelectedItems: vi.fn(),
                    // Read by the Action Center, which the shell renders for real inside the dialog.
                    currentUserIsAdmin: vi.fn().mockReturnValue(false),
                    // Resolved on portlet init; `false` disables Push Publish everywhere it
                    // is gated, which is all the shell's own tests need.
                    hasPushPublishEnvironments: vi.fn().mockReturnValue(false),
                    patchFilters: vi.fn(),
                    contextMenu: vi.fn().mockReturnValue(null),
                    dialog: dialogSignal,
                    dialogDrillDown: dialogDrillDownSignal,
                    // Read by the toolbar, which the shell renders for real.
                    actionExecution: signal(undefined),
                    // Read by the Locale chip inside that toolbar: the store resolves the languages
                    // once and seeds the environment default into the `languageId` filter.
                    languages: signal(mockLocales),
                    defaultLanguageId: vi.fn().mockReturnValue(1),
                    actionExecutionResult: actionExecutionResultSignal,
                    clearActionExecutionResult: vi.fn(),
                    setDialog: vi.fn(),
                    setDialogDrillDown: vi.fn(),
                    clearDialogDrillDown: vi.fn(),
                    loadFolders: vi.fn(),
                    loadChildFolders: vi.fn(),
                    updateFolders: vi.fn(),
                    folders: vi.fn(),
                    selectedNode: vi.fn(),
                    setSelectedNode: vi.fn(),
                    // The shell renders the sidebar, which asks the store which entry is selected.
                    $allSiteContentSelected: allSiteContentSelectedSignal,
                    $systemHostSelected: systemHostSelectedMock,
                    // Mirrors the store's own computed rather than hardcoding an answer, so these
                    // tests keep driving the destination through the signals they already control:
                    // System Host when that is selected, the current site otherwise.
                    $newContentHostId: vi.fn(() =>
                        systemHostSelectedMock() ? 'SYSTEM_HOST' : currentSiteMock()?.identifier
                    ),
                    selectAllSiteContent: vi.fn(),
                    selectSystemHost: vi.fn(),
                    sidebarLoading: vi.fn(),
                    closeDialog: vi.fn(),
                    sourceEditor: signal(null),
                    closeSourceEditor: vi.fn(),
                    patchContextMenu: vi.fn(),
                    resetContextMenu: vi.fn(),
                    setDragItems: vi.fn(),
                    cleanDragItems: vi.fn(),
                    dragItems: vi.fn().mockReturnValue({ folders: [], contentlets: [] }),
                    loadItems: vi.fn(),
                    reloadContentDrive: vi.fn(),
                    setPath: vi.fn(),
                    setShowAddToBundle: vi.fn(),
                    userSearchableFields: vi.fn().mockReturnValue([]),
                    userSearchableActive: vi.fn().mockReturnValue([]),
                    showInListFields: showInListFieldsSignal,
                    setUserSearchableFields: vi.fn(),
                    setShowInListFields: vi.fn(),
                    addUserSearchableField: vi.fn(),
                    clearUserSearchableFilters: vi.fn(),
                    viewType: viewTypeSignal,
                    setViewType: vi.fn()
                }),
                mockProvider(Router, {
                    createUrlTree: vi.fn(
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
                    go: vi.fn(),
                    replaceState: vi.fn(),
                    path: vi.fn().mockReturnValue(''),
                    // Return a real subscription so the shell's popstate listener can be captured
                    // and torn down without throwing on destroy.
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
                // The Action Center child looks up bulk actions on init, which happens as soon as a
                // selection is present in these tests.
                mockProvider(DotWorkflowsActionsService, {
                    getBulkActions: vi.fn().mockReturnValue(of({ schemes: [] }))
                }),
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
                    pageLeaveRequest$: pageLeaveRequestSubject
                })
            ]
        });
        store = spectator.inject(DotContentDriveStore, true);
        router = spectator.inject(Router);
        location = spectator.inject(Location);

        // Module-level, so whatever the last test left here is what the next one starts with. The
        // shell raises the status toast off these, which made an unrelated test see a message it
        // never asked for.
        toolbarRunSignal.set(undefined);
        toolbarRunCountSignal.set(0);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('grid view (#37930)', () => {
        const listing = () => spectator.query(DotFolderListViewComponent);
        const cards = () => spectator.queryAll(DotContentDriveGridCardComponent);

        const showGrid = () => {
            viewTypeSignal.set('card');
            spectator.detectChanges();
        };

        beforeEach(() => statusSignal.set(DotContentDriveStatus.LOADED));

        describe('US1 — one listing, two layouts', () => {
            it('should lay the listing out as a table in the list view', () => {
                spectator.detectChanges();

                expect(listing()?.$layout()).toBe('table');
                expect(cards().length).toBe(0);
            });

            it('should lay the same listing out as Drive cards in the card view', () => {
                spectator.detectChanges();
                showGrid();

                expect(spectator.queryAll(DotFolderListViewComponent).length).toBe(1);
                expect(listing()?.$layout()).toBe('grid');
                expect(spectator.query('p-table')).toBeNull();
                expect(cards().map((card) => card.$item())).toEqual(MOCK_ITEMS);
            });

            it('should give the listing the stored page size and sort, which a rebuilt table shows', () => {
                spectator.detectChanges();

                expect(listing()?.$rows()).toBe(DEFAULT_PAGINATION.limit);
                expect(listing()?.$sortField()).toBe('title');
                expect(listing()?.$sortOrder()).toBe(1);
            });
        });

        describe('US3 — one selection for both layouts', () => {
            it('should store what a card checkbox selects, like a row checkbox', () => {
                spectator.detectChanges();
                showGrid();

                spectator.click(
                    spectator.queryAll(byTestId('grid-card-checkbox'))[0] as HTMLElement
                );

                expect(store.setSelectedItems).toHaveBeenCalledWith([MOCK_ITEMS[0]]);
            });
        });

        describe('US4 — the URL follows the view', () => {
            it('should write the view to the URL without adding a history entry', () => {
                spectator.detectChanges();
                showGrid();

                expect(router.createUrlTree).toHaveBeenLastCalledWith([], {
                    queryParams: expect.objectContaining({ viewType: 'card' }),
                    queryParamsHandling: 'merge'
                });
                expect(location.replaceState).toHaveBeenLastCalledWith(
                    expect.stringContaining('viewType=card')
                );
                expect(location.go).not.toHaveBeenCalled();
            });

            it('should keep the view on Back, and not stack a history entry for it', () => {
                // Folder B was opened in the table, then the user switched to the grid. Back
                // restores folder A's URL, which still says `list`; the grid stays (the view is a
                // preference) and the URL is corrected in place, so Forward still reaches B.
                pathSignal.set('/B/');
                spectator.detectChanges();
                showGrid();
                location.go.mockClear();
                location.replaceState.mockClear();
                location.path.mockReturnValue('?isTreeExpanded=true&viewType=list&path=%2FA%2F');

                pathSignal.set('/A/');
                spectator.detectChanges();

                expect(location.go).not.toHaveBeenCalled();
                expect(location.replaceState).toHaveBeenLastCalledWith(
                    expect.stringContaining('viewType=card')
                );
            });
        });

        describe('US5 — a card does what a row does', () => {
            it('should open a clicked card the way a double-clicked row opens', () => {
                spectator.detectChanges();
                showGrid();
                const component = spectator.component as unknown as Record<string, () => void>;
                const open = vi
                    .spyOn(component, 'onDoubleClick')
                    .mockImplementation(() => undefined);

                spectator
                    .queryAll(byTestId('grid-card-title'))[1]
                    ?.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 1 }));

                expect(open).toHaveBeenCalledWith(MOCK_ITEMS[1]);
            });
        });
    });
});
