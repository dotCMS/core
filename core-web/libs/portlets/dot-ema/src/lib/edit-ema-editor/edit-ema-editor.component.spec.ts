import { describe, expect } from '@jest/globals';
import { SpectatorRouting, createRoutingFactory, byTestId } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotLanguagesService, DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import {
    DotLanguagesServiceMock,
    MockDotMessageService,
    DotPersonalizeServiceMock
} from '@dotcms/utils-testing';

import { EditEmaLanguageSelectorComponent } from './components/edit-ema-language-selector/edit-ema-language-selector.component';
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
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { ActionPayload } from '../shared/models';

const messagesMock = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject'
};

describe('EditEmaEditorComponent', () => {
    let spectator: SpectatorRouting<EditEmaEditorComponent>;
    let store: EditEmaStore;
    let confirmationService: ConfirmationService;

    const createComponent = createRoutingFactory({
        component: EditEmaEditorComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            MessageService,
            EditEmaStore,
            ConfirmationService,
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
                provide: DotPageApiService,
                useValue: {
                    get({ language_id }) {
                        return {
                            2: of({
                                page: {
                                    title: 'hello world',
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
        ]
    });

    describe('with queryParams', () => {
        beforeEach(() => {
            spectator = createComponent({
                queryParams: { language_id: 1, url: 'page-one' }
            });

            store = spectator.inject(EditEmaStore, true);
            confirmationService = spectator.inject(ConfirmationService, true);

            store.load({
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
        });

        describe('toast', () => {
            it('should trigger messageService when clicking on ema-copy-url', () => {
                spectator.detectChanges();

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
                    'http://localhost/api/v1/page/json/index?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
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
                    queryParams: { 'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier },
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
                            contentletsId: [],
                            maxContentlets: 1
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
                                name: NG_CUSTOM_EVENTS.SAVE_CONTENTLET
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
                            contentletsId: [],
                            maxContentlets: 1
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
                                name: NG_CUSTOM_EVENTS.SAVE_CONTENTLET,
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
                            contentletsId: ['contentlet-identifier-123'],
                            maxContentlets: 1
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
                            contentletsId: ['contentlet-identifier-123'],
                            maxContentlets: 1
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
                            contentletsId: [],
                            maxContentlets: 1
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
                                name: NG_CUSTOM_EVENTS.SAVE_CONTENTLET,
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
                            contentletsId: [],
                            maxContentlets: 1
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
                            contentletsId: [],
                            maxContentlets: 1
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

            it('should have a confirm dialog with acceptIcon and rejectIcon attribute', () => {
                spectator.detectChanges();

                const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                expect(confirmDialog.getAttribute('acceptIcon')).toBeNull();
                expect(confirmDialog.getAttribute('rejectIcon')).toBeNull();
            });
        });

        describe('palette', () => {
            it('should post to iframe to get bound on drag', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                const postMessageSpy = jest.spyOn(
                    iframe.nativeElement.contentWindow,
                    'postMessage'
                );

                spectator.triggerEventHandler('div[data-type="contentlet"]', 'dragstart', {
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

                expect(postMessageSpy).toHaveBeenCalledWith(
                    'ema-request-bounds',
                    'http://localhost:3000'
                );
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

                spectator.detectComponentChanges();

                dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.rows).toBe(BOUNDS_MOCK);
            });

            xit('should hide drop zone on palette drop', () => {
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

                let dropZone = spectator.query(EmaPageDropzoneComponent);

                expect(dropZone.rows).toBe(BOUNDS_MOCK);

                spectator.triggerEventHandler('div[data-type="contentlet"]', 'dragend', {});
                spectator.detectComponentChanges();
                dropZone = spectator.query(EmaPageDropzoneComponent);
                expect(dropZone.rows).toEqual([]);
            });
        });
    });
});
