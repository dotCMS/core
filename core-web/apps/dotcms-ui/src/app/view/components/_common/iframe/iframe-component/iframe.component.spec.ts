/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotSafeUrlPipe } from '@components/_common/iframe/pipes/dot-safe-url/dot-safe-url.pipe';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { DotRouterService, DotIframeService } from '@dotcms/data-access';
import { DotcmsEventsService, LoggerService, LoginService, StringUtils } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    DotcmsEventsServiceMock,
    LoginServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { IframeOverlayService } from './../service/iframe-overlay.service';
import { IframeComponent } from './iframe.component';

import { MockDotUiColorsService } from '../../../../../test/dot-test-bed';

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
    const dotcmsEventsService = new DotcmsEventsServiceMock();
    let dotRouterService: DotRouterService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [IframeComponent, MockDotLoadingIndicatorComponent],
            imports: [
                RouterTestingModule,
                DotOverlayMaskModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                DotSafeUrlPipe
            ],
            providers: [
                DotLoadingIndicatorService,
                IframeOverlayService,
                DotIframeService,
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotcmsEventsService, useValue: dotcmsEventsService },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                LoggerService,
                StringUtils
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(IframeComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        dotIframeService = TestBed.get(DotIframeService);
        dotUiColorsService = TestBed.get(DotUiColorsService);
        dotRouterService = TestBed.get(DotRouterService);
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
        expect(iframeEl.properties.srcdoc).toBe('');
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

        dotIframeService.run({ name: 'fakeFunction' });

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

        expect<any>(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
    });

    describe('bind iframe events', () => {
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

            expect<any>(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
        });
    });

    describe('iframe errors', () => {
        it('should logout on 401', () => {
            iframeEl.triggerEventHandler('load', {
                target: {
                    contentDocument: {
                        title: '401'
                    }
                }
            });

            expect(dotRouterService.doLogOut).toHaveBeenCalledTimes(1);
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

    it('should refresh OSGI Plugis list on OSGI_BUNDLES_LOADED websocket event', fakeAsync(() => {
        comp.iframeElement.nativeElement = {
            contentWindow: {
                getBundlesData: jasmine.createSpy('getBundlesData'),
                document: {
                    body: {
                        innerHTML: '<html></html>'
                    }
                }
            }
        };
        dotcmsEventsService.triggerSubscribeTo('OSGI_BUNDLES_LOADED', {
            name: 'OSGI_BUNDLES_LOADED'
        });
        tick(4500);
        expect(comp.iframeElement.nativeElement.contentWindow.getBundlesData).toHaveBeenCalledTimes(
            1
        );
    }));
});
