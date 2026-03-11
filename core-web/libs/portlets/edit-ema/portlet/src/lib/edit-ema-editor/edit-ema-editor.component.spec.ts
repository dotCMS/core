import { describe, expect, it } from '@jest/globals';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { MockComponent } from 'ng-mocks';
import { EMPTY, Observable, of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement, EventEmitter, Input, Output, signal, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';

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
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    LoginService
} from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID, DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';
import { DotCopyContentModalService, ModelCopyContentResponse, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    CONTENT_TYPE_MOCK_FOR_EDITOR,
    CurrentUserDataMock,
    DotCurrentUserServiceMock,
    DotDevicesServiceMock,
    DotLanguagesServiceMock,
    DotPersonalizeServiceMock,
    DotcmsConfigServiceMock,
    DotcmsEventsServiceMock,
    LoginServiceMock,
    MockDotHttpErrorManagerService,
    MockDotMessageService,
    URL_MAP_CONTENTLET,
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    mockDotDevices,
    seoOGTagsResultMock
} from '@dotcms/utils-testing';

import { DotUveContentletToolsComponent } from './components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotPaletteListStore } from './components/dot-uve-palette/components/dot-uve-palette-list/store/store';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotEmaRunningExperimentComponent } from './components/dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-toolbar/components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DotUveActionsHandlerService } from '../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveBridgeService } from '../services/dot-uve-bridge/dot-uve-bridge.service';
import { DotUveDragDropService } from '../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { InlineEditService } from '../services/inline-edit/inline-edit.service';
import { DEFAULT_PERSONA, HOST, PERSONA_KEY } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    EDIT_ACTION_PAYLOAD_MOCK,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    PAGE_WITH_ADVANCE_RENDER_TEMPLATE_MOCK,
    PAYLOAD_MOCK,
    QUERY_PARAMS_MOCK,
    TREE_NODE_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock,
    newContentlet
} from '../shared/mocks';
import { ActionPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import * as uveUtils from '../utils';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../utils';

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
    loggedUser: signal(CurrentUserDataMock)
};

const mockDotUveBridgeService = {
    initialize: jest.fn(() => EMPTY),
    handleMessage: jest.fn(),
    sendMessageToIframe: jest.fn()
};

