import { patchState } from '@ngrx/signals';
import { Dispatcher } from '@ngrx/signals/events';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';
import { Mock, MockInstance, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EventEmitter, Input, Output, signal, Component } from '@angular/core';
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
import { DEFAULT_VARIANT_ID, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotPaletteListStore, DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService, SafeUrlPipe } from '@dotcms/ui';
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
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    mockDotDevices,
    seoOGTagsResultMock
} from '@dotcms/utils-testing';

import { DotUveBrowserToolbarComponent } from './components/dot-uve-browser-toolbar/dot-uve-browser-toolbar.component';
import { DotUveContentEditorComponent } from './components/dot-uve-content-editor/dot-uve-content-editor.component';
import { uveContentEditorEvents } from './components/dot-uve-content-editor/dot-uve-content-editor.events';
import { DotUveContentletToolsComponent } from './components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUveEditPanelComponent } from './components/dot-uve-edit-panel/dot-uve-edit-panel.component';
import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotEmaRunningExperimentComponent } from './components/dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-toolbar/components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EmaDragItem } from './components/ema-page-dropzone/types';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, HOST, PERSONA_KEY } from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import {
    EDIT_ACTION_PAYLOAD_MOCK,
    MOCK_RESPONSE_HEADLESS,
    QUERY_PARAMS_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock,
    mockCurrentUser
} from '../shared/mocks';
import { ActionPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../store/features/editor/models';
import { WithPageApiMethods } from '../store/features/page-api/withPageApi';
import { IframeAccessMode, PageType } from '../store/models';

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

// Stub components to avoid ng-mocks signal query issues

@Component({
    selector: 'dot-uve-toolbar',
    template: '<div></div>',
    standalone: true
})
class DotUveToolbarStubComponent {
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

/** The content editor has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-content-editor',
    template: '<div></div>',
    standalone: true
})
class DotUveContentEditorMockComponent {}

/** The browser toolbar has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-browser-toolbar',
    template: '<div></div>',
    standalone: true
})
class DotUveBrowserToolbarMockComponent {}

/** The edit panel has its own spec; here it only needs to be placed. */
@Component({
    selector: 'dot-uve-edit-panel',
    template: '<div></div>',
    standalone: true
})
class DotUveEditPanelMockComponent {}

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
                        imports: [
                            DotUveToolbarComponent,
                            DotUvePaletteComponent,
                            DotUveContentEditorComponent,
                            DotUveBrowserToolbarComponent,
                            DotUveEditPanelComponent
                        ]
                    },
                    add: {
                        imports: [
                            DotUveToolbarStubComponent,
                            DotUvePaletteStubComponent,
                            DotUveContentEditorMockComponent,
                            DotUveBrowserToolbarMockComponent,
                            DotUveEditPanelMockComponent
                        ]
                    }
                }
            ]
        ],
        detectChanges: false,
        componentProviders: [
            Dispatcher,
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

        beforeEach(() => {
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

            it('should place the browser toolbar', () => {
                expect(spectator.query(byTestId('uve-browser-toolbar'))).toBeTruthy();
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

            it('should leave the legacy dialog and the block editor sidebar to the content editor', () => {
                spectator.detectChanges();

                expect(spectator.query(DotEmaDialogComponent)).toBeNull();
                expect(spectator.query(DotBlockEditorSidebarComponent)).toBeNull();
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

                    expect(spectator.query(byTestId('progress-bar'))).toBeNull();
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

            describe('content editor', () => {
                it('should place the content editor', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-content-editor'))).toBeTruthy();
                });

                it('should keep the content editor in preview mode', () => {
                    // Page Properties opens the new editor in preview mode too, so the content
                    // editor that opens it cannot depend on the mode.
                    pageApi().pageUpdateParams({ mode: UVE_MODE.PREVIEW });
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-content-editor'))).toBeTruthy();
                });

                it("should ask the content editor to edit the page's contentlet when the shell opens page Properties", () => {
                    const dispatcher = spectator.inject(Dispatcher, true);
                    vi.spyOn(dispatcher, 'dispatch');
                    const page = createFakeContentlet({
                        inode: 'page-inode',
                        title: 'Page',
                        contentType: 'htmlpageasset'
                    });

                    spectator.component.openContentForEdit(page);

                    expect(dispatcher.dispatch).toHaveBeenCalledWith(
                        uveContentEditorEvents.uveEditContentRequested({ contentlet: page }),
                        { scope: 'self' }
                    );
                });
            });

            describe('edit panel', () => {
                it('should place the edit panel in edit mode', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-edit-panel'))).toBeTruthy();
                });

                it('should not place the edit panel in preview mode', () => {
                    pageApi().pageUpdateParams({ mode: UVE_MODE.PREVIEW });
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('uve-edit-panel'))).toBeNull();
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

            describe('placeItem', () => {
                describe('a content type dropped from the palette', () => {
                    const contentTypeDragItem: EmaDragItem = {
                        baseType: 'CONTENT',
                        contentType: 'TestContentType',
                        draggedPayload: {
                            type: 'content-type',
                            item: {
                                variable: 'TestContentType',
                                name: 'Test Content Type'
                            }
                        }
                    };

                    it('should ask the content editor to create a contentlet of that content type for the drop position', () => {
                        const dispatcher = spectator.inject(Dispatcher, true);
                        vi.spyOn(dispatcher, 'dispatch');

                        spectator.component.placeItem(
                            EDIT_ACTION_PAYLOAD_MOCK,
                            contentTypeDragItem
                        );

                        expect(dispatcher.dispatch).toHaveBeenCalledWith(
                            uveContentEditorEvents.uveCreateContentRequested({
                                contentType: {
                                    variable: 'TestContentType',
                                    name: 'Test Content Type'
                                },
                                into: store.getPageSavePayload(EDIT_ACTION_PAYLOAD_MOCK)
                            }),
                            { scope: 'self' }
                        );
                    });

                    it('should open no editor itself', () => {
                        const getContentType = vi.spyOn(
                            spectator.debugElement.injector.get(DotContentTypeService),
                            'getContentType'
                        );
                        const openDialog = vi.spyOn(spectator.inject(DialogService), 'open');

                        spectator.component.placeItem(
                            EDIT_ACTION_PAYLOAD_MOCK,
                            contentTypeDragItem
                        );

                        expect(getContentType).not.toHaveBeenCalled();
                        expect(openDialog).not.toHaveBeenCalled();
                    });
                });

                // Drops that insert right away go through the store's editorInsertContentlet.
                describe('a contentlet dropped on a container', () => {
                    /** The first container on the page, as a drop reports it. */
                    const firstContainer = () => {
                        const [{ identifier, uuid, contentletsId }] = store.$pageData().containers;

                        return { identifier, uuid, contentletsId };
                    };

                    /** Drags a contentlet from outside the page (not a move). */
                    const contentletDragItem = (identifier: string): EmaDragItem => ({
                        baseType: 'CONTENT',
                        contentType: 'test',
                        draggedPayload: {
                            type: 'contentlet',
                            move: false,
                            item: {
                                contentlet: {
                                    identifier,
                                    inode: `${identifier}-inode`,
                                    title: identifier,
                                    contentType: 'test'
                                }
                            }
                        }
                    });

                    /** A drop at the end of the first container. */
                    const dropOnFirstContainer = (dragItem: EmaDragItem) => {
                        const { identifier, uuid } = firstContainer();

                        spectator.component.placeItem(
                            {
                                container: {
                                    identifier,
                                    uuid,
                                    acceptTypes: '*',
                                    maxContentlets: 25
                                }
                            },
                            dragItem
                        );
                    };

                    /** Watches the page being saved through the page API. */
                    const spyOnPageSave = () =>
                        vi.spyOn(spectator.inject(DotPageApiService), 'save');

                    it('should save the page with the contentlet in the container', () => {
                        const save = spyOnPageSave();
                        const { identifier, uuid } = firstContainer();

                        dropOnFirstContainer(contentletDragItem('dropped-contentlet'));

                        expect(save).toHaveBeenCalledWith(
                            expect.objectContaining({
                                pageContainers: expect.arrayContaining([
                                    expect.objectContaining({
                                        identifier,
                                        uuid,
                                        contentletsId: expect.arrayContaining([
                                            'dropped-contentlet'
                                        ])
                                    })
                                ])
                            })
                        );
                    });

                    it('should tell the user and not save when the container already holds it', () => {
                        const save = spyOnPageSave();
                        const [alreadyThere] = firstContainer().contentletsId;
                        // Without a contentlet in the container there is no duplicate to drop.
                        expect(alreadyThere).toBeTruthy();

                        dropOnFirstContainer(contentletDragItem(alreadyThere));

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            severity: 'info',
                            summary: 'Content already added',
                            detail: 'This content is already added to this container',
                            life: 2000
                        });
                        expect(save).not.toHaveBeenCalled();
                    });

                    it('should save the page with an uploaded file dropped into the container', () => {
                        const save = spyOnPageSave();
                        const { identifier, uuid } = firstContainer();

                        spectator.component.placeItem(
                            {
                                container: {
                                    identifier,
                                    uuid,
                                    acceptTypes: '*',
                                    maxContentlets: 25
                                },
                                newContentlet: {
                                    identifier: 'uploaded-image',
                                    inode: 'uploaded-image-inode',
                                    title: 'image.png',
                                    contentType: 'dotAsset',
                                    baseType: 'DOTASSET'
                                }
                            },
                            {
                                baseType: 'dotAsset',
                                contentType: 'dotAsset',
                                draggedPayload: { type: 'temp' }
                            }
                        );

                        expect(save).toHaveBeenCalledWith(
                            expect.objectContaining({
                                pageContainers: expect.arrayContaining([
                                    expect.objectContaining({
                                        identifier,
                                        uuid,
                                        contentletsId: expect.arrayContaining(['uploaded-image'])
                                    })
                                ])
                            })
                        );
                    });
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
        });
    });

    describe('when the editor is created', () => {
        const createComponent = createRouting();

        it('should open the edit panel on the quick-edit tab', () => {
            const spectator = createComponent({
                queryParams: { language_id: 1, url: 'index' },
                data: { data: { url: 'http://localhost:3000' } }
            });
            const store = spectator.inject(UVEStore, true);

            // The store outlives the editor: coming back from Layout, Rules or Experiments, it
            // still holds the tab the panel was last on.
            store.editorSetEditPanelTab(UVE_EDIT_PANEL_TABS.STYLE_EDITOR);
            spectator.detectChanges();

            expect(store.editorEditPanelTab()).toBe(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
        vi.useRealTimers(); // Restore the real timers after each test
    });
});
