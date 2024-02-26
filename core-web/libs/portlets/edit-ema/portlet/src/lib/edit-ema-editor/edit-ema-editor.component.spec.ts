import { describe, expect } from '@jest/globals';
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
import { DialogService } from 'primeng/dynamicdialog';

import { CUSTOMER_ACTIONS } from '@dotcms/client';
import {
    DotContentTypeService,
    DotCopyContentService,
    DotCurrentUserService,
    DotDevicesService,
    DotESContentService,
    DotFavoritePageService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock, LoginService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    CONTAINER_SOURCE,
    DotPageContainerStructure
} from '@dotcms/dotcms-models';
import { DotCopyContentModalService, ModelCopyContentResponse, SafeUrlPipe } from '@dotcms/ui';
import {
    DotLanguagesServiceMock,
    MockDotMessageService,
    DotPersonalizeServiceMock,
    DotDevicesServiceMock,
    mockDotDevices,
    LoginServiceMock,
    DotCurrentUserServiceMock,
    dotcmsContentletMock
} from '@dotcms/utils-testing';

import { DotEditEmaWorkflowActionsComponent } from './components/dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { CUSTOM_PERSONA } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component.spec';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import { BOUNDS_MOCK } from './components/ema-page-dropzone/ema-page-dropzone.component.spec';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiResponse, DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW, HOST, PAYLOAD_MOCK } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS } from '../shared/enums';
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

const dragEventMock = {
    target: {
        dataset: {
            type: 'contentlet',
            item: JSON.stringify({
                identifier: '123',
                title: 'hello world',
                contentType: 'File',
                baseType: 'CONTENT'
            })
        }
    }
};

const dotPageContainerStructureMock: DotPageContainerStructure = {
    '123': {
        container: {
            archived: false,
            categoryId: '123',
            deleted: false,
            friendlyName: '123',
            identifier: '123',
            live: false,
            locked: false,
            maxContentlets: 123,
            name: '123',
            path: '123',
            pathName: '123',
            postLoop: '123',
            preLoop: '123',
            source: CONTAINER_SOURCE.DB,
            title: '123',
            type: '123',
            working: false
        },
        containerStructures: [
            {
                contentTypeVar: '123'
            }
        ],
        contentlets: {
            '123': [
                {
                    baseType: '123',
                    content: 'something',
                    contentType: '123',
                    dateCreated: '123',
                    dateModifed: '123',
                    folder: '123',
                    host: '123',
                    identifier: '123',
                    inode: '123',
                    languageId: 123,
                    live: false,
                    locked: false,
                    modDate: '123',
                    modUser: '123',
                    owner: '123',
                    working: false,
                    title: '123',
                    url: '123',
                    __icon__: '123',
                    archived: false,
                    deleted: false,
                    hasTitleImage: false,
                    hostName: '123',
                    image: '123',
                    modUserName: '123',
                    sortOrder: 123,
                    stInode: '123',
                    titleImage: '123'
                },
                {
                    baseType: '456',
                    content: 'something',
                    contentType: '456',
                    dateCreated: '456',
                    dateModifed: '456',
                    folder: '456',
                    host: '456',
                    identifier: '456',
                    inode: '456',
                    languageId: 456,
                    live: false,
                    locked: false,
                    modDate: '456',
                    modUser: '456',
                    owner: '456',
                    working: false,
                    title: '456',
                    url: '456',
                    __icon__: '456',
                    archived: false,
                    deleted: false,
                    hasTitleImage: false,
                    hostName: '456',
                    image: '456',
                    modUserName: '456',
                    sortOrder: 456,
                    stInode: '456',
                    titleImage: '456'
                }
            ],
            '456': [
                {
                    baseType: '123',
                    content: 'something',
                    contentType: '123',
                    dateCreated: '123',
                    dateModifed: '123',
                    folder: '123',
                    host: '123',
                    identifier: '123',
                    inode: '123',
                    languageId: 123,
                    live: false,
                    locked: false,
                    modDate: '123',
                    modUser: '123',
                    owner: '123',
                    working: false,
                    title: '123',
                    url: '123',
                    __icon__: '123',
                    archived: false,
                    deleted: false,
                    hasTitleImage: false,
                    hostName: '123',
                    image: '123',
                    modUserName: '123',
                    sortOrder: 123,
                    stInode: '123',
                    titleImage: '123'
                }
            ]
        }
    }
};