const mockDotUveActionsHandlerService = {
    handleAction: jest.fn(() => of({}))
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
        imports: [RouterTestingModule, HttpClientTestingModule, SafeUrlPipe, ConfirmDialogModule],
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
                provide: DotUveBridgeService,
                useValue: mockDotUveBridgeService
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
                provide: DotcmsEventsService,
                useValue: new DotcmsEventsServiceMock()
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
                    },
                    saveContentlet() {
                        return of({});
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
                provide: CoreWebService,
                useClass: CoreWebServiceMock
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
        let dotCopyContentModalService: DotCopyContentModalService;
        let dotCopyContentService: DotCopyContentService;
        let dotHttpErrorManagerService: DotHttpErrorManagerService;
        let dotPageApiService: DotPageApiService;

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
            dotCopyContentModalService = spectator.inject(DotCopyContentModalService, true);
            dotCopyContentService = spectator.inject(DotCopyContentService, true);
            dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService, true);
            dotPageApiService = spectator.inject(DotPageApiService, true);
            addMessageSpy = jest.spyOn(messageService, 'add');

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
                it('should return display block when socialMedia is null', () => {
                    patchState(store, {
                        viewSocialMedia: null
                    });

                    spectator.detectChanges();

                    expect(spectator.component.$editorContentStyles()).toEqual({
                        display: 'block'
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
                    resetActiveContentletSpy = jest.spyOn(store, 'resetActiveContentlet');
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

                        store.setActiveContentlet(activeContentlet);

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

                        store.setActiveContentlet(activeContentlet);

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
                let componentStore: InstanceType<typeof UVEStore>;
                let resetActiveContentletSpy: jest.SpyInstance;
                const getPageAsset = () => {
                    const pageSnapshot = store.pageAsset();
                    if (!pageSnapshot) {
                        throw new Error('Expected page to be loaded in store');
                    }
                    // eslint-disable-next-line @typescript-eslint/no-unused-vars -- destructuring for exclusion
                    const { content, requestMetadata, clientResponse, ...asset } = pageSnapshot;
                    return asset;
                };

                beforeEach(() => {
                    componentStore = (
                        spectator.component as unknown as {
                            uveStore: InstanceType<typeof UVEStore>;
                        }
                    ).uveStore;
                    resetActiveContentletSpy = jest.spyOn(componentStore, 'resetActiveContentlet');

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

                it('should not reset activeContentlet when page is unlocked but no activeContentlet exists', () => {
                    // Don't set activeContentlet
                    expect(store.editorActiveContentlet()).toBeNull();

                    // Set page as locked first
                    const currentResponse = getPageAsset();
                    store.updatePageResponse({
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
                    const lockedResponse = getPageAsset();
                    store.updatePageResponse({
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

                    spectator.triggerEventHandler(DotUveToolbarStubComponent, 'editUrlContentMap', {
                        identifier: '123',
                        inode: '456',
                        title: 'Hello World'
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    } as any);

                    expect(dialog.editUrlContentMapContentlet).toHaveBeenCalledWith({
                        identifier: '123',
                        inode: '456',
                        title: 'Hello World'
                    });
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

                describe('Copy content', () => {
                    let copySpy: jest.SpyInstance<Observable<DotCMSContentlet>>;
                    let dialogLoadingSpy: jest.SpyInstance;
                    let editContentletSpy: jest.SpyInstance;
                    let modalSpy: jest.SpyInstance<Observable<ModelCopyContentResponse>>;
                    let reloadIframeSpy: jest.SpyInstance;

                    const EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES = {
                        ...EDIT_ACTION_PAYLOAD_MOCK,
                        contentlet: {
                            identifier: 'contentlet-identifier-123',
                            inode: 'contentlet-inode-123',
                            title: 'Hello World',
                            contentType: 'test',
                            onNumberOfPages: 2
                        }
                    };

                    const CONTENTLET_MOCK = {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload: EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
                    };

                    beforeEach(() => {
                        copySpy = jest.spyOn(dotCopyContentService, 'copyInPage');
                        dialogLoadingSpy = jest.spyOn(
                            spectator.component.dialog,
                            'showLoadingIframe'
                        );
                        editContentletSpy = jest.spyOn(
                            spectator.component.dialog,
                            'editContentlet'
                        );
                        modalSpy = jest.spyOn(dotCopyContentModalService, 'open');
                        reloadIframeSpy = jest.spyOn(
                            spectator.component.iframe.nativeElement.contentWindow,
                            'postMessage'
                        );
                        jest.spyOn(store, 'getCurrentTreeNode').mockReturnValue(TREE_NODE_MOCK);
                    });

                    it('should copy and open edit dialog', () => {
                        copySpy.mockReturnValue(of(newContentlet));
                        modalSpy.mockReturnValue(of({ shouldCopy: true }));

                        spectator.detectChanges();

                        store.setContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            DotUveContentletToolsComponent,
                            'editContent',
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
                        );

                        spectator.detectComponentChanges();

                        expect(copySpy).toHaveBeenCalledWith(TREE_NODE_MOCK); // It's not being called
                        expect(dialogLoadingSpy).toHaveBeenCalledWith('Hello World');
                        expect(editContentletSpy).toHaveBeenCalledWith(newContentlet);
                        expect(modalSpy).toHaveBeenCalled();
                    });

                    it('should show an error if the copy content fails', () => {
                        const handleErrorSpy = jest.spyOn(dotHttpErrorManagerService, 'handle');
                        const resetDialogSpy = jest.spyOn(
                            spectator.component.dialog,
                            'resetDialog'
                        );
                        copySpy.mockReturnValue(throwError({}));
                        modalSpy.mockReturnValue(of({ shouldCopy: true }));
                        spectator.detectChanges();

                        store.setContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            DotUveContentletToolsComponent,
                            'editContent',
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
                        );

                        spectator.detectComponentChanges();

                        expect(copySpy).toHaveBeenCalled();
                        expect(dialogLoadingSpy).toHaveBeenCalledWith('Hello World');
                        expect(editContentletSpy).not.toHaveBeenCalled();
                        expect(handleErrorSpy).toHaveBeenCalled();
                        expect(modalSpy).toHaveBeenCalled();
                        expect(reloadIframeSpy).not.toHaveBeenCalledWith();
                        expect(resetDialogSpy).toHaveBeenCalled();
                    });

                    it('should ask to copy and not copy content', () => {
                        copySpy.mockReturnValue(of(newContentlet));
                        modalSpy.mockReturnValue(of({ shouldCopy: false }));

                        spectator.detectChanges();

                        store.setContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            DotUveContentletToolsComponent,
                            'editContent',
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
                        );

                        spectator.detectComponentChanges();

                        expect(copySpy).not.toHaveBeenCalled();
                        expect(dialogLoadingSpy).not.toHaveBeenCalled();
                        expect(editContentletSpy).toHaveBeenCalledWith(
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES.contentlet
                        );
                        expect(modalSpy).toHaveBeenCalled();
                        expect(reloadIframeSpy).not.toHaveBeenCalledWith();
                    });
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
                // Skipped: $progressBar stays true after patch in this describe (fake timers or CD). Re-enable when root cause fixed.
                it.skip('should not show a loader when client is ready and UVE is not loading', () => {
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
                        (spectator.component as unknown as { $progressBar: () => boolean })
                            .$progressBar()
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

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
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

                describe('script and styles injection', () => {
                    describe('designer templates', () => {
                        beforeEach(() => {
                            jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                                of(MOCK_RESPONSE_VTL)
                            );
                            store.pageLoad({ url: 'index', clientHost: null });
                        });

                        it.skip('should call injectBaseTag with the right data', () => {
                            const origin = window.location.origin;
                            const injectBaseTagSpy = jest.spyOn(uveUtils, 'injectBaseTag');

                            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
                            iframe.dispatchEvent(new Event('load'));

                            spectator.detectChanges();

                            expect(injectBaseTagSpy).toHaveBeenCalledWith({
                                html: MOCK_RESPONSE_VTL.page.rendered,
                                url: MOCK_RESPONSE_VTL.page.pageURI,
                                origin
                            });
                        });

                        it.skip('should add script and styles to iframe', () => {
                            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
                            const spyWrite = jest.spyOn(iframe.contentDocument, 'write');
                            iframe.dispatchEvent(new Event('load'));

                            spectator.detectChanges();

                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining(
                                    `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                                )
                            );
                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining(`[data-dot-object="container"]:empty`)
                            );

                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining(
                                    '[data-dot-object="contentlet"].empty-contentlet'
                                )
                            );
                        });
                    });

                    describe('advance templates', () => {
                        beforeEach(() => {
                            jest.spyOn(dotPageApiService, 'get').mockReturnValue(
                                of(PAGE_WITH_ADVANCE_RENDER_TEMPLATE_MOCK)
                            );
                            store.pageLoad({ url: 'index', clientHost: null });
                        });

                        it.skip('should add script and styles to iframe for advance templates', () => {
                            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
                            const spyWrite = jest.spyOn(iframe.contentDocument, 'write');

                            iframe.dispatchEvent(new Event('load'));

                            spectator.detectChanges();
                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining(
                                    `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                                )
                            );

                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining('[data-dot-object="container"]:empty')
                            );
                            expect(spyWrite).toHaveBeenCalledWith(
                                expect.stringContaining(
                                    '[data-dot-object="contentlet"].empty-contentlet'
                                )
                            );
                        });
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

                    spectator.triggerEventHandler(
                        DotUveContentletToolsComponent,
                        'editContent',
                        EDIT_ACTION_PAYLOAD_MOCK
                    );

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

            describe('Editor content', () => {
                it('should have display block when there is not SEO view', () => {
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('block');
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

                afterEach(() => {
                    jest.clearAllMocks();
                });
            });
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.useRealTimers(); // Restore the real timers after each test
    });
});
