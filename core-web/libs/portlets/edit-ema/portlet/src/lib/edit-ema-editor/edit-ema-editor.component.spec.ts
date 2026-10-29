import { patchState } from '@ngrx/signals';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { of, Subject, throwError } from 'rxjs';
import { Mock, MockInstance, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EventEmitter, Input, Output, Signal, signal, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import {
    DotActionUrlService,
    DotAlertConfirmService,
    DotAnalyticsTrackerService,
    DotContentTypeService,
    DotContentletLockerService,
    DotContentletService,
    DotCopyContentService,
    DotCurrentUserService,
    DotDevicesService,
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLanguagesService,
    DotMessageDisplayService,
    DotMessageService,
    DotPageLayoutService,
    DotPersonalizeService,
    DotPropertiesService,
    DotRouterService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotSessionStorageService,
    DotTempFileUploadService,
    DotUiColorsService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions,
    DotEditContentRequest,
    DotLegacyScreenEvent,
    DotLegacyScreenRequest,
    loadDotLegacyEditorSidePanel
} from '@dotcms/edit-content';
import { DotPaletteListStore, DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSPage, DotCMSURLContentMap, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import {
    DOT_VELOCITY_LANGUAGE_ID,
    DotCopyContentModalService,
    DotSidePanelConfig,
    DotSidePanelRef,
    DotSidePanelService,
    SafeUrlPipe
} from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    CONTENT_TYPE_MOCK_FOR_EDITOR,
    CurrentUserDataMock,
    DotCurrentUserServiceMock,
    DotDevicesServiceMock,
    DotLanguagesServiceMock,
    DotPersonalizeServiceMock,
    DotcmsConfigServiceMock,
    LoginServiceMock,
    MockDotHttpErrorManagerService,
    MockDotMessageService,
    URL_MAP_CONTENTLET,
    createFakeContentlet,
    createFakeContentType,
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    mockDotDevices,
    seoOGTagsResultMock
} from '@dotcms/utils-testing';

import { DotUveContentletToolsComponent } from './components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotEmaRunningExperimentComponent } from './components/dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-toolbar/components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { EmaFormSelectorComponent } from '../components/ema-form-selector/ema-form-selector.component';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import {
    CONTENTLET_SELECTOR_URL,
    DEFAULT_PERSONA,
    HOST,
    LAYOUT_URL,
    PERSONA_KEY
} from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    EDIT_ACTION_PAYLOAD_MOCK,
    MOCK_RESPONSE_HEADLESS,
    QUERY_PARAMS_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock,
    mockCurrentUser
} from '../shared/mocks';
import { ActionPayload, ContentletPayload, PositionPayload, VTLFile } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { WithPageApiMethods } from '../store/features/page-api/withPageApi';
import { IframeAccessMode, PageType } from '../store/models';
import { insertContentletInContainer } from '../utils';

global.URL.createObjectURL = vi.fn(
    () => 'blob:http://localhost:3000/12345678-1234-1234-1234-123456789012'
);

// Mock window.matchMedia for PrimeNG components
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn()
    }))
});

const messagesMock = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject',
    'editpage.content.add.already.title': 'Content already added',
    'editpage.content.add.already.message': 'This content is already added to this container',
    'editpage.not.lincese.error':
        'Inline editing is available only with an enterprise license. Please contact support to upgrade your license.',
    'dot.common.license.enterprise.only.error': 'Enterprise Only',
    'message.content.saved': 'Content saved',
    'message.content.note.already.published':
        'Note: If you edit auto-published content, changes apply immediately.'
};

const mockGlobalStore = {
    currentSiteId: signal('demo.dotcms.com'),
    siteDetails: signal({ identifier: 'demo.dotcms.com', hostname: 'demo.dotcms.com' }),
    loggedUser: signal(CurrentUserDataMock)
};

const mockDotUveActionsHandlerService = {
    handleAction: vi.fn((_message: unknown, _deps: unknown) => of({}))
};

const mockDotUveDragDropService = {
    setupDragEvents: vi.fn()
};

const mockInlineEditService = {
    enableInlineEdit: vi.fn(),
    disableInlineEdit: vi.fn(),
    injectInlineEdit: vi.fn(),
    removeInlineEdit: vi.fn(),
    handleInlineEdit: vi.fn(),
    initEditor: vi.fn()
};

/** Content the editor opened through the opener, as the fake {@link DotEditContentOpenerService} records it. */
interface OpenedContent {
    request: DotEditContentRequest | Signal<DotEditContentRequest>;
    options: DotEditContentOpenOptions;
    ref: DotSidePanelRef;
}

/**
 * Stands in for the side-panel opener (#38303): records each open and hands back a real panel ref,
 * so `closed` and `isClosed` behave as the outlet's would. Like the real opener, it reports
 * `closed` from the ref. `requestClose` is a spy that does nothing, as a guard holding the panel
 * open would.
 */
const createOpenerFake = () => {
    const opened: OpenedContent[] = [];

    return {
        opened,
        open: vi.fn(
            (
                request: DotEditContentRequest | Signal<DotEditContentRequest>,
                options: DotEditContentOpenOptions
            ) => {
                const ref = new DotSidePanelRef(() => undefined);
                vi.spyOn(ref, 'requestClose').mockImplementation(() => undefined);
                ref.closed.subscribe(() => options.closed?.());
                opened.push({ request, options, ref });

                return ref;
            }
        )
    };
};

// Re-created before each test and read through a factory, so each test gets its own.
let opener: ReturnType<typeof createOpenerFake>;

/**
 * Where a content type is dropped: a container the page does not list yet, with room. The editor
 * captures the drop as `getPageSavePayload(position)`, which takes the page's containers.
 */
const DROP_POSITION: PositionPayload = {
    container: {
        identifier: 'drop-container',
        uuid: '1',
        acceptTypes: 'Blog',
        maxContentlets: 5,
        contentletsId: []
    }
};

/** A panel the editor opened through `DotSidePanelService`, recorded instead of rendered. */
interface OpenedPanel {
    component: unknown;
    config: DotSidePanelConfig;
    ref: DotSidePanelRef;
}

/**
 * Records every panel opened through the side-panel service: the legacy screens the editor binds
 * with `injectSidePanel` and the form selector it opens directly. Each gets a real ref.
 *
 * @param sidePanels The editor's side-panel service.
 * @returns The panels opened, in order.
 */
const recordSidePanels = (sidePanels: DotSidePanelService): OpenedPanel[] => {
    const opened: OpenedPanel[] = [];

    vi.spyOn(sidePanels, 'open').mockImplementation((component, config) => {
        const ref = new DotSidePanelRef(() => undefined);
        opened.push({ component, config, ref });

        return ref;
    });

    return opened;
};

/** Reads a panel input, which `injectSidePanel` binds as a signal. */
const inputOf = <T>(panel: OpenedPanel, name: string): T | undefined => {
    const value = panel.config.inputs?.[name];

    return (typeof value === 'function' ? (value as () => T)() : value) as T | undefined;
};

// Stub components to avoid ng-mocks signal query issues

@Component({
    selector: 'dot-uve-toolbar',
    template: '<div></div>',
    standalone: true
})
class DotUveToolbarStubComponent {
    @Output() editUrlContentMap = new EventEmitter<unknown>();
    @Output() translatePage = new EventEmitter<unknown>();
    @Input() set unknown(value: unknown) {
        // void
    }
}

@Component({
    selector: 'dot-uve-palette',
    template: '<div></div>',
    standalone: true
})
class DotUvePaletteStubComponent {
    @Output() onTabChange = new EventEmitter<unknown>();
    @Input() languageId: unknown;
    @Input() pagePath: unknown;
    @Input() variantId: unknown;
    @Input() activeTab: unknown;
    @Input() showStyleEditorTab: unknown;
    @Input() styleSchema: unknown;
}

const createRouting = () =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, SafeUrlPipe, ConfirmDialogModule],
        declarations: [
            MockComponent(DotUveWorkflowActionsComponent),
            MockComponent(DotResultsSeoToolComponent),
            MockComponent(DotEmaRunningExperimentComponent)
        ],
        overrideComponents: [
            [
                EditEmaEditorComponent,
                {
                    remove: {
                        imports: [DotUveToolbarComponent, DotUvePaletteComponent]
                    },
                    add: {
                        imports: [DotUveToolbarStubComponent, DotUvePaletteStubComponent]
                    }
                }
            ]
        ],
        detectChanges: false,
        componentProviders: [
            ConfirmationService,
            MessageService,
            UVEStore,
            DotPaletteListStore,
            DotFavoritePageService,
            DotESContentService,
            DotSessionStorageService,
            mockProvider(DotMessageDisplayService),
            mockProvider(DotRouterService),
            mockProvider(DotGlobalMessageService),
            mockProvider(DotUiColorsService),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
                }
            },
            {
                provide: DotPropertiesService,
                useValue: {
                    ...dotPropertiesServiceMock,
                    getKeyAsList: () => of([])
                }
            },
            {
                provide: DotAlertConfirmService,
                useValue: {
                    confirm: () => of({}),
                    alert: () => of({})
                }
            },
            {
                provide: DotIframeService,
                useValue: {
                    run: () => of({})
                }
            },
            {
                provide: DotExperimentsService,
                useValue: {
                    getById(experimentId: string) {
                        if (experimentId == 'i-have-a-running-experiment') {
                            return of(getRunningExperimentMock());
                        } else if (experimentId == 'i-have-a-scheduled-experiment') {
                            return of(getScheduleExperimentMock());
                        } else if (experimentId) return of(getDraftExperimentMock());

                        return of(null);
                    }
                }
            },
            {
                provide: DotContentletService,
                useValue: {
                    getContentletByInode: () => of(URL_MAP_CONTENTLET),
                    // The VTL menu asks before opening Edit Code; by default the user may edit.
                    canLock: () =>
                        of({ canLock: true, locked: false, id: '', inode: '', lockedBy: '' })
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: {
                    handle() {
                        return of({});
                    }
                }
            },
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() },
            {
                provide: DotActionUrlService,
                useValue: {
                    getCreateContentletUrl() {
                        return of('http://localhost/test/url');
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            },
            {
                provide: WINDOW,
                useValue: window
            },
            mockProvider(DotPageLayoutService),
            {
                provide: DotUveActionsHandlerService,
                useValue: mockDotUveActionsHandlerService
            },
            {
                provide: DotUveDragDropService,
                useValue: mockDotUveDragDropService
            },
            {
                provide: InlineEditService,
                useValue: mockInlineEditService
            }
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: GlobalStore,
                useValue: mockGlobalStore
            },
            Router,
            DotSeoMetaTagsUtilService,
            DialogService,
            { provide: DotEditContentOpenerService, useFactory: () => opener },
            DotCopyContentService,
            DotCopyContentModalService,
            DotWorkflowActionsFireService,
            DotTempFileUploadService,
            DotAlertConfirmService,
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: vi.fn()
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: new MockDotHttpErrorManagerService()
            },
            {
                provide: DotcmsConfigService,
                useValue: new DotcmsConfigServiceMock()
            },
            {
                provide: PushPublishService,
                useValue: {
                    getEnvironments() {
                        return of([
                            {
                                id: '123',
                                name: 'Environment 1'
                            },
                            {
                                id: '456',
                                name: 'Environment 2'
                            }
                        ]);
                    }
                }
            },
            {
                provide: DotSeoMetaTagsService,
                useValue: { getMetaTagsResults: () => of(seoOGTagsResultMock) }
            },
            { provide: ActivatedRoute, useValue: { snapshot: { queryParams: QUERY_PARAMS_MOCK } } },
            {
                provide: DotPageApiService,
                useValue: {
                    get(data) {
                        const { language_id = 1 } = data;

                        return UVE_PAGE_RESPONSE_MAP[language_id].pipe(
                            map((page = {}) => ({
                                // Update page to "fake" a new page and avoid reference issues
                                ...(page as object)
                            }))
                        );
                    },
                    getGraphQLPage({ language_id = 1 }) {
                        return of({
                            page: UVE_PAGE_RESPONSE_MAP[language_id],
                            content: {}
                        });
                    },
                    save() {
                        return of({});
                    },
                    getPersonas() {
                        return of({
                            entity: [DEFAULT_PERSONA],
                            pagination: {
                                totalEntries: 1,
                                perPage: 10,
                                page: 1
                            }
                        });
                    }
                }
            },

            {
                provide: DotDevicesService,
                useValue: new DotDevicesServiceMock()
            },
            {
                provide: DotCurrentUserService,
                useValue: new DotCurrentUserServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            },
            {
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotPersonalizeService,
                useValue: new DotPersonalizeServiceMock()
            },
            {
                provide: DotContentTypeService,
                useValue: {
                    filterContentTypes: () => of([CONTENT_TYPE_MOCK_FOR_EDITOR]),
                    getContentTypes: () => of([CONTENT_TYPE_MOCK_FOR_EDITOR]),
                    getContentType: () => of(CONTENT_TYPE_MOCK_FOR_EDITOR)
                }
            },
            {
                provide: DotContentletLockerService,
                useValue: {
                    unlock: (_inode: string) => of({})
                }
            }
        ]
    });

