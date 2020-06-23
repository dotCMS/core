import { RouterTestingModule } from '@angular/router/testing';
import { IframeOverlayService } from './../service/iframe-overlay.service';
import { DotLoadingIndicatorService } from './../dot-loading-indicator/dot-loading-indicator.service';
import { ComponentFixture, async } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { IframeComponent } from './iframe.component';
import { LoginService, DotcmsEventsService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotIframeService } from '../service/dot-iframe/dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotcmsEventsServiceMock } from '@tests/dotcms-events-service.mock';

const fakeHtmlEl = {
    hello: 'html'
};

@Component({
    selector: 'dot-loading-indicator',
    template: ''
})
class MockDotLoadingIndicatorComponent {}

describe('IframeComponent', () => {
    let comp: IframeComponent;
    let fixture: ComponentFixture<IframeComponent>;
    let de: DebugElement;
    let iframeEl: DebugElement;
    let dotIframeService: DotIframeService;
    let dotUiColorsService: DotUiColorsService;
    let dotcmsEventsService: DotcmsEventsServiceMock;
    let loginService: LoginService;

    dotcmsEventsService = new DotcmsEventsServiceMock();

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [IframeComponent, MockDotLoadingIndicatorComponent],
            imports: [RouterTestingModule, DotOverlayMaskModule],
            providers: [
                DotLoadingIndicatorService,
                IframeOverlayService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotcmsEventsService,
                    useValue: dotcmsEventsService
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(IframeComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        dotIframeService = de.injector.get(DotIframeService);
        dotUiColorsService = de.injector.get(DotUiColorsService);
        loginService = de.injector.get(LoginService);

        spyOn(dotUiColorsService, 'setColors');

        comp.isLoading = false;
        comp.src = 'etc/etc?hello=world';
        fixture.detectChanges();
        iframeEl = de.query(By.css('iframe'));
    });

    describe('trigger reload postMessage', () => {
        beforeEach(() => {
            comp.iframeElement.nativeElement = {
                location: {
                    reload: jasmine.createSpy('reload')
                },
                contentWindow: {
                    postMessage: jasmine.createSpy('postMessage'),
                    document: {
                        body: {
                            innerHTML: '<html></html>'
                        },
                        querySelector: () => fakeHtmlEl,
                        addEventListener: jasmine.createSpy('docAddEventListener'),
                        removeEventListener: jasmine.createSpy('docRemoveEventListener')
                    },
                    addEventListener: jasmine.createSpy('docAddEventListener'),
                    removeEventListener: jasmine.createSpy('docRemoveEventListener')
                }
            };
        });

        it('should reload on DELETE_BUNDLE and on publishing-queue portlet websocket event', () => {
            dotcmsEventsService.triggerSubscribeTo('DELETE_BUNDLE', {
                name: 'DELETE_BUNDLE'
            });
            expect(comp.iframeElement.nativeElement.contentWindow.postMessage).toHaveBeenCalledWith(
                'reload'
            );
        });

        it('should reload on PAGE_RELOAD websocket event', () => {
            dotcmsEventsService.triggerSubscribeTo('PAGE_RELOAD', {
                name: 'PAGE_RELOAD'
            });
            expect(comp.iframeElement.nativeElement.contentWindow.postMessage).toHaveBeenCalledWith(
                'reload'
            );
        });
    });

    it('should have iframe element', () => {
        expect(iframeEl).toBeTruthy();
    });

    it('should bind src to the iframe', () => {
        expect(iframeEl.properties.src).toContain('');
    });

    it('should reload iframe', () => {
        comp.iframeElement.nativeElement = {
            contentWindow: {
                document: {
                    body: {
                        innerHTML: '<html></html>'
                    }
                },
                location: {
                    reload: jasmine.createSpy('reload')
                }
            }
        };

        dotIframeService.reload();
        expect(
            comp.iframeElement.nativeElement.contentWindow.location.reload
        ).toHaveBeenCalledTimes(1);
    });

    it('should call function in the iframe window', () => {
        comp.iframeElement.nativeElement = {
            contentWindow: {
                fakeFunction: jasmine.createSpy('reload'),
                document: {
                    body: {
                        innerHTML: '<html></html>'
                    }
                }
            }
        };

        dotIframeService.run('fakeFunction');

        expect(comp.iframeElement.nativeElement.contentWindow.fakeFunction).toHaveBeenCalledTimes(
            1
        );
    });

    it('should reload colors', () => {
        comp.iframeElement = {
            nativeElement: {
                contentWindow: {
                    document: {
                        querySelector: () => fakeHtmlEl
                    }
                }
            }
        };

        dotIframeService.reloadColors();

        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
    });

    describe('bind iframe events', () => {
        let fakeHtmlEl;

        beforeEach(() => {
            comp.iframeElement.nativeElement = {
                contentWindow: {
                    document: {
                        body: {
                            innerHTML: '<html></html>'
                        },
                        querySelector: () => fakeHtmlEl,
                        addEventListener: jasmine.createSpy('docAddEventListener'),
                        removeEventListener: jasmine.createSpy('docRemoveEventListener')
                    },
                    addEventListener: jasmine.createSpy('addEventListener'),
                    removeEventListener: jasmine.createSpy('removeEventListener')
                }
            };
        });

        it('should remove and add listener on load', () => {
            iframeEl.triggerEventHandler('load', {
                target: {
                    contentDocument: {
                        title: ''
                    }
                }
            });

            expect(
                comp.iframeElement.nativeElement.contentWindow.removeEventListener
            ).toHaveBeenCalledWith('keydown', jasmine.any(Function));
            expect(
                comp.iframeElement.nativeElement.contentWindow.document.removeEventListener
            ).toHaveBeenCalledWith('ng-event', jasmine.any(Function));

            expect(
                comp.iframeElement.nativeElement.contentWindow.addEventListener
            ).toHaveBeenCalledWith('keydown', jasmine.any(Function));
            expect(
                comp.iframeElement.nativeElement.contentWindow.document.addEventListener
            ).toHaveBeenCalledWith('ng-event', jasmine.any(Function));
        });

        it('should set the colors to the jsp on load', () => {
            iframeEl.triggerEventHandler('load', {
                target: {
                    contentDocument: {
                        title: ''
                    }
                }
            });

            expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
        });
    });

    describe('iframe errors', () => {
        it('should logout on 401', () => {
            spyOn(loginService, 'logOutUser').and.callThrough();

            iframeEl.triggerEventHandler('load', {
                target: {
                    contentDocument: {
                        title: '401'
                    }
                }
            });

            expect(loginService.logOutUser).toHaveBeenCalledTimes(1);
        });
    });

    describe('dot-overlay-mask', () => {
        let iframeOverlayService: IframeOverlayService;
        beforeEach(() => {
            iframeOverlayService = de.injector.get(IframeOverlayService);
        });

        it('should not be present onload', () => {
            expect(de.query(By.css('dot-overlay-mask'))).toBeNull();
        });
        it('should show when the service emit true', () => {
            iframeOverlayService.$overlay.next(true);
            fixture.detectChanges();
            const dotOverlayMask = de.query(By.css('dot-overlay-mask'));
            expect(dotOverlayMask).toBeDefined();
        });

        it('should hide on click and call hide event', () => {
            comp.showOverlay = true;
            spyOn(iframeOverlayService, 'hide').and.callThrough();
            fixture.detectChanges();
            let dotOverlayMask = de.query(By.css('dot-overlay-mask'));
            dotOverlayMask.triggerEventHandler('click', {});
            fixture.detectChanges();

            dotOverlayMask = de.query(By.css('dot-overlay-mask'));
            expect(dotOverlayMask).toBeNull();
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });
    });
});
