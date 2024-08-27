import { describe, expect, it } from '@jest/globals';
import {
    SpectatorRouting,
    createRoutingFactory,
    byTestId,
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

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotContentletLockerService,
    DotContentletService,
    DotCopyContentService,
    DotCurrentUserService,
    DotDevicesService,
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    LoginService
} from '@dotcms/dotcms-js';
import { DotCMSContentlet, DEFAULT_VARIANT_ID, DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotCopyContentModalService, ModelCopyContentResponse, SafeUrlPipe } from '@dotcms/ui';
import {
    DotLanguagesServiceMock,
    MockDotMessageService,
    DotDevicesServiceMock,
    mockDotDevices,
    LoginServiceMock,
    DotCurrentUserServiceMock,
    seoOGTagsResultMock,
    URL_MAP_CONTENTLET,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    getDraftExperimentMock,
    DotcmsConfigServiceMock,
    DotcmsEventsServiceMock,
    DotPersonalizeServiceMock,
    MockDotHttpErrorManagerService
} from '@dotcms/utils-testing';

import { DotEditEmaWorkflowActionsComponent } from './components/dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { CONTENT_TYPE_MOCK } from './components/edit-ema-palette/components/edit-ema-palette-content-type/edit-ema-palette-content-type.component.spec';
import { CONTENTLETS_MOCK } from './components/edit-ema-palette/edit-ema-palette.component.spec';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW, HOST } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import {
    QUERY_PARAMS_MOCK,
    URL_CONTENT_MAP_MOCK,
    EDIT_ACTION_PAYLOAD_MOCK,
    TREE_NODE_MOCK,
    newContentlet,
    PAYLOAD_MOCK,
    UVE_PAGE_RESPONSE_MAP
} from '../shared/mocks';
import { ActionPayload, ContentTypeDragPayload } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../utils';

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
    'editpage.content.add.already.message': 'This content is already added to this container'
};

const IFRAME_MOCK = {
    nativeElement: {
        contentDocument: {
            getElementsByTagName: () => [],
            querySelectorAll: () => [],
            write: function (html) {
                this.body.innerHTML = html;
            },
            body: {
                innerHTML: ''
            },
            open: jest.fn(),
            close: jest.fn()
        }
    }
};

