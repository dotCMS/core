import { describe, expect, it } from '@jest/globals';
import {
    SpectatorRouting,
    byTestId,
    createRoutingFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { Observable, of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
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
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
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
import { DEFAULT_VARIANT_ID, DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { DotCopyContentModalService, ModelCopyContentResponse, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
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

import { DotUvePageVersionNotFoundComponent } from './components/dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { DotEmaRunningExperimentComponent } from './components/dot-uve-toolbar/components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { DotUveWorkflowActionsComponent } from './components/dot-uve-toolbar/components/dot-uve-workflow-actions/dot-uve-workflow-actions.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { CONTENT_TYPE_MOCK } from './components/edit-ema-palette/components/edit-ema-palette-content-type/edit-ema-palette-content-type.component.spec';
import { CONTENTLETS_MOCK } from './components/edit-ema-palette/edit-ema-palette.component.spec';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotBlockEditorSidebarComponent } from '../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, HOST, PERSONA_KEY } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, PALETTE_CLASSES, UVE_STATUS } from '../shared/enums';
import {
    EDIT_ACTION_PAYLOAD_MOCK,
    EMA_DRAG_ITEM_CONTENTLET_MOCK,
    MOCK_RESPONSE_VTL,
    PAGE_WITH_ADVANCE_RENDER_TEMPLATE_MOCK,
    PAYLOAD_MOCK,
    QUERY_PARAMS_MOCK,
    TREE_NODE_MOCK,
    URL_CONTENT_MAP_MOCK,
    UVE_PAGE_RESPONSE_MAP,
    dotPropertiesServiceMock,
    newContentlet
} from '../shared/mocks';
import { ActionPayload, ContentTypeDragPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { SDK_EDITOR_SCRIPT_SOURCE, TEMPORAL_DRAG_ITEM } from '../utils';

global.URL.createObjectURL = jest.fn(
    () => 'blob:http://localhost:3000/12345678-1234-1234-1234-123456789012'
);

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

const createRouting = () =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, HttpClientTestingModule, SafeUrlPipe, ConfirmDialogModule],
        declarations: [
            MockComponent(DotUveWorkflowActionsComponent),
            MockComponent(DotResultsSeoToolComponent),
            MockComponent(DotEmaRunningExperimentComponent)
        ],
        detectChanges: false,
        componentProviders: [
            ConfirmationService,
            MessageService,
            UVEStore,
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
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
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
            }
        ],
        providers: [
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
                    filterContentTypes: () => of([CONTENT_TYPE_MOCK]),
                    getContentTypes: () => of([CONTENT_TYPE_MOCK]),
                    getContentType: () => of(CONTENT_TYPE_MOCK)
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
        let dotLicenseService: DotLicenseService;
        let dotCopyContentModalService: DotCopyContentModalService;
        let dotCopyContentService: DotCopyContentService;
        let dotContentletService: DotContentletService;
        let dotAlertConfirmService: DotAlertConfirmService;
        let dotHttpErrorManagerService: DotHttpErrorManagerService;
        let dotTempFileUploadService: DotTempFileUploadService;
        let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
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
            dotLicenseService = spectator.inject(DotLicenseService, true);
            dotCopyContentModalService = spectator.inject(DotCopyContentModalService, true);
            dotCopyContentService = spectator.inject(DotCopyContentService, true);
            dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService, true);
            dotContentletService = spectator.inject(DotContentletService, true);
            dotAlertConfirmService = spectator.inject(DotAlertConfirmService, true);
            dotTempFileUploadService = spectator.inject(DotTempFileUploadService, true);
            dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);
            dotPageApiService = spectator.inject(DotPageApiService, true);
            addMessageSpy = jest.spyOn(messageService, 'add');
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

            store.loadPageAsset({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                mode: UVE_MODE.EDIT,
                [PERSONA_KEY]: DEFAULT_PERSONA.identifier
            });

            spectator.detectChanges();
        });

        describe('DOM', () => {
            beforeEach(() => {
                jest.useFakeTimers(); // Mock the timers
            });

            afterEach(() => {
                jest.useRealTimers(); // Restore the real timers after each test
            });

            it('should hide components when the store changes', () => {
                const componentsToHide = ['palette', 'dropzone', 'contentlet-tools', 'dialog']; // Test id of components that should hide when entering preview modes

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                store.setDevice(iphone);

                spectator.detectChanges();

                componentsToHide.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).toBeNull();
                });
            });

            it('should hide palette when state changes', () => {
                // First, make sure palette is visible by default
                expect(spectator.query(byTestId('palette')).classList).toContain(
                    PALETTE_CLASSES.OPEN
                );

                // Simulate Click the toggle button
                store.setPaletteOpen(false);

                spectator.detectChanges();

                // Palette should now be hidden
                expect(spectator.query(byTestId('palette')).classList).toContain(
                    PALETTE_CLASSES.CLOSED
                );
            });

            // TODO: Skipped until we discuss with design about the new toggle button for the palette
            xit('should have a placeholder for the palette toggle button', () => {
                store.setPaletteOpen(true);

                spectator.detectChanges();

                const placeholder = spectator.query(byTestId('toggle-palette-placeholder'));

                expect(placeholder).not.toBeNull();
            });

            it('should have a toolbar', () => {
                const toolbar = spectator.query(DotUveToolbarComponent);

                expect(toolbar).not.toBeNull();
            });

            it('should hide components when the store changes for a variant', () => {
                const componentsToHide = ['palette', 'dropzone', 'contentlet-tools', 'dialog']; // Test id of components that should hide when entering preview modes

                spectator.detectChanges();

                spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                spectator.detectChanges();
                store.loadPageAsset({
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

                store.loadPageAsset({
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
                const spy = jest.spyOn(store, 'reloadCurrentPage');
                blockEditorSidebar.onSaved.emit();
                expect(spy).toHaveBeenCalled();
            });

            it('should show the error component when there is no live version', () => {
                const errorComponent = spectator.query(DotUvePageVersionNotFoundComponent);

                spectator.detectChanges();

                store.loadPageAsset({
                    url: 'index',
                    language_id: '9'
                });

                spectator.detectChanges();

                expect(errorComponent).toBeDefined();
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
                            contentletsId: ['123'],
                            variantId: '123'
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

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectChanges();

                    const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                    const saveMock = jest.spyOn(store, 'savePage');
                    const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                    spectator.triggerEventHandler(EmaContentletToolsComponent, 'delete', payload);

                    spectator.detectComponentChanges();

                    expect(confirmDialogOpen).toHaveBeenCalled();

                    confirmDialog
                        .querySelector('.p-confirm-dialog-accept')
                        .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

                    expect(saveMock).toHaveBeenCalledWith([
                        { contentletsId: [], identifier: '123', personaTag: undefined, uuid: '123' }
                    ]);
                });
            });

            describe('edit', () => {
                beforeEach(() => {
                    store.setIsClientReady(true);
                });
                const baseContentletPayload = {
                    x: 100,
                    y: 100,
                    width: 500,
                    height: 500,
                    payload: EDIT_ACTION_PAYLOAD_MOCK
                };

                it('should edit urlContentMap page', () => {
                    spectator.detectChanges();
                    const dialog = spectator.query(DotEmaDialogComponent);
                    jest.spyOn(dialog, 'editUrlContentMapContentlet');

                    spectator.triggerEventHandler(DotUveToolbarComponent, 'editUrlContentMap', {
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

                it('should open a dialog and save after backend emit', (done) => {
                    spectator.detectChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    store.setEditorContentletArea(baseContentletPayload);

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'edit',
                        EDIT_ACTION_PAYLOAD_MOCK
                    );

                    spectator.detectComponentChanges();

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                payload: {
                                    htmlPageReferer: '/my-awesome-page'
                                }
                            }
                        })
                    });

                    spectator.detectChanges();

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                    iframe.nativeElement.contentWindow.addEventListener(
                        'message',
                        (event: MessageEvent) => {
                            expect(event).toBeTruthy();
                            done();
                        }
                    );
                });

                it('should open a dialog to edit contentlet using custom action and trigger reload after saving', (done) => {
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.EDIT_CONTENTLET,
                                payload: CONTENTLETS_MOCK[0]
                            }
                        })
                    );

                    spectator.detectComponentChanges();

                    const dialog = spectator.debugElement.query(
                        By.css("[data-testId='ema-dialog']")
                    );

                    const pDialog = dialog.query(By.css('p-dialog'));

                    expect(pDialog.attributes['ng-reflect-visible']).toBe('true');

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                    iframe.nativeElement.contentWindow.addEventListener(
                        'message',
                        (event: MessageEvent) => {
                            expect(event).toBeTruthy();
                            done();
                        }
                    );

                    triggerCustomEvent(dialog, 'action', {
                        event: new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                payload: {}
                            }
                        })
                    });

                    spectator.detectChanges();
                });

                it('should notify block-editor-sidebar to enable editing', () => {
                    const spy = jest.spyOn(spectator.component.blockSidebar, 'open');

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.INIT_INLINE_EDITING,
                                payload: {
                                    type: 'BLOCK_EDITOR',
                                    data: {}
                                }
                            }
                        })
                    );

                    spectator.detectComponentChanges();

                    expect(spy).toHaveBeenCalledWith({});
                });

                it('should show a message and not notify the event if there is not enterprise lincese', () => {
                    const spyAlert = jest.spyOn(dotAlertConfirmService, 'alert');

                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));

                    spectator.detectChanges();

                    store.loadPageAsset({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    spectator.detectChanges();

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.INIT_INLINE_EDITING,
                                payload: {}
                            }
                        })
                    );

                    spectator.detectComponentChanges();

                    expect(spyAlert).toHaveBeenCalledWith({
                        header: 'Enterprise Only',
                        message:
                            'Inline editing is available only with an enterprise license. Please contact support to upgrade your license.'
                    });
                });

                describe('reorder navigation', () => {
                    it('should open a dialog to reorder the navigation', () => {
                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: DotCMSUVEAction.REORDER_MENU,
                                    payload: {
                                        startLevel: 1,
                                        depth: 2
                                    }
                                }
                            })
                        );

                        spectator.detectComponentChanges();

                        const dialog = spectator.debugElement.query(
                            By.css("[data-testId='ema-dialog']")
                        );

                        const pDialog = dialog.query(By.css('p-dialog'));

                        expect(pDialog.attributes['ng-reflect-visible']).toBe('true');
                    });

                    it('should reload the page after saving the new navigation order', () => {
                        const reloadSpy = jest.spyOn(store, 'reloadCurrentPage');
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

                        expect(pDialog.attributes['ng-reflect-visible']).toBe('false');
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

                    it('should close the dialog if the users cancel the reorder action', () => {
                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: DotCMSUVEAction.REORDER_MENU,
                                    payload: {
                                        startLevel: 1,
                                        depth: 2
                                    }
                                }
                            })
                        );

                        spectator.detectComponentChanges();

                        let dialog = spectator.debugElement.query(
                            By.css("[data-testId='ema-dialog']")
                        );

                        let pDialog = dialog.query(By.css('p-dialog'));

                        expect(pDialog.attributes['ng-reflect-visible']).toBe('true');

                        dialog = spectator.debugElement.query(By.css("[data-testId='ema-dialog']"));

                        triggerCustomEvent(dialog, 'action', {
                            event: new CustomEvent('ng-event', {
                                detail: {
                                    name: NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER
                                }
                            })
                        });

                        pDialog = dialog.query(By.css('p-dialog'));

                        expect(pDialog.attributes['ng-reflect-visible']).toBe('false');
                    });

                    afterEach(() => jest.clearAllMocks());
                });

                xdescribe('reload', () => {
                    let spyContentlet: jest.SpyInstance;
                    let spyDialog: jest.SpyInstance;
                    let spyReloadIframe: jest.SpyInstance;
                    let spyStoreReload: jest.SpyInstance;
                    let spyUpdateQueryParams: jest.SpyInstance;

                    const emulateEditURLMapContent = () => {
                        const editURLContentButton = spectator.debugElement.query(
                            By.css('[data-testId="edit-url-content-map"]')
                        );
                        const dialog = spectator.debugElement.query(
                            By.css('[data-testId="ema-dialog"]')
                        );

                        store.setEditorContentletArea(baseContentletPayload);

                        editURLContentButton.triggerEventHandler('onClick', {});

                        triggerCustomEvent(dialog, 'action', {
                            event: new CustomEvent('ng-event', {
                                detail: {
                                    name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                    payload: {
                                        shouldReloadPage: true,
                                        contentletIdentifier: URL_MAP_CONTENTLET.identifier,
                                        htmlPageReferer: '/my-awesome-page'
                                    }
                                }
                            })
                        });
                    };

                    beforeEach(() => {
                        const router = spectator.inject(Router, true);
                        const dialog = spectator.component.dialog;
                        spyContentlet = jest.spyOn(dotContentletService, 'getContentletByInode');
                        spyDialog = jest.spyOn(dialog, 'editUrlContentMapContentlet');
                        spyReloadIframe = jest.spyOn(spectator.component, 'reloadIframeContent');
                        spyUpdateQueryParams = jest.spyOn(router, 'navigate');
                        spyStoreReload = jest.spyOn(store, 'reloadCurrentPage');

                        spectator.detectChanges();
                    });

                    it('should reload the page after editing a urlContentMap if the url do not change', () => {
                        const storeReloadPayload = {
                            params: {
                                language_id: 1,
                                url: 'page-one'
                            }
                        };

                        spyContentlet.mockReturnValue(
                            of({
                                ...URL_MAP_CONTENTLET,
                                URL_MAP_FOR_CONTENT: 'page-one'
                            })
                        );

                        emulateEditURLMapContent();
                        expect(spyContentlet).toHaveBeenCalledWith(URL_MAP_CONTENTLET.identifier);
                        expect(spyDialog).toHaveBeenCalledWith(URL_CONTENT_MAP_MOCK);
                        expect(spyReloadIframe).toHaveBeenCalled();
                        expect(spyStoreReload).toHaveBeenCalledWith(storeReloadPayload);
                        expect(spyUpdateQueryParams).not.toHaveBeenCalled();
                    });

                    it('should update the query params after editing a urlContentMap if the url changed', () => {
                        const SpyEditorState = jest.spyOn(store, 'setEditorState');
                        const queryParams = {
                            queryParams: {
                                url: URL_MAP_CONTENTLET.URL_MAP_FOR_CONTENT
                            },
                            queryParamsHandling: 'merge'
                        };

                        spyContentlet.mockReturnValue(of(URL_MAP_CONTENTLET));

                        emulateEditURLMapContent();
                        expect(spyDialog).toHaveBeenCalledWith(URL_CONTENT_MAP_MOCK);
                        expect(SpyEditorState).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
                        expect(spyContentlet).toHaveBeenCalledWith(URL_MAP_CONTENTLET.identifier);
                        expect(spyUpdateQueryParams).toHaveBeenCalledWith([], queryParams);
                        expect(spyStoreReload).not.toHaveBeenCalled();
                        expect(spyReloadIframe).toHaveBeenCalled();
                    });

                    it('should handler error ', () => {
                        const SpyEditorState = jest.spyOn(store, 'setEditorState');
                        const SpyHandlerError = jest
                            .spyOn(dotHttpErrorManagerService, 'handle')
                            .mockReturnValue(of(null));

                        spyContentlet.mockReturnValue(throwError({}));

                        emulateEditURLMapContent();
                        expect(spyDialog).toHaveBeenCalledWith(URL_CONTENT_MAP_MOCK);
                        expect(SpyHandlerError).toHaveBeenCalledWith({});
                        expect(SpyEditorState).toHaveBeenCalledWith(EDITOR_STATE.ERROR);
                        expect(spyContentlet).toHaveBeenCalledWith(URL_MAP_CONTENTLET.identifier);
                        expect(spyUpdateQueryParams).not.toHaveBeenCalled();
                        expect(spyStoreReload).not.toHaveBeenCalled();
                        expect(spyReloadIframe).not.toHaveBeenCalled();
                    });
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

                        store.setEditorContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            EmaContentletToolsComponent,
                            'edit',
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

                        store.setEditorContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            EmaContentletToolsComponent,
                            'edit',
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

                        store.setEditorContentletArea(CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            EmaContentletToolsComponent,
                            'edit',
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

                    it('should trigger copy contentlet dialog', () => {
                        store.setEditorContentletArea(CONTENTLET_MOCK);
                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
                                    payload: {
                                        inode: '123'
                                    }
                                }
                            })
                        );

                        spectator.detectComponentChanges();

                        expect(modalSpy).toHaveBeenCalled();
                    });
                });

                beforeEach(() => {
                    jest.clearAllMocks();
                });
            });

            describe('add', () => {
                it('should add contentlet after backend emit SAVE_CONTENTLET', () => {
                    spectator.detectChanges();

                    const savePageMock = jest.spyOn(store, 'savePage');

                    const payload: ActionPayload = { ...PAYLOAD_MOCK };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addContent',
                        payload
                    );

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

                    expect(savePageMock).toHaveBeenCalledWith(PAYLOAD_MOCK.pageContainers);

                    spectator.detectChanges();
                });

                it('should not add contentlet after backend emit SAVE_CONTENTLET and contentlet is dupe', () => {
                    spectator.detectChanges();

                    const payload: ActionPayload = { ...PAYLOAD_MOCK };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addContent',
                        payload
                    );

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
                    const saveMock = jest.spyOn(store, 'savePage');

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
                            contentletsId: ['123'],
                            variantId: '123'
                        },
                        pageId: 'test',
                        position: 'after'
                    };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addContent',
                        payload
                    );

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
                            contentletsId: ['contentlet-identifier-123'],
                            variantId: '123'
                        },
                        pageId: 'test',
                        position: 'before'
                    };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addContent',
                        payload
                    );

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
                    const saveMock = jest.spyOn(store, 'savePage');

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
                            contentletsId: ['123'],
                            variantId: '123'
                        },
                        pageId: 'test',
                        position: 'after'
                    };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addWidget',
                        payload
                    );

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
                            contentletsId: ['contentlet-identifier-123'],
                            variantId: '123'
                        },
                        pageId: 'test',
                        position: 'before'
                    };

                    store.setEditorContentletArea({
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'addWidget',
                        payload
                    );

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

            describe('drag and drop', () => {
                describe('drag start', () => {
                    it('should call the setEditorDragItem from the store for content-types and set the `dotcms/item` type ', () => {
                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');
                        const dataTransfer = {
                            writable: false,
                            value: {
                                setData: jest.fn()
                            }
                        };

                        const target = {
                            target: {
                                dataset: {
                                    type: 'content-type',
                                    item: JSON.stringify({
                                        contentType: {
                                            variable: 'test',
                                            name: 'test',
                                            baseType: 'test'
                                        },
                                        move: false
                                    })
                                }
                            }
                        };

                        const dragStart = new Event('dragstart');

                        Object.defineProperty(dragStart, 'target', {
                            writable: false,
                            value: target.target
                        });

                        Object.defineProperty(dragStart, 'dataTransfer', dataTransfer);

                        window.dispatchEvent(dragStart);

                        expect(setEditorDragItemSpy).toHaveBeenCalledWith({
                            baseType: 'test',
                            contentType: 'test',
                            draggedPayload: {
                                item: {
                                    variable: 'test',
                                    name: 'test'
                                },
                                type: 'content-type',
                                move: false
                            }
                        });

                        expect(dataTransfer.value.setData).toHaveBeenCalledWith('dotcms/item', '');
                    });

                    it('should call the setEditorDragItem from the store for contentlets', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');

                        const target = {
                            target: {
                                dataset: {
                                    type: 'contentlet',
                                    item: JSON.stringify({
                                        contentlet,
                                        move: false
                                    })
                                }
                            }
                        };

                        const dragStart = new Event('dragstart');

                        Object.defineProperty(dragStart, 'target', {
                            writable: false,
                            value: target.target
                        });

                        Object.defineProperty(dragStart, 'data', {
                            writable: false,
                            value: target.target
                        });

                        window.dispatchEvent(dragStart);

                        expect(setEditorDragItemSpy).toHaveBeenCalledWith({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    contentlet
                                },
                                type: 'contentlet',
                                move: false
                            }
                        });
                    });

                    it('should call the setEditorDragItem from the store for contentlets and move', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const container = {
                            acceptTypes:
                                'CallToAction,webPageContent,calendarEvent,Image,Product,Video,dotAsset,Blog,Banner,Activity,WIDGET,FORM',
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            maxContentlets: '25',
                            uuid: '2',
                            contentletsId: [
                                '4694d40b-d9be-4e09-b031-64ee3e7c9642',
                                '6ac5921e-e062-49a6-9808-f41aff9343c5'
                            ]
                        };

                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');

                        const target = {
                            target: {
                                dataset: {
                                    type: 'contentlet',
                                    item: JSON.stringify({
                                        contentlet,
                                        container,
                                        move: true
                                    })
                                }
                            }
                        };

                        const dragStart = new Event('dragstart');

                        Object.defineProperty(dragStart, 'target', {
                            writable: false,
                            value: target.target
                        });

                        window.dispatchEvent(dragStart);

                        expect(setEditorDragItemSpy).toHaveBeenCalledWith({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    contentlet,
                                    container
                                },
                                type: 'contentlet',
                                move: true
                            }
                        });
                    });

                    it('should not call the setEditorDragItem if it is an invalid drag item', () => {
                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');
                        const dragStart = new Event('dragstart');
                        const target = {
                            target: {
                                dataset: {}
                            }
                        };
                        const dataTransfer = {
                            writable: false,
                            value: {
                                setData: jest.fn()
                            }
                        };

                        Object.defineProperty(dragStart, 'dataTransfer', dataTransfer);
                        Object.defineProperty(dragStart, 'target', {
                            writable: false,
                            value: target.target
                        });

                        window.dispatchEvent(dragStart);
                        expect(setEditorDragItemSpy).not.toHaveBeenCalled();
                        expect(dataTransfer.value.setData).toHaveBeenCalledWith('dotcms/item', '');
                    });
                });

                describe('drag over', () => {
                    it('should prevent default to avoid opening files', () => {
                        store.setEditorDragItem(TEMPORAL_DRAG_ITEM);
                        const dragOver = new Event('dragover');
                        const preventDefaultSpy = jest.spyOn(dragOver, 'preventDefault');

                        window.dispatchEvent(dragOver);

                        expect(preventDefaultSpy).toHaveBeenCalled();
                    });
                });

                describe('drag end', () => {
                    it('should reset the editor properties when dropEffect is none', () => {
                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        const dragEnd = new Event('dragend');

                        Object.defineProperty(dragEnd, 'dataTransfer', {
                            writable: false,
                            value: {
                                dropEffect: 'none'
                            }
                        });

                        window.dispatchEvent(dragEnd);

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });
                    it('should not reset the editor properties when dropEffect is not none', () => {
                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        const dragEnd = new Event('dragend');

                        Object.defineProperty(dragEnd, 'dataTransfer', {
                            writable: false,
                            value: {
                                dropEffect: 'copy'
                            }
                        });

                        window.dispatchEvent(dragEnd);

                        expect(resetEditorPropertiesSpy).not.toHaveBeenCalled();
                    });
                });

                describe('drag leave', () => {
                    const createDragLeaveEvent = () => {
                        const dragLeave = new Event('dragleave');
                        Object.defineProperties(dragLeave, {
                            x: { value: 0 },
                            y: { value: 0 },
                            relatedTarget: { value: undefined } // this is undefined when the mouse leaves the window
                        });

                        return dragLeave;
                    };

                    beforeEach(() => store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK));

                    it('should reset editor properties', () => {
                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');
                        const dragLeave = createDragLeaveEvent();
                        window.dispatchEvent(dragLeave);
                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });
                });

                describe('drag enter', () => {
                    it('should call the event prevent default to prevent file opening', () => {
                        const dragEnter = new Event('dragenter');

                        const preventDefaultSpy = jest.spyOn(dragEnter, 'preventDefault');

                        Object.defineProperty(dragEnter, 'fromElement', {
                            writable: false,
                            value: undefined
                        }); // fromElement is falsy when the mouse enters the window

                        window.dispatchEvent(dragEnter);

                        expect(preventDefaultSpy).toHaveBeenCalled();
                    });

                    it('should set the dragItem if there is no dragItem', () => {
                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');

                        const dragEnter = new Event('dragenter');

                        Object.defineProperty(dragEnter, 'fromElement', {
                            writable: false,
                            value: undefined
                        }); // fromElement is falsy when the mouse enters the window

                        window.dispatchEvent(dragEnter);

                        expect(setEditorDragItemSpy).toHaveBeenCalledWith({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        });
                    });

                    it('should set the editor to DRAGGING if there is dragItem and the state is IDLE', () => {
                        store.setEditorDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        }); // Simulate drag start

                        store.setEditorState(EDITOR_STATE.IDLE); // Simulate drag leave

                        const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                        const dragEnter = new Event('dragenter');

                        Object.defineProperty(dragEnter, 'fromElement', {
                            writable: false,
                            value: undefined
                        }); // fromElement is falsy when the mouse enters the window

                        window.dispatchEvent(dragEnter);

                        expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.DRAGGING);
                    });

                    it('should set ignore drag events if the file type is `dotcms/item`', () => {
                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');
                        const setEditorStateSpy = jest.spyOn(store, 'setEditorState');
                        const dragEnter = new Event('dragenter');

                        Object.defineProperty(dragEnter, 'dataTransfer', {
                            writable: false,
                            value: {
                                types: ['dotcms/item']
                            }
                        });

                        window.dispatchEvent(dragEnter);

                        expect(store.state()).toBe(EDITOR_STATE.IDLE);
                        expect(setEditorDragItemSpy).not.toHaveBeenCalled();
                        expect(setEditorStateSpy).not.toHaveBeenCalled();
                    });
                });

                describe('drop', () => {
                    it("should call prevent default to avoid opening files when it's not a contentlet", () => {
                        const drop = new Event('drop');

                        const preventDefaultSpy = jest.spyOn(drop, 'preventDefault');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'false'
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(preventDefaultSpy).toHaveBeenCalled();
                    });
                    it('should update the editor state when the drop is not in a dropzone', () => {
                        const drop = new Event('drop');

                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'false'
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });

                    it('should do the place item flow when dropping a contentlet and is not moving', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const savePageSpy = jest.spyOn(store, 'savePage');

                        store.setEditorDragItem({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    contentlet
                                },
                                type: 'contentlet',
                                move: false
                            }
                        });

                        const drop = new Event('drop');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'true',
                                    position: 'before',
                                    payload: JSON.stringify({
                                        container: {
                                            acceptTypes: 'Banner,Activity',
                                            identifier: '123',
                                            maxContentlets: 25,
                                            variantId: 'DEFAULT',
                                            uuid: '123'
                                        },
                                        contentlet: {
                                            identifier: '456',
                                            title: 'Explore the World',
                                            inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                            contentType: 'Banner'
                                        }
                                    })
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(savePageSpy).toHaveBeenCalledWith([
                            {
                                identifier: '123',
                                uuid: '123',
                                personaTag: 'dot:persona',
                                contentletsId: ['123', contentlet.identifier, '456'] // Before 456
                            },
                            {
                                identifier: '123',
                                uuid: '456',
                                personaTag: 'dot:persona',
                                contentletsId: ['123']
                            }
                        ]);
                    });

                    it('should handle duplicated content', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const savePapeSpy = jest.spyOn(store, 'savePage');

                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        store.setEditorDragItem({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    contentlet: {
                                        ...contentlet,
                                        identifier: '123' // Already added
                                    }
                                },
                                type: 'contentlet',
                                move: false
                            }
                        });

                        const drop = new Event('drop');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'true',
                                    position: 'before',
                                    payload: JSON.stringify({
                                        container: {
                                            acceptTypes: 'Banner,Activity',
                                            identifier: '123',
                                            maxContentlets: 25,
                                            variantId: 'DEFAULT',
                                            uuid: '123'
                                        },
                                        contentlet: {
                                            identifier: '456',
                                            title: 'Explore the World',
                                            inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                            contentType: 'Banner'
                                        }
                                    })
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(savePapeSpy).not.toHaveBeenCalled();

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            detail: 'This content is already added to this container',
                            life: 2000,
                            severity: 'info',
                            summary: 'Content already added'
                        });

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });

                    it('should do the place item flow when dropping a contentlet and is moving', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const savePapeSpy = jest.spyOn(store, 'savePage');

                        store.setEditorDragItem({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    // Moving contentlet
                                    contentlet: {
                                        ...contentlet,
                                        identifier: '456' // Existent one
                                    },
                                    // Move it from this container
                                    container: {
                                        acceptTypes: 'Banner,Activity',
                                        identifier: '123',
                                        maxContentlets: 25,
                                        variantId: 'DEFAULT',
                                        uuid: '123'
                                    }
                                },
                                type: 'contentlet',
                                move: true
                            }
                        });

                        const drop = new Event('drop');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'true',
                                    position: 'before',
                                    payload: JSON.stringify({
                                        // Container where we dropped
                                        container: {
                                            acceptTypes: 'Banner,Activity',
                                            identifier: '123',
                                            maxContentlets: 25,
                                            variantId: 'DEFAULT',
                                            uuid: '456'
                                        },
                                        // Pivot contentlet
                                        contentlet: {
                                            identifier: '123',
                                            title: 'Explore the World',
                                            inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                            contentType: 'Banner'
                                        }
                                    })
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(savePapeSpy).toHaveBeenCalledWith([
                            {
                                identifier: '123',
                                uuid: '123',
                                personaTag: 'dot:persona',
                                contentletsId: ['123']
                            },
                            {
                                identifier: '123',
                                uuid: '456',
                                personaTag: 'dot:persona',
                                contentletsId: ['456', '123'] // before pivot contentlet
                            }
                        ]);
                    });

                    it('should handle duplicated content when moving', () => {
                        const contentlet = CONTENTLETS_MOCK[0];

                        const savePageSpy = jest.spyOn(store, 'savePage');
                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        store.setEditorDragItem({
                            baseType: contentlet.baseType,
                            contentType: contentlet.contentType,
                            draggedPayload: {
                                item: {
                                    // Moving contentlet
                                    contentlet: {
                                        ...contentlet,
                                        identifier: '123' // Existent one
                                    },
                                    // Move it from this container
                                    container: {
                                        acceptTypes: 'Banner,Activity',
                                        identifier: '123',
                                        maxContentlets: 25,
                                        variantId: 'DEFAULT',
                                        uuid: '123'
                                    }
                                },
                                type: 'contentlet',
                                move: true
                            }
                        });

                        const drop = new Event('drop');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'true',
                                    position: 'before',
                                    payload: JSON.stringify({
                                        // Container where we dropped
                                        container: {
                                            acceptTypes: 'Banner,Activity',
                                            identifier: '123',
                                            maxContentlets: 25,
                                            variantId: 'DEFAULT',
                                            uuid: '456'
                                        },
                                        // Pivot contentlet
                                        contentlet: {
                                            identifier: '123',
                                            title: 'Explore the World',
                                            inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                            contentType: 'Banner'
                                        }
                                    })
                                }
                            }
                        });

                        window.dispatchEvent(drop);
                        expect(savePageSpy).not.toHaveBeenCalled();

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            detail: 'This content is already added to this container',
                            life: 2000,
                            severity: 'info',
                            summary: 'Content already added'
                        });

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });

                    it('should open dialog when dropping a content-type', () => {
                        const contentType = CONTENT_TYPE_MOCK[0];

                        jest.spyOn(store, 'setEditorState');

                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        store.setEditorDragItem({
                            baseType: contentType.baseType,
                            contentType: contentType.variable,
                            draggedPayload: {
                                item: {
                                    variable: contentType.variable,
                                    name: contentType.name
                                },
                                type: 'content-type',
                                move: false
                            } as ContentTypeDragPayload
                        });

                        const drop = new Event('drop');

                        Object.defineProperty(drop, 'target', {
                            writable: false,
                            value: {
                                dataset: {
                                    dropzone: 'true',
                                    position: 'before',
                                    payload: JSON.stringify({
                                        // Container where we dropped
                                        container: {
                                            acceptTypes: 'Banner,Activity',
                                            identifier: '123',
                                            maxContentlets: 25,
                                            variantId: 'DEFAULT',
                                            uuid: '456'
                                        },
                                        // Pivot contentlet
                                        contentlet: {
                                            identifier: '123',
                                            title: 'Explore the World',
                                            inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                            contentType: 'Banner'
                                        }
                                    })
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        spectator.detectChanges();

                        const dialog = spectator.debugElement.query(
                            By.css('[data-testId="dialog"]')
                        );

                        expect(dialog.attributes['ng-reflect-visible']).toBe('true');
                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });

                    it('should advice and reset the state to IDLE when the dropped file is not an image', () => {
                        const drop = new Event('drop');

                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        store.setEditorDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        });

                        Object.defineProperties(drop, {
                            dataTransfer: {
                                writable: false,
                                value: {
                                    files: [new File([''], 'test.pdf', { type: 'application/pdf' })]
                                }
                            },
                            target: {
                                value: {
                                    dataset: {
                                        dropzone: 'true',
                                        position: 'before',
                                        payload: JSON.stringify({
                                            container: {
                                                acceptTypes: 'Banner,Activity,DotAsset',
                                                identifier: '123',
                                                maxContentlets: 25,
                                                variantId: 'DEFAULT',
                                                uuid: '456'
                                            }
                                        })
                                    }
                                }
                            }
                        });

                        window.dispatchEvent(drop);

                        expect(addMessageSpy).toHaveBeenCalledWith({
                            severity: 'error',
                            summary: 'file-upload',
                            detail: 'editpage.file.upload.not.image',
                            life: 3000
                        });

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });

                    it('should add an image successfully', () => {
                        const drop = new Event('drop');
                        const savePageSpy = jest.spyOn(store, 'savePage');

                        store.setEditorDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        });

                        jest.spyOn(dotTempFileUploadService, 'upload')
                            .mockReset()
                            .mockReturnValueOnce(
                                of([
                                    {
                                        image: true,
                                        id: 'temp_file_test'
                                    }
                                ] as DotCMSTempFile[])
                            );

                        jest.spyOn(
                            dotWorkflowActionsFireService,
                            'publishContentletAndWaitForIndex'
                        ).mockReturnValue(
                            of({
                                identifier: '789',
                                inode: '123',
                                title: 'test',
                                contentType: 'dotAsset',
                                baseType: 'IMAGE'
                            })
                        );

                        Object.defineProperties(drop, {
                            dataTransfer: {
                                writable: false,
                                value: {
                                    files: [new File([''], 'test.png', { type: 'image/png' })]
                                }
                            },
                            target: {
                                value: {
                                    dataset: {
                                        dropzone: 'true',
                                        position: 'before',
                                        payload: JSON.stringify({
                                            container: {
                                                acceptTypes: 'Banner,Activity,DotAsset',
                                                identifier: '123',
                                                maxContentlets: 25,
                                                variantId: 'DEFAULT',
                                                uuid: '456'
                                            },
                                            contentlet: {
                                                identifier: '123',
                                                title: 'Explore the World',
                                                inode: 'bef551b3-77ae-4dc8-a030-fe27a2ac056f',
                                                contentType: 'Banner'
                                            }
                                        })
                                    }
                                }
                            }
                        });

                        window.dispatchEvent(drop);
                        expect(addMessageSpy).toHaveBeenNthCalledWith(1, {
                            severity: 'info',
                            summary: 'upload-image',
                            detail: 'editpage.file.uploading',
                            life: 3000
                        });

                        expect(addMessageSpy).toHaveBeenNthCalledWith(2, {
                            severity: 'info',
                            summary: 'Workflow-Action',
                            detail: 'editpage.file.publishing',
                            life: 3000
                        });

                        expect(savePageSpy).toHaveBeenCalledWith([
                            {
                                contentletsId: ['123', '456'],
                                identifier: '123',
                                personaTag: 'dot:persona',
                                uuid: '123'
                            },
                            {
                                contentletsId: ['789', '123'], // image inserted before
                                identifier: '123',
                                personaTag: 'dot:persona',
                                uuid: '456'
                            }
                        ]);
                    });

                    it('should advice and reset editor properties when the dropped image failed uploading ', () => {
                        const drop = new Event('drop');
                        jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValueOnce(
                            of([
                                {
                                    image: null,
                                    id: 'temp_file_test'
                                }
                            ] as DotCMSTempFile[])
                        );

                        store.setEditorDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        });

                        const resetEditorPropertiesSpy = jest.spyOn(store, 'resetEditorProperties');

                        Object.defineProperties(drop, {
                            dataTransfer: {
                                writable: false,
                                value: {
                                    files: [new File([''], 'test.png', { type: 'image/png' })]
                                }
                            },
                            target: {
                                value: {
                                    dataset: {
                                        dropzone: 'true',
                                        position: 'before',
                                        payload: JSON.stringify({
                                            container: {
                                                acceptTypes: 'Banner,Activity,DotAsset',
                                                identifier: '123',
                                                maxContentlets: 25,
                                                variantId: 'DEFAULT',
                                                uuid: '456'
                                            }
                                        })
                                    }
                                }
                            }
                        });

                        window.dispatchEvent(drop);
                        expect(addMessageSpy).toHaveBeenNthCalledWith(1, {
                            severity: 'info',
                            summary: 'upload-image',
                            detail: 'editpage.file.uploading',
                            life: 3000
                        });

                        expect(addMessageSpy).toHaveBeenNthCalledWith(2, {
                            severity: 'error',
                            summary: 'upload-image',
                            detail: 'editpage.file.upload.error',
                            life: 3000
                        });

                        expect(resetEditorPropertiesSpy).toHaveBeenCalled();
                    });
                });
            });

            describe('scroll inside iframe', () => {
                beforeEach(() => store.setEditorDragItem(TEMPORAL_DRAG_ITEM));

                it('should emit postMessage and change state to Scroll', () => {
                    const dragOver = new Event('dragover');

                    Object.defineProperty(dragOver, 'clientY', { value: 200, enumerable: true });
                    Object.defineProperty(dragOver, 'clientX', { value: 120, enumerable: true });

                    const postMessageSpy = jest.spyOn(
                        spectator.component.iframe.nativeElement.contentWindow,
                        'postMessage'
                    );

                    const updateEditorScrollDragStateSpy = jest.spyOn(
                        store,
                        'updateEditorScrollDragState'
                    );

                    jest.spyOn(
                        spectator.component.iframe.nativeElement,
                        'getBoundingClientRect'
                    ).mockReturnValue({
                        top: 150,
                        bottom: 700,
                        left: 100,
                        right: 500
                    } as DOMRect);

                    window.dispatchEvent(dragOver);
                    spectator.detectChanges();
                    expect(postMessageSpy).toHaveBeenCalled();
                    expect(updateEditorScrollDragStateSpy).toHaveBeenCalled();
                });

                it('should reset state to dragging when drag outside iframe', () => {
                    const dragOver = new Event('dragover');

                    Object.defineProperty(dragOver, 'clientY', { value: 200, enumerable: true });
                    Object.defineProperty(dragOver, 'clientX', { value: 90, enumerable: true });

                    const setEditorState = jest.spyOn(store, 'setEditorState');

                    jest.spyOn(
                        spectator.component.iframe.nativeElement,
                        'getBoundingClientRect'
                    ).mockReturnValue({
                        top: 150,
                        bottom: 700,
                        left: 100,
                        right: 500
                    } as DOMRect);

                    window.dispatchEvent(dragOver);
                    spectator.detectChanges();
                    expect(setEditorState).toHaveBeenCalledWith(EDITOR_STATE.DRAGGING);
                });

                it('should change state to dragging when drag outsite scroll trigger area', () => {
                    const dragOver = new Event('dragover');

                    Object.defineProperty(dragOver, 'clientY', { value: 300, enumerable: true });
                    Object.defineProperty(dragOver, 'clientX', { value: 120, enumerable: true });

                    const setEditorState = jest.spyOn(store, 'setEditorState');

                    jest.spyOn(
                        spectator.component.iframe.nativeElement,
                        'getBoundingClientRect'
                    ).mockReturnValue({
                        top: 150,
                        bottom: 700,
                        left: 100,
                        right: 500
                    } as DOMRect);

                    window.dispatchEvent(dragOver);
                    spectator.detectChanges();
                    expect(setEditorState).toHaveBeenCalledWith(EDITOR_STATE.DRAGGING);
                });
            });

            describe('DOM', () => {
                it('should not show a loader when client is ready and UVE is not loading', () => {
                    store.setIsClientReady(true);
                    store.setUveStatus(UVE_STATUS.LOADED);
                    spectator.detectChanges();

                    const progressbar = spectator.query(byTestId('progress-bar'));

                    expect(progressbar).toBeNull();
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

                it('iframe should have the correct src when is HEADLESS', () => {
                    spectator.detectChanges();

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                    expect(iframe.nativeElement.src).toBe(
                        'http://localhost:3000/index?language_id=1&mode=EDIT_MODE&dotCMSHost=http://localhost'
                    );
                });

                describe('VTL Page', () => {
                    beforeEach(() => {
                        jest.useFakeTimers(); // Mock the timers
                        spectator.detectChanges();

                        store.loadPageAsset({
                            url: 'index',
                            language_id: '3',
                            [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                            clientHost: undefined
                        });
                    });

                    it('iframe should have the correct content when is VTL', () => {
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

                    it('iframe should have reload the page and add the new content, maintaining scroll', () => {
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

                        store.loadPageAsset({
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

                it('should navigate to new url and change persona when postMessage SET_URL', () => {
                    const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');

                    spectator.detectChanges();

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'set-url',
                                payload: {
                                    url: '/some'
                                }
                            }
                        })
                    );

                    expect(spyloadPageAsset).toHaveBeenCalledWith({
                        url: '/some',
                        [PERSONA_KEY]: 'modes.persona.no.persona'
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

                it('set url to a different route should set the editor state to loading', () => {
                    const spyloadPageAsset = jest.spyOn(store, 'loadPageAsset');

                    spectator.detectChanges();

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'set-url',
                                payload: {
                                    url: '/some'
                                }
                            }
                        })
                    );

                    expect(spyloadPageAsset).toHaveBeenCalledWith({
                        [PERSONA_KEY]: 'modes.persona.no.persona',
                        url: '/some'
                    });
                });

                it('set url to the same route should set the editor state to IDLE', () => {
                    const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                    const url = "/ultra-cool-url-that-doesn't-exist";

                    store.loadPageAsset({
                        url,
                        language_id: '5',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    spectator.detectChanges();
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'set-url',
                                payload: {
                                    url
                                }
                            }
                        })
                    );

                    expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
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
                    store.loadPageAsset({
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
                            store.loadPageAsset({ url: 'index', clientHost: null });
                        });

                        it('should add script and styles to iframe', () => {
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
                            store.loadPageAsset({ url: 'index', clientHost: null });
                        });

                        it('should add script and styles to iframe for advance templates', () => {
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

            describe('inline editing', () => {
                it('should save from inline edited contentlet', () => {
                    const saveContentletSpy = jest.spyOn(dotPageApiService, 'saveContentlet');

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                                payload: {
                                    dataset: {
                                        inode: '123',
                                        fieldName: 'title',
                                        mode: 'full',
                                        language: '1'
                                    },
                                    content: 'Hello World',
                                    element: {},
                                    eventType: '',
                                    isNotDirty: false
                                }
                            }
                        })
                    );

                    expect(saveContentletSpy).toHaveBeenCalledWith({
                        contentlet: {
                            inode: '123',
                            title: 'Hello World'
                        }
                    });
                });

                it('should not trigger save from inline edited contentlet when dont have changes', () => {
                    const saveContentletSpy = jest
                        .spyOn(dotPageApiService, 'saveContentlet')
                        .mockClear();

                    const setEditorState = jest.spyOn(store, 'setEditorState');

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                                payload: null
                            }
                        })
                    );

                    expect(saveContentletSpy).not.toHaveBeenCalled();
                    expect(setEditorState).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
                });

                it('should show a helper message when save content when inline editing', () => {
                    const messageSpy = jest.spyOn(messageService, 'add');

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                                payload: {
                                    dataset: {
                                        inode: '123',
                                        fieldName: 'title',
                                        mode: 'full',
                                        language: '1'
                                    },
                                    content: 'Hello World II',
                                    element: {},
                                    eventType: '',
                                    isNotDirty: false
                                }
                            }
                        })
                    );

                    expect(messageSpy).toHaveBeenCalledWith({
                        severity: 'success',
                        summary: 'Content saved',
                        detail: 'Note: If you edit auto-published content, changes apply immediately.',
                        life: 2000
                    });
                });
            });

            describe('CUSTOMER ACTIONS', () => {
                describe('CLIENT_READY', () => {
                    it('should set client GraphQL configuration and call the reload', () => {
                        const setClientConfigurationSpy = jest.spyOn(store, 'setCustomGraphQL');
                        const reloadSpy = jest.spyOn(store, 'reloadCurrentPage');

                        const config = {
                            query: '{ query: { hello } }',
                            variables: undefined
                        };

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: DotCMSUVEAction.CLIENT_READY,
                                    payload: config
                                }
                            })
                        );

                        expect(setClientConfigurationSpy).toHaveBeenCalledWith(config, true);
                        expect(reloadSpy).toHaveBeenCalled();
                    });

                    it('should set call reloadCurrentPage when client is ready', () => {
                        const setCustomGraphQLSpy = jest.spyOn(store, 'setCustomGraphQL');
                        const reloadSpy = jest.spyOn(store, 'reloadCurrentPage');

                        const config = { params: { depth: '1' } };

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: DotCMSUVEAction.CLIENT_READY,
                                    payload: config
                                }
                            })
                        );

                        expect(setCustomGraphQLSpy).not.toHaveBeenCalled();
                        expect(reloadSpy).toHaveBeenCalled();
                    });
                });
            });

            describe('language selected', () => {
                it('should update the URL and language when the user create a new translation changing the URL', () => {
                    store.loadPageAsset({
                        clientHost: 'http://localhost:3000',
                        url: 'index',
                        language_id: '2',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    const loadPageAssetSpy = jest.spyOn(store, 'loadPageAsset');

                    spectator.detectChanges();
                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(
                        EmaContentletToolsComponent,
                        'edit',
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

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
                        url: '/new-url-here',
                        language_id: '1'
                    });
                });

                it('should update the language when the user create a new translation', () => {
                    store.loadPageAsset({
                        clientHost: 'http://localhost:3000',
                        url: 'test-url',
                        language_id: '1',
                        [PERSONA_KEY]: DEFAULT_PERSONA.identifier
                    });

                    const loadPageAssetSpy = jest.spyOn(store, 'loadPageAsset');
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

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
                        language_id: '2'
                    });
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
                    store.setSEO('test');
                    spectator.detectChanges();
                    const editorContent = spectator.query(
                        byTestId('editor-content')
                    ) as HTMLElement;
                    expect(editorContent.style.display).toBe('none');
                });
            });

            describe('handleInternalNav', () => {
                let loadPageAssetSpy: jest.SpyInstance;
                let windowOpenSpy: jest.SpyInstance;

                beforeEach(() => {
                    loadPageAssetSpy = jest.spyOn(store, 'loadPageAsset');
                    windowOpenSpy = jest.spyOn(window, 'open').mockImplementation();

                    // Mock location.origin
                    Object.defineProperty(window, 'location', {
                        value: {
                            origin: 'http://localhost:3000',
                            hostname: 'localhost'
                        },
                        writable: true
                    });
                });

                const createMockEvent = (href: string, isInlineEditing = false): MouseEvent => {
                    const mockAnchor = {
                        href,
                        getAttribute: jest.fn().mockReturnValue(href),
                        closest: jest.fn().mockReturnValue({ getAttribute: () => href })
                    };

                    const mockEvent = {
                        target: mockAnchor,
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    // Mock the store state for inline editing
                    jest.spyOn(store, 'state').mockReturnValue(
                        isInlineEditing ? EDITOR_STATE.INLINE_EDITING : EDITOR_STATE.IDLE
                    );

                    return mockEvent;
                };

                it('should not do anything if href is empty', () => {
                    const mockEvent = {
                        target: { href: '', closest: () => null },
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    jest.spyOn(store, 'state').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(loadPageAssetSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should not do anything if isInlineEditing is true', () => {
                    const mockEvent = createMockEvent('/test-page', true);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(loadPageAssetSpy).not.toHaveBeenCalled();
                    expect(mockEvent.preventDefault).not.toHaveBeenCalled();
                });

                it('should open external URL in new tab', () => {
                    const externalUrl = 'https://external-site.com/page';
                    const mockEvent = createMockEvent(externalUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(windowOpenSpy).toHaveBeenCalledWith(externalUrl, '_blank');
                    expect(loadPageAssetSpy).not.toHaveBeenCalled();
                });

                it('should load page asset with pathname only for internal URL without query params', () => {
                    const internalUrl = 'http://localhost:3000/test-page';
                    const mockEvent = createMockEvent(internalUrl);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
                        url: '/test-page'
                    });
                    expect(mockEvent.preventDefault).toHaveBeenCalled();
                });

                it('should extract and pass query parameters from URL', () => {
                    const urlWithParams =
                        'http://localhost:3000/test-page?param1=value1&param2=value2';
                    const mockEvent = createMockEvent(urlWithParams);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
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

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
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

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
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

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
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
                                getAttribute: jest
                                    .fn()
                                    .mockReturnValue('http://localhost:3000/fallback-page?test=123')
                            })
                        },
                        preventDefault: jest.fn()
                    } as unknown as MouseEvent;

                    jest.spyOn(store, 'state').mockReturnValue(EDITOR_STATE.IDLE);

                    spectator.component.handleInternalNav(mockEvent);

                    expect(loadPageAssetSpy).toHaveBeenCalledWith({
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
