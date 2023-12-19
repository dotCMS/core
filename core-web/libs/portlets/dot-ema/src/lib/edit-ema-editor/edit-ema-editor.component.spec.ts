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
        });

        describe('router', () => {
            it('should initialize with route query parameters', () => {
                const mockQueryParams = {
                    language_id: 1,
                    url: 'page-one',
                    persona_id: 'modes.persona.no.persona'
                };

                jest.spyOn(store, 'load');

                spectator.detectChanges();

                expect(store.load).toHaveBeenCalledWith(mockQueryParams);
            });

            it('should update the iframe url when the queryParams changes', () => {
                spectator.detectChanges();

                const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                spectator.triggerNavigation({
                    url: [],
                    queryParams: { language_id: 2, url: 'my-awesome-route' }
                });

                expect(iframe.nativeElement.src).toBe(
                    HOST +
                        '/my-awesome-route?language_id=2&com.dotmarketing.persona.id=modes.persona.no.persona'
                );
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

            it("should open a toast when messageService's add is called", () => {
                spectator.detectChanges();

                const button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));

                spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

                const toastItem = spectator.query('p-toastitem');

                expect(toastItem).not.toBeNull();
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
        });

        describe('customer actions', () => {
            describe('delete', () => {
                it('should open a confirm dialog and save on confirm', () => {
                    spectator.detectChanges();

                    const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
                    const saveMock = jest.spyOn(store, 'savePage');
                    const confirmDialog = spectator.query(byTestId('confirm-dialog'));

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'delete-contentlet',
                                payload: {
                                    pageId: '123',
                                    container: {
                                        identifier: '123',
                                        uuid: '123'
                                    },
                                    pageContainers: [
                                        {
                                            identifier: '123',
                                            uuid: '123',
                                            acceptTypes: '123',
                                            contentletsId: ['123']
                                        }
                                    ],
                                    contentlet: {
                                        identifier: '123'
                                    }
                                }
                            }
                        })
                    );

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
                                acceptTypes: '123',
                                contentletsId: []
                            }
                        ],
                        pageId: '123',
                        whenSaved: expect.any(Function)
                    });
                });
            });

            describe('add', () => {
                it('should trigger save when ng-event select-contentlet is dispatched', () => {
                    const saveMock = jest.spyOn(store, 'savePage');

                    spectator.detectChanges();

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'add-contentlet',
                                payload: {
                                    language_id: '1',
                                    pageContainers: [
                                        {
                                            identifier: 'test',
                                            uuid: 'test',
                                            contentletsId: []
                                        }
                                    ],
                                    container: {
                                        identifier: 'test',
                                        acceptTypes: 'test',
                                        uuid: 'test',
                                        contentletsId: [],
                                        maxContentlets: 1
                                    },
                                    pageId: 'test'
                                } as ActionPayload
                            }
                        })
                    );

                    spectator.detectChanges();

                    const dialogIframe = spectator.debugElement.query(
                        By.css('[data-testId="dialog-iframe"]')
                    );

                    spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

                    dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                        new CustomEvent('ng-event', {
                            detail: {
                                name: NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT,
                                data: {
                                    identifier: '123',
                                    inode: '123'
                                }
                            }
                        })
                    );

                    expect(saveMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                identifier: 'test',
                                uuid: 'test',
                                contentletsId: ['123'],
                                personaTag: undefined
                            }
                        ],
                        pageId: 'test',
                        whenSaved: expect.any(Function)
                    });

                    expect(saveMock).toHaveBeenCalled();
                });
            });

            describe('edit', () => {
                it('should open a dialog and send a post message when saving the contentlet', (done) => {
                    spectator.detectChanges();

                    const initiEditIframeDialogMock = jest.spyOn(store, 'initActionEdit');
                    const dialog = spectator.query(byTestId('dialog'));

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'edit-contentlet',
                                payload: {
                                    contentlet: {
                                        inode: '123',
                                        title: 'hello world'
                                    }
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
                    expect(initiEditIframeDialogMock).toHaveBeenCalledWith({
                        inode: '123',
                        title: 'hello world'
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

            describe('create', () => {
                it('should open a dialog, trigger a save from the store and send a post message when saving a contentlet', (done) => {
                    spectator.detectChanges();

                    const initAddIframeDialogMock = jest.spyOn(store, 'initActionAdd');
                    const savePageMock = jest.spyOn(store, 'savePage');

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: 'http://localhost:3000',
                            data: {
                                action: 'add-contentlet',
                                payload: {
                                    pageContainers: [
                                        {
                                            identifier: 'test',
                                            uuid: 'test',
                                            contentletsId: []
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
                                    pageId: 'test',
                                    language_id: 'test'
                                } as ActionPayload
                            }
                        })
                    );

                    spectator.detectChanges();
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
                                    contentlet: {
                                        identifier: '123',
                                        title: '123'
                                    }
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

                    expect(savePageMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                contentletsId: ['123'],
                                identifier: 'test',
                                uuid: 'test',
                                personaTag: undefined
                            }
                        ],
                        pageId: 'test',
                        whenSaved: expect.any(Function)
                    });

                    iframe.nativeElement.contentWindow.addEventListener(
                        'message',
                        (event: MessageEvent) => {
                            expect(event).toBeTruthy();
                            done();
                        }
                    );
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

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: HOST,
                            data: {
                                action: 'edit-contentlet',
                                payload: {
                                    contentlet: {
                                        inode: '123',
                                        title: 'some title'
                                    }
                                }
                            }
                        })
                    );

                    spectator.detectComponentChanges();

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
                    spectator.detectChanges();

                    const spinner = spectator.query(byTestId('spinner'));

                    expect(spinner).toBeTruthy();
                });

                it('should not show the spinner after iframe load', () => {
                    spectator.detectChanges();

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

                    spectator.detectChanges();

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
            it('should have a dialog for the actions iframe', () => {
                spectator.detectChanges();

                const dialog = spectator.query(byTestId('dialog'));

                expect(dialog).toBeTruthy();
                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: 'my.super.cool.website.xyz',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                contentlet: {
                                    inode: '123'
                                }
                            }
                        }
                    })
                );

                spectator.detectChanges();

                expect(dialog.getAttribute('ng-reflect-visible')).toBe('false');
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

            it('should trigger onIframeLoad when the dialog is opened', (done) => {
                spectator.detectChanges();

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: HOST,
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                contentlet: {
                                    inode: '123',
                                    title: 'some title'
                                }
                            }
                        }
                    })
                );

                spectator.detectChanges();

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

    describe('no queryParams', () => {
        beforeEach(() => {
            spectator = createComponent({
                queryParams: { language_id: undefined, url: undefined }
            });
            store = spectator.inject(EditEmaStore, true);
        });

        it('should initialize with default value', () => {
            const mockQueryParams = {
                language_id: 1,
                url: 'index',
                persona_id: 'modes.persona.no.persona'
            };
            jest.spyOn(store, 'load');
            spectator.detectChanges();
            expect(store.load).toHaveBeenCalledWith(mockQueryParams);
        });
    });
});
