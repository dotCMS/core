import { describe, expect } from '@jest/globals';
import {
    SpectatorRouting,
    createRoutingFactory,
    byTestId,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotDevicesService,
    DotESContentService,
    DotFavoritePageService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock, LoginService } from '@dotcms/dotcms-js';
import {
    DotLanguagesServiceMock,
    MockDotMessageService,
    DotPersonalizeServiceMock,
    DotDevicesServiceMock,
    mockDotDevices,
    LoginServiceMock,
    DotCurrentUserServiceMock
} from '@dotcms/utils-testing';

import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPaletteComponent } from './components/edit-ema-palette/edit-ema-palette.component';
import { EditEmaPersonaSelectorComponent } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component';
import { CUSTOM_PERSONA } from './components/edit-ema-persona-selector/edit-ema-persona-selector.component.spec';
import { EmaContentletToolsComponent } from './components/ema-contentlet-tools/ema-contentlet-tools.component';
import { EmaPageDropzoneComponent } from './components/ema-page-dropzone/ema-page-dropzone.component';
import { BOUNDS_MOCK } from './components/ema-page-dropzone/ema-page-dropzone.component.spec';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { EditEmaStore } from '../dot-ema-shell/store/dot-ema.store';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW, HOST } from '../shared/consts';
import { EDITOR_STATE, NG_CUSTOM_EVENTS } from '../shared/enums';
import { ActionPayload } from '../shared/models';

const messagesMock = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject'
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

