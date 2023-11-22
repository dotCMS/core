import { describe } from '@jest/globals';
import { Spectator, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaComponent } from './dot-ema.component';
import { EditEmaStore } from './store/dot-ema.store';

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
    let spectator: Spectator<DotEmaComponent>;
    let store: EditEmaStore;
    let confirmationService: ConfirmationService;

    const createComponent = createRoutingFactory({
        component: DotEmaComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            EditEmaStore,
            ConfirmationService,
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world'
                            }
                        });
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
                queryParams: { language_id: '1', url: 'page-one' }
            });

            store = spectator.inject(EditEmaStore, true);
            confirmationService = spectator.inject(ConfirmationService, true);
        });

        it('should initialize with route query parameters', () => {
            const mockQueryParams = { language_id: '1', url: 'page-one' };

            jest.spyOn(store, 'load');

            spectator.detectChanges();

            expect(store.load).toHaveBeenCalledWith(mockQueryParams);
        });

        it('should update store and update the route on page change', () => {
            const router = spectator.inject(Router);

            jest.spyOn(store, 'setLanguage');
            jest.spyOn(router, 'navigate');

            spectator.detectChanges();

            spectator.triggerEventHandler('select[data-testId="language_id"]', 'change', {
                target: { name: 'language_id', value: '2' }
            });

            expect(store.setLanguage).toHaveBeenCalledWith('2');
            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: { language_id: '2' },
                queryParamsHandling: 'merge'
            });

            const iframe = spectator.query(byTestId('iframe'));
            expect(iframe).toHaveAttribute('src', 'http://localhost:3000/page-one?language_id=2');
        });

        it('should open a dialog when the iframe sends a postmessage with the edit-contenlet action', () => {
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
        });

        it('should open a confirm dialog and save on confirm when the iframe sends a postmessage with the delete-contenlet action', () => {
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
                            pageContainers: [],
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

            expect(saveMock).toHaveBeenCalled();
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

        it('should trigger save when ng-event select-contentlet is dispatched', () => {
            const saveMock = jest.spyOn(store, 'savePage');

            spectator.detectChanges();

            window.dispatchEvent(
                new MessageEvent('message', {
                    origin: 'http://localhost:3000',
                    data: {
                        action: 'add-contentlet',
                        payload: {
                            pageContainers: [],
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

            expect(saveMock).toHaveBeenCalled();
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

    describe('no queryParams', () => {
        beforeEach(() => {
            spectator = createComponent({
                queryParams: { language_id: undefined, url: undefined }
            });

            store = spectator.inject(EditEmaStore, true);
        });

        it('should initialize with default value', () => {
            const mockQueryParams = { language_id: '1', url: 'index' };

            jest.spyOn(store, 'load');

            spectator.detectChanges();

            expect(store.load).toHaveBeenCalledWith(mockQueryParams);
        });
    });
});
