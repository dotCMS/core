import { describe } from '@jest/globals';
import { Spectator, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotEmaComponent } from './dot-ema.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/models';

describe('DotEmaComponent', () => {
    let spectator: Spectator<DotEmaComponent>;
    let store: EditEmaStore;

    const createComponent = createRoutingFactory({
        component: DotEmaComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            EditEmaStore,
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world'
                            }
                        });
                    }
                }
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

            spectator.detectChanges();

            expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
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
                        action: 'edit-contentlet-updated',
                        payload: {
                            inode: '123'
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

            spectator.dispatchFakeEvent(dialog, 'onHide');
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