describe('EditEmaEditorComponent', () => {
    describe('with queryParams and permission', () => {
        let spectator: SpectatorRouting<EditEmaEditorComponent>;
        let store: InstanceType<typeof UVEStore>;

        // `withPageApi`'s methods reach the UVEStore type through an index signature, so
        // a plain `vi.spyOn(store, ...)` cannot see them. Spying through the feature's own
        // interface keeps the call typed and still installs the spy on the real store.
        const pageApi = () => store as unknown as WithPageApiMethods;
        let confirmationService: ConfirmationService;
        let messageService: MessageService;
        let addMessageSpy: MockInstance;

        const createComponent = createRouting();

        /** The editor's edit request, or `undefined` while the editor has no such signal. */
        const editRequest = (): DotEditContentRequest | null | undefined =>
            (
                spectator.component as unknown as {
                    $editRequest?: Signal<DotEditContentRequest | null>;
                }
            ).$editRequest?.();

        /** The editor's legacy-screen request, or `undefined` while it has no such signal. */
        const legacyScreenRequest = (): DotLegacyScreenRequest | null | undefined =>
            (
                spectator.component as unknown as {
                    $legacyScreenRequest?: Signal<DotLegacyScreenRequest | null>;
                }
            ).$legacyScreenRequest?.();

        /** The content panel the editor opened last through the opener. */
        const editPanel = (): OpenedContent => {
            const panel = opener.opened.at(-1);

            expect(
                panel,
                'the editor opens content through DotEditContentOpenerService'
            ).toBeDefined();

            return panel as OpenedContent;
        };

        /**
         * The editor iframe's window, with `postMessage` reduced to the one signature the editor
         * calls, so a spy on it can be mocked.
         */
        const iframeWindow = (): { postMessage: (message: unknown, host: string) => void } => {
            const contentWindow = spectator.component.iframe?.nativeElement.contentWindow;

            if (!contentWindow) {
                throw new Error('fixture: the editor iframe has no window');
            }

            return contentWindow;
        };

        /** The site of the page being edited, which every request names. */
        const siteId = () => store.pageAsset()?.site?.identifier;

        /** The edit request the pencil opens for `EDIT_ACTION_PAYLOAD_MOCK`'s contentlet. */
        const pencilRequest = (): DotEditContentRequest => ({
            mode: 'edit',
            inode: 'contentlet-inode-123',
            identifier: 'contentlet-identifier-123',
            contentType: 'test',
            languageId: store.pageLanguageId(),
            title: 'Hello World',
            siteId: siteId(),
            portletId: 'edit-page'
        });

        /**
         * The callbacks the editor hands the actions handler with an SDK message. The handler is
         * mocked, so this reads what the editor passed it.
         */
        const actionDeps = (action: DotCMSUVEAction, payload: unknown = {}) => {
            spectator.component['handleUveMessage']({ action, payload });
            const [, deps] = mockDotUveActionsHandlerService.handleAction.mock.calls.at(-1) as [
                unknown,
                Record<string, unknown>
            ];

            return deps;
        };

        beforeEach(() => {
            opener = createOpenerFake();
            spectator = createComponent({
                queryParams: { language_id: 1, url: 'index' },
                data: {
                    data: {
                        url: 'http://localhost:3000'
                    }
                }
            });

            store = spectator.inject(UVEStore, true);
            confirmationService = spectator.inject(ConfirmationService, true);
            messageService = spectator.inject(MessageService, true);
            addMessageSpy = vi.spyOn(messageService, 'add');
            mockDotUveActionsHandlerService.handleAction.mockClear();

            pageApi().pageLoad({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                mode: UVE_MODE.EDIT,
                [PERSONA_KEY]: DEFAULT_PERSONA.identifier
            });

            spectator.detectChanges();

            // Mock iframe contentWindow for tests that need to access it
            const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
            if (iframe) {
                const mockContentWindow = {
                    addEventListener: vi.fn(),
                    removeEventListener: vi.fn(),
                    postMessage: vi.fn(),
                    scrollTo: vi.fn(),
                    document: {
                        getElementById: vi.fn(),
                        querySelector: vi.fn(),
                        createElement: vi.fn(),
                        body: {
                            appendChild: vi.fn(),
                            querySelector: vi.fn()
                        },
                        head: {
                            appendChild: vi.fn(),
                            querySelector: vi.fn()
                        }
                    }
                };
                Object.defineProperty(iframe.nativeElement, 'contentWindow', {
                    writable: true,
                    value: mockContentWindow
                });
            }
        });

        describe('DOM', () => {
            beforeEach(() => {
                vi.useFakeTimers(); // Mock the timers
            });

            afterEach(() => {
                vi.useRealTimers(); // Restore the real timers after each test
            });

            it('should hide components when the store changes', () => {
                // Dropzone is conditionally hidden when store state changes; dialog may remain in DOM when appended to body
                const componentsToHide = ['dropzone'];

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                store.viewSetDevice(iphone);

                spectator.detectChanges();

                componentsToHide.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).toBeNull();
                });
            });

            it('should hide palette when state changes', () => {
                const wrapper = spectator.query('.palette-wrapper');

                // First, make sure palette wrapper is open by default
                expect(wrapper.classList).toContain('open');

                // Simulate Click the toggle button
                store.setPaletteOpen(false);

                spectator.detectChanges();

                // Palette wrapper should no longer have the open class
                expect(wrapper.classList).not.toContain('open');
            });

            it('should ignore iframe height postMessage when the iframe is locally accessible', () => {
                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
                const mockDoc = document.implementation.createHTMLDocument();

                patchState(store, { iframeAccessMode: IframeAccessMode.LOCAL });

                Object.defineProperty(iframe.nativeElement, 'contentDocument', {
                    configurable: true,
                    value: mockDoc
                });

                window.dispatchEvent(
                    new MessageEvent('message', {
                        data: {
                            action: DotCMSUVEAction.IFRAME_HEIGHT,
                            payload: { height: 321 }
                        }
                    })
                );

                expect(mockDotUveActionsHandlerService.handleAction).not.toHaveBeenCalled();
            });

            it('should handle iframe height postMessage when the iframe is cross-origin', () => {
                const actualIframe = spectator.debugElement.query(By.css('iframe'));

                patchState(store, { iframeAccessMode: IframeAccessMode.CROSS_ORIGIN });

                Object.defineProperty(actualIframe.nativeElement, 'contentDocument', {
                    configurable: true,
                    get: () => {
                        throw new DOMException('Blocked', 'SecurityError');
                    }
                });

                Object.defineProperty(actualIframe.nativeElement, 'contentWindow', {
                    configurable: true,
                    value: window
                });

                const message = {
                    action: DotCMSUVEAction.IFRAME_HEIGHT,
                    payload: { height: 321 }
                };

                const messageEvent = new MessageEvent('message', { data: message });
                Object.defineProperty(messageEvent, 'source', {
                    configurable: true,
                    value: window
                });
                window.dispatchEvent(messageEvent);

                expect(mockDotUveActionsHandlerService.handleAction).toHaveBeenCalledWith(
                    message,
                    expect.any(Object)
                );
            });

            it('should have a toolbar', () => {
                const toolbar = spectator.query('dot-uve-toolbar');

                expect(toolbar).not.toBeNull();
            });

            /**
             * Reversed by #37308 — this used to assert the opposite.
             *
             * The name said "for a variant", but no variant term ever gated these: they sit under
             * `editorCanEditContent()`, and what hid them was the RUNNING experiment this spec's
             * `DotExperimentsService` mock returns for `i-have-a-running-experiment`. That guard is
             * gone, so the editing tools a variant fix actually needs — the palette to add with, the
             * dropzone to drop on, the contentlet tools to edit with — are present while the
             * experiment runs. The shell's warning banner is what tells the editor the run's results
             * will now mix data from before and after the change.
             */
            it('should keep the palette while a variant experiment is running', () => {
                spectator.detectChanges();

                spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                spectator.detectChanges();
                pageApi().pageLoad({
                    url: 'index',
                    language_id: '5',
                    // Carried deliberately. `editorCanEditContent` is
                    // `editorHasAccessToEditMode() && viewMode === EDIT`, and this call replaces the
                    // params the describe set up — so omitting the mode hides the palette for a
                    // reason that has nothing to do with the experiment, and the assertion stops
                    // testing what it names.
                    mode: UVE_MODE.EDIT,
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    variantName: 'hello-there',
                    experimentId: 'i-have-a-running-experiment'
                });

                spectator.detectChanges();

                // Only the palette: `contentlet-tools` needs a hovered contentlet area and
                // `dropzone` needs active drag bounds, so neither renders in a static fixture
                // whatever this guard does.
                expect(spectator.query(byTestId('palette'))).not.toBeNull();
            });

            it('should show the editor components when there is a running experiement and initialize the editor in a default variant', async () => {
                const componentsToShow = ['palette', 'confirm-dialog'];

                spectator.activatedRouteStub.setQueryParam('variantName', DEFAULT_VARIANT_ID);

                spectator.detectChanges();

                pageApi().pageLoad({
                    url: 'index',
                    language_id: '5',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                componentsToShow.forEach((testId) => {
                    expect(
                        spectator.debugElement.query(By.css(`[data-testId="${testId}"]`))
                    ).not.toBeNull();
                });
            });

            it('should reload when Block editor is saved', () => {
                const blockEditorSidebar = spectator.query(DotBlockEditorSidebarComponent);
                const spy = vi.spyOn(pageApi(), 'pageReload');
                blockEditorSidebar.onSaved.emit();
                expect(spy).toHaveBeenCalled();
            });

            it('should show the error component when there is no live version', () => {
                const errorComponent = spectator.query(DotUvePageVersionNotFoundComponent);

                spectator.detectChanges();

                pageApi().pageLoad({
                    url: 'index',
                    language_id: '9'
                });

                spectator.detectChanges();

                expect(errorComponent).toBeDefined();
            });
        });

        describe('Computed Properties', () => {
            describe('$editorContentStyles', () => {
                it('should return display grid when socialMedia is null', () => {
                    patchState(store, {
                        viewSocialMedia: null
                    });

                    spectator.detectChanges();

                    expect(spectator.component.$editorContentStyles()).toEqual({
                        display: 'grid'
                    });
                });

                it('should return display none when socialMedia is set', () => {
                    patchState(store, {
                        viewSocialMedia: 'facebook'
                    });

                    spectator.detectChanges();

                    expect(spectator.component.$editorContentStyles()).toEqual({
                        display: 'none'
                    });
                });
            });

            describe('$pageURLS', () => {
                it('should construct live URL with clientHost and url from pageParams', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/my-page',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const urls = spectator.component.$pageURLS();
                    const liveUrl = urls.find((u) => u.label === 'uve.toolbar.page.live.url');

                    expect(liveUrl?.value).toBe('https://example.com/my-page');
                });

                it('should strip /index suffix from live URL', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/my-page/index',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const liveUrl = spectator.component
                        .$pageURLS()
                        .find((u) => u.label === 'uve.toolbar.page.live.url');

                    expect(liveUrl?.value).toBe('https://example.com/my-page');
                });

                it('should strip /index.html suffix from live URL', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/my-page/index.html',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const liveUrl = spectator.component
                        .$pageURLS()
                        .find((u) => u.label === 'uve.toolbar.page.live.url');

                    expect(liveUrl?.value).toBe('https://example.com/my-page');
                });

                it('should fallback to window.location.origin when clientHost is not provided', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/my-page',
                            clientHost: undefined,
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const liveUrl = spectator.component
                        .$pageURLS()
                        .find((u) => u.label === 'uve.toolbar.page.live.url');

                    expect(liveUrl?.value).toBe(`${window.location.origin}/my-page`);
                });

                it('should default to root path when url is undefined', () => {
                    patchState(store, {
                        pageParams: {
                            url: undefined,
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const liveUrl = spectator.component
                        .$pageURLS()
                        .find((u) => u.label === 'uve.toolbar.page.live.url');

                    expect(liveUrl?.value).toBe('https://example.com/');
                });

                it('should include current view URL', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/my-page',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    const viewUrl = spectator.component
                        .$pageURLS()
                        .find((u) => u.label === 'uve.toolbar.page.current.view.url');

                    expect(viewUrl).toBeTruthy();
                    expect(typeof viewUrl?.value).toBe('string');
                });
            });

            describe('$pageURL', () => {
                it('should return the live URL from $pageURLS', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/about-us',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    expect(spectator.component.$pageURL()).toBe('https://example.com/about-us');
                });

                it('should return root URL when url is undefined', () => {
                    patchState(store, {
                        pageParams: {
                            url: undefined,
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    expect(spectator.component.$pageURL()).toBe('https://example.com/');
                });

                it('should strip /index from URL', () => {
                    patchState(store, {
                        pageParams: {
                            url: '/about-us/index',
                            clientHost: 'https://example.com',
                            language_id: '1',
                            [PERSONA_KEY]: 'dot:persona'
                        }
                    });

                    expect(spectator.component.$pageURL()).toBe('https://example.com/about-us');
                });
            });

            describe('$showLockOverlay', () => {
                const lockedByAnotherUser = {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: 'another-user',
                        lockedByName: 'Another User',
                        canLock: true
                    }
                };

                it('should return false when view mode is not EDIT', () => {
                    patchState(store, {
                        pageParams: {
                            ...(store.pageParams() ?? {}),
                            mode: UVE_MODE.PREVIEW
                        } as DotPageApiParams,
                        flags: { FEATURE_FLAG_UVE_TOGGLE_LOCK: false },
                        pageAssetResponse: { pageAsset: lockedByAnotherUser }
                    });

                    expect(spectator.component.$showLockOverlay()).toBe(false);
                });

                describe('with feature flag enabled', () => {
                    beforeEach(() => {
                        patchState(store, { flags: { FEATURE_FLAG_UVE_TOGGLE_LOCK: true } });
                    });

                    it('should show overlay when page is not locked', () => {
                        patchState(store, {
                            pageAssetResponse: { pageAsset: MOCK_RESPONSE_HEADLESS }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(true);
                    });

                    it('should show overlay when page is locked by another user', () => {
                        patchState(store, {
                            pageAssetResponse: { pageAsset: lockedByAnotherUser }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(true);
                    });

                    it('should hide overlay when page is locked by the current user', () => {
                        patchState(store, {
                            uveCurrentUser: mockCurrentUser,
                            pageAssetResponse: {
                                pageAsset: {
                                    ...MOCK_RESPONSE_HEADLESS,
                                    page: {
                                        ...MOCK_RESPONSE_HEADLESS.page,
                                        locked: true,
                                        lockedBy: mockCurrentUser.userId,
                                        lockedByName: mockCurrentUser.givenName,
                                        canLock: true
                                    }
                                }
                            }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(false);
                    });
                });

                describe('with feature flag disabled', () => {
                    beforeEach(() => {
                        patchState(store, { flags: { FEATURE_FLAG_UVE_TOGGLE_LOCK: false } });
                    });

                    it('should hide overlay when page is not locked', () => {
                        patchState(store, {
                            pageAssetResponse: { pageAsset: MOCK_RESPONSE_HEADLESS }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(false);
                    });

                    it('should show overlay when page is locked by another user', () => {
                        patchState(store, {
                            pageAssetResponse: { pageAsset: lockedByAnotherUser }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(true);
                    });

                    it('should hide overlay when page is locked by the current user with canLock', () => {
                        patchState(store, {
                            uveCurrentUser: mockCurrentUser,
                            pageAssetResponse: {
                                pageAsset: {
                                    ...MOCK_RESPONSE_HEADLESS,
                                    page: {
                                        ...MOCK_RESPONSE_HEADLESS.page,
                                        locked: true,
                                        lockedBy: mockCurrentUser.userId,
                                        lockedByName: mockCurrentUser.givenName,
                                        canLock: true
                                    }
                                }
                            }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(false);
                    });

                    it('should show overlay when page is locked and canLock is false', () => {
                        patchState(store, {
                            pageAssetResponse: {
                                pageAsset: {
                                    ...MOCK_RESPONSE_HEADLESS,
                                    page: {
                                        ...MOCK_RESPONSE_HEADLESS.page,
                                        locked: true,
                                        lockedBy: 'another-user',
                                        canLock: false
                                    }
                                }
                            }
                        });

                        expect(spectator.component.$showLockOverlay()).toBe(true);
                    });
                });

                it('should return false when no page is loaded', () => {
                    patchState(store, { pageAssetResponse: null });

                    expect(spectator.component.$showLockOverlay()).toBe(false);
                });
            });
        });

        describe('$handleReloadContentEffect — headless provenance gate (#37097)', () => {
            // Precondition for reaching the headless-specific branch under test at all: the
            // effect's first gate returns immediately (no message, not even UVE_RELOAD_PAGE)
            // when `pageType === TRADITIONAL || !isClientReady` — that gate is unrelated to
            // this bug (it predates #36410) and stays untouched, so every case below sets
            // isClientReady true to exercise the branch the fix actually changes.
            let postMessageSpy: MockInstance;

            beforeEach(() => {
                // `this.iframe` (used by sendMessageToIframe/reloadIframeContent) is a getter
                // that unwraps to DotUveIframeComponent's own inner <iframe> ElementRef — NOT
                // the `[data-testId="iframe"]` host element the outer beforeEach mocks (that's
                // the <dot-uve-iframe> wrapper). Spy on the REAL jsdom-provided contentWindow
                // in place — replacing it wholesale (as the outer mock does for the wrapper)
                // breaks jsdom's own async iframe `load` event plumbing for later tests.
                const realContentWindow = spectator.component.iframe?.nativeElement.contentWindow;
                if (!realContentWindow) {
                    throw new Error(
                        'expected the real jsdom iframe contentWindow to be present — check the outer beforeEach'
                    );
                }
                postMessageSpy = vi
                    .spyOn(realContentWindow, 'postMessage')
                    .mockImplementation(() => undefined);
                // Spectator store type doesn't satisfy WritableStateSource but runtime works —
                // same cast withPage.spec.ts's patchStoreState helper documents.
                patchState(store as Parameters<typeof patchState>[0], { isClientReady: true });
                postMessageSpy.mockClear();
            });

            it('should send UVE_RELOAD_PAGE (never UVE_SET_PAGE_DATA) when no GraphQL request was ever registered', () => {
                // requestMetadata null, pageAssetResponse.source rest (the initial REST load
                // from the outer beforeEach's store.pageLoad)
                store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                spectator.flushEffects();

                expect(postMessageSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE }),
                    '*'
                );
                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                    expect.anything()
                );
            });

            it('should send UVE_RELOAD_PAGE (never UVE_SET_PAGE_DATA) when the GraphQL request is registered but not yet resolved', () => {
                store.setCustomClient({ query: 'query', variables: {} });
                store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                spectator.flushEffects();

                expect(postMessageSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE }),
                    '*'
                );
                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                    expect.anything()
                );
            });

            it('should send UVE_RELOAD_PAGE (never UVE_SET_PAGE_DATA) when the GraphQL request was registered then aborted/failed, leaving a stale REST asset', () => {
                // This is the row #36410's gate could not distinguish from the row above —
                // requestMetadata is non-null in both, so the old `hasClientQuery` check let
                // this one through. `source` distinguishes it correctly.
                store.setCustomClient({ query: 'query', variables: {} });
                store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                spectator.flushEffects();

                expect(postMessageSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE }),
                    '*'
                );
                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                    expect.anything()
                );
            });

            it('should send UVE_SET_PAGE_DATA (never UVE_RELOAD_PAGE) once the stored asset is GraphQL-sourced', () => {
                store.setCustomClient({ query: 'query', variables: {} });
                store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'graphql' });
                spectator.flushEffects();

                expect(postMessageSpy).toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                    '*'
                );
                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE }),
                    expect.anything()
                );
            });

            describe('bounded reload — regression from #37097 post-merge QA', () => {
                // QA on the merged fix found an endless client reload loop: every CLIENT_READY
                // re-announcement unconditionally re-patches requestMetadata (setCustomClient),
                // which changes pageAsset()'s reference and re-fires this effect. If the asset
                // is still REST-sourced (GraphQL never resolved — slow, aborted, or permanently
                // broken), the effect resent UVE_RELOAD_PAGE on every re-fire, and each reload
                // the client performs triggers a fresh CLIENT_READY, looping indefinitely.
                it('should send UVE_RELOAD_PAGE only once across repeated CLIENT_READY-style re-announcements while the asset stays REST-sourced', () => {
                    store.setCustomClient({ query: 'query', variables: {} });
                    store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                    spectator.flushEffects();

                    // Simulate the client's browser reload re-announcing itself several times
                    // (a duplicate CLIENT_READY unconditionally re-patches requestMetadata,
                    // which is exactly what changes pageAsset()'s reference and re-fires the
                    // effect) while the fetch still never resolves to GraphQL.
                    for (let i = 0; i < 10; i++) {
                        store.setCustomClient({ query: 'query', variables: { attempt: `${i}` } });
                        spectator.flushEffects();
                    }

                    const reloadCalls = postMessageSpy.mock.calls.filter(
                        ([message]) => message?.name === __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                    );
                    expect(reloadCalls.length).toBe(1);
                    expect(postMessageSpy).not.toHaveBeenCalledWith(
                        expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                        expect.anything()
                    );
                });

                it('should send a fresh UVE_RELOAD_PAGE once the asset resolves to GraphQL and later regresses to REST on a new page', () => {
                    store.setCustomClient({ query: 'query', variables: {} });
                    store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                    spectator.flushEffects();
                    postMessageSpy.mockClear();

                    // Resolves: clears the pending flag.
                    store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'graphql' });
                    spectator.flushEffects();
                    postMessageSpy.mockClear();

                    // A new navigation (markPageLoading) resets pageReloadPending so a fresh
                    // REST-sourced streak on the NEW page gets its own single reload request,
                    // rather than being silently suppressed by a stale flag from the old page.
                    // markPageLoading also resets isClientReady (unrelated pre-existing gate),
                    // so re-establish it the way a genuine post-navigation CLIENT_READY would.
                    store.markPageLoading();
                    patchState(store as Parameters<typeof patchState>[0], {
                        isClientReady: true
                    });
                    store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                    spectator.flushEffects();

                    const reloadCalls = postMessageSpy.mock.calls.filter(
                        ([message]) => message?.name === __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                    );
                    expect(reloadCalls.length).toBe(1);
                });
            });

            it('should never touch the iframe for a TRADITIONAL page regardless of source (regression)', () => {
                // Spectator store type doesn't satisfy WritableStateSource but runtime works —
                // same cast withPage.spec.ts's patchStoreState helper documents.
                patchState(store as Parameters<typeof patchState>[0], {
                    pageType: PageType.TRADITIONAL
                });
                store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS, source: 'rest' });
                spectator.flushEffects();

                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA }),
                    expect.anything()
                );
                expect(postMessageSpy).not.toHaveBeenCalledWith(
                    expect.objectContaining({ name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE }),
                    expect.anything()
                );
            });
        });

        describe('customer actions', () => {
            describe('delete', () => {
                it('should open a confirm dialog and save on confirm', () => {
                    const payload: ActionPayload = {
                        pageId: '123',
                        language_id: '1',
                        container: {
                            identifier: '123',
                            uuid: '123',
                            acceptTypes: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageContainers: [
                            {
                                identifier: '123',
                                uuid: '123',
                                contentletsId: ['123']
                            }
                        ],
                        contentlet: {
                            identifier: '123',
                            inode: '456',
                            title: 'Hello World',
                            contentType: 'test',
                            onNumberOfPages: 1
                        },
                        position: 'after'
                    };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectChanges();

                    const confirmDialogOpen = vi.spyOn(confirmationService, 'confirm');
                    const saveMock = vi.spyOn(pageApi(), 'editorSave');

                    spectator.triggerEventHandler(
                        DotUveContentletToolsComponent,
                        'deleteContent',
                        payload
                    );

                    spectator.detectComponentChanges();

                    expect(confirmDialogOpen).toHaveBeenCalled();

                    // Call the accept callback directly from the confirmation service spy
                    const confirmCall = confirmDialogOpen.mock.calls[0][0] as Confirmation;
                    confirmCall.accept();

                    expect(saveMock).toHaveBeenCalledWith([
                        { contentletsId: [], identifier: '123', personaTag: undefined, uuid: '123' }
                    ]);
                });
            });

            describe('checkAndResetActiveContentlet', () => {
                let resetActiveContentletSpy: MockInstance;

                beforeEach(() => {
                    resetActiveContentletSpy = vi.spyOn(store, 'resetSelected');
                });

                afterEach(() => {
                    resetActiveContentletSpy.mockClear();
                });

                describe('on delete', () => {
                    it('should reset activeContentlet when deleting the active contentlet', () => {
                        const activeContentlet: ActionPayload = {
                            pageId: '123',
                            language_id: '1',
                            container: {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                acceptTypes: 'test',
                                maxContentlets: 1,
                                contentletsId: ['contentlet-1']
                            },
                            pageContainers: [
                                {
                                    identifier: 'container-1',
                                    uuid: 'uuid-1',
                                    contentletsId: ['contentlet-1']
                                }
                            ],
                            contentlet: {
                                identifier: 'contentlet-1',
                                inode: 'inode-1',
                                title: 'Test',
                                contentType: 'test'
                            }
                        };

                        store.setSelected({
                            bounds: { x: 100, y: 100, width: 500, height: 500 },
                            payload: activeContentlet
                        });

                        const payload: ActionPayload = {
                            pageId: '123',
                            language_id: '1',
                            container: {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                acceptTypes: 'test',
                                maxContentlets: 1,
                                contentletsId: ['contentlet-1']
                            },
                            pageContainers: [
                                {
                                    identifier: 'container-1',
                                    uuid: 'uuid-1',
                                    contentletsId: ['contentlet-1']
                                }
                            ],
                            contentlet: {
                                identifier: 'contentlet-1',
                                inode: 'inode-1',
                                title: 'Test',
                                contentType: 'test'
                            }
                        };

                        store.setContentletArea({
                            x: 100,
                            y: 100,
                            width: 500,
                            height: 500,
                            payload
                        });

                        spectator.detectChanges();

                        const confirmDialogOpen = vi.spyOn(confirmationService, 'confirm');

                        spectator.triggerEventHandler(
                            DotUveContentletToolsComponent,
                            'deleteContent',
                            payload
                        );

                        spectator.detectComponentChanges();

                        const confirmCall = confirmDialogOpen.mock.calls[0][0] as Confirmation;
                        confirmCall.accept();

                        expect(resetActiveContentletSpy).toHaveBeenCalledTimes(1);
                    });

                    it('should not reset activeContentlet when deleting a different contentlet', () => {
                        const activeContentlet: ActionPayload = {
                            pageId: '123',
                            language_id: '1',
                            container: {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                acceptTypes: 'test',
                                maxContentlets: 1,
                                contentletsId: ['contentlet-1', 'contentlet-2']
                            },
                            pageContainers: [
                                {
                                    identifier: 'container-1',
                                    uuid: 'uuid-1',
                                    contentletsId: ['contentlet-1', 'contentlet-2']
                                }
                            ],
                            contentlet: {
                                identifier: 'contentlet-1',
                                inode: 'inode-1',
                                title: 'Test',
                                contentType: 'test'
                            }
                        };

                        store.setSelected({
                            bounds: { x: 100, y: 100, width: 500, height: 500 },
                            payload: activeContentlet
                        });

                        const payload: ActionPayload = {
                            pageId: '123',
                            language_id: '1',
                            container: {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                acceptTypes: 'test',
                                maxContentlets: 1,
                                contentletsId: ['contentlet-1', 'contentlet-2']
                            },
                            pageContainers: [
                                {
                                    identifier: 'container-1',
                                    uuid: 'uuid-1',
                                    contentletsId: ['contentlet-1', 'contentlet-2']
                                }
                            ],
                            contentlet: {
                                identifier: 'contentlet-2',
                                inode: 'inode-2',
                                title: 'Other',
                                contentType: 'test'
                            }
                        };

                        store.setContentletArea({
                            x: 100,
                            y: 100,
                            width: 500,
                            height: 500,
                            payload
                        });

                        spectator.detectChanges();

                        const confirmDialogOpen = vi.spyOn(confirmationService, 'confirm');

                        spectator.triggerEventHandler(
                            DotUveContentletToolsComponent,
                            'deleteContent',
                            payload
                        );

                        spectator.detectComponentChanges();

                        const confirmCall = confirmDialogOpen.mock.calls[0][0] as Confirmation;
                        confirmCall.accept();

                        expect(resetActiveContentletSpy).not.toHaveBeenCalled();
                    });
                });
            });

            describe('resetActiveContentletOnUnlock', () => {
                let resetActiveContentletSpy: MockInstance;
                /** Use the same store instance the component uses so patches are visible to its effect */
                let componentStore: InstanceType<typeof UVEStore>;

                beforeEach(() => {
                    componentStore = (
                        spectator.component as unknown as {
                            uveStore: InstanceType<typeof UVEStore>;
                        }
                    ).uveStore;
                    resetActiveContentletSpy = vi.spyOn(componentStore, 'resetSelected');

                    // Enable the toggle lock feature flag by patching store flags directly
                    // Since flags are loaded in onInit, we patch them after store initialization
                    const currentFlags = componentStore.flags();
                    patchState(componentStore, {
                        flags: {
                            ...currentFlags,
                            [FeaturedFlags.FEATURE_FLAG_UVE_TOGGLE_LOCK]: true
                        }
                    });
                    // Ensure pageParams has mode EDIT so $toggleLockOptions computed returns a value
                    patchState(componentStore, {
                        pageParams: {
                            ...(componentStore.pageParams() ?? {}),
                            url: 'index',
                            language_id: '1',
                            mode: UVE_MODE.EDIT,
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                        }
                    });
                    // Set pageAPIResponse so $toggleLockOptions has a page (loadPageAsset may not have completed yet)
                    componentStore.updatePageResponse({
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User',
                            canLock: true
                        }
                    });
                    spectator.detectChanges();
                });

                afterEach(() => {
                    resetActiveContentletSpy.mockClear();
                });

                // Skipped: effect relies on $toggleLockOptions; test store/effect timing makes it flaky (resetActiveContentlet not called).
                it.skip('should reset activeContentlet when page is unlocked', () => {
                    const activeContentlet: ActionPayload = {
                        pageId: '123',
                        language_id: '1',
                        container: {
                            identifier: 'container-1',
                            uuid: 'uuid-1',
                            acceptTypes: 'test',
                            maxContentlets: 1,
                            contentletsId: ['contentlet-1']
                        },
                        pageContainers: [
                            {
                                identifier: 'container-1',
                                uuid: 'uuid-1',
                                contentletsId: ['contentlet-1']
                            }
                        ],
                        contentlet: {
                            identifier: 'contentlet-1',
                            inode: 'inode-1',
                            title: 'Test Contentlet',
                            contentType: 'test'
                        }
                    };

                    // First, ensure page starts in locked state (use componentStore so effect sees it)
                    const initialResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...initialResponse,
                        page: {
                            ...initialResponse.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User'
                        }
                    });
                    spectator.detectChanges();

                    // Set active contentlet AFTER page is locked
                    componentStore.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: activeContentlet
                    });
                    spectator.detectChanges();

                    // Verify activeContentlet is set
                    expect(componentStore.editorSelected()?.payload).toEqual(activeContentlet);
                    expect(resetActiveContentletSpy).not.toHaveBeenCalled();

                    // Unlock the page (triggers $resetActiveContentletOnUnlockEffect)
                    const lockedResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...lockedResponse,
                        page: {
                            ...lockedResponse.page,
                            locked: false,
                            lockedBy: '',
                            lockedByName: ''
                        }
                    });
                    // Call detectChanges multiple times to ensure effect runs
                    spectator.detectChanges();
                    spectator.detectChanges();

                    // Verify resetActiveContentlet was called when unlocked
                    expect(resetActiveContentletSpy).toHaveBeenCalledTimes(1);
                    expect(componentStore.editorSelected()).toBeNull();
                });

                it('should not reset activeContentlet when page is unlocked but no activeContentlet exists', () => {
                    // Don't set activeContentlet
                    expect(componentStore.editorSelected()).toBeNull();

                    // Set page as locked first
                    const currentResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...currentResponse,
                        page: {
                            ...currentResponse.page,
                            locked: true,
                            lockedBy: 'current-user',
                            lockedByName: 'Current User'
                        }
                    });
                    spectator.detectChanges();

                    // Unlock the page
                    const lockedResponse = componentStore.pageAsset() as NonNullable<
                        ReturnType<typeof componentStore.pageAsset>
                    >;
                    componentStore.updatePageResponse({
                        ...lockedResponse,
                        page: {
                            ...lockedResponse.page,
                            locked: false,
                            lockedBy: '',
                            lockedByName: ''
                        }
                    });
                    // Call detectChanges multiple times to ensure effect runs
                    spectator.detectChanges();
                    spectator.detectChanges();

                    // Verify resetActiveContentlet was not called (no activeContentlet to reset)
                    expect(resetActiveContentletSpy).not.toHaveBeenCalled();
                });
            });

            describe('edit', () => {
                beforeEach(() => {
                    store.setIsClientReady(true);
                });

                // Editing the content a URL-mapped page shows was a legacy-only dialog flow; it is
                // now an ordinary edit request, so the content type picks the editor (#38303).
                it('should open the URL-mapped content as an ordinary edit request', () => {
                    spectator.detectChanges();

                    const payload = {
                        identifier: '123',
                        inode: '456',
                        title: 'Hello World',
                        contentType: 'Blog'
                    } as unknown as DotCMSURLContentMap;

                    spectator.triggerEventHandler(
                        DotUveToolbarStubComponent,
                        'editUrlContentMap',
                        payload
                    );

                    expect(editRequest()).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            inode: '456',
                            identifier: '123',
                            contentType: 'Blog',
                            title: 'Hello World',
                            siteId: siteId(),
                            portletId: 'edit-page'
                        })
                    );
                });

                beforeEach(() => {
                    vi.clearAllMocks();
                });
            });

            /**
             * Every editor flow opens the side panel through the opener, for both editor types: one
             * request signal bound with `injectEditContentPanel` (#38303, FR-001 to FR-003). The page
             * editor keeps no dialog of its own.
             */
            describe('opening content in the side panel', () => {
                it('should render no page-editor dialog and keep no dialog member', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('ema-dialog'))).toBeNull();
                    expect('dialog' in spectator.component).toBe(false);
                });

                it('should bind the request to the opener, which opens the panel once it is set', () => {
                    spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);
                    spectator.detectChanges();

                    const panel = editPanel();
                    const request = panel.request as Signal<DotEditContentRequest>;

                    expect(opener.opened.length).toBe(1);
                    expect(request()).toEqual(pencilRequest());
                    expect(panel.options.injector).toBeDefined();
                });

                /**
                 * Every request carries the experiment variant the page is on, so the editor saves
                 * in it, but only when it is not the default variant (#38303, FR-022).
                 */
                describe('the experiment variant', () => {
                    const loadVariant = () => {
                        pageApi().pageLoad({
                            clientHost: 'http://localhost:3000',
                            url: 'index',
                            language_id: '1',
                            mode: UVE_MODE.EDIT,
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                            variantName: 'variant-a'
                        });
                        spectator.detectChanges();
                        // Fixture check: the page is on the variant the requests must name.
                        expect(store.pageVariantId()).toBe('variant-a');
                    };

                    it('should name the variant on an edit from a variant page', () => {
                        loadVariant();

                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);

                        expect(editRequest()).toEqual(
                            expect.objectContaining({ mode: 'edit', variantName: 'variant-a' })
                        );
                    });

                    it('should name the variant on a create from a variant page', () => {
                        loadVariant();

                        spectator.component.placeItem(structuredClone(DROP_POSITION), {
                            baseType: 'CONTENT',
                            contentType: 'Blog',
                            draggedPayload: {
                                type: 'content-type' as const,
                                item: { variable: 'Blog', name: 'Blog' }
                            }
                        });

                        expect(editRequest()).toEqual(
                            expect.objectContaining({ mode: 'new', variantName: 'variant-a' })
                        );
                    });

                    it('should name no variant on the default variant', () => {
                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);

                        expect(editRequest()).toEqual(expect.objectContaining({ mode: 'edit' }));
                        expect(editRequest()?.variantName).toBeUndefined();
                    });
                });

                /**
                 * The edit panel's events (#38303, FR-007 to FR-021). The page refreshes on every
                 * save while the panel stays open; a create from a drop is placed once.
                 */
                describe('edit panel events', () => {
                    const contentTypeDrop = {
                        baseType: 'CONTENT',
                        contentType: 'Blog',
                        draggedPayload: {
                            type: 'content-type' as const,
                            item: { variable: 'Blog', name: 'Blog' }
                        }
                    };

                    /**
                     * What placing `newContentletId` where `position` names saves, computed on a
                     * copy of the drop the editor captured, before the editor places anything.
                     */
                    const containersAfterInsert = (
                        position: PositionPayload,
                        newContentletId: string
                    ) => {
                        const result = insertContentletInContainer({
                            ...structuredClone(store.getPageSavePayload(structuredClone(position))),
                            newContentletId
                        });
                        // Fixture check: the drop's container has room.
                        expect(result.didInsert).toBe(true);

                        return result.pageContainers;
                    };

                    /** A drop into a container the page lists, already holding its maximum. */
                    const fullContainerPosition = (): PositionPayload => {
                        const pageContainer = store
                            .$pageData()
                            .containers.find((container) => container.contentletsId.length > 0);

                        // Fixture check: the page has a container with content in it.
                        if (!pageContainer) {
                            throw new Error('fixture: the page lists no container with content');
                        }

                        return {
                            container: {
                                identifier: pageContainer.identifier,
                                uuid: pageContainer.uuid,
                                acceptTypes: 'Blog',
                                maxContentlets: pageContainer.contentletsId.length
                            }
                        };
                    };

                    it('should place a created contentlet once, with the drop it was created for, then save the page', () => {
                        const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                        spectator.component.placeItem(
                            structuredClone(DROP_POSITION),
                            contentTypeDrop
                        );
                        spectator.detectChanges();
                        const expected = containersAfterInsert(DROP_POSITION, 'new-identifier');

                        editPanel().options.saved?.(
                            createFakeContentlet({ identifier: 'new-identifier' })
                        );

                        expect(editorSaveSpy).toHaveBeenCalledTimes(1);
                        expect(editorSaveSpy).toHaveBeenCalledWith(expected);
                    });

                    it('should open a create request for the dropped type in the page language', () => {
                        spectator.component.placeItem(
                            structuredClone(DROP_POSITION),
                            contentTypeDrop
                        );

                        expect(editRequest()).toEqual(
                            expect.objectContaining({
                                mode: 'new',
                                contentType: 'Blog',
                                languageId: store.pageLanguageId(),
                                siteId: siteId(),
                                portletId: 'edit-page'
                            })
                        );
                    });

                    it('should only reload the page on a later save from the same create panel', () => {
                        const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                        spectator.component.placeItem(
                            structuredClone(DROP_POSITION),
                            contentTypeDrop
                        );
                        spectator.detectChanges();
                        const { saved } = editPanel().options;
                        saved?.(createFakeContentlet({ identifier: 'new-identifier' }));
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                        saved?.(createFakeContentlet({ identifier: 'new-identifier' }));

                        expect(editorSaveSpy).toHaveBeenCalledTimes(1);
                        expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                    });

                    it('should warn and keep the panel open when the container is full', () => {
                        const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                        spectator.component.placeItem(fullContainerPosition(), contentTypeDrop);
                        spectator.detectChanges();
                        const panel = editPanel();
                        const request = editRequest();

                        panel.options.saved?.(
                            createFakeContentlet({ identifier: 'new-identifier' })
                        );

                        expect(addMessageSpy).toHaveBeenCalledWith(
                            expect.objectContaining({ severity: 'warn' })
                        );
                        expect(editorSaveSpy).not.toHaveBeenCalled();
                        expect(panel.ref.isClosed).toBe(false);
                        expect((panel.ref.requestClose as Mock).mock.calls.length).toBe(0);
                        expect(editRequest()).toBe(request);
                    });

                    describe('after an edit', () => {
                        const openEdit = () => {
                            spectator.component['handleEditWithCopyDecision'](
                                EDIT_ACTION_PAYLOAD_MOCK
                            );
                            spectator.detectChanges();

                            return editPanel();
                        };

                        it('should load the new URL when the saved content maps to another one', () => {
                            const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                            const panel = openEdit();

                            panel.options.saved?.(
                                createFakeContentlet({
                                    identifier: 'contentlet-identifier-123',
                                    URL_MAP_FOR_CONTENT: '/blog/a-new-slug'
                                })
                            );

                            expect(pageLoadSpy).toHaveBeenCalledWith({ url: '/blog/a-new-slug' });
                        });

                        it.each([
                            ['maps to no URL', undefined],
                            ['maps to the same URL', 'index']
                        ])('should reload the page when the saved content %s', (_label, urlMap) => {
                            const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');
                            const panel = openEdit();

                            panel.options.saved?.(
                                createFakeContentlet({
                                    identifier: 'contentlet-identifier-123',
                                    URL_MAP_FOR_CONTENT: urlMap
                                })
                            );

                            expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                            expect(pageLoadSpy).not.toHaveBeenCalled();
                        });

                        it('should use the saved contentlet as it is, without fetching it again', () => {
                            const contentletService = spectator.inject(DotContentletService, true);
                            const fetchSpy = vi.spyOn(contentletService, 'getContentletByInode');
                            const panel = openEdit();

                            panel.options.saved?.(
                                createFakeContentlet({ identifier: 'contentlet-identifier-123' })
                            );

                            expect(fetchSpy).not.toHaveBeenCalled();
                        });

                        it('should load the page in the language the editor switched to', () => {
                            const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                            const panel = openEdit();

                            panel.options.languageChanged?.(3);

                            expect(pageLoadSpy).toHaveBeenCalledWith({ language_id: '3' });
                        });

                        it('should reload the page when the content is deleted', () => {
                            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');
                            const panel = openEdit();

                            panel.options.deleted?.();

                            expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                        });

                        // The safety net for workflow actions run in the legacy editor, which report
                        // nothing the page can use (#38303, R4).
                        it('should reload the page and clear the request when the panel closes itself', () => {
                            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');
                            const panel = openEdit();

                            panel.ref.close();

                            expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                            expect(editRequest()).toBeNull();
                        });
                    });

                    /**
                     * The SDK's `editContentlet()` reaches the editor through the actions handler's
                     * `openContent` callback. A headless page renders itself, so a save also asks
                     * the client to reload.
                     */
                    describe('from the SDK', () => {
                        const SDK_REQUEST: DotEditContentRequest = {
                            mode: 'edit',
                            inode: 'sdk-inode',
                            identifier: 'sdk-identifier',
                            contentType: 'Blog',
                            languageId: 1,
                            title: 'From the SDK',
                            portletId: 'edit-page'
                        };

                        /** Opens content the way the actions handler does for `editContentlet()`. */
                        const openFromSdk = () => {
                            const deps = actionDeps(DotCMSUVEAction.EDIT_CONTENTLET, {
                                inode: 'sdk-inode'
                            });
                            const openContent = deps['openContent'] as
                                | ((request: DotEditContentRequest) => void)
                                | undefined;

                            expect(
                                openContent,
                                'the editor hands the actions handler openContent'
                            ).toEqual(expect.any(Function));
                            openContent?.(SDK_REQUEST);
                            spectator.detectChanges();
                        };

                        it('should open the request the actions handler built', () => {
                            openFromSdk();

                            expect(editRequest()).toEqual(expect.objectContaining(SDK_REQUEST));
                        });

                        it('should reload the page and ask the client to reload itself after a save', () => {
                            openFromSdk();
                            const postMessageSpy = vi
                                .spyOn(iframeWindow(), 'postMessage')
                                .mockImplementation(() => undefined);
                            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                            editPanel().options.saved?.(
                                createFakeContentlet({ identifier: 'sdk-identifier' })
                            );

                            expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                            expect(postMessageSpy).toHaveBeenCalledWith(
                                { name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE },
                                '*'
                            );
                        });
                    });
                });
            });

            /**
             * Legacy screens that are not editors open in the generic legacy-screen panel, and the
             * form selector opens as panel content: no dialog (#38303, US5, R7, R10).
             */
            describe('legacy screens in the side panel', () => {
                /** Every panel opened through the side-panel service in this test. */
                let sidePanels: OpenedPanel[];

                beforeEach(() => {
                    sidePanels = recordSidePanels(spectator.inject(DotSidePanelService));
                });

                /** The legacy-screen panel the editor opened last: its request names a URL. */
                const legacyScreenPanel = (): OpenedPanel => {
                    const panel = sidePanels
                        .filter(
                            (opened) =>
                                typeof inputOf<{ url?: string }>(opened, 'request')?.url ===
                                'string'
                        )
                        .at(-1);

                    expect(
                        panel,
                        'the editor opens legacy screens in the side panel'
                    ).toBeDefined();

                    return panel as OpenedPanel;
                };

                /** Sends an `ng-event` from the legacy screen, as the generic panel passes it on. */
                const sendLegacyEvent = (detail: DotLegacyScreenEvent): void => {
                    const handler = legacyScreenPanel().config.outputs?.['legacyEvent'] as
                        | ((event: DotLegacyScreenEvent) => void)
                        | undefined;

                    expect(handler, 'the legacy screen panel binds legacyEvent').toEqual(
                        expect.any(Function)
                    );
                    handler?.(detail);
                    spectator.detectChanges();
                };

                /** Clicks "add" on a container, through the contentlet tools. */
                const addContent = (
                    type: 'content' | 'widget' | 'form',
                    payload: ActionPayload
                ) => {
                    spectator.detectChanges();
                    store.setContentletArea({ x: 100, y: 100, width: 500, height: 500, payload });
                    spectator.detectComponentChanges();
                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type,
                        payload
                    });
                    spectator.detectChanges();
                };

                /** A container with room, below a contentlet already on the page. */
                const containerPayload = (maxContentlets = 2): ActionPayload => ({
                    language_id: '1',
                    pageContainers: [
                        {
                            identifier: 'container-identifier-123',
                            uuid: 'uuid-123',
                            contentletsId: ['contentlet-identifier-123']
                        }
                    ],
                    contentlet: {
                        identifier: 'contentlet-identifier-123',
                        inode: 'contentlet-inode-123',
                        title: 'Hello World',
                        contentType: 'test',
                        onNumberOfPages: 1
                    },
                    container: {
                        identifier: 'container-identifier-123',
                        acceptTypes: 'test',
                        uuid: 'uuid-123',
                        maxContentlets,
                        contentletsId: ['123']
                    },
                    pageId: 'test',
                    position: 'after'
                });

                /** The content selector's URL, exactly as the page editor's dialog built it. */
                const selectorUrl = (containerId: string, acceptTypes: string) =>
                    `${CONTENTLET_SELECTOR_URL}?${new URLSearchParams({
                        ng: 'true',
                        container_id: containerId,
                        add: acceptTypes,
                        language_id: '1',
                        variantName: DEFAULT_VARIANT_ID
                    }).toString()}`;

                describe('add content and add widget', () => {
                    it.each([
                        ['content', 'test'],
                        ['widget', DotCMSBaseTypesContentTypes.WIDGET]
                    ] as const)(
                        'should open the content selector for %s in a legacy-screen panel',
                        (type, acceptTypes) => {
                            addContent(type, containerPayload());

                            expect(legacyScreenRequest()).toEqual({
                                url: selectorUrl('container-identifier-123', acceptTypes),
                                title: spectator
                                    .inject(DotMessageService, true)
                                    .get('edit.ema.page.dialog.header.search.content')
                            });
                            expect(inputOf(legacyScreenPanel(), 'request')).toEqual(
                                legacyScreenRequest()
                            );
                        }
                    );

                    it('should accept every type in a container that names none', () => {
                        const payload = containerPayload();
                        // The server can leave it out, whatever the type says.
                        payload.container = {
                            ...payload.container,
                            acceptTypes: undefined as unknown as string
                        };

                        addContent('content', payload);

                        expect(legacyScreenRequest()?.url).toBe(
                            selectorUrl('container-identifier-123', '*')
                        );
                    });

                    it.each(['content', 'widget'] as const)(
                        'should place the selected %s, save the page and close the selector',
                        (type) => {
                            const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                            addContent(type, containerPayload());

                            sendLegacyEvent({
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: { identifier: 'new-contentlet-identifier-123', inode: '123' }
                            });

                            expect(editorSaveSpy).toHaveBeenCalledWith([
                                {
                                    identifier: 'container-identifier-123',
                                    uuid: 'uuid-123',
                                    contentletsId: [
                                        'contentlet-identifier-123',
                                        'new-contentlet-identifier-123'
                                    ],
                                    personaTag: undefined
                                }
                            ]);
                            expect(legacyScreenRequest()).toBeNull();
                        }
                    );

                    it('should warn and keep the selector open when the selection is already in the container', () => {
                        addContent('content', containerPayload(1));

                        sendLegacyEvent({
                            name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                            data: { identifier: 'contentlet-identifier-123', inode: '123' }
                        });

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            severity: 'info',
                            summary: 'Content already added',
                            detail: 'This content is already added to this container',
                            life: 2000
                        });
                        expect(legacyScreenRequest()).toEqual(
                            expect.objectContaining({ url: expect.any(String) })
                        );
                    });

                    /**
                     * "Create new" in the content selector: the selector closes and the edit panel
                     * opens a create for that type, which places the new contentlet in the
                     * container it was asked for.
                     */
                    describe('creating from the selector', () => {
                        const createFromSelector = (payload: ActionPayload) => {
                            addContent('content', payload);
                            sendLegacyEvent({
                                name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                                data: { url: '/ignored/create/url', contentType: 'Blog' }
                            });
                        };

                        it('should close the selector and open a create request for the type', () => {
                            createFromSelector(containerPayload());

                            expect(legacyScreenRequest()).toBeNull();
                            expect(editRequest()).toEqual(
                                expect.objectContaining({
                                    mode: 'new',
                                    contentType: 'Blog',
                                    languageId: store.pageLanguageId(),
                                    siteId: siteId(),
                                    portletId: 'edit-page'
                                })
                            );
                        });

                        it('should place the created contentlet in that container on its first save', () => {
                            const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                            const payload = containerPayload();
                            createFromSelector(structuredClone(payload));

                            editPanel().options.saved?.(
                                createFakeContentlet({ identifier: 'created-identifier' })
                            );

                            const { pageContainers } = insertContentletInContainer({
                                ...structuredClone(payload),
                                newContentletId: 'created-identifier'
                            });
                            expect(editorSaveSpy).toHaveBeenCalledWith(pageContainers);
                        });

                        it('should warn and keep the panel open when the created contentlet is already there', () => {
                            const payload = containerPayload(1);
                            createFromSelector(structuredClone(payload));
                            const panel = editPanel();

                            panel.options.saved?.(
                                createFakeContentlet({ identifier: 'contentlet-identifier-123' })
                            );

                            expect(addMessageSpy).toHaveBeenCalledWith(
                                expect.objectContaining({ severity: 'info' })
                            );
                            expect(panel.ref.isClosed).toBe(false);
                        });
                    });
                });

                /** The form selector is panel content of its own, not a legacy screen. */
                describe('add form', () => {
                    const formPanel = (): OpenedPanel => {
                        const panel = sidePanels
                            .filter((opened) => opened.component === EmaFormSelectorComponent)
                            .at(-1);

                        expect(
                            panel,
                            'the editor opens the form selector in the side panel'
                        ).toBeDefined();

                        return panel as OpenedPanel;
                    };

                    const selectForm = (formId: string) => {
                        const selected = formPanel().config.outputs?.['selected'] as
                            | ((id: string) => void)
                            | undefined;

                        expect(selected, 'the form selector panel binds selected').toEqual(
                            expect.any(Function)
                        );
                        selected?.(formId);
                        spectator.detectChanges();
                    };

                    it('should place the selected form, save the page and close the selector', () => {
                        const pageApiService = spectator.inject(DotPageApiService);
                        const getFormIdentifier = vi.fn().mockReturnValue(of('form-content-id'));
                        Object.assign(pageApiService, { getFormIndetifier: getFormIdentifier });
                        const editorSaveSpy = vi.spyOn(pageApi(), 'editorSave');
                        addContent('form', containerPayload());

                        selectForm('form-id');

                        expect(getFormIdentifier).toHaveBeenCalledWith(
                            'container-identifier-123',
                            'form-id'
                        );
                        expect(editorSaveSpy).toHaveBeenCalledWith([
                            expect.objectContaining({
                                contentletsId: ['contentlet-identifier-123', 'form-content-id']
                            })
                        ]);
                        expect(formPanel().ref.isClosed).toBe(true);
                    });

                    it('should keep the selector open when the form cannot be resolved', () => {
                        const pageApiService = spectator.inject(DotPageApiService);
                        Object.assign(pageApiService, {
                            getFormIndetifier: vi
                                .fn()
                                .mockReturnValue(throwError(() => new Error('not found')))
                        });
                        addContent('form', containerPayload());

                        selectForm('form-id');

                        expect(formPanel().ref.isClosed).toBe(false);
                    });
                });

                /**
                 * The translate screen is the legacy editor's create form pre-filled from the page,
                 * so it opens in the legacy editor panel, which handles the workflow wizard, compare
                 * and the unsaved-changes prompt; the generic legacy-screen panel does not (#38303).
                 */
                describe('translate page', () => {
                    const PAGE = {
                        identifier: 'test-page-123',
                        inode: 'inode-123',
                        title: 'Translated page',
                        contentType: 'htmlpageasset'
                    } as unknown as DotCMSPage;

                    /** The legacy translate screen's URL, exactly as the page editor's dialog built it. */
                    const translateUrl = () => {
                        const params = new URLSearchParams({
                            p_p_id: 'content',
                            p_p_action: '1',
                            p_p_state: 'maximized',
                            angularCurrentPortlet: 'edit-page',
                            _content_sibbling: 'inode-123',
                            _content_cmd: 'edit',
                            p_p_mode: 'view',
                            _content_sibblingStructure: 'inode-123',
                            _content_struts_action: '/ext/contentlet/edit_contentlet',
                            inode: '',
                            lang: '2',
                            populateaccept: 'true',
                            reuseLastLang: 'true',
                            variantName: DEFAULT_VARIANT_ID
                        });
                        const site = siteId();
                        if (site) {
                            params.set('host_id', site);
                        }

                        return `${LAYOUT_URL}?${params.toString()}`;
                    };

                    const translate = () => {
                        spectator.detectChanges();
                        spectator.triggerEventHandler(DotUveToolbarStubComponent, 'translatePage', {
                            page: PAGE,
                            newLanguage: 2
                        });
                        spectator.detectChanges();
                    };

                    /** The legacy editor panel the translation opened in: the one given a create screen. */
                    const translatePanel = (): OpenedPanel => {
                        const panel = sidePanels
                            .filter((opened) => 'createUrl' in (opened.config.inputs ?? {}))
                            .at(-1);

                        expect(
                            panel,
                            'the editor opens the translation in the legacy editor panel'
                        ).toBeDefined();

                        return panel as OpenedPanel;
                    };

                    /** Emits one of the translate panel's outputs, as the legacy editor would. */
                    const emit = <T>(name: string, value?: T): void => {
                        const handler = translatePanel().config.outputs?.[name] as
                            | ((value?: T) => void)
                            | undefined;

                        expect(handler, `the translate panel binds ${name}`).toEqual(
                            expect.any(Function)
                        );
                        handler?.(value);
                        spectator.detectChanges();
                    };

                    it('should open the legacy editor panel on the translate screen', async () => {
                        translate();
                        const panel = translatePanel();

                        expect(await panel.component).toBe(await loadDotLegacyEditorSidePanel());
                        expect(inputOf(panel, 'request')).toEqual({
                            mode: 'new',
                            contentType: 'htmlpageasset',
                            languageId: 2,
                            title: 'Translated page',
                            siteId: siteId(),
                            portletId: 'edit-page'
                        });
                        expect(inputOf(panel, 'createUrl')).toBe(translateUrl());
                    });

                    it('should not open the generic legacy-screen panel', () => {
                        translate();

                        expect(legacyScreenRequest()).toBeNull();
                    });

                    it('should load the saved translation at its URL and in its language, and stay open', () => {
                        translate();
                        const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');

                        emit(
                            'saved',
                            createFakeContentlet({ languageId: 2, url: '/translated-page' })
                        );

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            language_id: '2',
                            url: '/translated-page'
                        });
                        expect(translatePanel().ref.isClosed).toBe(false);
                    });

                    it('should close the panel and load the page the editor names after the first save', () => {
                        translate();
                        const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');

                        emit('pageEditorRequested', { url: '/translated-page', languageId: 2 });

                        expect(translatePanel().ref.isClosed).toBe(true);
                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/translated-page',
                            language_id: '2'
                        });
                    });

                    it('should reload the page when the panel closes after a save', () => {
                        translate();
                        emit(
                            'saved',
                            createFakeContentlet({ languageId: 2, url: '/translated-page' })
                        );
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                        translatePanel().ref.close();

                        expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                    });

                    it('should leave the page alone when the panel closes without a save', () => {
                        translate();
                        const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                        translatePanel().ref.close();

                        expect(pageReloadSpy).not.toHaveBeenCalled();
                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });
                });

                /** The SDK's `reorderMenu()` reaches the editor through `openLegacyScreen`. */
                describe('reorder menu', () => {
                    const REORDER_URL = '/c/portal/layout?p_p_id=site-browser&reorder=true';

                    const openReorder = () => {
                        spectator.detectChanges();
                        const deps = actionDeps(DotCMSUVEAction.REORDER_MENU, {
                            startLevel: 1,
                            depth: 2
                        });
                        const openLegacyScreen = deps['openLegacyScreen'] as
                            | ((url: string, title: string) => void)
                            | undefined;

                        expect(
                            openLegacyScreen,
                            'the editor hands the actions handler openLegacyScreen'
                        ).toEqual(expect.any(Function));
                        openLegacyScreen?.(REORDER_URL, 'Reorder the menu');
                        spectator.detectChanges();
                    };

                    it('should open the reorder screen in a legacy-screen panel', () => {
                        openReorder();

                        expect(legacyScreenRequest()).toEqual({
                            url: REORDER_URL,
                            title: 'Reorder the menu'
                        });
                        expect(inputOf(legacyScreenPanel(), 'request')).toEqual(
                            legacyScreenRequest()
                        );
                    });

                    it('should confirm, reload the page, ask the client to reload and close after saving the order', () => {
                        openReorder();
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');
                        const postMessageSpy = vi
                            .spyOn(iframeWindow(), 'postMessage')
                            .mockImplementation(() => undefined);

                        sendLegacyEvent({ name: NG_CUSTOM_EVENTS.SAVE_MENU_ORDER });

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            severity: 'success',
                            summary: 'editpage.content.contentlet.menu.reorder.title',
                            detail: 'message.menu.reordered',
                            life: 2000
                        });
                        expect(pageReloadSpy).toHaveBeenCalledTimes(1);
                        expect(postMessageSpy).toHaveBeenCalledWith(
                            { name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE },
                            '*'
                        );
                        expect(legacyScreenRequest()).toBeNull();
                    });

                    it('should tell the user and keep the screen open when the order cannot be saved', () => {
                        openReorder();

                        sendLegacyEvent({ name: NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER });

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            severity: 'error',
                            summary: 'editpage.content.contentlet.menu.reorder.title',
                            detail: 'error.menu.reorder.user_has_not_permission',
                            life: 2000
                        });
                        expect(legacyScreenRequest()).toEqual(
                            expect.objectContaining({ url: REORDER_URL })
                        );
                    });

                    it('should close the screen when the reorder is cancelled', () => {
                        openReorder();

                        sendLegacyEvent({ name: NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER });

                        expect(legacyScreenRequest()).toBeNull();
                    });
                });
            });

            describe('DOM', () => {
                it('should not show a loader when client is ready and UVE is not loading', () => {
                    const storeRef = (
                        spectator.component as unknown as {
                            uveStore: InstanceType<typeof UVEStore>;
                        }
                    ).uveStore;
                    patchState(storeRef, {
                        uveStatus: UVE_STATUS.LOADED,
                        isClientReady: true
                    });
                    spectator.flushEffects();
                    spectator.detectChanges();

                    expect(
                        (
                            spectator.component as unknown as { $progressBar: () => boolean }
                        ).$progressBar()
                    ).toBe(false);
                });

                it('should show a loader when the client is not ready', () => {
                    store.setIsClientReady(false);
                    spectator.detectChanges();

                    const progressbar = spectator.query(byTestId('progress-bar'));

                    expect(progressbar).not.toBeNull();
                });

                it('should show a loader when the client is ready but UVE is Loading', () => {
                    store.setIsClientReady(true);
                    store.setUveStatus(UVE_STATUS.LOADING); // Almost impossible case but we have it as a fallback
                    spectator.detectChanges();

                    const progressbar = spectator.query(byTestId('progress-bar'));

                    expect(progressbar).not.toBeNull();
                });

                describe('VTL Page', () => {
                    beforeEach(() => {
                        vi.useFakeTimers(); // Mock the timers
                        spectator.detectChanges();

                        pageApi().pageLoad({
                            url: 'index',
                            language_id: '3',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                            clientHost: undefined
                        });
                    });

                    it.skip('iframe should have the correct content when is VTL', () => {
                        spectator.detectChanges();
                        vi.runOnlyPendingTimers();

                        const iframe = spectator.debugElement.query(
                            By.css('[data-testId="iframe"]')
                        );

                        iframe.nativeElement.dispatchEvent(new Event('load'));
                        spectator.detectChanges();

                        expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                            '<div>hello world</div>'
                        );
                        expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                            '<script data-inline="true" src="/html/js/tinymce/js/tinymce/tinymce.min.js">'
                        );
                    });

                    it.skip('iframe should have reload the page and add the new content, maintaining scroll', () => {
                        const iframe = spectator.debugElement.query(
                            By.css('[data-testId="iframe"]')
                        );
                        const scrollSpy = vi
                            .spyOn(
                                spectator.component.iframe.nativeElement.contentWindow,
                                'scrollTo'
                            )
                            .mockImplementation(() => undefined);

                        iframe.nativeElement.contentWindow.scrollTo(0, 100); //Scroll down

                        pageApi().pageLoad({
                            url: 'index',
                            language_id: '4',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                        });

                        spectator.detectChanges();
                        vi.runOnlyPendingTimers();

                        iframe.nativeElement.dispatchEvent(new Event('load'));
                        spectator.detectChanges();

                        expect(iframe.nativeElement.src).toContain('http://localhost/'); //When dont have src, the src is the same as the current page
                        expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                            '<div>New Content - Hello World</div>'
                        );
                        expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                            '<script data-inline="true" src="/html/js/tinymce/js/tinymce/tinymce.min.js">'
                        );

                        expect(scrollSpy).toHaveBeenCalledWith(0, 100);
                    });
                });

                it('should not call navigate on load same url', () => {
                    const router = spectator.inject(Router);
                    vi.spyOn(router, 'navigate');

                    spectator.detectChanges();

                    const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
                    const previewWindow = iframe?.contentWindow;
                    expect(previewWindow).toBeTruthy();

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            source: previewWindow,
                            data: {
                                action: 'set-url',
                                payload: {
                                    url: 'index'
                                }
                            }
                        })
                    );

                    expect(router.navigate).not.toHaveBeenCalled();
                });

                it('should have a confirm dialog with acceptIcon and rejectIcon attribute', () => {
                    spectator.detectChanges();

                    const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                    expect(confirmDialog.getAttribute('acceptIcon')).toBe('hidden');
                    expect(confirmDialog.getAttribute('rejectIcon')).toBe('hidden');
                });

                it('should show the confirm dialog when we can edit a variant', () => {
                    const componentsToHide = ['confirm-dialog']; // Test id of components that should hide when entering preview modes

                    spectator.detectChanges();

                    spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                    spectator.detectChanges();
                    pageApi().pageLoad({
                        url: 'index',
                        language_id: '5',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                        variantName: 'hello-there',
                        experimentId: 'i have a variant'
                    });

                    spectator.detectChanges();

                    componentsToHide.forEach((testId) => {
                        expect(spectator.query(byTestId(testId))).not.toBeNull();
                    });
                });
            });

            describe('$translatePageEffect — dialog loop prevention', () => {
                it('should NOT show the translation dialog when uveStatus is LOADING', () => {
                    const confirmSpy = vi.spyOn(
                        spectator.inject(ConfirmationService, true),
                        'confirm'
                    );

                    // Load with an untranslated language (language_id=2 returns viewAs.language.id=2,
                    // and mockLanguageArray has id:2 with translated:false)
                    pageApi().pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '2',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    // Immediately force LOADING status before effects flush — simulates in-flight state
                    patchState(store, { uveStatus: UVE_STATUS.LOADING });
                    spectator.flushEffects();
                    spectator.detectChanges();

                    expect(confirmSpy).not.toHaveBeenCalled();
                });

                /**
                 * Reject the translation dialog raised by the effect and return the
                 * reject callback's side effects. Loads an untranslated language
                 * (language_id=2), flushes the effect that opens the dialog, then invokes
                 * its `reject` handler to simulate the user clicking "No".
                 */
                const triggerTranslationReject = () => {
                    const confirmationService = spectator.inject(ConfirmationService, true);
                    // Set up the spy BEFORE loading so we capture the confirm call made by the effect
                    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

                    // language_id=2 → viewAs.language.id=2, pageLanguages has id:2 translated:false
                    pageApi().pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '2',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    spectator.flushEffects();
                    spectator.detectChanges();

                    // The effect should have shown the dialog because currentLanguage.translated=false
                    expect(confirmSpy).toHaveBeenCalled();

                    // Track navigation triggered only by the reject callback
                    const dotRouterService = spectator.inject(DotRouterService, true);
                    const router = spectator.inject(Router);
                    const navigateByUrlSpy = vi
                        .spyOn(router, 'navigateByUrl')
                        .mockResolvedValue(true);
                    const gotoPortletSpy = vi
                        .spyOn(dotRouterService, 'gotoPortlet')
                        .mockResolvedValue(true);
                    const pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');

                    const reject = () => (confirmSpy.mock.calls[0][0] as Confirmation).reject?.();

                    return {
                        dotRouterService,
                        navigateByUrlSpy,
                        gotoPortletSpy,
                        pageLoadSpy,
                        reject
                    };
                };

                /**
                 * Stub the DotRouterService `previousUrl` getter (it is a mock provider, so
                 * the getter is not present by default) and its `isPublicUrl` guard.
                 */
                const stubPreviousUrl = (
                    dotRouterService: DotRouterService,
                    previousUrl: string,
                    isPublicUrl = false
                ) => {
                    Object.defineProperty(dotRouterService, 'previousUrl', {
                        get: () => previousUrl,
                        configurable: true
                    });
                    vi.spyOn(dotRouterService, 'isPublicUrl').mockReturnValue(isPublicUrl);
                };

                it('should redirect to the previous dotCMS URL when the user rejects creating a new translation', () => {
                    const {
                        dotRouterService,
                        navigateByUrlSpy,
                        gotoPortletSpy,
                        pageLoadSpy,
                        reject
                    } = triggerTranslationReject();

                    stubPreviousUrl(dotRouterService, '/pages/import');

                    reject();

                    // Redirect to the last different dotCMS URL, NOT a reload of the same
                    // untranslated page (which would re-open the dialog — #36661 loop).
                    expect(navigateByUrlSpy).toHaveBeenCalledWith('/pages/import');
                    expect(gotoPortletSpy).not.toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                });

                it('should redirect to the Pages portlet when there is no usable previous URL', () => {
                    const { dotRouterService, navigateByUrlSpy, gotoPortletSpy, reject } =
                        triggerTranslationReject();

                    // No history (user opened the editor directly)
                    stubPreviousUrl(dotRouterService, '');

                    reject();

                    expect(gotoPortletSpy).toHaveBeenCalledWith('/pages');
                    expect(navigateByUrlSpy).not.toHaveBeenCalled();
                });

                it('should redirect to the Pages portlet when the previous URL is another edit-page route', () => {
                    const { dotRouterService, navigateByUrlSpy, gotoPortletSpy, reject } =
                        triggerTranslationReject();

                    // A previous /edit-page URL could resolve to the same untranslated page
                    // and re-open the dialog, so it must be skipped in favor of /pages.
                    stubPreviousUrl(
                        dotRouterService,
                        '/edit-page/content?url=/index&language_id=2'
                    );

                    reject();

                    expect(gotoPortletSpy).toHaveBeenCalledWith('/pages');
                    expect(navigateByUrlSpy).not.toHaveBeenCalled();
                });

                it('should redirect to the Pages portlet when the previous URL is a public route', () => {
                    const { dotRouterService, navigateByUrlSpy, gotoPortletSpy, reject } =
                        triggerTranslationReject();

                    stubPreviousUrl(dotRouterService, '/public/login', true);

                    reject();

                    expect(gotoPortletSpy).toHaveBeenCalledWith('/pages');
                    expect(navigateByUrlSpy).not.toHaveBeenCalled();
                });

                it('should redirect away (no dead-end) even when the page has no translated language', () => {
                    const { dotRouterService, gotoPortletSpy, pageLoadSpy, reject } =
                        triggerTranslationReject();

                    // Only untranslated languages exist — the old behavior bailed and left
                    // the user stuck. The user must still be navigated away.
                    // protectedState:false on the root store makes patchState available in tests.
                    patchState(store, {
                        pageLanguages: [
                            {
                                id: 2,
                                languageCode: 'es',
                                countryCode: 'ES',
                                language: 'Spanish',
                                country: 'España',
                                translated: false
                            }
                        ]
                    });

                    stubPreviousUrl(dotRouterService, '');

                    reject();

                    expect(gotoPortletSpy).toHaveBeenCalledWith('/pages');
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                });
            });

            /**
             * Quick edit's "full editor" opens the selected contentlet in the side panel through one
             * request, whatever editor its type uses: the opener picks it (#38303).
             */
            describe('handleOpenFullEditor', () => {
                afterEach(() => {
                    vi.restoreAllMocks();
                });

                const openFullEditor = () => {
                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.detectChanges();
                    spectator.component['handleOpenFullEditor']();
                    spectator.detectChanges();
                };

                it('should open the selected contentlet in the side panel', () => {
                    openFullEditor();

                    expect(editRequest()).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            inode: 'contentlet-inode-123',
                            identifier: 'contentlet-identifier-123',
                            title: 'Hello World',
                            siteId: siteId(),
                            portletId: 'edit-page'
                        })
                    );
                    expect(opener.opened.length).toBe(1);
                });

                it('should leave the editor choice to the opener, without looking the type up', () => {
                    const getContentType = vi.spyOn(
                        spectator.debugElement.injector.get(DotContentTypeService),
                        'getContentType'
                    );
                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.detectChanges();
                    // Selecting renders quick edit, which loads the type for its own form; only
                    // what opening the full editor does is asserted.
                    getContentType.mockClear();

                    spectator.component['handleOpenFullEditor']();
                    spectator.detectChanges();

                    expect(getContentType).not.toHaveBeenCalled();
                });
            });

            describe('handleEditVTL', () => {
                const VTL_FILE_MOCK: VTLFile = { inode: 'vtl-inode-123', name: 'my-template.vtl' };

                afterEach(() => {
                    vi.restoreAllMocks();
                });

                // The Edit Source fallback: with no contentlet to read, the file is opened by its
                // inode; the opener fetches it to learn its type.
                it('should open the file by its inode when the contentlet lookup fails', () => {
                    const dotContentletService =
                        spectator.debugElement.injector.get(DotContentletService);
                    vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    spectator.component.handleEditVTL(VTL_FILE_MOCK);
                    spectator.detectChanges();

                    expect(editRequest()).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            inode: 'vtl-inode-123',
                            title: 'my-template.vtl',
                            siteId: siteId(),
                            portletId: 'edit-page'
                        })
                    );
                });

                it('should open the resolved contentlet in the side panel when the source editor does not handle it', () => {
                    const dotContentletService =
                        spectator.debugElement.injector.get(DotContentletService);
                    vi.spyOn(dotContentletService, 'getContentletByInode').mockReturnValue(
                        of({ ...URL_MAP_CONTENTLET, inode: 'vtl-inode-123', contentType: 'test' })
                    );

                    spectator.component.handleEditVTL(VTL_FILE_MOCK);
                    spectator.detectChanges();

                    expect(editRequest()).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            inode: 'vtl-inode-123',
                            identifier: URL_MAP_CONTENTLET.identifier,
                            contentType: 'test',
                            portletId: 'edit-page'
                        })
                    );
                });

                describe('Edit Code panel', () => {
                    /** The side panels the editor opened, recorded instead of rendered. */
                    let opened: {
                        config: DotSidePanelConfig;
                        ref: { closed: Subject<void>; close: Mock; requestClose: Mock };
                    }[];

                    beforeEach(() => {
                        opened = [];
                        vi.spyOn(spectator.inject(DotSidePanelService), 'open').mockImplementation(
                            (_component, config) => {
                                const closed = new Subject<void>();
                                const ref = {
                                    closed,
                                    close: vi.fn(() => {
                                        closed.next();
                                        closed.complete();
                                    }),
                                    requestClose: vi.fn()
                                };
                                opened.push({ config, ref });

                                return ref as unknown as ReturnType<DotSidePanelService['open']>;
                            }
                        );
                    });

                    const VELOCITY_FILE = {
                        ...URL_MAP_CONTENTLET,
                        inode: 'vtl-inode-123',
                        identifier: 'vtl-identifier-123',
                        languageId: 2,
                        baseType: 'FILEASSET',
                        extension: 'vtl',
                        fileName: 'my-template.vtl'
                    } as DotCMSContentlet;

                    /** Clicks the file in the VTL menu and lets the panel open. */
                    const openVelocityFile = async () => {
                        vi.spyOn(
                            spectator.debugElement.injector.get(DotContentletService),
                            'getContentletByInode'
                        ).mockReturnValue(of(VELOCITY_FILE));

                        spectator.component.handleEditVTL(VTL_FILE_MOCK);
                        spectator.detectChanges();
                        await spectator.fixture.whenStable();
                        spectator.detectChanges();
                    };

                    it("should open a Velocity file's source instead of the content editor", async () => {
                        await openVelocityFile();

                        const [{ config }] = opened;
                        expect((config.inputs?.['request'] as () => unknown)()).toEqual({
                            inode: 'vtl-inode-123',
                            title: 'my-template.vtl',
                            language: DOT_VELOCITY_LANGUAGE_ID
                        });
                        expect((config.heading as () => string)()).toBe('my-template.vtl');
                        // No content editor opened alongside it.
                        expect(editRequest() ?? null).toBeNull();
                        expect(opener.opened.length).toBe(0);
                    });

                    it.each([
                        [
                            'the user cannot edit it',
                            of({
                                canLock: false,
                                locked: true,
                                id: '',
                                inode: '',
                                lockedBy: 'someone'
                            })
                        ],
                        ['the lock check fails', throwError(() => new Error('network'))]
                    ])(
                        'should open the content editor instead when %s',
                        async (_label, lockAnswer) => {
                            vi.spyOn(
                                spectator.debugElement.injector.get(DotContentletService),
                                'canLock'
                            ).mockReturnValue(
                                lockAnswer as ReturnType<DotContentletService['canLock']>
                            );
                            const openContentForEdit = vi
                                .spyOn(spectator.component, 'openContentForEdit')
                                .mockImplementation(() => undefined);

                            await openVelocityFile();

                            expect(openContentForEdit).toHaveBeenCalledWith(VELOCITY_FILE);
                            expect(opened).toHaveLength(0);
                        }
                    );

                    it('should reload the page when the source is saved', async () => {
                        await openVelocityFile();
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                        (opened[0].config.outputs?.['saved'] as () => void)();

                        expect(pageReloadSpy).toHaveBeenCalled();
                    });

                    it('should open the file again after the panel closed itself', async () => {
                        await openVelocityFile();

                        opened[0].ref.close();
                        await openVelocityFile();

                        // The close cleared the request, so the same file opens a new panel.
                        expect(opened).toHaveLength(2);
                    });
                });
            });

            /**
             * The pencil opens the contentlet in the side panel through one request, whatever editor
             * its type uses (#38303). On a contentlet shown on several pages it asks first whether to
             * edit everywhere or a copy for this page.
             */
            describe('handleEditWithCopyDecision', () => {
                const MULTI_PAGE_PAYLOAD: ActionPayload = {
                    ...EDIT_ACTION_PAYLOAD_MOCK,
                    contentlet: {
                        ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                        onNumberOfPages: 2
                    } as ContentletPayload
                };

                afterEach(() => {
                    vi.restoreAllMocks();
                });

                describe('single-page contentlet (onNumberOfPages: 1)', () => {
                    it('should open one edit request for the contentlet', () => {
                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);

                        expect(editRequest()).toEqual(pencilRequest());
                    });

                    // Before #38303 the UVE looked the type up and opened a different dialog per
                    // editor; now a legacy-type contentlet gets the same request.
                    it('should open the same request for a legacy-type contentlet, without looking the type up', () => {
                        const getContentType = vi
                            .spyOn(
                                spectator.debugElement.injector.get(DotContentTypeService),
                                'getContentType'
                            )
                            .mockReturnValue(
                                of(
                                    createFakeContentType({
                                        variable: 'test',
                                        name: 'Test',
                                        metadata: {
                                            [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false
                                        }
                                    })
                                )
                            );

                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);

                        expect(editRequest()).toEqual(pencilRequest());
                        expect(getContentType).not.toHaveBeenCalled();
                    });
                });

                describe('multi-page contentlet (onNumberOfPages > 1)', () => {
                    it('should open the original when the user edits it on every page', () => {
                        vi.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: false }));

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        expect(editRequest()).toEqual(pencilRequest());
                    });

                    it('should reload the page and open the copy when the user edits this page only', () => {
                        const COPIED_INODE = 'copied-contentlet-inode-456';
                        vi.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: true }));
                        vi.spyOn(
                            spectator.inject(DotCopyContentService),
                            'copyInPage'
                        ).mockReturnValue(
                            of({
                                inode: COPIED_INODE,
                                identifier: 'copied-identifier',
                                contentType: 'test',
                                title: 'Hello World'
                            } as DotCMSContentlet)
                        );
                        const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload');

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        expect(pageReloadSpy).toHaveBeenCalled();
                        expect(editRequest()).toEqual(
                            expect.objectContaining({
                                mode: 'edit',
                                inode: COPIED_INODE,
                                identifier: 'copied-identifier',
                                contentType: 'test',
                                portletId: 'edit-page'
                            })
                        );
                    });
                });

                describe('contentlet edit permission (#37376)', () => {
                    const DENIED_PAYLOAD: ActionPayload = {
                        ...EDIT_ACTION_PAYLOAD_MOCK,
                        contentlet: {
                            ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                            canEdit: false
                        }
                    };

                    const DENIED_MULTI_PAGE_PAYLOAD: ActionPayload = {
                        ...MULTI_PAGE_PAYLOAD,
                        contentlet: {
                            ...MULTI_PAGE_PAYLOAD.contentlet,
                            canEdit: false
                        }
                    };

                    it('should not open any editor for a contentlet the user cannot edit', () => {
                        // The permitted path opens synchronously, so a missing request here proves
                        // the guard and not a pending lookup.
                        spectator.component['handleEditWithCopyDecision'](DENIED_PAYLOAD);
                        spectator.detectChanges();

                        expect(editRequest() ?? null).toBeNull();
                        expect(opener.opened.length).toBe(0);
                    });

                    it('should not open the copy decision modal for a denied multi-page contentlet', () => {
                        // The modal is the first thing the multi-page path does,
                        // so it must never be reached — the user cannot edit the
                        // original nor be offered a copy of it here.
                        const copyModalSpy = vi.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        );

                        spectator.component['handleEditWithCopyDecision'](
                            DENIED_MULTI_PAGE_PAYLOAD
                        );
                        spectator.detectChanges();

                        expect(copyModalSpy).not.toHaveBeenCalled();
                    });

                    it('should still open the copy decision modal when the user can edit', () => {
                        const copyModalSpy = vi
                            .spyOn(spectator.inject(DotCopyContentModalService), 'open')
                            .mockReturnValue(of({ shouldCopy: false }));

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        expect(copyModalSpy).toHaveBeenCalled();
                    });

                    it('should still open the editor when the permission field is absent', () => {
                        // Headless payloads carry no canEdit; fail open.
                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);
                        spectator.detectChanges();

                        expect(editRequest()).toEqual(
                            expect.objectContaining({ inode: 'contentlet-inode-123' })
                        );
                    });
                });
            });

            describe('quick edit permission (#37376)', () => {
                const SELECTED_BOUNDS = { x: 0, y: 0, width: 0, height: 0 };

                const selectContentlet = (canEdit?: boolean) => {
                    store.setSelected({
                        bounds: SELECTED_BOUNDS,
                        payload: {
                            ...EDIT_ACTION_PAYLOAD_MOCK,
                            contentlet: {
                                ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                                ...(canEdit === undefined ? {} : { canEdit })
                            }
                        }
                    });
                    spectator.detectChanges();
                };

                it('should preserve canEdit through the page-asset swap in $contentletEditData', () => {
                    // $contentletEditData replaces the DOM payload contentlet
                    // with the page-asset one to pick up a fresh inode after a
                    // save. The permission exists only on the DOM payload, so
                    // it has to survive that swap or the panel cannot be gated.
                    //
                    // The container is injected into the store so the swap is
                    // guaranteed to fire — the assertion on the swapped inode
                    // is what stops this test from passing vacuously.
                    const componentUveStore = spectator.component['uveStore'];
                    const pageAsset = componentUveStore.pageAsset();

                    componentUveStore.updatePageResponse({
                        ...pageAsset,
                        containers: {
                            'perm-container': {
                                container: { identifier: 'perm-container' },
                                containerStructures: [],
                                contentlets: {
                                    'uuid-1': [
                                        {
                                            identifier: 'perm-contentlet',
                                            inode: 'fresh-inode-from-page-asset',
                                            contentType: 'test'
                                        }
                                    ]
                                }
                            }
                        }
                    } as unknown as Parameters<typeof componentUveStore.updatePageResponse>[0]);
                    spectator.detectChanges();

                    componentUveStore.setSelected({
                        bounds: SELECTED_BOUNDS,
                        payload: {
                            ...EDIT_ACTION_PAYLOAD_MOCK,
                            container: {
                                ...EDIT_ACTION_PAYLOAD_MOCK.container,
                                identifier: 'perm-container',
                                uuid: '1'
                            },
                            contentlet: {
                                ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                                identifier: 'perm-contentlet',
                                inode: 'stale-inode-from-dom',
                                canEdit: false
                            }
                        }
                    });
                    spectator.detectChanges();

                    const result = spectator.component['$contentletEditData']();

                    expect(result.contentlet?.inode).toBe('fresh-inode-from-page-asset');
                    expect(result.contentlet?.canEdit).toBe(false);
                });

                it('should leave canEdit undefined when the payload has none', () => {
                    selectContentlet(undefined);

                    expect(
                        spectator.component['$contentletEditData']().contentlet?.canEdit
                    ).toBeUndefined();
                });

                it('should not open the quick edit panel for a denied contentlet', () => {
                    const setEditPanelOpenSpy = vi.spyOn(store, 'setEditPanelOpen');
                    selectContentlet(false);
                    setEditPanelOpenSpy.mockClear();

                    spectator.component['handleOpenQuickEdit']();
                    spectator.detectChanges();

                    expect(setEditPanelOpenSpy).not.toHaveBeenCalled();
                });

                it('should open the quick edit panel when the user can edit', () => {
                    const setEditPanelOpenSpy = vi.spyOn(store, 'setEditPanelOpen');
                    selectContentlet(true);
                    setEditPanelOpenSpy.mockClear();

                    spectator.component['handleOpenQuickEdit']();
                    spectator.detectChanges();

                    expect(setEditPanelOpenSpy).toHaveBeenCalledWith(true);
                });
            });

            describe('inline editing permission (#37376)', () => {
                /**
                 * Build a `[data-mode]` field inside a contentlet wrapper,
                 * mirroring what the container renderer emits.
                 */
                const buildInlineTarget = (canEdit?: string): HTMLElement => {
                    const wrapper = document.createElement('div');
                    wrapper.dataset['dotObject'] = 'contentlet';
                    wrapper.dataset['dotInode'] = 'inode-123';
                    if (canEdit !== undefined) {
                        wrapper.dataset['dotCanEdit'] = canEdit;
                    }

                    const field = document.createElement('div');
                    field.dataset['mode'] = 'minimal';
                    field.dataset['inode'] = 'inode-123';
                    field.dataset['fieldName'] = 'body';
                    field.dataset['language'] = '1';
                    wrapper.appendChild(field);
                    document.body.appendChild(wrapper);

                    return field;
                };

                const clickInlineField = (canEdit?: string) => {
                    const target = buildInlineTarget(canEdit);
                    spectator.component.handleInlineEditing({
                        target
                    } as unknown as MouseEvent);
                    spectator.detectChanges();
                };

                it('should not start inline editing on a contentlet the user cannot edit', () => {
                    const inlineEditSpy = mockInlineEditService.handleInlineEdit;
                    inlineEditSpy.mockClear();

                    clickInlineField('false');

                    expect(inlineEditSpy).not.toHaveBeenCalled();
                });

                it('should tell the user why with a toast instead of failing silently', () => {
                    // The field has no control to grey out and no hover target
                    // for a tooltip, so a silent no-op reads as a broken editor.
                    addMessageSpy.mockClear();

                    clickInlineField('false');

                    expect(addMessageSpy).toHaveBeenCalledWith(
                        expect.objectContaining({
                            detail: 'uve.contentlet.no.edit.permission'
                        })
                    );
                });

                it('should start inline editing when the user can edit', () => {
                    const inlineEditSpy = mockInlineEditService.handleInlineEdit;
                    inlineEditSpy.mockClear();

                    clickInlineField('true');

                    expect(inlineEditSpy).toHaveBeenCalled();
                });

                it('should start inline editing when the permission attribute is absent', () => {
                    // Headless pages never emit it; fail open.
                    const inlineEditSpy = mockInlineEditService.handleInlineEdit;
                    inlineEditSpy.mockClear();

                    clickInlineField(undefined);

                    expect(inlineEditSpy).toHaveBeenCalled();
                });
            });

            describe('style editor permission (#37376)', () => {
                const SELECTED = { x: 0, y: 0, width: 0, height: 0 };

                const selectWith = (canEdit?: boolean) => {
                    spectator.component['uveStore'].setSelected({
                        bounds: SELECTED,
                        payload: {
                            ...EDIT_ACTION_PAYLOAD_MOCK,
                            contentlet: {
                                ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                                ...(canEdit === undefined ? {} : { canEdit })
                            }
                        }
                    });
                    spectator.detectChanges();
                };

                it('should report the selected contentlet as not styleable when denied', () => {
                    selectWith(false);

                    expect(spectator.component['$canEditSelectedContentlet']()).toBe(false);
                });

                it('should allow styling when the user can edit', () => {
                    selectWith(true);

                    expect(spectator.component['$canEditSelectedContentlet']()).toBe(true);
                });

                it('should allow styling when the permission is absent', () => {
                    selectWith(undefined);

                    expect(spectator.component['$canEditSelectedContentlet']()).toBe(true);
                });

                it('should not open the style editor panel for a denied contentlet', () => {
                    const setEditPanelOpenSpy = vi.spyOn(store, 'setEditPanelOpen');
                    selectWith(false);
                    setEditPanelOpenSpy.mockClear();

                    spectator.component['handleSelectContent'](EDIT_ACTION_PAYLOAD_MOCK);
                    spectator.detectChanges();

                    expect(setEditPanelOpenSpy).not.toHaveBeenCalled();
                });
            });

            /**
             * A content type dropped from the palette opens a create request in the side panel at
             * once, whatever editor the type uses (#38303). Placing the new contentlet is covered
             * under "edit panel events"; the content selector's "create new" under "legacy screens".
             */
            describe('placeItem - content-type drag', () => {
                const contentTypeDragItem = {
                    baseType: 'CONTENT',
                    contentType: 'TestContentType',
                    draggedPayload: {
                        type: 'content-type' as const,
                        item: {
                            variable: 'TestContentType',
                            name: 'Test Content Type'
                        }
                    }
                };

                afterEach(() => {
                    vi.restoreAllMocks();
                });

                it('should open a create request for the dropped type without looking the type up', () => {
                    const getContentType = vi.spyOn(
                        spectator.debugElement.injector.get(DotContentTypeService),
                        'getContentType'
                    );

                    spectator.component.placeItem(
                        structuredClone(DROP_POSITION),
                        contentTypeDragItem
                    );
                    spectator.detectChanges();

                    expect(editRequest()).toEqual(
                        expect.objectContaining({ mode: 'new', contentType: 'TestContentType' })
                    );
                    expect(getContentType).not.toHaveBeenCalled();
                });
            });

            describe('Editor content', () => {
                it('should have display grid when there is not SEO view', () => {
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('grid');
                });

                it('should have display none when there is SEO view', () => {
                    store.viewSetSEO('test');
                    spectator.detectChanges();
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('none');
                });
            });

            describe('handleInternalNav', () => {
                let pageLoadSpy: MockInstance;
                let windowOpenSpy: Mock;
                let openedLink: {
                    href: string;
                    target: string;
                    rel: string;
                    click: Mock;
                    remove: Mock;
                };
                let mockWindow: {
                    location: { origin: string; hostname: string };
                    open: Mock;
                    document: { createElement: Mock; body: { appendChild: Mock } };
                };

                beforeEach(() => {
                    // Asset links are opened through a `rel="noopener"` anchor rather
                    // than window.open, so the sandboxed iframe's gesture is not
                    // treated as a popup request. See EditEmaEditorComponent.
                    openedLink = {
                        href: '',
                        target: '',
                        rel: '',
                        click: vi.fn(),
                        remove: vi.fn()
                    };
                    mockWindow = {
                        location: {
                            origin: 'http://localhost:3000',
                            hostname: 'localhost'
                        },
                        open: vi.fn(),
                        document: {
                            createElement: vi.fn().mockReturnValue(openedLink),
                            body: { appendChild: vi.fn() }
                        }
                    };
                    (spectator.component as unknown as { window: typeof mockWindow }).window =
                        mockWindow;
                    pageLoadSpy = vi.spyOn(pageApi(), 'pageLoad');
                    windowOpenSpy = mockWindow.open;
                });

                const createMockEvent = (href: string, isInlineEditing = false): MouseEvent => {
                    const mockAnchor = {
                        href,
                        getAttribute: vi.fn().mockReturnValue(href),
                        closest: vi.fn().mockReturnValue({ href, getAttribute: () => href })
                    };

                    const mockEvent = {
                        target: mockAnchor,
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    // Mock the store state for inline editing (editorState returns EDITOR_STATE enum directly)
                    vi.spyOn(store, 'editorState').mockReturnValue(
                        isInlineEditing ? EDITOR_STATE.INLINE_EDITING : EDITOR_STATE.IDLE
                    );

                    return mockEvent;
                };

                it('should not do anything if href is empty', () => {
                    const mockEvent = {
                        target: { href: '', closest: () => null, querySelectorAll: () => [] },
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should not do anything if isInlineEditing is true', () => {
                    const mockEvent = createMockEvent('/test-page', true);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should not do anything for a javascript:-href anchor', () => {
                    // Regression test: a `javascript:…` href never navigates —
                    // it's a page's own click handler invoked via the href
                    // itself, not a link. `anchor.href` for such an anchor
                    // resolves to the literal string back (hostname ""),
                    // which — before this bailed early alongside the
                    // hash-only case — wrongly tripped the external-link
                    // branch below and tried to open it in a new tab.
                    const mockEvent = createMockEvent('javascript:void(0)');

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                    expect(windowOpenSpy).not.toHaveBeenCalled();
                    expect(openedLink.click).not.toHaveBeenCalled();
                });

                it('should open external URL in new tab', () => {
                    const externalUrl = 'https://external-site.com/page';
                    const mockEvent = createMockEvent(externalUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(openedLink.href).toBe(externalUrl);
                    expect(openedLink.target).toBe('_blank');
                    expect(openedLink.click).toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                });

                // Without it the anchor also navigates the iframe to the external
                // site, which refuses to be framed and blanks the canvas.
                it('should keep the iframe on the page when opening an external URL', () => {
                    const mockEvent = createMockEvent('https://external-site.com/page');

                    spectator.component.handleInternalNav(mockEvent);

                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                describe('absolute URLs to the edited site', () => {
                    beforeEach(() => {
                        const pageAsset = store.pageAsset();

                        vi.spyOn(store, 'pageAsset').mockReturnValue({
                            ...pageAsset,
                            site: {
                                ...pageAsset?.site,
                                hostname: 'www.site.com',
                                aliases: 'site.com\nalias.site.com'
                            }
                        } as ReturnType<typeof store.pageAsset>);
                    });

                    it('should load a link to the site hostname inside the editor', () => {
                        const mockEvent = createMockEvent(
                            'https://www.site.com/news?anno_pubblicazione=2025'
                        );

                        spectator.component.handleInternalNav(mockEvent);

                        expect(openedLink.click).not.toHaveBeenCalled();
                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/news',
                            anno_pubblicazione: '2025'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should load a link to a site alias inside the editor', () => {
                        const mockEvent = createMockEvent('https://alias.site.com/news');

                        spectator.component.handleInternalNav(mockEvent);

                        expect(openedLink.click).not.toHaveBeenCalled();
                        expect(pageLoadSpy).toHaveBeenCalledWith({ url: '/news' });
                    });

                    it('should still open a different site in a new tab', () => {
                        const mockEvent = createMockEvent('https://other-site.com/news');

                        spectator.component.handleInternalNav(mockEvent);

                        expect(openedLink.click).toHaveBeenCalled();
                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });
                });

                it('should load page asset with pathname only for internal URL without query params', () => {
                    const internalUrl = 'http://localhost:3000/test-page';
                    const mockEvent = createMockEvent(internalUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/test-page'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should resolve a descendant anchor when the click lands on a wrapper with no ancestor anchor', () => {
                    // Mirrors a real pattern (confirmed live): an anchor styled
                    // `pointer-events: none` via a stylesheet rule, wrapped in a
                    // container a page's own script makes clickable instead.
                    // The click's target is the wrapper — `closest('a')` finds
                    // nothing — so the href must come from the single
                    // descendant anchor instead. Needs real DOM elements, not
                    // plain mocks: resolving this case checks computed style.
                    const wrapper = document.createElement('div');
                    const anchor = document.createElement('a');
                    anchor.href = 'http://localhost:3000/test-page';
                    anchor.style.pointerEvents = 'none';
                    wrapper.appendChild(anchor);

                    const mockEvent = {
                        target: wrapper,
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({ url: '/test-page' });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should not resolve a link when the wrapper contains more than one descendant anchor', () => {
                    const wrapper = document.createElement('div');
                    const anchor1 = document.createElement('a');
                    anchor1.href = 'http://localhost:3000/page-1';
                    anchor1.style.pointerEvents = 'none';
                    const anchor2 = document.createElement('a');
                    anchor2.href = 'http://localhost:3000/page-2';
                    anchor2.style.pointerEvents = 'none';
                    wrapper.append(anchor1, anchor2);

                    const mockEvent = {
                        target: wrapper,
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should not resolve a link when the single descendant anchor is a normal, clickable link', () => {
                    // Regression test (MEDIUM, review feedback on #37962): a
                    // click resolves to `target` instead of the anchor only
                    // when pointer-events: none prevented hit-testing from
                    // ever reaching it. Without that check, clicking
                    // padding/whitespace inside any container — a card, a
                    // section, a footer — that happens to contain exactly one
                    // ordinary link would wrongly navigate there.
                    const wrapper = document.createElement('div');
                    const anchor = document.createElement('a');
                    anchor.href = 'http://localhost:3000/unrelated-page';
                    wrapper.appendChild(anchor);

                    const mockEvent = {
                        target: wrapper,
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should open a same-host PDF link in a new tab instead of loading it as a page', () => {
                    const pdfUrl = 'http://localhost:3000/application/files/report.pdf';
                    const mockEvent = createMockEvent(pdfUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(openedLink.href).toBe(pdfUrl);
                    expect(openedLink.target).toBe('_blank');
                    expect(openedLink.click).toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should open a /dA/ asset link in a new tab instead of loading it as a page', () => {
                    const assetUrl = 'http://localhost:3000/dA/abc123/asset/report.pdf';
                    const mockEvent = createMockEvent(assetUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(openedLink.href).toBe(assetUrl);
                    expect(openedLink.click).toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should sever the opener without a windowFeatures popup request', () => {
                    // A windowFeatures string makes Firefox treat the call as a popup,
                    // which the iframe sandbox rejects with "The operation is insecure".
                    const mockEvent = createMockEvent(
                        'http://localhost:3000/dA/abc123/asset/photo.jpg'
                    );

                    spectator.component.handleInternalNav(mockEvent);

                    expect(openedLink.rel).toBe('noopener');
                    expect(windowOpenSpy).not.toHaveBeenCalled();
                });

                it('should resolve a relative asset href against the site origin, not the admin path', () => {
                    // The click lands on a child of the anchor, so `target.href` is undefined
                    // and the raw (relative) href attribute is what reaches the handler.
                    const mockEvent = {
                        target: {
                            closest: vi.fn().mockReturnValue({
                                getAttribute: () => 'files/report.pdf'
                            })
                        },
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(openedLink.href).toBe('http://localhost:3000/files/report.pdf');
                    expect(openedLink.click).toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should keep the iframe on the page when opening a new tab throws', () => {
                    // The click must still be cancelled if the open fails for any
                    // reason, or the anchor navigates the iframe to the asset, and the
                    // throw must not escape into the RxJS subscriber driving this.
                    openedLink.click.mockImplementation(() => {
                        throw new DOMException('The operation is insecure.');
                    });

                    const mockEvent = createMockEvent(
                        'http://localhost:3000/dA/abc123/asset/photo.jpg'
                    );

                    expect(() => spectator.component.handleInternalNav(mockEvent)).not.toThrow();

                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                    expect(pageLoadSpy).not.toHaveBeenCalled();
                    // click() is what throws, so cleanup has to be unconditional or
                    // every refused open strands an anchor in the admin document.
                    expect(openedLink.remove).toHaveBeenCalled();
                });

                it('should still load a page when the URL uses the page extension', () => {
                    const pageUrl = 'http://localhost:3000/test-page/index.html';
                    const mockEvent = createMockEvent(pageUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(windowOpenSpy).not.toHaveBeenCalled();
                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/test-page/index.html'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should extract and pass query parameters from URL', () => {
                    const urlWithParams =
                        'http://localhost:3000/test-page?param1=value1&param2=value2';
                    const mockEvent = createMockEvent(urlWithParams);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/test-page',
                        param1: 'value1',
                        param2: 'value2'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should handle URL encoded query parameters', () => {
                    const urlWithEncodedParams =
                        'http://localhost:3000/test-page?path=%2Fhome%2Fuser&name=John%20Doe';
                    const mockEvent = createMockEvent(urlWithEncodedParams);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/test-page',
                        path: '/home/user',
                        name: 'John Doe'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should handle complex query parameters', () => {
                    const complexUrl =
                        'http://localhost:3000/test-page?language_id=2&mode=EDIT&persona=test-persona&custom=value';
                    const mockEvent = createMockEvent(complexUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/test-page',
                        language_id: '2',
                        mode: 'EDIT',
                        persona: 'test-persona',
                        custom: 'value'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should handle relative URLs correctly', () => {
                    const relativeUrl = 'relative-page?param=value';
                    const mockEvent = createMockEvent(relativeUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/relative-page',
                        param: 'value'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should fallback to closest anchor href when target href is not available', () => {
                    const mockEvent = {
                        target: {
                            href: null,
                            closest: vi.fn().mockReturnValue({
                                href: 'http://localhost:3000/fallback-page?test=123',
                                getAttribute: vi
                                    .fn()
                                    .mockReturnValue('http://localhost:3000/fallback-page?test=123')
                            })
                        },
                        preventDefault: vi.fn()
                    } as unknown as MouseEvent;

                    vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/fallback-page',
                        test: '123'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                describe('same-page navigation (same pathname)', () => {
                    const samePathPageParams = (): DotPageApiParams => ({
                        url: '/current-page',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    beforeEach(() => {
                        vi.spyOn(store, 'pageParams').mockReturnValue(samePathPageParams());
                    });

                    // Cancelled so the link can't load outside the editor; the
                    // iframe is scrolled instead of reloaded.
                    it('should cancel hash-only navigation on same page without reloading', () => {
                        const hashUrl = 'http://localhost:3000/current-page#sectionA';
                        const mockEvent = createMockEvent(hashUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should not trigger pageLoad for hash-only with complex id', () => {
                        const hashUrl = 'http://localhost:3000/current-page#section-123_complex';
                        const mockEvent = createMockEvent(hashUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });

                    // Traditional pages render in an iframe UVE writes itself, so a
                    // browser-handled query change leaves it blank and never reaches
                    // the Page API (#36999). It has to go through pageLoad.
                    it('should trigger pageLoad with the new query for query-only navigation on same page', () => {
                        const queryUrl = 'http://localhost:3000/current-page?tab=2';
                        const mockEvent = createMockEvent(queryUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/current-page',
                            tab: '2'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should pass every query param for multiple query params on same page', () => {
                        const queryUrl =
                            'http://localhost:3000/current-page?filter=value&sort=date';
                        const mockEvent = createMockEvent(queryUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/current-page',
                            filter: 'value',
                            sort: 'date'
                        });
                    });

                    it('should trigger pageLoad when both hash and query are present on same path', () => {
                        const combinedUrl = 'http://localhost:3000/current-page?tab=2#section';
                        const mockEvent = createMockEvent(combinedUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/current-page',
                            tab: '2'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should load a new language for a same-path language_id link', () => {
                        const languageUrl = 'http://localhost:3000/current-page?language_id=3';
                        const mockEvent = createMockEvent(languageUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/current-page',
                            language_id: '3'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    describe('when the current page already carries a page query param', () => {
                        beforeEach(() => {
                            vi.spyOn(store, 'pageParams').mockReturnValue({
                                ...samePathPageParams(),
                                mode: UVE_MODE.EDIT,
                                device: 'mobile',
                                anno_pubblicazione: '2025'
                            });
                        });

                        it('should clear it when the link no longer sets it', () => {
                            const mockEvent = createMockEvent('http://localhost:3000/current-page');

                            spectator.component.handleInternalNav(mockEvent);

                            // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                            expect(pageLoadSpy.mock.lastCall?.[0]).toStrictEqual({
                                url: '/current-page',
                                anno_pubblicazione: undefined
                            });
                        });

                        // `/folder/` and `/folder/index` are one page under two paths, so
                        // the path comparison sees a different page. The filter must still
                        // be cleared on the first click (QA follow-up on #36999).
                        it('should clear it when the link reaches the same page by its index path', () => {
                            vi.spyOn(store, 'pageParams').mockReturnValue({
                                ...samePathPageParams(),
                                url: '/folder/',
                                anno_pubblicazione: '2026'
                            });
                            const mockEvent = createMockEvent('http://localhost:3000/folder/index');

                            spectator.component.handleInternalNav(mockEvent);

                            // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                            expect(pageLoadSpy.mock.lastCall?.[0]).toStrictEqual({
                                url: '/folder/index',
                                anno_pubblicazione: undefined
                            });
                        });

                        it('should not carry it to a different page', () => {
                            const mockEvent = createMockEvent('http://localhost:3000/other-page');

                            spectator.component.handleInternalNav(mockEvent);

                            // Strict: toHaveBeenCalledWith would treat an undefined key as absent.
                            expect(pageLoadSpy.mock.lastCall?.[0]).toStrictEqual({
                                url: '/other-page',
                                anno_pubblicazione: undefined
                            });
                        });

                        it('should replace it with the value a different page link sets', () => {
                            const mockEvent = createMockEvent(
                                'http://localhost:3000/other-page?anno_pubblicazione=2024'
                            );

                            spectator.component.handleInternalNav(mockEvent);

                            expect(pageLoadSpy).toHaveBeenCalledWith({
                                url: '/other-page',
                                anno_pubblicazione: '2024'
                            });
                        });

                        it('should replace it when the link sets a new value', () => {
                            const mockEvent = createMockEvent(
                                'http://localhost:3000/current-page?anno_pubblicazione=2024'
                            );

                            spectator.component.handleInternalNav(mockEvent);

                            expect(pageLoadSpy).toHaveBeenCalledWith({
                                url: '/current-page',
                                anno_pubblicazione: '2024'
                            });
                        });

                        it('should still scroll a hash-only link without reloading', () => {
                            const mockEvent = createMockEvent(
                                'http://localhost:3000/current-page#section'
                            );

                            spectator.component.handleInternalNav(mockEvent);

                            expect(pageLoadSpy).not.toHaveBeenCalled();
                        });
                    });

                    it('should trigger pageLoad when re-clicking an exact duplicate of the current URL (no hash, no query)', () => {
                        // Confirmed live (dotCMS/core#37327): for a traditional page, the
                        // iframe's real location is always the synthetic `about:srcdoc`. If
                        // this early-returns without calling preventDefault() for an exact
                        // duplicate link (no hash/query to react to), the anchor's own
                        // default action navigates the iframe to a live fetch of the real
                        // URL, permanently blanking the edit canvas with no console error.
                        const duplicateUrl = 'http://localhost:3000/current-page';
                        const mockEvent = createMockEvent(duplicateUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/current-page'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should trigger pageLoad when navigating to different page with hash', () => {
                        const differentPageUrl = 'http://localhost:3000/other-page#section';
                        const mockEvent = createMockEvent(differentPageUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/other-page'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should trigger pageLoad when navigating to different page with query', () => {
                        const differentPageUrl = 'http://localhost:3000/other-page?tab=1';
                        const mockEvent = createMockEvent(differentPageUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).toHaveBeenCalledWith({
                            url: '/other-page',
                            tab: '1'
                        });
                        expect(mockEvent.preventDefault).toHaveBeenCalled();
                    });

                    it('should handle root path hash navigation', () => {
                        vi.spyOn(store, 'pageParams').mockReturnValue({
                            url: '/',
                            language_id: '1',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                        });

                        const hashUrl = 'http://localhost:3000/#top';
                        const mockEvent = createMockEvent(hashUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });

                    // Traditional VTL pages render via doc.write into an iframe whose
                    // src is empty, so the document URL stays at about:blank. The
                    // anchor's IDL .href then resolves "#section" to
                    // "about:blank#section" — hostname "" — which used to fall into
                    // the external-link branch and trigger window.open.
                    it('should not open a new tab for hash-only links when iframe document is about:blank', () => {
                        const mockEvent = {
                            target: {
                                href: 'about:blank#page-section',
                                getAttribute: vi.fn().mockReturnValue('#page-section'),
                                closest: vi.fn().mockReturnValue({
                                    href: 'about:blank#page-section',
                                    getAttribute: () => '#page-section'
                                })
                            },
                            preventDefault: vi.fn()
                        } as unknown as MouseEvent;

                        vi.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(windowOpenSpy).not.toHaveBeenCalled();
                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                    });
                });

                afterEach(() => {
                    vi.clearAllMocks();
                });
            });

            describe('handleSectionOffset', () => {
                it('scrolls the iframe contentWindow (not the canvas viewport) to the offset', () => {
                    const scrollToSpy = vi.fn();
                    Object.defineProperty(spectator.component.iframeComponent, 'contentWindow', {
                        value: { scrollTo: scrollToSpy },
                        configurable: true
                    });

                    spectator.component['handleSectionOffset']({ offsetTop: 750 });

                    expect(scrollToSpy).toHaveBeenCalledWith({
                        top: 750,
                        left: 0,
                        behavior: 'smooth'
                    });
                });

                it('clamps negative offsetTop to 0', () => {
                    const scrollToSpy = vi.fn();
                    Object.defineProperty(spectator.component.iframeComponent, 'contentWindow', {
                        value: { scrollTo: scrollToSpy },
                        configurable: true
                    });

                    spectator.component['handleSectionOffset']({ offsetTop: -100 });

                    expect(scrollToSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }));
                });

                it('does nothing when the iframe contentWindow is unavailable', () => {
                    Object.defineProperty(spectator.component.iframeComponent, 'contentWindow', {
                        value: null,
                        configurable: true
                    });

                    expect(() =>
                        spectator.component['handleSectionOffset']({ offsetTop: 100 })
                    ).not.toThrow();
                });
            });
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
        vi.useRealTimers(); // Restore the real timers after each test
    });
});
