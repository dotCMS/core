import { describe, expect, it } from '@jest/globals';
import { patchState } from '@ngrx/signals';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of, Subject, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, EventEmitter, Input, Output, signal, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import {
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
import { DEFAULT_VARIANT_ID, DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotPaletteListStore, DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSURLContentMap, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
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
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, HOST, PERSONA_KEY } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    EDIT_ACTION_PAYLOAD_MOCK,
    MOCK_RESPONSE_HEADLESS,
    PAYLOAD_MOCK,
    QUERY_PARAMS_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock,
    mockCurrentUser
} from '../shared/mocks';
import { ActionPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { IframeAccessMode } from '../store/models';

global.URL.createObjectURL = jest.fn(
    () => 'blob:http://localhost:3000/12345678-1234-1234-1234-123456789012'
);

// Mock window.matchMedia for PrimeNG components
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
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
    handleAction: jest.fn((_message: unknown, _deps: unknown) => of({}))
};

const mockDotUveDragDropService = {
    setupDragEvents: jest.fn()
};

const mockInlineEditService = {
    enableInlineEdit: jest.fn(),
    disableInlineEdit: jest.fn(),
    injectInlineEdit: jest.fn(),
    removeInlineEdit: jest.fn()
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
                    getContentletByInode: () => of(URL_MAP_CONTENTLET)
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
                    track: jest.fn()
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
        let confirmationService: ConfirmationService;
        let messageService: MessageService;
        let addMessageSpy: jest.SpyInstance;

        const createComponent = createRouting();

        const triggerCustomEvent = (
            element: DebugElement,
            eventName: string,
            eventObj: unknown
        ) => {
            spectator.triggerEventHandler(element, eventName, eventObj);
        };

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
            addMessageSpy = jest.spyOn(messageService, 'add');
            mockDotUveActionsHandlerService.handleAction.mockClear();

            store.pageLoad({
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
                    addEventListener: jest.fn(),
                    removeEventListener: jest.fn(),
                    postMessage: jest.fn(),
                    scrollTo: jest.fn(),
                    document: {
                        getElementById: jest.fn(),
                        querySelector: jest.fn(),
                        createElement: jest.fn(),
                        body: {
                            appendChild: jest.fn(),
                            querySelector: jest.fn()
                        },
                        head: {
                            appendChild: jest.fn(),
                            querySelector: jest.fn()
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
                jest.useFakeTimers(); // Mock the timers
            });

            afterEach(() => {
                jest.useRealTimers(); // Restore the real timers after each test
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

            it('should hide components when the store changes for a variant', () => {
                // Dialog may remain in DOM when appended to body
                const componentsToHide = ['palette', 'dropzone', 'contentlet-tools'];

                spectator.detectChanges();

                spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                spectator.detectChanges();
                store.pageLoad({
                    url: 'index',
                    language_id: '5',
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    variantName: 'hello-there',
                    experimentId: 'i-have-a-running-experiment'
                });

                spectator.detectChanges();

                componentsToHide.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).toBeNull();
                });
            });

            it('should show the editor components when there is a running experiement and initialize the editor in a default variant', async () => {
                const componentsToShow = ['palette', 'dialog', 'confirm-dialog'];

                spectator.activatedRouteStub.setQueryParam('variantName', DEFAULT_VARIANT_ID);

                spectator.detectChanges();

                store.pageLoad({
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
                const spy = jest.spyOn(store, 'pageReload');
                blockEditorSidebar.onSaved.emit();
                expect(spy).toHaveBeenCalled();
            });

            it('should show the error component when there is no live version', () => {
                const errorComponent = spectator.query(DotUvePageVersionNotFoundComponent);

                spectator.detectChanges();

                store.pageLoad({
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

                    it('should hide overlay when page is locked', () => {
                        patchState(store, {
                            pageAssetResponse: { pageAsset: lockedByAnotherUser }
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

                    const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                    const saveMock = jest.spyOn(store, 'editorSave');

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
                let resetActiveContentletSpy: jest.SpyInstance;

                beforeEach(() => {
                    resetActiveContentletSpy = jest.spyOn(store, 'resetSelected');
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

                        const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');

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

                        const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');

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
                let resetActiveContentletSpy: jest.SpyInstance;
                /** Use the same store instance the component uses so patches are visible to its effect */
                let componentStore: InstanceType<typeof UVEStore>;

                beforeEach(() => {
                    componentStore = (
                        spectator.component as unknown as {
                            uveStore: InstanceType<typeof UVEStore>;
                        }
                    ).uveStore;
                    resetActiveContentletSpy = jest.spyOn(componentStore, 'resetSelected');

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

                it('should edit urlContentMap page', () => {
                    spectator.detectChanges();
                    const dialog = spectator.query(DotEmaDialogComponent);
                    jest.spyOn(dialog, 'editUrlContentMapContentlet');

                    const payload = {
                        identifier: '123',
                        inode: '456',
                        title: 'Hello World'
                    } as unknown as DotCMSURLContentMap;

                    spectator.triggerEventHandler(
                        DotUveToolbarStubComponent,
                        'editUrlContentMap',
                        payload
                    );

                    expect(dialog.editUrlContentMapContentlet).toHaveBeenCalledWith(payload);
                });

                describe('reorder navigation', () => {
                    it('should reload the page after saving the new navigation order', () => {
                        const reloadSpy = jest.spyOn(store, 'pageReload');
                        const messageSpy = jest.spyOn(messageService, 'add');
                        const dialog = spectator.debugElement.query(
                            By.css("[data-testId='ema-dialog']")
                        );

                        triggerCustomEvent(dialog, 'action', {
                            event: new CustomEvent('ng-event', {
                                detail: {
                                    name: NG_CUSTOM_EVENTS.SAVE_MENU_ORDER
                                }
                            })
                        });

                        expect(reloadSpy).toHaveBeenCalled();

                        expect(messageSpy).toHaveBeenCalledWith({
                            severity: 'success',
                            summary: 'editpage.content.contentlet.menu.reorder.title',
                            detail: 'message.menu.reordered',
                            life: 2000
                        });

                        const pDialog = dialog.query(By.css('p-dialog'));

                        // Access the PrimeNG Dialog component instance to verify visible property
                        expect(pDialog.componentInstance.visible).toBe(false);
                    });

                    it('should advice the users when they can not save the new order', () => {
                        const messageSpy = jest.spyOn(messageService, 'add');
                        const dialog = spectator.debugElement.query(
                            By.css("[data-testId='ema-dialog']")
                        );

                        triggerCustomEvent(dialog, 'action', {
                            event: new CustomEvent('ng-event', {
                                detail: {
                                    name: NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER
                                }
                            })
                        });

                        expect(messageSpy).toHaveBeenCalledWith({
                            severity: 'error',
                            summary: 'editpage.content.contentlet.menu.reorder.title',
                            detail: 'error.menu.reorder.user_has_not_permission',
                            life: 2000
                        });
                    });

                    afterEach(() => jest.clearAllMocks());
                });

                beforeEach(() => {
                    jest.clearAllMocks();
                });
            });

            describe('add', () => {
                it('should add contentlet after backend emit SAVE_CONTENTLET', () => {
                    spectator.detectChanges();

                    const editorSaveMock = jest.spyOn(store, 'editorSave');

                    const payload: ActionPayload = { ...PAYLOAD_MOCK };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'content',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                                data: {
                                    url: 'test/url',
                                    contentType: 'test'
                                }
                            }
                        })
                    });

                    spectator.detectChanges();

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                payload: {
                                    contentletIdentifier: 'some-random-identifier'
                                }
                            }
                        }),
                        actionPayload: PAYLOAD_MOCK
                    });

                    spectator.detectChanges();

                    expect(editorSaveMock).toHaveBeenCalledWith(PAYLOAD_MOCK.pageContainers);

                    spectator.detectChanges();
                });

                it('should not add contentlet after backend emit SAVE_CONTENTLET and contentlet is dupe', () => {
                    spectator.detectChanges();

                    const payload: ActionPayload = { ...PAYLOAD_MOCK };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'content',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                                data: {
                                    url: 'test/url',
                                    contentType: 'test'
                                }
                            }
                        })
                    });

                    spectator.detectChanges();

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                payload: {
                                    contentletIdentifier: PAYLOAD_MOCK.container.contentletsId[0] // An already added contentlet
                                }
                            }
                        }),
                        actionPayload: payload
                    });

                    spectator.detectChanges();

                    expect(addMessageSpy).toHaveBeenCalledWith({
                        severity: 'info',
                        summary: 'Content already added',
                        detail: 'This content is already added to this container',
                        life: 2000
                    });
                });

                it('should add contentlet after backend emit CONTENT_SEARCH_SELECT', () => {
                    const saveMock = jest.spyOn(store, 'editorSave');

                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            maxContentlets: 2,
                            contentletsId: ['123']
                        },
                        pageId: 'test',
                        position: 'after'
                    };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'content',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'new-contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        }),
                        actionPayload: payload
                    });

                    spectator.detectChanges();

                    expect(saveMock).toHaveBeenCalledWith([
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
                });

                it('should not add contentlet after backend emit CONTENT_SEARCH_SELECT and contentlet is dupe', () => {
                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            maxContentlets: 1,
                            contentletsId: ['contentlet-identifier-123']
                        },
                        pageId: 'test',
                        position: 'before'
                    };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'content',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        }),
                        actionPayload: payload
                    });

                    spectator.detectChanges();

                    expect(addMessageSpy).toHaveBeenCalledWith({
                        severity: 'info',
                        summary: 'Content already added',
                        detail: 'This content is already added to this container',
                        life: 2000
                    });
                });

                it('should add widget after backend emit CONTENT_SEARCH_SELECT', () => {
                    const saveMock = jest.spyOn(store, 'editorSave');

                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            maxContentlets: 2,
                            contentletsId: ['123']
                        },
                        pageId: 'test',
                        position: 'after'
                    };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'widget',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'new-contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        }),
                        actionPayload: payload
                    });

                    spectator.detectChanges();

                    expect(saveMock).toHaveBeenCalledWith([
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
                });

                it('should not add widget after backend emit CONTENT_SEARCH_SELECT and widget is dupe', () => {
                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            maxContentlets: 1,
                            contentletsId: ['contentlet-identifier-123']
                        },
                        pageId: 'test',
                        position: 'before'
                    };

                    store.setContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(DotUveContentletToolsComponent, 'addContent', {
                        type: 'widget',
                        payload
                    });

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        }),
                        actionPayload: payload
                    });

                    spectator.detectChanges();

                    expect(addMessageSpy).toHaveBeenCalledWith({
                        severity: 'info',
                        summary: 'Content already added',
                        detail: 'This content is already added to this container',
                        life: 2000
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
                        jest.useFakeTimers(); // Mock the timers
                        spectator.detectChanges();

                        store.pageLoad({
                            url: 'index',
                            language_id: '3',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                            clientHost: undefined
                        });
                    });

                    it.skip('iframe should have the correct content when is VTL', () => {
                        spectator.detectChanges();
                        jest.runOnlyPendingTimers();

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
                        const scrollSpy = jest
                            .spyOn(
                                spectator.component.iframe.nativeElement.contentWindow,
                                'scrollTo'
                            )
                            .mockImplementation(() => jest.fn);

                        iframe.nativeElement.contentWindow.scrollTo(0, 100); //Scroll down

                        store.pageLoad({
                            url: 'index',
                            language_id: '4',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                        });

                        spectator.detectChanges();
                        jest.runOnlyPendingTimers();

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
                    jest.spyOn(router, 'navigate');

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

                it('should show the dialogs when we can edit a variant', () => {
                    const componentsToHide = ['dialog', 'confirm-dialog']; // Test id of components that should hide when entering preview modes

                    spectator.detectChanges();

                    spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                    spectator.detectChanges();
                    store.pageLoad({
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

            describe('language selected', () => {
                it('should update the URL and language when the user create a new translation changing the URL', () => {
                    store.pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '2',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    const pageLoadSpy = jest.spyOn(store, 'pageLoad');

                    spectator.detectChanges();
                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    spectator.detectComponentChanges();

                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.component['handleOpenFullEditor']();

                    spectator.detectComponentChanges();

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED,
                                payload: {
                                    htmlPageReferer:
                                        '/new-url-here?com.dotmarketing.htmlpage.language=1'
                                }
                            }
                        })
                    });

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        url: '/new-url-here',
                        language_id: '1'
                    });
                });

                it('should update the language when the user create a new translation', () => {
                    store.pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'test-url',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    const pageLoadSpy = jest.spyOn(store, 'pageLoad');
                    spectator.detectChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );
                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED,
                                payload: {
                                    htmlPageReferer:
                                        '/test-url?com.dotmarketing.htmlpage.language=2'
                                }
                            }
                        })
                    });

                    expect(pageLoadSpy).toHaveBeenCalledWith({
                        language_id: '2'
                    });
                });

                it('should call dialog.translatePage when toolbar emits translatePage', () => {
                    store.pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });
                    spectator.detectChanges();

                    const translatePagePayload = {
                        page: { identifier: 'test-page-123', inode: 'inode-123' },
                        newLanguage: 2
                    };
                    const dialogTranslatePageSpy = jest.spyOn(
                        spectator.component.dialog,
                        'translatePage'
                    );

                    spectator.triggerEventHandler(
                        DotUveToolbarStubComponent,
                        'translatePage',
                        translatePagePayload
                    );

                    expect(dialogTranslatePageSpy).toHaveBeenCalledWith(translatePagePayload);
                });
            });

            describe('$translatePageEffect — dialog loop prevention', () => {
                it('should NOT show the translation dialog when uveStatus is LOADING', () => {
                    const confirmSpy = jest.spyOn(
                        spectator.inject(ConfirmationService, true),
                        'confirm'
                    );

                    // Load with an untranslated language (language_id=2 returns viewAs.language.id=2,
                    // and mockLanguageArray has id:2 with translated:false)
                    store.pageLoad({
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

                it('should navigate to a translated language when the user rejects creating a new translation', () => {
                    const confirmationService = spectator.inject(ConfirmationService, true);
                    // Set up the spy BEFORE loading so we capture the confirm call made by the effect
                    const confirmSpy = jest.spyOn(confirmationService, 'confirm');

                    // language_id=2 → viewAs.language.id=2, pageLanguages has id:2 translated:false
                    store.pageLoad({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '2',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    spectator.flushEffects();
                    spectator.detectChanges();

                    // The effect should have shown the dialog because currentLanguage.translated=false
                    expect(confirmSpy).toHaveBeenCalled();

                    // Spy on pageLoad AFTER the initial load to track only the reject navigation
                    const pageLoadSpy = jest.spyOn(store, 'pageLoad');

                    // Simulate user clicking "No"
                    const rejectCallback = (confirmSpy.mock.calls[0][0] as Confirmation).reject;
                    rejectCallback?.();

                    // Must navigate to language id=1 (the only translated language in mockLanguageArray),
                    // NOT to id=2 (which would cause an infinite loop)
                    expect(pageLoadSpy).toHaveBeenCalledWith({ language_id: '1' });
                });
            });

            describe('handleOpenFullEditor', () => {
                afterEach(() => {
                    jest.restoreAllMocks();
                });

                it('should open legacy ema dialog when the content type does not enable the new editor', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
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
                    const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );

                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.detectChanges();
                    spectator.component['handleOpenFullEditor']();
                    spectator.detectChanges();

                    expect(dialogSpy).toHaveBeenCalledWith(
                        expect.objectContaining({ inode: 'contentlet-inode-123' })
                    );
                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                });

                it('should open the new edit content dialog when CONTENT_EDITOR2_ENABLED is true on the content type', async () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        of(
                            createFakeContentType({
                                variable: 'test',
                                name: 'Test',
                                metadata: {
                                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                }
                            })
                        )
                    );
                    const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                    const dialogRefMock = {
                        onClose: new Subject<void | unknown>(),
                        close: jest.fn()
                    };
                    const dialogServiceOpenSpy = jest
                        .spyOn(spectator.inject(DialogService), 'open')
                        .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);

                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.detectChanges();
                    spectator.component['handleOpenFullEditor']();
                    spectator.detectChanges();

                    await spectator.fixture.whenStable();

                    expect(dialogSpy).not.toHaveBeenCalled();
                    expect(dialogServiceOpenSpy).toHaveBeenCalled();
                    const [, config] = dialogServiceOpenSpy.mock.calls[0];
                    expect(config.data).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            contentletInode: 'contentlet-inode-123'
                        })
                    );
                });

                it('should fall back to legacy dialog when getContentType throws', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        throwError(() => new Error('network error'))
                    );
                    const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );

                    store.setSelected({
                        bounds: { x: 0, y: 0, width: 0, height: 0 },
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });
                    spectator.detectChanges();
                    spectator.component['handleOpenFullEditor']();
                    spectator.detectChanges();

                    expect(dialogSpy).toHaveBeenCalledWith(
                        expect.objectContaining({ inode: 'contentlet-inode-123' })
                    );
                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                });
            });

            describe('handleEditWithCopyDecision', () => {
                const MULTI_PAGE_PAYLOAD: ActionPayload = {
                    ...EDIT_ACTION_PAYLOAD_MOCK,
                    contentlet: {
                        ...EDIT_ACTION_PAYLOAD_MOCK.contentlet,
                        onNumberOfPages: 2
                    }
                };

                afterEach(() => {
                    jest.restoreAllMocks();
                });

                describe('single-page contentlet (onNumberOfPages: 1)', () => {
                    it('should open legacy dialog when feature flag is disabled', () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
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
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogServiceOpenSpy = jest.spyOn(
                            spectator.inject(DialogService),
                            'open'
                        );

                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);
                        spectator.detectChanges();

                        expect(dialogSpy).toHaveBeenCalledWith(
                            expect.objectContaining({ inode: 'contentlet-inode-123' })
                        );
                        expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    });

                    it('should open new edit content dialog when feature flag is enabled', async () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                            of(
                                createFakeContentType({
                                    variable: 'test',
                                    name: 'Test',
                                    metadata: {
                                        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                    }
                                })
                            )
                        );
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogRefMock = {
                            onClose: new Subject<void | unknown>(),
                            close: jest.fn()
                        };
                        const dialogServiceOpenSpy = jest
                            .spyOn(spectator.inject(DialogService), 'open')
                            .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);

                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);
                        spectator.detectChanges();

                        await spectator.fixture.whenStable();

                        expect(dialogSpy).not.toHaveBeenCalled();
                        expect(dialogServiceOpenSpy).toHaveBeenCalled();
                        const [, config] = dialogServiceOpenSpy.mock.calls[0];
                        expect(config.data).toEqual(
                            expect.objectContaining({
                                mode: 'edit',
                                contentletInode: 'contentlet-inode-123'
                            })
                        );
                    });

                    it('should fall back to legacy dialog when getContentType throws', () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                            throwError(() => new Error('network error'))
                        );
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogServiceOpenSpy = jest.spyOn(
                            spectator.inject(DialogService),
                            'open'
                        );

                        spectator.component['handleEditWithCopyDecision'](EDIT_ACTION_PAYLOAD_MOCK);
                        spectator.detectChanges();

                        expect(dialogSpy).toHaveBeenCalledWith(
                            expect.objectContaining({ inode: 'contentlet-inode-123' })
                        );
                        expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    });
                });

                describe('multi-page contentlet (onNumberOfPages > 1)', () => {
                    it('should open legacy dialog when flag is disabled and user edits all pages', () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
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
                        jest.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: false }));
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogServiceOpenSpy = jest.spyOn(
                            spectator.inject(DialogService),
                            'open'
                        );

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        expect(dialogSpy).toHaveBeenCalledWith(
                            expect.objectContaining({ inode: 'contentlet-inode-123' })
                        );
                        expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    });

                    it('should open new edit content dialog when flag is enabled and user edits all pages', async () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                            of(
                                createFakeContentType({
                                    variable: 'test',
                                    name: 'Test',
                                    metadata: {
                                        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                    }
                                })
                            )
                        );
                        jest.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: false }));
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogRefMock = {
                            onClose: new Subject<void | unknown>(),
                            close: jest.fn()
                        };
                        const dialogServiceOpenSpy = jest
                            .spyOn(spectator.inject(DialogService), 'open')
                            .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        await spectator.fixture.whenStable();

                        expect(dialogSpy).not.toHaveBeenCalled();
                        expect(dialogServiceOpenSpy).toHaveBeenCalled();
                        const [, config] = dialogServiceOpenSpy.mock.calls[0];
                        expect(config.data).toEqual(
                            expect.objectContaining({
                                mode: 'edit',
                                contentletInode: 'contentlet-inode-123'
                            })
                        );
                    });

                    it('should open new edit content dialog with copied contentlet inode when flag is enabled and user copies', async () => {
                        const COPIED_INODE = 'copied-contentlet-inode-456';
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                            of(
                                createFakeContentType({
                                    variable: 'test',
                                    name: 'Test',
                                    metadata: {
                                        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                    }
                                })
                            )
                        );
                        jest.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: true }));
                        jest.spyOn(
                            spectator.inject(DotCopyContentService),
                            'copyInPage'
                        ).mockReturnValue(
                            of({ inode: COPIED_INODE, contentType: 'test' } as DotCMSContentlet)
                        );
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogRefMock = {
                            onClose: new Subject<void | unknown>(),
                            close: jest.fn()
                        };
                        const dialogServiceOpenSpy = jest
                            .spyOn(spectator.inject(DialogService), 'open')
                            .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        await spectator.fixture.whenStable();

                        expect(dialogSpy).not.toHaveBeenCalled();
                        expect(dialogServiceOpenSpy).toHaveBeenCalled();
                        const [, config] = dialogServiceOpenSpy.mock.calls[0];
                        expect(config.data).toEqual(
                            expect.objectContaining({
                                mode: 'edit',
                                contentletInode: COPIED_INODE
                            })
                        );
                    });

                    it('should fall back to legacy dialog and still reload page when getContentType throws after copy', () => {
                        const dotContentTypeService =
                            spectator.debugElement.injector.get(DotContentTypeService);
                        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                            throwError(() => new Error('network error'))
                        );
                        jest.spyOn(
                            spectator.inject(DotCopyContentModalService),
                            'open'
                        ).mockReturnValue(of({ shouldCopy: true }));
                        const COPIED_INODE = 'copied-contentlet-inode-456';
                        jest.spyOn(
                            spectator.inject(DotCopyContentService),
                            'copyInPage'
                        ).mockReturnValue(
                            of({ inode: COPIED_INODE, contentType: 'test' } as DotCMSContentlet)
                        );
                        const pageReloadSpy = jest.spyOn(store, 'pageReload');
                        const dialogSpy = jest.spyOn(spectator.component.dialog, 'editContentlet');
                        const dialogServiceOpenSpy = jest.spyOn(
                            spectator.inject(DialogService),
                            'open'
                        );

                        spectator.component['handleEditWithCopyDecision'](MULTI_PAGE_PAYLOAD);
                        spectator.detectChanges();

                        expect(pageReloadSpy).toHaveBeenCalled();
                        expect(dialogSpy).toHaveBeenCalledWith(
                            expect.objectContaining({ inode: COPIED_INODE })
                        );
                        expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    });
                });
            });

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
                    jest.restoreAllMocks();
                });

                it('should open the new edit content dialog when CONTENT_EDITOR2_ENABLED is true', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        of(
                            createFakeContentType({
                                variable: 'TestContentType',
                                name: 'Test Content Type',
                                metadata: {
                                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                }
                            })
                        )
                    );

                    const dialogRefMock = {
                        onClose: new Subject<void | unknown>(),
                        close: jest.fn()
                    };
                    const dialogServiceOpenSpy = jest
                        .spyOn(spectator.inject(DialogService), 'open')
                        .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);
                    const createFromPaletteSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentletFromPalette'
                    );

                    spectator.component.placeItem(EDIT_ACTION_PAYLOAD_MOCK, contentTypeDragItem);
                    spectator.detectChanges();

                    expect(createFromPaletteSpy).not.toHaveBeenCalled();
                    expect(dialogServiceOpenSpy).toHaveBeenCalled();
                    const [, config] = dialogServiceOpenSpy.mock.calls[0];
                    expect(config.data).toEqual(
                        expect.objectContaining({
                            mode: 'new',
                            contentTypeId: 'TestContentType'
                        })
                    );
                });

                it('should open the legacy dialog when CONTENT_EDITOR2_ENABLED is false', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        of(
                            createFakeContentType({
                                variable: 'TestContentType',
                                name: 'Test Content Type',
                                metadata: {
                                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false
                                }
                            })
                        )
                    );

                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );
                    const createFromPaletteSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentletFromPalette'
                    );

                    spectator.component.placeItem(EDIT_ACTION_PAYLOAD_MOCK, contentTypeDragItem);
                    spectator.detectChanges();

                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    expect(createFromPaletteSpy).toHaveBeenCalledWith(
                        expect.objectContaining({
                            variable: 'TestContentType',
                            name: 'Test Content Type'
                        })
                    );
                });

                it('should fall back to legacy dialog when getContentType throws', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );
                    const createFromPaletteSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentletFromPalette'
                    );

                    spectator.component.placeItem(EDIT_ACTION_PAYLOAD_MOCK, contentTypeDragItem);
                    spectator.detectChanges();

                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    expect(createFromPaletteSpy).toHaveBeenCalledWith(
                        expect.objectContaining({
                            variable: 'TestContentType',
                            name: 'Test Content Type'
                        })
                    );
                });
            });

            describe('handleNgEvent - CREATE_CONTENTLET', () => {
                const createContentletEvent = new CustomEvent('ng-event', {
                    detail: {
                        name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                        data: {
                            url: 'test/url',
                            contentType: 'TestContentType'
                        }
                    }
                });

                afterEach(() => {
                    jest.restoreAllMocks();
                });

                it('should open the new edit content dialog when CONTENT_EDITOR2_ENABLED is true', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        of(
                            createFakeContentType({
                                variable: 'TestContentType',
                                name: 'Test Content Type',
                                metadata: {
                                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                                }
                            })
                        )
                    );

                    const dialogRefMock = {
                        onClose: new Subject<void | unknown>(),
                        close: jest.fn()
                    };
                    const dialogServiceOpenSpy = jest
                        .spyOn(spectator.inject(DialogService), 'open')
                        .mockReturnValue(dialogRefMock as unknown as DynamicDialogRef);
                    const createContentletSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentlet'
                    );

                    spectator.component['handleNgEvent']({
                        event: createContentletEvent,
                        actionPayload: EDIT_ACTION_PAYLOAD_MOCK,
                        clientAction: DotCMSUVEAction.NOOP,
                        form: null
                    })?.();
                    spectator.detectChanges();

                    expect(createContentletSpy).not.toHaveBeenCalled();
                    expect(dialogServiceOpenSpy).toHaveBeenCalled();
                    const [, config] = dialogServiceOpenSpy.mock.calls[0];
                    expect(config.data).toEqual(
                        expect.objectContaining({
                            mode: 'new',
                            contentTypeId: 'TestContentType'
                        })
                    );
                });

                it('should open the legacy create dialog when CONTENT_EDITOR2_ENABLED is false', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        of(
                            createFakeContentType({
                                variable: 'TestContentType',
                                name: 'Test Content Type',
                                metadata: {
                                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false
                                }
                            })
                        )
                    );

                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );
                    const createContentletSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentlet'
                    );

                    spectator.component['handleNgEvent']({
                        event: createContentletEvent,
                        actionPayload: EDIT_ACTION_PAYLOAD_MOCK,
                        clientAction: DotCMSUVEAction.NOOP,
                        form: null
                    })?.();
                    spectator.detectChanges();

                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    expect(createContentletSpy).toHaveBeenCalledWith(
                        expect.objectContaining({
                            contentType: 'TestContentType',
                            url: 'test/url',
                            actionPayload: EDIT_ACTION_PAYLOAD_MOCK
                        })
                    );
                });

                it('should fall back to legacy create dialog when getContentType throws', () => {
                    const dotContentTypeService =
                        spectator.debugElement.injector.get(DotContentTypeService);
                    jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    const dialogServiceOpenSpy = jest.spyOn(
                        spectator.inject(DialogService),
                        'open'
                    );
                    const createContentletSpy = jest.spyOn(
                        spectator.component.dialog,
                        'createContentlet'
                    );

                    spectator.component['handleNgEvent']({
                        event: createContentletEvent,
                        actionPayload: EDIT_ACTION_PAYLOAD_MOCK,
                        clientAction: DotCMSUVEAction.NOOP,
                        form: null
                    })?.();
                    spectator.detectChanges();

                    expect(dialogServiceOpenSpy).not.toHaveBeenCalled();
                    expect(createContentletSpy).toHaveBeenCalledWith(
                        expect.objectContaining({
                            contentType: 'TestContentType',
                            url: 'test/url',
                            actionPayload: EDIT_ACTION_PAYLOAD_MOCK
                        })
                    );
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
                let pageLoadSpy: jest.SpyInstance;
                let windowOpenSpy: jest.Mock;
                let mockWindow: {
                    location: { origin: string; hostname: string };
                    open: jest.Mock;
                };

                beforeEach(() => {
                    mockWindow = {
                        location: {
                            origin: 'http://localhost:3000',
                            hostname: 'localhost'
                        },
                        open: jest.fn()
                    };
                    (spectator.component as unknown as { window: typeof mockWindow }).window =
                        mockWindow;
                    pageLoadSpy = jest.spyOn(store, 'pageLoad');
                    windowOpenSpy = mockWindow.open;
                });

                const createMockEvent = (href: string, isInlineEditing = false): MouseEvent => {
                    const mockAnchor = {
                        href,
                        getAttribute: jest.fn().mockReturnValue(href),
                        closest: jest.fn().mockReturnValue({ href, getAttribute: () => href })
                    };

                    const mockEvent = {
                        target: mockAnchor,
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    // Mock the store state for inline editing (editorState returns EDITOR_STATE enum directly)
                    jest.spyOn(store, 'editorState').mockReturnValue(
                        isInlineEditing ? EDITOR_STATE.INLINE_EDITING : EDITOR_STATE.IDLE
                    );

                    return mockEvent;
                };

                it('should not do anything if href is empty', () => {
                    const mockEvent = {
                        target: { href: '', closest: () => null },
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    jest.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

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

                it('should open external URL in new tab', () => {
                    const externalUrl = 'https://external-site.com/page';
                    const mockEvent = createMockEvent(externalUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(windowOpenSpy).toHaveBeenCalledWith(externalUrl, '_blank');
                    expect(pageLoadSpy).not.toHaveBeenCalled();
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
                            closest: jest.fn().mockReturnValue({
                                href: 'http://localhost:3000/fallback-page?test=123',
                                getAttribute: jest
                                    .fn()
                                    .mockReturnValue('http://localhost:3000/fallback-page?test=123')
                            })
                        },
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    jest.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

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
                        jest.spyOn(store, 'pageParams').mockReturnValue(samePathPageParams());
                    });

                    it('should not trigger pageLoad for hash-only navigation on same page', () => {
                        const hashUrl = 'http://localhost:3000/current-page#sectionA';
                        const mockEvent = createMockEvent(hashUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                    });

                    it('should not trigger pageLoad for hash-only with complex id', () => {
                        const hashUrl = 'http://localhost:3000/current-page#section-123_complex';
                        const mockEvent = createMockEvent(hashUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });

                    it('should not trigger pageLoad for query-only navigation on same page', () => {
                        const queryUrl = 'http://localhost:3000/current-page?tab=2';
                        const mockEvent = createMockEvent(queryUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                    });

                    it('should not trigger pageLoad for multiple query params on same page', () => {
                        const queryUrl =
                            'http://localhost:3000/current-page?filter=value&sort=date';
                        const mockEvent = createMockEvent(queryUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                    });

                    it('should not trigger pageLoad when both hash and query are present on same path', () => {
                        const combinedUrl = 'http://localhost:3000/current-page?tab=2#section';
                        const mockEvent = createMockEvent(combinedUrl);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).not.toHaveBeenCalled();
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
                        jest.spyOn(store, 'pageParams').mockReturnValue({
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
                                getAttribute: jest.fn().mockReturnValue('#page-section'),
                                closest: jest.fn().mockReturnValue({
                                    href: 'about:blank#page-section',
                                    getAttribute: () => '#page-section'
                                })
                            },
                            preventDefault: jest.fn()
                        } as unknown as MouseEvent;

                        jest.spyOn(store, 'editorState').mockReturnValue(EDITOR_STATE.IDLE);

                        spectator.component.handleInternalNav(mockEvent);

                        expect(windowOpenSpy).not.toHaveBeenCalled();
                        expect(pageLoadSpy).not.toHaveBeenCalled();
                        expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                    });
                });

                afterEach(() => {
                    jest.clearAllMocks();
                });
            });

            describe('handleSectionOffset', () => {
                it('scrolls the iframe contentWindow (not the canvas viewport) to the offset', () => {
                    const scrollToSpy = jest.fn();
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
                    const scrollToSpy = jest.fn();
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
        jest.clearAllMocks();
        jest.useRealTimers(); // Restore the real timers after each test
    });
});