const createRouting = (permissions: { canEdit: boolean; canRead: boolean }) =>
    createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            MessageService,
            EditEmaStore,
            ConfirmationService,
            DotFavoritePageService,
            DotESContentService,
            DialogService,
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
            {
                provide: DotPageApiService,
                useValue: {
                    get({ language_id }) {
                        return {
                            2: of({
                                page: {
                                    title: 'hello world',
                                    identifier: '123',
                                    ...permissions,
                                    url: 'page-one'
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
                                }
                            }),
                            1: of({
                                page: {
                                    title: 'hello world',
                                    identifier: '123',
                                    ...permissions,
                                    url: 'page-one'
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
                                }
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

        const createComponent = createRouting({ canEdit: true, canRead: true });

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
                    'http://localhost/api/v1/page/json/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
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
                            title: 'Hello World'
                        }
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
                        whenSaved: expect.any(Function)
                    });
                });
            });

            describe('edit', () => {
                it('should open a dialog and save after backend emit', (done) => {
                    spectator.detectChanges();

                    const initiEditIframeDialogMock = jest.spyOn(store, 'initActionEdit');
                    const dialog = spectator.query(byTestId('dialog'));

                    const payload: ActionPayload = {
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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'test',
                            acceptTypes: 'test',
                            uuid: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
                    };

                    spectator.setInput('contentlet', {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(EmaContentletToolsComponent, 'edit', payload);

                    spectator.detectComponentChanges();

                    expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
                    expect(initiEditIframeDialogMock).toHaveBeenCalledWith({
                        inode: 'contentlet-inode-123',
                        title: 'Hello World',
                        type: 'content'
                    });

                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE
                            }
                        })
                    );
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
            });

            describe('add', () => {
                it('should add contentlet after backend emit SAVE_CONTENTLET', () => {
                    spectator.detectChanges();

                    const initAddIframeDialogMock = jest.spyOn(store, 'initActionAdd');
                    const savePageMock = jest.spyOn(store, 'savePage');

                    const payload: ActionPayload = {
                        pageContainers: [
                            {
                                identifier: 'test',
                                uuid: 'test',
                                contentletsId: ['456', '123']
                            }
                        ],
                        container: {
                            identifier: 'test',
                            acceptTypes: 'test',
                            uuid: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        contentlet: {
                            inode: '123',
                            title: 'Hello World',
                            identifier: '123'
                        },
                        pageId: 'test1',
                        language_id: 'test',
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

                    const dialog = spectator.query(byTestId('dialog'));

                    expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
                    expect(initAddIframeDialogMock).toHaveBeenCalledWith({
                        containerId: 'test',
                        acceptTypes: 'test',
                        language_id: 'test'
                    });

                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CREATE_CONTENTLET,
                                data: {
                                    url: 'test/url',
                                    contentType: 'test'
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    expect(dialogIframe.nativeElement.src).toBe('http://localhost/test/url');

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                payload: {
                                    contentletIdentifier: 'new-contentlet-123'
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    expect(savePageMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                contentletsId: ['456', 'new-contentlet-123', '123'],
                                identifier: 'test',
                                uuid: 'test',
                                personaTag: undefined
                            }
                        ],
                        pageId: 'test1',
                        whenSaved: expect.any(Function)
                    });

                    spectator.detectChanges();
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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'container-identifier-123',
                            acceptTypes: 'test',
                            uuid: 'uuid-123',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
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

                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'new-contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        })
                    );

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
                        whenSaved: expect.any(Function)
                    });

                    expect(saveMock).toHaveBeenCalled();
                });

                it('should add widget after backend emit CONTENT_SEARCH_SELECT', () => {
                    const saveMock = jest.spyOn(store, 'savePage');
                    const actionAdd = jest.spyOn(store, 'initActionAdd');

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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'container-identifier-123',
                            acceptTypes: 'test',
                            uuid: 'uuid-123',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
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

                    expect(actionAdd).toHaveBeenCalledWith({
                        containerId: 'container-identifier-123',
                        acceptTypes: 'WIDGET',
                        language_id: '1'
                    });

                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: 'new-contentlet-identifier-123',
                                    inode: '123'
                                }
                            }
                        })
                    );

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
                        whenSaved: expect.any(Function)
                    });

                    expect(saveMock).toHaveBeenCalled();
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

                it('should not open a dialog when the iframe sends a postmessage with a different origin', () => {
                    spectator.detectChanges();

                    const dialog = spectator.query(byTestId('dialog'));

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: 'my.super.cool.website.xyz',
                            data: {
                                action: 'edit-contentlet',
                                payload: {
                                    contentlet: {
                                        identifier: '123'
                                    }
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    expect(dialog.getAttribute('ng-reflect-visible')).toBe('false');
                });

                it('should trigger onIframeLoad when the dialog is opened', (done) => {
                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'test',
                            acceptTypes: 'test',
                            uuid: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
                    };

                    spectator.setInput('contentlet', {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(EmaContentletToolsComponent, 'edit', payload);

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                                data: {
                                    contentlet: {
                                        identifier: '123'
                                    }
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    iframe.nativeElement.contentWindow.addEventListener('message', (event) => {
                        expect(event).toBeTruthy();
                        done();
                    });

                    const nullSpinner = spectator.query(byTestId('spinner'));

                    expect(nullSpinner).toBeNull();
                });

                it('should show an spinner when triggering an action for the dialog', () => {
                    spectator.detectChanges();

                    const payload: ActionPayload = {
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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'test',
                            acceptTypes: 'test',
                            uuid: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
                    };

                    spectator.setInput('contentlet', {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(EmaContentletToolsComponent, 'edit', payload);

                    const spinner = spectator.query(byTestId('spinner'));

                    expect(spinner).toBeTruthy();
                });

                it('should not show the spinner after iframe load', () => {
                    const payload: ActionPayload = {
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
                            title: 'Hello World'
                        },
                        container: {
                            identifier: 'test',
                            acceptTypes: 'test',
                            uuid: 'test',
                            maxContentlets: 1,
                            contentletsId: ['123']
                        },
                        pageId: 'test'
                    };

                    spectator.setInput('contentlet', {
                        x: 100,
                        y: 100,
                        width: 500,
                        height: 500,
                        payload
                    });

                    spectator.detectComponentChanges();

                    spectator.triggerEventHandler(EmaContentletToolsComponent, 'edit', payload);

                    const spinner = spectator.query(byTestId('spinner'));

                    expect(spinner).toBeTruthy();

                    const dialogIframe = spectator.debugElement.query(
                        By.css("[data-testId='dialog-iframe']")
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    const nullSpinner = spectator.query(byTestId('spinner'));

                    expect(nullSpinner).toBeFalsy();
                });

                it('should reset the dialog properties when the dialog closes', () => {
                    spectator.detectChanges();
                    const resetDialogMock = jest.spyOn(store, 'resetDialog');
                    const dialog = spectator.query(byTestId('dialog'));

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'edit-contentlet',
                                payload: {
                                    contentlet: {
                                        inode: '123',
                                        title: 'Hello World'
                                    }
                                }
                            }
                        })
                    );

                    spectator.dispatchFakeEvent(dialog, 'visibleChange');
                    spectator.detectChanges();

                    expect(resetDialogMock).toHaveBeenCalled();

                    resetDialogMock.mockRestore();
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
            it('iframe should have the correct src', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                expect(iframe.nativeElement.src).toBe(
                    'http://localhost:3000/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
                );
            });

            it('should navigate to new url when postMessage SET_URL', () => {
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
                    queryParams: { url: '/some' },
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

                expect(confirmDialog.getAttribute('acceptIcon')).toBeNull();
                expect(confirmDialog.getAttribute('rejectIcon')).toBeNull();
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
                                identifier: '123',
                                title: 'hello world'
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
                                identifier: '123',
                                title: 'hello world'
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

            it('should reset the rowa when we update query params', () => {
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
