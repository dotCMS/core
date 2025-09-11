/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

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

import { DotUiColorsService } from '../../../../../api/services/dot-ui-colors/dot-ui-colors.service';
import { MockDotUiColorsService } from '../../../../../test/dot-test-bed';
import { DotOverlayMaskModule } from '../../dot-overlay-mask/dot-overlay-mask.module';
import { DotSafeUrlPipe } from '../pipes/dot-safe-url/dot-safe-url.pipe';

const fakeHtmlEl = {
    hello: 'html'
};

@Component({
    selector: 'dot-loading-indicator',
    template: '',
    standalone: false
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

        dotIframeService = TestBed.inject(DotIframeService);
        dotUiColorsService = TestBed.inject(DotUiColorsService);
        dotRouterService = TestBed.inject(DotRouterService);
        jest.spyOn(dotUiColorsService, 'setColors');

        comp.isLoading = false;
        comp.src = 'etc/etc?hello=world';
        fixture.detectChanges();
        iframeEl = de.query(By.css('iframe'));
    });

    describe('trigger reload postMessage', () => {
        beforeEach(() => {
            comp.iframeElement.nativeElement = {
                location: {
                    reload: jest.fn()
                },
                contentWindow: {
                    postMessage: jest.fn(),
                    document: {
                        body: {
                            innerHTML: '<html></html>'
                        },
                        querySelector: () => fakeHtmlEl,
                        addEventListener: jest.fn(),
                        removeEventListener: jest.fn()
                    },
                    addEventListener: jest.fn(),
                    removeEventListener: jest.fn()
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
                    reload: jest.fn()
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
                fakeFunction: jest.fn(),
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
                        addEventListener: jest.fn(),
                        removeEventListener: jest.fn()
                    },
                    addEventListener: jest.fn(),
                    removeEventListener: jest.fn()
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
            ).toHaveBeenCalledWith('keydown', expect.any(Function));
            expect(
                comp.iframeElement.nativeElement.contentWindow.document.removeEventListener
            ).toHaveBeenCalledWith('ng-event', expect.any(Function));

            expect(
                comp.iframeElement.nativeElement.contentWindow.addEventListener
            ).toHaveBeenCalledWith('keydown', expect.any(Function));
            expect(
                comp.iframeElement.nativeElement.contentWindow.document.addEventListener
            ).toHaveBeenCalledWith('ng-event', expect.any(Function));
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
            jest.spyOn(iframeOverlayService, 'hide');
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
                getBundlesData: jest.fn(),
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