const createRouting = () =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, HttpClientTestingModule, SafeUrlPipe, ConfirmDialogModule],
        declarations: [
            MockComponent(DotEditEmaWorkflowActionsComponent),
            MockComponent(DotResultsSeoToolComponent),
            MockComponent(DotEmaRunningExperimentComponent),
            MockComponent(EditEmaToolbarComponent)
        ],
        detectChanges: false,
        componentProviders: [
            ConfirmationService,
            MessageService,
            UVEStore,
            DotFavoritePageService,
            DotESContentService,
            {
                provide: DotAlertConfirmService,
                useValue: {
                    confirm: () => of({})
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
                    get({ language_id }) {
                        // We use the language_id to determine the response, use this to test different behaviors
                        return UVE_PAGE_RESPONSE_MAP[language_id];
                    },
                    getClientPage({ language_id }, _clientConfig) {
                        // We use the language_id to determine the response, use this to test different behaviors
                        return UVE_PAGE_RESPONSE_MAP[language_id];
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
            mockProvider(DotContentTypeService),
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
        let dotContentletService: DotContentletService;
        let dotHttpErrorManagerService: DotHttpErrorManagerService;
        let dotTempFileUploadService: DotTempFileUploadService;
        let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
        let router: Router;
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
                queryParams: { language_id: 1, url: 'page-one' },
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
            dotContentletService = spectator.inject(DotContentletService, true);
            dotTempFileUploadService = spectator.inject(DotTempFileUploadService, true);
            dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);
            router = spectator.inject(Router, true);
            dotPageApiService = spectator.inject(DotPageApiService, true);

            addMessageSpy = jest.spyOn(messageService, 'add');

            store.load({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
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
                const componentsToHide = [
                    'palette',
                    'dropzone',
                    'contentlet-tools',
                    'dialog',
                    'confirm-dialog'
                ]; // Test id of components that should hide when entering preview modes

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                store.setDevice(iphone);

                spectator.detectChanges();

                componentsToHide.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).toBeNull();
                });
            });

            it('should hide components when the store changes for a variant', () => {
                const componentsToHide = [
                    'palette',
                    'dropzone',
                    'contentlet-tools',
                    'dialog',
                    'confirm-dialog'
                ]; // Test id of components that should hide when entering preview modes

                spectator.detectChanges();

                spectator.activatedRouteStub.setQueryParam('variantName', 'hello-there');

                spectator.detectChanges();
                store.load({
                    url: 'index',
                    language_id: '5',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
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

                store.load({
                    url: 'index',
                    language_id: '5',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });

                spectator.detectChanges();

                componentsToShow.forEach((testId) => {
                    expect(
                        spectator.debugElement.query(By.css(`[data-testId="${testId}"]`))
                    ).not.toBeNull();
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
                const baseContentletPayload = {
                    x: 100,
                    y: 100,
                    width: 500,
                    height: 500,
                    payload: EDIT_ACTION_PAYLOAD_MOCK
                };

                it('should edit urlContentMap page', () => {
                    const dialog = spectator.query(DotEmaDialogComponent);

                    jest.spyOn(dialog, 'editUrlContentMapContentlet');

                    spectator.triggerEventHandler(EditEmaToolbarComponent, 'editUrlContentMap', {
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

                describe('reorder navigation', () => {
                    it('should open a dialog to reorder the navigation', () => {
                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: CUSTOMER_ACTIONS.REORDER_MENU,
                                    payload: {
                                        reorderUrl: 'http://localhost:3000/reorder-menu'
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
                        const reloadSpy = jest.spyOn(store, 'reload');
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
                                    action: CUSTOMER_ACTIONS.REORDER_MENU,
                                    payload: {
                                        reorderUrl: 'http://localhost:3000/reorder-menu'
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
                        spyStoreReload = jest.spyOn(store, 'reload');

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
                                    action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
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
                        payload: PAYLOAD_MOCK
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
                        payload
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
                        payload
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
                        payload
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
                        payload
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
                        payload
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
                    it('should call the setEditorDragItem from the store for content-types', () => {
                        const setEditorDragItemSpy = jest.spyOn(store, 'setEditorDragItem');

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
                });

                describe('drag over', () => {
                    it('should prevent default to avoid opening files', () => {
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
                    it('should set the editor state to OUT_OF_BOUNDS', () => {
                        const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                        const dragLeave = new Event('dragleave');

                        Object.defineProperties(dragLeave, {
                            x: {
                                value: 0
                            },
                            y: {
                                value: 0
                            },
                            relatedTarget: {
                                value: undefined // this is undefined when the mouse leaves the window
                            }
                        });

                        window.dispatchEvent(dragLeave);

                        expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.OUT_OF_BOUNDS);
                    });
                    it('should not set the editor state to OUT_OF_BOUNDS when the leave is from an element in the window', () => {
                        const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                        const dragLeave = new Event('dragleave');

                        Object.defineProperties(dragLeave, {
                            x: {
                                value: 900
                            },
                            y: {
                                value: 1200
                            },
                            relatedTarget: {
                                value: {}
                            }
                        });

                        window.dispatchEvent(dragLeave);

                        expect(setEditorStateSpy).not.toHaveBeenCalled();
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

                    it('should set the editor to DRAGGING if there is dragItem and the state is OUT_OF_BOUNDS', () => {
                        store.setEditorDragItem({
                            baseType: 'dotAsset',
                            contentType: 'dotAsset',
                            draggedPayload: {
                                type: 'temp'
                            }
                        }); // Simulate drag start

                        store.setEditorState(EDITOR_STATE.OUT_OF_BOUNDS); // Simulate drag leave

                        const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                        const dragEnter = new Event('dragenter');

                        Object.defineProperty(dragEnter, 'fromElement', {
                            writable: false,
                            value: undefined
                        }); // fromElement is falsy when the mouse enters the window

                        window.dispatchEvent(dragEnter);

                        expect(setEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.DRAGGING);
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
                it("should not show a loader when the editor state is not 'loading'", () => {
                    spectator.detectChanges();

                    const progressbar = spectator.query(byTestId('progress-bar'));

                    expect(progressbar).toBeNull();
                });

                it('should show a loader when the UVE is loading', () => {
                    store.setUveStatus(UVE_STATUS.LOADING);

                    spectator.detectChanges();

                    const progressbar = spectator.query(byTestId('progress-bar'));

                    expect(progressbar).not.toBeNull();
                });
                it('iframe should have the correct src when is HEADLESS', () => {
                    spectator.detectChanges();

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                    expect(iframe.nativeElement.src).toBe(
                        'http://localhost:3000/index?clientHost=http%3A%2F%2Flocalhost%3A3000&language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT'
                    );
                });

                describe('VTL Page', () => {
                    beforeEach(() => {
                        jest.useFakeTimers(); // Mock the timers
                        spectator.detectChanges();

                        store.load({
                            url: 'index',
                            language_id: '3',
                            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                        });
                    });

                    afterEach(() => {
                        jest.useRealTimers(); // Restore the real timers after each test
                    });

                    it('iframe should have the correct content when is VTL', () => {
                        spectator.detectChanges();
                        jest.runOnlyPendingTimers();

                        const iframe = spectator.debugElement.query(
                            By.css('[data-testId="iframe"]')
                        );

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

                        store.load({
                            url: 'index',
                            language_id: '4',
                            'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                        });
                        spectator.detectChanges();

                        jest.runOnlyPendingTimers();

                        expect(iframe.nativeElement.src).toBe('http://localhost/'); //When dont have src, the src is the same as the current page
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
                    const router = spectator.inject(Router);
                    jest.spyOn(router, 'navigate');

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

                    expect(router.navigate).toHaveBeenCalledWith([], {
                        queryParams: {
                            url: '/some',
                            'com.dotmarketing.persona.id': 'modes.persona.no.persona'
                        },
                        queryParamsHandling: 'merge'
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
                    const navigateSpy = jest.spyOn(router, 'navigate');

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

                    expect(navigateSpy).toHaveBeenCalledWith([], {
                        queryParams: {
                            'com.dotmarketing.persona.id': 'modes.persona.no.persona',
                            url: '/some'
                        },
                        queryParamsHandling: 'merge'
                    });
                });

                it('set url to the same route should set the editor state to IDLE', () => {
                    const setEditorStateSpy = jest.spyOn(store, 'setEditorState');

                    const url = "/ultra-cool-url-that-doesn't-exist";

                    store.load({
                        url,
                        language_id: '5',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
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
                    store.load({
                        url: 'index',
                        language_id: '5',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
                        variantName: 'hello-there',
                        experimentId: 'i have a variant'
                    });

                    spectator.detectChanges();

                    componentsToHide.forEach((testId) => {
                        expect(spectator.query(byTestId(testId))).not.toBeNull();
                    });
                });

                describe('script and styles injection', () => {
                    let iframeDocument: Document;
                    let spy: jest.SpyInstance;

                    beforeEach(() => {
                        jest.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
                            cb(0); // Pass a dummy value to satisfy the expected argument count

                            return 0;
                        });

                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        spectator.component.iframe = IFRAME_MOCK as any;
                        iframeDocument = spectator.component.iframe.nativeElement.contentDocument;
                        spy = jest.spyOn(iframeDocument, 'write');
                    });

                    it('should add script and styles to iframe', () => {
                        spectator.component.setIframeContent(`<head></head></body></body>`);

                        expect(spy).toHaveBeenCalled();
                        expect(iframeDocument.body.innerHTML).toContain(
                            `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                        );
                        expect(iframeDocument.body.innerHTML).toContain(
                            '[data-dot-object="container"]:empty'
                        );
                        expect(iframeDocument.body.innerHTML).toContain(
                            '[data-dot-object="contentlet"].empty-contentlet'
                        );
                    });

                    it('should add script and styles to iframe for advance templates', () => {
                        spectator.component.setIframeContent(`<div>Advanced Template</div>`);

                        expect(spy).toHaveBeenCalled();
                        expect(iframeDocument.body.innerHTML).toContain(
                            `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                        );

                        expect(iframeDocument.body.innerHTML).toContain(
                            '[data-dot-object="container"]:empty'
                        );
                        expect(iframeDocument.body.innerHTML).toContain(
                            '[data-dot-object="contentlet"].empty-contentlet'
                        );
                    });

                    afterEach(() => {
                        (window.requestAnimationFrame as jest.Mock).mockRestore();
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
                                action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
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
                                action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                                payload: null
                            }
                        })
                    );

                    expect(saveContentletSpy).not.toHaveBeenCalled();
                    expect(setEditorState).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
                });
            });

            describe('CUSTOMER ACTIONS', () => {
                describe('CLIENT_READY', () => {
                    it('should set client is ready when not extra configuration is send', () => {
                        const setIsClientReadySpy = jest.spyOn(store, 'setIsClientReady');

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: CUSTOMER_ACTIONS.CLIENT_READY
                                }
                            })
                        );

                        expect(setIsClientReadySpy).toHaveBeenCalledWith(true);
                    });

                    it('should set client GraphQL configuration and call the reload', () => {
                        const setClientConfigurationSpy = jest.spyOn(
                            store,
                            'setClientConfiguration'
                        );
                        const reloadSpy = jest.spyOn(store, 'reload');

                        const config = {
                            params: {},
                            query: '{ query: { hello } }'
                        };

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: CUSTOMER_ACTIONS.CLIENT_READY,
                                    payload: config
                                }
                            })
                        );

                        expect(setClientConfigurationSpy).toHaveBeenCalledWith(config);
                        expect(reloadSpy).toHaveBeenCalled();
                    });

                    it('should set client PAGEAPI configuration and call the reload', () => {
                        const setClientConfigurationSpy = jest.spyOn(
                            store,
                            'setClientConfiguration'
                        );
                        const reloadSpy = jest.spyOn(store, 'reload');

                        const config = {
                            params: {
                                depth: '1'
                            },
                            query: ''
                        };

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: CUSTOMER_ACTIONS.CLIENT_READY,
                                    payload: config
                                }
                            })
                        );

                        expect(setClientConfigurationSpy).toHaveBeenCalledWith(config);
                        expect(reloadSpy).toHaveBeenCalled();
                    });
                });
            });
        });
    });
});
