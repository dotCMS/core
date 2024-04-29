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
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
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
import { DotCMSContentlet, DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
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
    DotPersonalizeServiceMock
} from '@dotcms/utils-testing';

import { DotEditEmaWorkflowActionsComponent } from './components/dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaRunningExperimentComponent } from './components/dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaToolbarComponent } from './components/edit-ema-toolbar/edit-ema-toolbar.component';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import { BOUNDS_MOCK } from './components/ema-page-dropzone/ema-page-dropzone.component.spec';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import {
    DEFAULT_PERSONA,
    WINDOW,
    HOST,
    PAYLOAD_MOCK,
    EDIT_ACTION_PAYLOAD_MOCK,
    PAGE_INODE_MOCK,
    QUERY_PARAMS_MOCK,
    TREE_NODE_MOCK,
    URL_CONTENT_MAP_MOCK,
    newContentlet,
    dotPageContainerStructureMock,
    dragAddEventMock,
    dragMoveEventMock
} from '../shared/consts';
import { EDITOR_MODE, EDITOR_STATE, NG_CUSTOM_EVENTS } from '../shared/enums';
import { ActionPayload } from '../shared/models';

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

const createRouting = (permissions: { canEdit: boolean; canRead: boolean }) =>
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
            EditEmaStore,
            DotFavoritePageService,
            DotESContentService,
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
            DotSeoMetaTagsUtilService,
            DialogService,
            DotCopyContentService,
            DotCopyContentModalService,
            DotWorkflowActionsFireService,

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
                        return {
                            // Locked without unlock permission
                            8: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    canEdit: true,
                                    canLock: false,
                                    isLocked: true,
                                    lockedByUser: 'user'
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 2,
                                        language: 'Spanish',
                                        countryCode: 'ES',
                                        languageCode: 'es',
                                        country: 'España'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                containers: dotPageContainerStructureMock
                            }),
                            //Locked  with unlock permission
                            7: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    canEdit: true,
                                    canLock: true,
                                    locked: true,
                                    lockedByName: 'user'
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 2,
                                        language: 'Spanish',
                                        countryCode: 'ES',
                                        languageCode: 'es',
                                        country: 'España'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                containers: dotPageContainerStructureMock
                            }),
                            6: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    canEdit: false
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 6,
                                        language: 'Portuguese',
                                        countryCode: 'BR',
                                        languageCode: 'br',
                                        country: 'Brazil'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                containers: dotPageContainerStructureMock
                            }),
                            5: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: 'i-have-a-running-experiment',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    rendered: '<div>New Content - Hello World</div>',
                                    canEdit: true
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 4,
                                        language: 'Russian',
                                        countryCode: 'Ru',
                                        languageCode: 'ru',
                                        country: 'Russia'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                containers: dotPageContainerStructureMock
                            }),
                            4: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    rendered: '<div>New Content - Hello World</div>',
                                    canEdit: true
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 4,
                                        language: 'German',
                                        countryCode: 'DE',
                                        languageCode: 'de',
                                        country: 'Germany'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                containers: dotPageContainerStructureMock
                            }),
                            3: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    rendered: '<div>hello world</div>',
                                    canEdit: true
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 3,
                                        language: 'German',
                                        countryCode: 'DE',
                                        languageCode: 'de',
                                        country: 'Germany'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                containers: dotPageContainerStructureMock
                            }),
                            2: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one',
                                    canEdit: true
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 2,
                                        language: 'Spanish',
                                        countryCode: 'ES',
                                        languageCode: 'es',
                                        country: 'España'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                containers: dotPageContainerStructureMock
                            }),
                            1: of({
                                page: {
                                    title: 'hello world',
                                    inode: PAGE_INODE_MOCK,
                                    identifier: '123',
                                    ...permissions,
                                    pageURI: 'page-one'
                                },
                                site: {
                                    identifier: '123'
                                },
                                viewAs: {
                                    language: {
                                        id: 1,
                                        language: 'English',
                                        countryCode: 'US',
                                        languageCode: 'EN',
                                        country: 'United States'
                                    },
                                    persona: DEFAULT_PERSONA
                                },
                                urlContentMap: URL_CONTENT_MAP_MOCK,
                                containers: dotPageContainerStructureMock
                            })
                        }[language_id];
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
        let store: EditEmaStore;
        let confirmationService: ConfirmationService;
        let messageService: MessageService;
        let addMessageSpy: jest.SpyInstance;
        let dotCopyContentModalService: DotCopyContentModalService;
        let dotCopyContentService: DotCopyContentService;
        let dotContentletService: DotContentletService;
        let dotHttpErrorManagerService: DotHttpErrorManagerService;

        const createComponent = createRouting({ canEdit: true, canRead: true });

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

            store = spectator.inject(EditEmaStore, true);
            confirmationService = spectator.inject(ConfirmationService, true);
            messageService = spectator.inject(MessageService, true);
            dotCopyContentModalService = spectator.inject(DotCopyContentModalService, true);
            dotCopyContentService = spectator.inject(DotCopyContentService, true);
            dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService, true);
            dotContentletService = spectator.inject(DotContentletService, true);

            addMessageSpy = jest.spyOn(messageService, 'add');

            store.load({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });

            spectator.detectChanges();

            store.updateEditorState(EDITOR_STATE.IDLE);
        });

        describe('Preview mode', () => {
            beforeEach(() => {
                jest.useFakeTimers(); // Mock the timers
            });

            afterEach(() => {
                jest.useRealTimers(); // Restore the real timers after each test
            });

            it('should hide the components that are not needed for preview mode', () => {
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

            it('should hide the editor components when there is a running experiement and initialize the editor in a variant', () => {
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

                    store.setContentletArea({
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

                    expect(saveMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                identifier: '123',
                                uuid: '123',
                                contentletsId: [],
                                personaTag: undefined
                            }
                        ],
                        pageId: '123',
                        whenSaved: expect.any(Function),
                        params: {
                            language_id: 1,
                            url: 'page-one'
                        }
                    });
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

                    store.setContentletArea(baseContentletPayload);

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

                        expect(reloadSpy).toHaveBeenCalledWith({
                            params: {
                                language_id: 1,
                                url: 'page-one'
                            }
                        });

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

                        store.setContentletArea(baseContentletPayload);

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
                        spyReloadIframe = jest.spyOn(spectator.component, 'reloadIframe');
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
                        const SpyEditorState = jest.spyOn(store, 'updateEditorState');
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
                        const SpyEditorState = jest.spyOn(store, 'updateEditorState');
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
                        jest.spyOn(spectator.component, 'currentTreeNode').mockReturnValue(
                            TREE_NODE_MOCK
                        );
                    });

                    it('should copy and open edit dialog', () => {
                        copySpy.mockReturnValue(of(newContentlet));
                        modalSpy.mockReturnValue(of({ shouldCopy: true }));

                        spectator.detectChanges();

                        store.setContentletArea(CONTENTLET_MOCK);

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

                        store.setContentletArea(CONTENTLET_MOCK);

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

                        store.setContentletArea(CONTENTLET_MOCK);

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

                    store.setContentletArea({
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

                    expect(savePageMock).toHaveBeenCalledWith({
                        pageContainers: PAYLOAD_MOCK.pageContainers,
                        pageId: PAYLOAD_MOCK.pageId,
                        whenSaved: expect.any(Function),
                        params: {
                            language_id: 1,
                            url: 'page-one'
                        }
                    });

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

                    store.setContentletArea({
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

                    expect(saveMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                identifier: 'container-identifier-123',
                                uuid: 'uuid-123',
                                contentletsId: [
                                    'contentlet-identifier-123',
                                    'new-contentlet-identifier-123'
                                ],
                                personaTag: undefined
                            }
                        ],
                        pageId: 'test',
                        whenSaved: expect.any(Function),
                        params: {
                            language_id: 1,
                            url: 'page-one'
                        }
                    });
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

                    store.setContentletArea({
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

                    store.setContentletArea({
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

                    expect(saveMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                identifier: 'container-identifier-123',
                                uuid: 'uuid-123',
                                contentletsId: [
                                    'contentlet-identifier-123',
                                    'new-contentlet-identifier-123'
                                ],
                                personaTag: undefined
                            }
                        ],
                        pageId: 'test',
                        whenSaved: expect.any(Function),
                        params: {
                            language_id: 1,
                            url: 'page-one'
                        }
                    });
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

                    store.setContentletArea({
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
        });

        describe('DOM', () => {
            it("should not show a loader when the editor state is not 'loading'", () => {
                spectator.detectChanges();

                const progressbar = spectator.query(byTestId('progress-bar'));

                expect(progressbar).toBeNull();
            });

            it('should show a loader when the editor state is loading', () => {
                store.updateEditorState(EDITOR_STATE.LOADING);

                spectator.detectChanges();

                const progressbar = spectator.query(byTestId('progress-bar'));

                expect(progressbar).not.toBeNull();
            });
            it('iframe should have the correct src when is HEADLESS', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                expect(iframe.nativeElement.src).toBe(
                    'http://localhost:3000/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE'
                );
            });

            describe('VTL Page', () => {
                beforeEach(() => {
                    jest.useFakeTimers(); // Mock the timers
                    store.load({
                        url: 'index',
                        language_id: '3',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    });
                    spectator.detectChanges();
                });

                afterEach(() => {
                    jest.useRealTimers(); // Restore the real timers after each test
                });

                it('iframe should have the correct content when is VTL', () => {
                    spectator.detectChanges();

                    jest.runOnlyPendingTimers();
                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
                    expect(iframe.nativeElement.src).toBe('http://localhost/'); //When dont have src, the src is the same as the current page
                    expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                        '<div>hello world</div>'
                    );
                    expect(iframe.nativeElement.contentDocument.body.innerHTML).toContain(
                        '<script data-inline="true" src="/html/js/tinymce/js/tinymce/tinymce.min.js">'
                    );
                });

                it('iframe should have reload the page and add the new content, maintaining scroll', () => {
                    const params = {
                        language_id: '4',
                        url: 'index',
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    };

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
                    const scrollSpy = jest
                        .spyOn(spectator.component.iframe.nativeElement.contentWindow, 'scrollTo')
                        .mockImplementation(() => jest.fn);

                    iframe.nativeElement.contentWindow.scrollTo(0, 100); //Scroll down

                    store.reload({
                        params,
                        whenReloaded: () => {
                            /* */
                        }
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
                                url: 'page-one'
                            }
                        }
                    })
                );

                expect(router.navigate).not.toHaveBeenCalled();
            });

            it('set url to a different route should set the editor state to loading', () => {
                const updateEditorStateSpy = jest.spyOn(store, 'updateEditorState');

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

                expect(updateEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.LOADING);
            });

            it('set url to the same route should set the editor state to IDLE', () => {
                const updateEditorStateSpy = jest.spyOn(store, 'updateEditorState');

                const url = "/ultra-cool-url-that-doesn't-exist";

                spectator.detectChanges();
                spectator.triggerNavigation({
                    url: [],
                    queryParams: { url }
                });

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

                expect(updateEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
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
        });

        describe('move contentlet', () => {
            it('should post to iframe to get bound on move contentlet and show bounds', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                const postMessageSpy = jest.spyOn(
                    iframe.nativeElement.contentWindow,
                    'postMessage'
                );

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
                            payload: {
                                x: 100,
                                y: 100,
                                width: 500,
                                height: 500,
                                payload: PAYLOAD_MOCK
                            }
                        }
                    })
                );

                spectator.detectChanges();

                const emaTools = spectator.debugElement.query(
                    By.css('[data-testId="contentlet-tools"]')
                );

                spectator.triggerEventHandler(emaTools, 'moveStart', {
                    ...PAYLOAD_MOCK
                });

                spectator.detectComponentChanges();

                expect(postMessageSpy).toHaveBeenCalledWith('ema-request-bounds', '*');

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                ); // Simulate the iframe response

                spectator.detectComponentChanges();

                expect(spectator.query(EmaPageDropzoneComponent)).not.toBeNull();
            });

            it('should hide drop zone on contentlet tools drop', () => {
                spectator.detectChanges();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
                            payload: {
                                x: 100,
                                y: 100,
                                width: 500,
                                height: 500,
                                payload: PAYLOAD_MOCK
                            }
                        }
                    })
                );

                spectator.detectChanges();

                const emaTools = spectator.debugElement.query(
                    By.css('[data-testId="contentlet-tools"]')
                );

                spectator.triggerEventHandler(emaTools, 'moveStart', {
                    ...PAYLOAD_MOCK
                });

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                );

                spectator.detectComponentChanges();

                let dropZoneComponent = spectator.query(EmaPageDropzoneComponent);

                const dropZoneDebugElement = spectator.debugElement.query(
                    By.css('[data-testId="dropzone"]')
                );

                expect(dropZoneComponent.item).toEqual({
                    contentType: 'Banner',
                    baseType: 'CONTENT'
                });
                expect(dropZoneComponent.containers).toBe(BOUNDS_MOCK);

                spectator.triggerEventHandler(emaTools, 'moveStop', {
                    dataTransfer: {
                        dropEffect: 'move'
                    }
                });
                spectator.triggerEventHandler(dropZoneDebugElement, 'place', {
                    ...PAYLOAD_MOCK,
                    position: 'after'
                });

                spectator.detectComponentChanges();
                dropZoneComponent = spectator.query(EmaPageDropzoneComponent);
                expect(dropZoneComponent).toBeNull();
            });

            it('should move a contentlet from position in the same contentlet', () => {
                const saveSpy = jest.spyOn(store, 'savePage');
                spectator.detectChanges();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
                            payload: {
                                x: 100,
                                y: 100,
                                width: 500,
                                height: 500,
                                payload: PAYLOAD_MOCK
                            }
                        }
                    })
                );

                spectator.detectChanges();

                const emaTools = spectator.debugElement.query(
                    By.css('[data-testId="contentlet-tools"]')
                );

                spectator.triggerEventHandler(emaTools, 'moveStart', {
                    container: {
                        acceptTypes: '123,456',
                        identifier: '123',
                        contentletsId: ['123', '456'],
                        maxContentlets: 123,
                        uuid: '123'
                    }, // Same container
                    contentlet: {
                        identifier: '123' // The pivot
                    }
                });

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                );

                spectator.detectComponentChanges();

                const dropZone = spectator.debugElement.query(By.css('[data-testId="dropzone"]'));

                spectator.triggerEventHandler(dropZone, 'place', {
                    container: {
                        acceptTypes: '123,456',
                        identifier: '123',
                        contentletsId: ['123', '456'],
                        maxContentlets: 123,
                        uuid: '123'
                    }, // Same container
                    position: 'after',
                    contentlet: {
                        identifier: '456' // The pivot
                    }
                });

                const newPageContainers = [
                    {
                        identifier: '123',
                        uuid: '123',
                        contentletsId: ['456', '123'],
                        personaTag: 'dot:persona'
                    },
                    {
                        identifier: '123',
                        uuid: '456',
                        contentletsId: ['123'],
                        personaTag: 'dot:persona'
                    }
                ];

                expect(saveSpy).toHaveBeenCalledWith({
                    pageContainers: newPageContainers,
                    pageId: '123',
                    params: {
                        language_id: 1,
                        url: 'page-one'
                    }
                });
            });

            it('should move a contentlet to another container', () => {
                const saveSpy = jest.spyOn(store, 'savePage');
                spectator.detectChanges();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
                            payload: {
                                x: 100,
                                y: 100,
                                width: 500,
                                height: 500,
                                payload: PAYLOAD_MOCK
                            }
                        }
                    })
                );

                spectator.detectChanges();

                const emaTools = spectator.debugElement.query(
                    By.css('[data-testId="contentlet-tools"]')
                );

                spectator.triggerEventHandler(emaTools, 'moveStart', {
                    container: {
                        acceptTypes: '123,456',
                        identifier: '123',
                        contentletsId: ['123', '456'],
                        maxContentlets: 123,
                        uuid: '123'
                    }, // From container
                    contentlet: {
                        identifier: '456' // The contentlet to move
                    }
                });

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                );

                spectator.detectComponentChanges();

                const dropZone = spectator.debugElement.query(By.css('[data-testId="dropzone"]'));

                spectator.triggerEventHandler(dropZone, 'place', {
                    container: {
                        acceptTypes: '123,456',
                        identifier: '123',
                        contentletsId: ['123'],
                        maxContentlets: 123,
                        uuid: '456'
                    }, // Another container
                    position: 'after',
                    contentlet: {
                        identifier: '123' // The pivot
                    }
                });

                const newPageContainers = [
                    {
                        identifier: '123',
                        uuid: '123',
                        contentletsId: ['123'],
                        personaTag: 'dot:persona'
                    },
                    {
                        identifier: '123',
                        uuid: '456',
                        contentletsId: ['123', '456'],
                        personaTag: 'dot:persona'
                    }
                ];

                expect(saveSpy).toHaveBeenCalledWith({
                    pageContainers: newPageContainers,
                    pageId: '123',
                    params: {
                        language_id: 1,
                        url: 'page-one'
                    }
                });
            });
        });

        describe('palette', () => {
            it('should render a palette', () => {
                spectator.detectChanges();

                const palette = spectator.query(EditEmaPaletteComponent);
                expect(palette).toBeDefined();
            });

            it('should post to iframe to get bound on drag', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                const postMessageSpy = jest.spyOn(
                    iframe.nativeElement.contentWindow,
                    'postMessage'
                );

                spectator.triggerEventHandler(EditEmaPaletteComponent, 'dragStart', {
                    target: {
                        dataset: {
                            type: 'contentlet',
                            item: JSON.stringify({
                                contentlet: {
                                    identifier: '123',
                                    title: 'hello world'
                                }
                            })
                        }
                    }
                });

                spectator.detectComponentChanges();

                expect(postMessageSpy).toHaveBeenCalledWith('ema-request-bounds', '*');
            });

            it('should show drop zone on iframe message', () => {
                spectator.detectChanges();

                let dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone).toBeNull();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                );

                spectator.triggerEventHandler(
                    EditEmaPaletteComponent,
                    'dragStart',
                    dragMoveEventMock
                );

                spectator.detectComponentChanges();

                dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.containers).toBe(BOUNDS_MOCK);
                expect(dropZone.item).toEqual({
                    contentType: 'File',
                    baseType: 'CONTENT'
                });
            });

            it('should hide drop zone on palette drop', () => {
                spectator.detectChanges();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'set-bounds',
                            payload: BOUNDS_MOCK
                        }
                    })
                );

                spectator.triggerEventHandler(
                    EditEmaPaletteComponent,
                    'dragStart',
                    dragAddEventMock
                );

                spectator.detectComponentChanges();

                let dropZone = spectator.query(EmaPageDropzoneComponent);

                const dropZoneDebugElement = spectator.debugElement.query(
                    By.css('[data-testId="dropzone"]')
                );

                expect(dropZone.item).toEqual({
                    contentType: 'Banner',
                    baseType: 'CONTENT'
                });
                expect(dropZone.containers).toBe(BOUNDS_MOCK);

                spectator.triggerEventHandler(dropZoneDebugElement, 'place', {
                    ...PAYLOAD_MOCK,
                    position: 'after'
                });
                spectator.detectComponentChanges();

                dropZone = spectator.query(EmaPageDropzoneComponent);
                expect(dropZone).toBeNull();
            });
        });

        describe('inline editing', () => {
            it('should save from inline edited contentlet', () => {
                const saveFromInlineEditedContentletSpy = jest.spyOn(
                    store,
                    'saveFromInlineEditedContentlet'
                );
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
                                innerHTML: 'Hello World',
                                element: {},
                                eventType: '',
                                isNotDirty: false
                            }
                        }
                    })
                );

                expect(saveFromInlineEditedContentletSpy).toHaveBeenCalledWith({
                    contentlet: {
                        inode: '123',
                        title: 'Hello World'
                    },
                    params: {
                        language_id: 1,
                        url: 'page-one'
                    }
                });
            });

            it('should dont trigger save from inline edited contentlet when dont have changes', () => {
                const saveFromInlineEditedContentletSpy = jest.spyOn(
                    store,
                    'saveFromInlineEditedContentlet'
                );
                const setEditorModeSpy = jest.spyOn(store, 'setEditorMode');
                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                            payload: null
                        }
                    })
                );

                expect(saveFromInlineEditedContentletSpy).not.toHaveBeenCalled();
                expect(setEditorModeSpy).toHaveBeenCalledWith(EDITOR_MODE.EDIT);
            });

            it('should trigger copy contentlet dialog when inline editing', () => {
                const copyContentletSpy = jest.spyOn(dotCopyContentModalService, 'open');
                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
                            payload: {
                                inode: '123',
                                language: '1'
                            }
                        }
                    })
                );

                expect(copyContentletSpy).toHaveBeenCalledWith();
            });
        });
    });

    describe('without edit permission', () => {
        let spectator: SpectatorRouting<EditEmaEditorComponent>;
        let store: EditEmaStore;

        const createComponent = createRouting({ canEdit: false, canRead: true });
        beforeEach(() => {
            spectator = createComponent({
                queryParams: { language_id: 1, url: 'page-one' }
            });

            store = spectator.inject(EditEmaStore, true);

            store.load({
                url: 'index',
                language_id: '1',
                clientHost: '',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
        });

        it('should not render components', () => {
            spectator.detectChanges();
            expect(spectator.query(EmaContentletToolsComponent)).toBeNull();
            expect(spectator.query(EditEmaPaletteComponent)).toBeNull();
        });

        it('should render a "Dont have permission" message', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('editor-banner'))).toBeDefined();
        });

        it('should iframe wrapper to be expanded', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('editor-content')).classList).toContain(
                'editor-content--expanded'
            );
        });
    });

    describe('locked', () => {
        describe('locked with unlock permission', () => {
            let spectator: SpectatorRouting<EditEmaEditorComponent>;
            let store: EditEmaStore;

            const createComponent = createRouting({ canEdit: true, canRead: true });
            beforeEach(() => {
                spectator = createComponent({
                    queryParams: { language_id: 7, url: 'page-one' }
                });

                store = spectator.inject(EditEmaStore, true);

                store.load({
                    url: 'index',
                    language_id: '7',
                    clientHost: '',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
            });

            it('should not render components', () => {
                spectator.detectChanges();
                expect(spectator.query(EmaContentletToolsComponent)).toBeNull();
                expect(spectator.query(EditEmaPaletteComponent)).toBeNull();
            });

            it('should render a banner', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('editor-banner'))).toBeDefined();
            });

            it('should iframe wrapper to be expanded', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('editor-content')).classList).toContain(
                    'editor-content--expanded'
                );
            });
        });
    });
});