const PAGE_INODE_MOCK = '1234';
const QUERY_PARAMS_MOCK = { language_id: 1, url: 'page-one' };

const TREE_NODE_MOCK = {
    containerId: '123',
    contentId: '123',
    pageId: '123',
    relationType: 'test',
    treeOrder: '1',
    variantId: 'test',
    personalization: 'dot:default'
};

const newContentlet = {
    ...dotcmsContentletMock,
    inode: '123',
    title: 'test'
};

const EDIT_ACTION_PAYLOAD_MOCK: ActionPayload = {
    language_id: '1',
    pageContainers: [
        {
            identifier: 'test',
            uuid: 'test',
            contentletsId: []
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
        identifier: 'test',
        acceptTypes: 'test',
        uuid: 'test',
        maxContentlets: 1,
        contentletsId: ['123'],
        variantId: '123'
    },
    pageId: 'test',
    position: 'before'
};

const createRouting = (permissions: { canEdit: boolean; canRead: boolean }) =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, HttpClientTestingModule, SafeUrlPipe],
        declarations: [
            MockComponent(DotEditEmaWorkflowActionsComponent),
            MockComponent(DotEmaDialogComponent)
        ],
        detectChanges: false,
        componentProviders: [
            MessageService,
            EditEmaStore,
            ConfirmationService,
            DotFavoritePageService,
            DotESContentService,
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
            },
            {
                provide: DotPersonalizeService,
                useValue: new DotPersonalizeServiceMock()
            }
        ],
        providers: [
            DialogService,
            DotCopyContentService,
            DotCopyContentModalService,
            { provide: ActivatedRoute, useValue: { snapshot: { queryParams: QUERY_PARAMS_MOCK } } },
            {
                provide: DotPageApiService,
                useValue: {
                    get({ language_id }) {
                        return {
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
            mockProvider(DotContentTypeService)
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

            addMessageSpy = jest.spyOn(messageService, 'add');

            store.load({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });

            spectator.detectChanges();

            store.updateEditorState(EDITOR_STATE.LOADED);
        });

        describe('toast', () => {
            it('should trigger messageService when clicking on ema-copy-url', () => {
                const messageService = spectator.inject(MessageService, true);
                const messageServiceSpy = jest.spyOn(messageService, 'add');
                spectator.detectChanges();

                const button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));

                spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

                expect(messageServiceSpy).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Copied',
                    life: 3000
                });
            });
        });

        describe('API URL', () => {
            it('should have the url setted with the current language and persona', () => {
                spectator.detectChanges();

                const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

                expect(button.nativeElement.href).toBe(
                    'http://localhost/api/v1/page/json/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&mode=EDIT_MODE'
                );
            });

            it('should open a new tab', () => {
                spectator.detectChanges();

                const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

                expect(button.nativeElement.target).toBe('_blank');
            });
        });

        describe('language selector', () => {
            it('should have a language selector', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('language-selector'))).not.toBeNull();
            });

            it("should have the current language as label in the language selector's button", () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('language-button')).textContent).toBe(
                    'English - US'
                );
            });

            it('should call navigate when selecting a language', () => {
                spectator.detectChanges();
                const router = spectator.inject(Router);

                jest.spyOn(router, 'navigate');

                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 2 },
                    queryParamsHandling: 'merge'
                });
            });
        });

        describe('Preview mode', () => {
            it('should reset the selection on click on the x button', () => {
                spectator.detectChanges();

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                const iphone = mockDotDevices[0];

                spectator.triggerEventHandler(deviceSelector, 'selected', iphone);
                spectator.detectChanges();

                const deviceDisplay = spectator.debugElement.query(
                    By.css('[data-testId="device-display"]')
                );

                spectator.triggerEventHandler(deviceDisplay, 'resetDevice', {});

                const selectedDevice = spectator.query(byTestId('selected-device'));

                expect(selectedDevice).toBeNull();
            });

            it('should hide the components that are not needed for preview mode', () => {
                const componentsToHide = [
                    'palette',
                    'dropzone',
                    'contentlet-tools',
                    'dialog',
                    'confirm-dialog'
                ]; // Test id of components that should hide when entering preview modes

                spectator.detectChanges();

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                spectator.triggerEventHandler(deviceSelector, 'selected', iphone);
                spectator.detectChanges();

                componentsToHide.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).toBeNull();
                });
            });

            it('should show the components that need showed on preview mode', () => {
                const componentsToShow = ['ema-back-to-edit', 'device-display']; // Test id of components that should show when entering preview modes

                spectator.detectChanges();

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                spectator.triggerEventHandler(deviceSelector, 'selected', iphone);
                spectator.detectChanges();

                componentsToShow.forEach((testId) => {
                    expect(spectator.query(byTestId(testId))).not.toBeNull();
                });
            });
        });

        describe('persona selector', () => {
            it('should have a persona selector', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('persona-selector'))).not.toBeNull();
            });

            it('should call navigate when selecting a persona', () => {
                spectator.detectChanges();
                const router = spectator.inject(Router);

                jest.spyOn(router, 'navigate');

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...DEFAULT_PERSONA,
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                });
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { 'com.dotmarketing.persona.id': '123' },
                    queryParamsHandling: 'merge'
                });
            });

            it("should open a confirmation dialog when selecting a persona that it's not personalized", () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...DEFAULT_PERSONA,
                    identifier: '123',
                    pageId: '123',
                    personalized: false
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();
            });

            it('should fetchPersonas and navigate when confirming the personalization', () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...DEFAULT_PERSONA,
                    identifier: '123',
                    pageId: '123',
                    personalized: false
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));
                const personaSelector = spectator.debugElement.query(
                    By.css('[data-testId="persona-selector"]')
                ).componentInstance;
                const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
                const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                confirmDialog
                    .querySelector('.p-confirm-dialog-accept')
                    .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

                spectator.detectChanges();

                expect(routerSpy).toBeCalledWith([], {
                    queryParams: { 'com.dotmarketing.persona.id': '123' },
                    queryParamsHandling: 'merge'
                });
                expect(fetchPersonasSpy).toHaveBeenCalled();
            });

            it('should reset the value on personalization rejection', () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    ...DEFAULT_PERSONA,
                    identifier: '123',
                    pageId: '123',
                    personalized: false
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));
                const personaSelector = spectator.debugElement.query(
                    By.css('[data-testId="persona-selector"]')
                ).componentInstance;

                const resetValueSpy = jest.spyOn(personaSelector, 'resetValue');

                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                confirmDialog
                    .querySelector('.p-confirm-dialog-reject')
                    .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

                spectator.detectChanges();

                expect(resetValueSpy).toHaveBeenCalled();
            });

            it('should open a confirmation dialog when despersonalize is triggered', () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    ...DEFAULT_PERSONA,
                    pageId: '123',
                    selected: false
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();
            });

            it('should fetchPersonas when confirming the despersonalization', () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    ...DEFAULT_PERSONA,
                    pageId: '123',
                    selected: false
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));
                const personaSelector = spectator.debugElement.query(
                    By.css('[data-testId="persona-selector"]')
                ).componentInstance;

                const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                confirmDialog
                    .querySelector('.p-confirm-dialog-accept')
                    .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

                spectator.detectChanges();

                expect(fetchPersonasSpy).toHaveBeenCalled();
            });

            it('should navigate with default persona as current persona when the selected is the same as the despersonalized', () => {
                const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                spectator.detectChanges();

                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    ...CUSTOM_PERSONA,
                    pageId: '123',
                    selected: true
                });
                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');

                spectator.detectChanges();

                expect(confirmDialogOpen).toHaveBeenCalled();

                confirmDialog
                    .querySelector('.p-confirm-dialog-accept')
                    .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

                spectator.detectChanges();

                expect(routerSpy).toHaveBeenCalledWith([], {
                    queryParams: {
                        'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                    },
                    queryParamsHandling: 'merge'
                });
            });
        });

        describe('customer actions', () => {
            describe('delete', () => {
                it('should open a confirm dialog and save on confirm', () => {
                    spectator.detectChanges();

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

                    spectator.setInput('contentlet', {
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

                    spectator.detectChanges();

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
                it('should open a dialog and save after backend emit', (done) => {
                    spectator.detectChanges();

                    const dialog = spectator.debugElement.query(
                        By.css('[data-testId="ema-dialog"]')
                    );

                    spectator.setInput('contentlet', {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload: EDIT_ACTION_PAYLOAD_MOCK
                    });

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

                        spectator.setInput('contentlet', CONTENTLET_MOCK);

                        spectator.detectComponentChanges();

                        spectator.triggerEventHandler(
                            EmaContentletToolsComponent,
                            'edit',
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
                        );

                        spectator.detectComponentChanges();

                        expect(copySpy).toHaveBeenCalledWith(TREE_NODE_MOCK); // It's not being called
                        expect(dialogLoadingSpy).toHaveBeenCalledWith('Hello World');
                        expect(editContentletSpy).toHaveBeenCalledWith({
                            ...EDIT_ACTION_PAYLOAD_MOCK,
                            contentlet: newContentlet
                        });
                        expect(modalSpy).toHaveBeenCalled();
                        expect(reloadIframeSpy).toHaveBeenCalledWith('ema-reload-page', '*');
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

                        spectator.setInput('contentlet', CONTENTLET_MOCK);

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

                        spectator.setInput('contentlet', CONTENTLET_MOCK);

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
                            EDIT_ACTION_PAYLOAD_IN_MULTIPLE_PAGES
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

                    spectator.setInput('contentlet', {
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

                    spectator.setInput('contentlet', {
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

                    spectator.setInput('contentlet', {
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

                    spectator.setInput('contentlet', {
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

                    spectator.setInput('contentlet', {
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

                    spectator.setInput('contentlet', {
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

                describe('misc', () => {
                    it('should set the editorState to loaded when the iframe sends a postmessage of content changed', () => {
                        const editorStateSpy = jest.spyOn(store, 'updateEditorState');

                        window.dispatchEvent(
                            new MessageEvent('message', {
                                origin: HOST,
                                data: {
                                    action: 'content-change'
                                }
                            })
                        );

                        expect(editorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.LOADED);
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
                    'http://localhost:3000/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&mode=EDIT_MODE'
                );
            });

            it('iframe should have the correct content when is VTL', () => {
                store.load({
                    url: 'index',
                    language_id: '3',
                    'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                });
                spectator.detectChanges();

                spectator.component.onIframePageLoad({
                    clientHost: '',
                    editor: { page: { rendered: '<div>hello world</div>' } }
                } as { clientHost: string; editor: DotPageApiResponse }); // I didn't find another way to mock the iframe loading

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                expect(iframe.nativeElement.src).toBe('http://localhost/'); //When dont have src, the src is the same as the current page
                expect(iframe.nativeElement.contentDocument.body.innerHTML).toEqual(
                    '<div>hello world</div>'
                );
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

            it('should not change persona on load same url', () => {
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

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        url: 'page-one' //Same page as init
                    },
                    queryParamsHandling: 'merge'
                });
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

            it('set url to the same route should set the editor state to loaded', () => {
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

                expect(updateEditorStateSpy).toHaveBeenCalledWith(EDITOR_STATE.LOADED);
            });

            it('should have a confirm dialog with acceptIcon and rejectIcon attribute', () => {
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                expect(confirmDialog.getAttribute('acceptIcon')).toBe('hidden');
                expect(confirmDialog.getAttribute('rejectIcon')).toBe('hidden');
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

                expect(spectator.query(EmaPageDropzoneComponent)).not.toBeNull();
            });

            it('should hide drop zone on palette drop', () => {
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

                let dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.item).toEqual({
                    contentType: 'Banner',
                    baseType: 'CONTENT'
                });
                expect(dropZone.rows).toBe(BOUNDS_MOCK);

                spectator.triggerEventHandler(emaTools, 'moveStop', undefined);
                spectator.detectComponentChanges();
                dropZone = spectator.query(EmaPageDropzoneComponent);
                expect(dropZone).toBeNull();
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
                    whenSaved: expect.any(Function),
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
                    whenSaved: expect.any(Function),
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

            it('should reset the contentlet when we update query params', () => {
                spectator.detectChanges();

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

                expect(spectator.component.contentlet).not.toBeNull();

                spectator.component.onLanguageSelected(2); // triggers a query param change

                spectator.detectComponentChanges();

                expect(spectator.component.contentlet).toBeNull();
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

                spectator.triggerEventHandler(EditEmaPaletteComponent, 'dragStart', dragEventMock);

                spectator.detectComponentChanges();

                dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.rows).toBe(BOUNDS_MOCK);
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

                spectator.triggerEventHandler(EditEmaPaletteComponent, 'dragStart', dragEventMock);

                spectator.detectComponentChanges();

                let dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.item).toEqual({
                    contentType: 'File',
                    baseType: 'CONTENT'
                });
                expect(dropZone.rows).toBe(BOUNDS_MOCK);

                spectator.triggerEventHandler(EditEmaPaletteComponent, 'dragEnd', {});
                spectator.detectComponentChanges();
                dropZone = spectator.query(EmaPageDropzoneComponent);
                expect(dropZone).toBeNull();
            });

            it('should reset the rows when we update query params', () => {
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

                spectator.detectComponentChanges();

                expect(spectator.component.rows.length).toBe(BOUNDS_MOCK.length);

                spectator.component.onLanguageSelected(2); // triggers a query param change

                spectator.detectComponentChanges();

                expect(spectator.component.rows.length).toBe(0);
            });
        });

        describe('Workflow actions', () => {
            it('should set the inputs correctly', () => {
                const component = spectator.query(DotEditEmaWorkflowActionsComponent);

                expect(component.inode).toBe(PAGE_INODE_MOCK);
            });

            it('should update reload if the page url changes', () => {
                const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
                const component = spectator.query(DotEditEmaWorkflowActionsComponent);

                component.newPage.emit({
                    ...dotcmsContentletMock,
                    url: 'new-page'
                });

                spectator.detectChanges();

                expect(routerSpy).toHaveBeenCalledWith([], {
                    queryParams: {
                        ...QUERY_PARAMS_MOCK,
                        url: 'new-page',
                        language_id: '1'
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should update reload if the language changes', () => {
                const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
                const component = spectator.query(DotEditEmaWorkflowActionsComponent);

                component.newPage.emit({
                    ...dotcmsContentletMock,
                    url: 'index',
                    languageId: 2
                });

                spectator.detectChanges();

                expect(routerSpy).toHaveBeenCalledWith([], {
                    queryParams: {
                        ...QUERY_PARAMS_MOCK,
                        url: 'index',
                        language_id: '2'
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should not reload if neither the url or language changes ', () => {
                const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
                const component = spectator.query(DotEditEmaWorkflowActionsComponent);

                component.newPage.emit({
                    ...dotcmsContentletMock,
                    url: QUERY_PARAMS_MOCK.url,
                    languageId: QUERY_PARAMS_MOCK.language_id
                });

                spectator.detectChanges();

                expect(routerSpy).not.toHaveBeenCalled();
            });
        });

        // describe('reaload iframe', () => {
        //     let spy: jest.SpyInstance;
        //     let dialog: DebugElement;

        //     beforeEach(() => {
        //         spy = jest.spyOn(
        //             spectator.component.iframe.nativeElement.contentWindow,
        //             'postMessage'
        //         );
        //         spectator.detectChanges();
        //         dialog = spectator.debugElement.query(By.css('[data-testId="ema-dialog"]'));
        //     });

        //     it('should update to Loading state', () => {
        //         triggerCustomEvent(dialog, 'reloadIframe', true);
        //         spectator.detectChanges();
        //         expect(spy).toHaveBeenCalledWith('ema-reload-page', '*');
        //     });
        // });
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
});
