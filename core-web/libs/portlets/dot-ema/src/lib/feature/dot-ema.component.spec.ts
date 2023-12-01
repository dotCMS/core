import { describe, expect } from '@jest/globals';
import { SpectatorRouting } from '@ngneat/spectator';
import { byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguagesServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaComponent } from './dot-ema.component';
import { EditEmaStore } from './store/dot-ema.store';

import { EmaLanguageSelectorComponent } from '../components/edit-ema-language-selector/edit-ema-language-selector.component';
import { DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { AddContentletPayload } from '../shared/models';

const messagesMock = {
    'editpage.content.contentlet.remove.confirmation_message.header': 'Deleting Content',
    'editpage.content.contentlet.remove.confirmation_message.message':
        'Are you sure you want to remove this content?',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Reject'
};

describe('DotEmaComponent', () => {
    let spectator: SpectatorRouting<DotEmaComponent>;
    let store: EditEmaStore;
    let confirmationService: ConfirmationService;

    const createComponent = createRoutingFactory({
        component: DotEmaComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            EditEmaStore,
            ConfirmationService,
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() },
            {
                provide: DotPageApiService,
                useValue: {
                    get({ language_id }) {
                        return {
                            2: of({
                                page: {
                                    title: 'hello world'
                                },
                                viewAs: {
                                    language: {
                                        id: 2,
                                        language: 'Spanish',
                                        countryCode: 'ES',
                                        languageCode: 'es',
                                        country: 'España'
                                    }
                                }
                            }),
                            1: of({
                                page: {
                                    title: 'hello world'
                                },
                                viewAs: {
                                    language: {
                                        id: 1,
                                        language: 'English',
                                        countryCode: 'US',
                                        languageCode: 'EN',
                                        country: 'United States'
                                    }
                                }
                            })
                        }[language_id];
                    },
                    save() {
                        return of({});
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

                spectator.triggerEventHandler(EmaLanguageSelectorComponent, 'selected', 2);
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 2 },
                    queryParamsHandling: 'merge'
                });
            });
        });

        it('should update the iframe url when the queryParams changes', () => {
            spectator.detectChanges();

            const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));

            spectator.triggerNavigation({
                url: [],
                queryParams: { language_id: 2, url: 'my-awesome-route' }
            });

            expect(iframe.nativeElement.src).toBe(
                'http://localhost:3000/my-awesome-route?language_id=2&com.dotmarketing.persona.id=modes.persona.no.persona'
            );
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
                            origin: 'http://localhost:3000',
                            data: {
                                action: 'delete-contentlet',
                                payload: {
                                    pageID: '123',
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
                                    contentletId: '123'
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
                        pageID: '123',
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
                            origin: 'http://localhost:3000',
                            data: {
                                action: 'add-contentlet',
                                payload: {
                                    pageContainers: [
                                        {
                                            identifier: 'test',
                                            acceptTypes: 'test',
                                            uuid: 'test',
                                            contentletsId: []
                                        }
                                    ],
                                    container: {
                                        identifier: 'test',
                                        acceptTypes: 'test',
                                        uuid: 'test',
                                        contentletsId: []
                                    },
                                    pageID: 'test'
                                } as AddContentletPayload
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
                                    identifier: '123'
                                }
                            }
                        })
                    );

                    spectator.detectChanges();

                    expect(saveMock).toHaveBeenCalledWith({
                        pageContainers: [
                            {
                                identifier: 'test',
                                acceptTypes: 'test',
                                uuid: 'test',
                                contentletsId: ['123']
                            }
                        ],
                        pageID: 'test',
                        whenSaved: expect.any(Function)
                    });
                });
            });

            describe('edit', () => {
                it('should open a dialog and send a post message when saving the contentlet', (done) => {
                    spectator.detectChanges();

                    const initiEditIframeDialogMock = jest.spyOn(store, 'initActionEdit');
                    const dialog = spectator.query(byTestId('dialog'));

                    window.dispatchEvent(
                        new MessageEvent('message', {
                            origin: 'http://localhost:3000',
                            data: {
                                action: 'edit-contentlet',
                                payload: {
                                    inode: '123',
                                    title: 'hello world'
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
                                name: NG_CUSTOM_EVENTS.CONTENTLET_UPDATED
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

            it('should not open a dialog when the iframe sends a postmessage with a different origin', () => {
                spectator.detectChanges();

                const dialog = spectator.query(byTestId('dialog'));

                window.dispatchEvent(
                    new MessageEvent('message', {
                        origin: 'my.super.cool.website.xyz',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                        origin: 'http://localhost:3000',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                            name: NG_CUSTOM_EVENTS.CONTENTLET_UPDATED,
                            data: {
                                identifier: '123'
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
                        origin: 'http://localhost:3000',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                        origin: 'http://localhost:3000',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                        origin: 'http://localhost:3000',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                                inode: '123'
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
                        origin: 'http://localhost:3000',
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
                        origin: 'http://localhost:3000',
                        data: {
                            action: 'edit-contentlet',
                            payload: {
                                inode: '123'
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
                            name: NG_CUSTOM_EVENTS.CONTENTLET_UPDATED,
                            data: {
                                identifier: '123'
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
