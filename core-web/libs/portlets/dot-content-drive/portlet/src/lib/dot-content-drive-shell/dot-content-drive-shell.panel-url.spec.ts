import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { NEVER, of, Subject } from 'rxjs';
import { Mock, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { Location, PopStateEvent } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import {
    ActivatedRoute,
    DefaultUrlSerializer,
    Event as RouterEvent,
    NavigationEnd,
    NavigationStart,
    Router
} from '@angular/router';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    AddToBundleService,
    DotActionUrlService,
    DotAlertConfirmService,
    DotContentletEditUrlService,
    DotContentletService,
    DotContentSearchService,
    DotContentTypeService,
    DotCurrentUserService,
    DotFolderBulkDeleteRefusalKind,
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
    DotSourceEditorRequest,
    loadDotLegacyEditorSidePanel,
    loadDotSourceEditorSidePanel
} from '@dotcms/edit-content';
import { GlobalStore } from '@dotcms/store';
import { DotSidePanelConfig, DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';
import {
    createFakeContentlet,
    DOT_SYSTEM_CONFIG_SERVICE_MOCK,
    mockLocales
} from '@dotcms/utils-testing';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { DEFAULT_PAGE, DEFAULT_PAGINATION } from '../shared/constants';
import { MOCK_ITEMS, MOCK_SEARCH_RESPONSE, MOCK_SITES, MOCK_BASE_TYPES } from '../shared/mocks';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { provideContentDriveFieldFilterHost } from '../store/content-drive-field-filter-host';
import { provideContentDriveFilterFacade } from '../store/content-drive-filter-facade';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

const CONTENT_ID = '4b1d7c1e-8a2f-4c3b-9d5e-6f7a8b9c0d1e';
const RELATED_ID = '7c2e9b14-5a3d-4f6e-8b1c-2d9a0e4f6b38';
const SAVED_ID = 'b4e1f0a2-9c3d-4e5f-a6b7-c8d9e0f1a2b3';

/** Content Drive's address bar on arrival: a folder, and the menu's param. */
const ARRIVAL_URL = '/content-drive?mId=a1b2&path=%2Ftest%2Fpath';

/** Content Drive's own params once it has written them, with the menu's. */
const DRIVE_PARAMS = {
    mId: 'a1b2',
    path: '/test/path',
    isTreeExpanded: 'false',
    viewType: 'list'
};

const BLOG_POST = createFakeContentlet({
    contentType: 'Blog',
    inode: 'blog-inode',
    identifier: CONTENT_ID,
    languageId: 1,
    title: 'My post'
});

/** A side panel the opener or Edit Code put on screen. */
interface OpenedPanel {
    config: DotSidePanelConfig;
    ref: DotSidePanelRef;
    /** The panel's guard, which holds the panel open: a close the author has not confirmed. */
    requestClose: Mock;
}

/** Stands in for the root side-panel outlet: hands out real refs and renders nothing. */
const createSidePanelsFake = () => {
    const opened: OpenedPanel[] = [];

    return {
        opened,
        collapsesNavigation: vi.fn().mockReturnValue(false),
        open: vi.fn((_component: unknown, config: DotSidePanelConfig) => {
            const ref = new DotSidePanelRef(() => undefined);
            const requestClose = vi.fn();
            vi.spyOn(ref, 'requestClose').mockImplementation(requestClose);
            opened.push({ config, ref, requestClose });

            return ref;
        })
    };
};

/**
 * Stands in for Angular's `Location`: keeps the URL the last write left, as the browser does, and
 * plays Back or Forward to everyone subscribed.
 */
const createLocationDouble = (initialUrl: string) => {
    let current = initialUrl;
    const listeners: Array<(event: PopStateEvent) => void> = [];
    const write = (path: string, query = ''): void => {
        current = query ? `${path}?${query.replace(/^\?/, '')}` : path;
    };

    return {
        path: vi.fn(() => current),
        go: vi.fn(write),
        replaceState: vi.fn(write),
        subscribe: vi.fn((onNext: (event: PopStateEvent) => void) => {
            listeners.push(onNext);

            return { unsubscribe: vi.fn() };
        }),
        /** Browser Back or Forward, landing on `url`. */
        pop(url: string): void {
            current = url;
            listeners.forEach((listener) =>
                listener({ url, pop: true, type: 'popstate', state: null })
            );
        }
    };
};

/** A URL's query params as the router reads them. */
const paramsOf = (url: string): Record<string, string> =>
    new DefaultUrlSerializer().parse(url).queryParams as Record<string, string>;

/**
 * Content Drive on the shared panel URL (#38303, FR-044 to FR-048): the real navigation service,
 * opener and `DotPanelUrlStateService`, so what the address bar shows is what an author sees. Only
 * the panel itself is faked: the editor is never chosen, so none loads, and each panel's guard
 * holds it open until the test closes it.
 *
 * In its own file, as the deep-link and grid-view tests are: the main shell spec sits at the
 * worker's memory ceiling.
 */
describe('DotContentDriveShellComponent — the open panel in the URL', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let sidePanels: ReturnType<typeof createSidePanelsFake>;
    let location: ReturnType<typeof createLocationDouble>;
    let filters: WritableSignal<Record<string, unknown>>;
    let sourceEditor: WritableSignal<DotSourceEditorRequest | null>;

    // Edit Code and the switch to the old editor load their editors' modules (Monaco among them).
    // Loaded once up front, the tests' own loads resolve from the module cache; otherwise a load can
    // still be pending when the file ends, and the worker reports it as an unhandled error.
    beforeAll(async () => {
        await Promise.all([loadDotSourceEditorSidePanel(), loadDotLegacyEditorSidePanel()]);
    });

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        providers: [
            GlobalStore,
            DotContentDriveNavigationService,
            mockProvider(DotSiteService, {
                getCurrentSite: vi.fn().mockReturnValue(of(MOCK_SITES[0]))
            }),
            mockProvider(DotContentSearchService, {
                get: vi.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(ActivatedRoute, {
                snapshot: { queryParams: { mId: 'a1b2', path: '/test/path' } }
            }),
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            mockProvider(DotAlertConfirmService, { confirm: vi.fn() }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                getContentTypes: vi.fn().mockImplementation(() => of([]))
            }),
            mockProvider(DotLanguagesService, { get: vi.fn().mockReturnValue(of()) }),
            mockProvider(DotFolderService, { getFolders: vi.fn().mockReturnValue(of([])) }),
            mockProvider(DotUploadFileService, {
                uploadFileByBaseType: vi.fn().mockReturnValue(of({}))
            }),
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService, {
                get: vi.fn().mockImplementation((key: string) => key)
            }),
            LoggerService,
            StringUtils,
            mockProvider(PushPublishService, { getEnvironments: vi.fn().mockReturnValue(of([])) }),
            mockProvider(AddToBundleService, {
                getBundles: vi.fn().mockReturnValue(of([])),
                addToBundle: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService),
            // The opener's own lookups: the editor is never chosen, so no editor loads.
            mockProvider(DotContentletEditUrlService, {
                resolveEditUrl: vi.fn(),
                resolveEditor: vi.fn(() => NEVER)
            }),
            mockProvider(DotContentletService),
            mockProvider(DotActionUrlService),
            { provide: DotSidePanelService, useFactory: () => sidePanels },
            { provide: Location, useFactory: () => location }
        ],
        componentProviders: [
            DotContentDriveStore,
            mockProvider(DialogService, { open: vi.fn() }),
            provideContentDriveFilterFacade(),
            provideContentDriveFieldFilterHost()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        sidePanels = createSidePanelsFake();
        location = createLocationDouble(ARRIVAL_URL);
        filters = signal({});
        sourceEditor = signal(null);

        spectator = createComponent({
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
                    uploadCeilings: vi.fn().mockReturnValue(null),
                    $canAddChildren: signal(true),
                    siteCanAddChildren: signal<boolean | undefined>(undefined),
                    systemHostCanRead: signal(true),
                    $allSiteContentSelected: signal(false),
                    $canDuplicateHere: signal(true),
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
                    currentSite: signal(MOCK_SITES[0]),
                    isTreeExpanded: vi.fn().mockReturnValue(false),
                    items: vi.fn().mockReturnValue(MOCK_ITEMS),
                    pagination: vi.fn().mockReturnValue(DEFAULT_PAGINATION),
                    path: vi.fn().mockReturnValue('/test/path'),
                    filters,
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
                    defaultLanguageLoaded: signal(true),
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
                    folders: signal([]),
                    selectedNode: vi.fn(),
                    setSelectedNode: vi.fn(),
                    sidebarLoading: signal(false),
                    closeDialog: vi.fn(),
                    sourceEditor,
                    closeSourceEditor: vi.fn(() => sourceEditor.set(null)),
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
                // The toolbar's content type filter, which renders for real here, pages through these.
                mockProvider(DotContentTypeService, {
                    getAllContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypes: vi.fn().mockReturnValue(of(MOCK_BASE_TYPES)),
                    getContentTypesWithPagination: vi.fn().mockReturnValue(
                        of({
                            contentTypes: MOCK_BASE_TYPES,
                            pagination: {
                                currentPage: 1,
                                totalEntries: MOCK_BASE_TYPES.length,
                                totalPages: 1
                            }
                        })
                    )
                }),
                mockProvider(DotWorkflowsActionsService),
                mockProvider(DotWorkflowActionsFireService),
                mockProvider(DotWorkflowEventHandlerService),
                mockProvider(MessageService, { messageObserver: of({}), clearObserver: of({}) }),
                mockProvider(DotRouterService, {
                    goToEditPage: vi.fn(),
                    forbidRouteDeactivation: vi.fn(),
                    allowRouteDeactivation: vi.fn(),
                    pageLeaveRequest$: NEVER
                })
            ]
        });

        // As in the app: the router has navigated, so it answers every Back with a navigation of
        // its own (`onSameUrlNavigation: 'reload'`).
        spectator.inject(Router).navigated = true;
        spectator.detectChanges();
    });

    const navigation = () => spectator.inject(DotContentDriveNavigationService);

    /**
     * Lets the panel bindings and the URL effect run. Through the fixture only: an application tick
     * (`flushEffects`) here re-enters itself under zone change detection (NG0101).
     */
    const settle = () => spectator.detectChanges();

    /** The router's answer to a Back landing on `url`: a navigation that rewrites that URL. */
    const routerAnswersBack = (url: string) => {
        const events = spectator.inject(Router).events as Subject<RouterEvent>;

        events.next(new NavigationStart(2, url, 'popstate'));
        location.replaceState(url);
        events.next(new NavigationEnd(2, url, url));
    };

    /** Browser Back landing on `url`, and the router's navigation for it. */
    const back = (url: string) => {
        location.pop(url);
        routerAnswersBack(url);
        settle();
    };

    /** The URL the address bar shows, as params. */
    const shownParams = () => paramsOf(location.path());

    /** Opens the blog post from the list, as a double-click does. */
    const openBlogPost = (): OpenedPanel => {
        navigation().editContent(BLOG_POST);
        settle();

        return sidePanels.opened[sidePanels.opened.length - 1];
    };

    it('names the content opened from the list in one history entry, beside the folder and the menu param', () => {
        location.go.mockClear();

        openBlogPost();

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: CONTENT_ID,
            editContentLang: '1'
        });
    });

    it('keeps the open panel in the URL when the author changes a filter', () => {
        openBlogPost();

        filters.set({ contentType: ['Blog'] });
        settle();

        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            filters: 'contentType:Blog',
            editContent: CONTENT_ID,
            editContentLang: '1'
        });
    });

    it('names a create by its content type, then the saved content in place on the first save', () => {
        location.go.mockClear();
        navigation().createContent('Blog');
        settle();
        const panel = sidePanels.opened[0];

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(shownParams()).toEqual({ ...DRIVE_PARAMS, createContent: 'Blog' });

        panel.config.outputs?.['saved'](
            createFakeContentlet({ identifier: SAVED_ID, languageId: 1 }) as never
        );
        settle();

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: SAVED_ID,
            editContentLang: '1'
        });
    });

    it('follows a language switch inside the editor, in place', () => {
        const panel = openBlogPost();
        location.go.mockClear();

        panel.config.outputs?.['languageChanged'](2 as never);
        settle();

        expect(location.go).not.toHaveBeenCalled();
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: CONTENT_ID,
            editContentLang: '2'
        });
    });

    it("routes Back through the open panel's guard, naming the panel while the author decides", () => {
        const listUrl = location.path();
        const panel = openBlogPost();
        const opened = location.path();

        back(listUrl);

        expect(panel.requestClose).toHaveBeenCalledTimes(1);
        expect(shownParams()).toEqual(paramsOf(opened));

        // The author discards: the panel closes and the URL no longer names it.
        panel.ref.close();
        settle();

        expect(shownParams()).toEqual(DRIVE_PARAMS);
        expect(navigation().$editPanelRequest()).toBeNull();
    });

    /**
     * A related content opened from inside the panel stacks a panel of its own (FR-048): its own
     * entry, the URL names only it, so a refresh reopens only the front one, and Back closes it
     * first and names the one underneath again.
     */
    it('stacks a related content opened from inside the panel, and Back closes it first', () => {
        const underneath = openBlogPost();
        const underneathUrl = location.path();
        location.go.mockClear();

        underneath.config.outputs?.['openContent']({
            request: {
                mode: 'edit',
                inode: 'related-inode',
                identifier: RELATED_ID,
                contentType: 'Banner',
                languageId: 2,
                title: 'Spring sale'
            }
        } as never);
        settle();
        const front = sidePanels.opened[sidePanels.opened.length - 1];

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: RELATED_ID,
            editContentLang: '2'
        });

        back(underneathUrl);

        expect(front.requestClose).toHaveBeenCalledTimes(1);
        expect(underneath.requestClose).not.toHaveBeenCalled();

        front.ref.close();
        settle();

        expect(underneath.ref.isClosed).toBe(false);
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: CONTENT_ID,
            editContentLang: '1'
        });
    });

    /**
     * "Switch to the old editor" closes the panel and reopens it in the legacy editor on the
     * content it shows, which may be related content reached in place. The URL names what
     * reopened, and the reopen takes the closed panel's history entry, so Back still leaves it in
     * one step (#37759, FR-028; #38303).
     */
    it('names the content that reopened in the legacy editor, without a history entry of its own', async () => {
        openBlogPost();
        location.go.mockClear();

        navigation().switchToLegacyEditor(
            { inode: 'blog-inode' },
            createFakeContentlet({
                contentType: 'Banner',
                inode: 'related-inode',
                identifier: RELATED_ID,
                languageId: 3,
                title: 'Spring sale'
            }),
            'Banner'
        );
        settle();
        // The panel is held closed for one tick, then reopens (see `switchToLegacyEditor`).
        await new Promise((resolve) => setTimeout(resolve));
        settle();

        expect(sidePanels.opened).toHaveLength(2);
        expect(shownParams()).toEqual({
            ...DRIVE_PARAMS,
            editContent: RELATED_ID,
            editContentLang: '3'
        });
        expect(location.go).not.toHaveBeenCalled();
    });

    describe('Edit Code', () => {
        const SOURCE_REQUEST: DotSourceEditorRequest = {
            inode: '0d4b8e2c-1a57-4c1e-9a6f-3f0c8e5a7b2d',
            title: 'header.vtl',
            language: 'velocity'
        };

        const openSource = (): OpenedPanel => {
            sourceEditor.set(SOURCE_REQUEST);
            settle();

            return sidePanels.opened[sidePanels.opened.length - 1];
        };

        it('names the open file in one history entry', () => {
            location.go.mockClear();

            openSource();

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(shownParams()).toEqual({ ...DRIVE_PARAMS, editCode: SOURCE_REQUEST.inode });
        });

        it('routes Back through its prompt, and clears the param once it closes', () => {
            const listUrl = location.path();
            const panel = openSource();

            back(listUrl);

            expect(panel.requestClose).toHaveBeenCalledTimes(1);
            expect(shownParams()).toEqual({ ...DRIVE_PARAMS, editCode: SOURCE_REQUEST.inode });

            panel.ref.close();
            settle();

            expect(shownParams()).toEqual(DRIVE_PARAMS);
        });
    });

    it('never navigates through the router for the panel', () => {
        const router = spectator.inject(Router);
        const navigate = vi.spyOn(router, 'navigate');
        const navigateByUrl = vi.spyOn(router, 'navigateByUrl');

        const listUrl = location.path();
        const panel = openBlogPost();
        panel.config.outputs?.['languageChanged'](2 as never);
        back(listUrl);
        panel.ref.close();
        settle();

        expect(navigate).not.toHaveBeenCalled();
        expect(navigateByUrl).not.toHaveBeenCalled();
    });
});
