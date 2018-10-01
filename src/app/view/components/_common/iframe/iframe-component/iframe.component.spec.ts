import { RouterTestingModule } from '@angular/router/testing';
import { IframeOverlayService } from './../service/iframe-overlay.service';
import { DotLoadingIndicatorService } from './../dot-loading-indicator/dot-loading-indicator.service';
import { DotLoadingIndicatorComponent } from './../dot-loading-indicator/dot-loading-indicator.component';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { SafePipe } from './../../../../pipes/safe-url.pipe';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { IframeComponent } from './iframe.component';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotIframeService } from '../service/dot-iframe/dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';

describe('IframeComponent', () => {
    let comp: IframeComponent;
    let fixture: ComponentFixture<IframeComponent>;
    let de: DebugElement;
    let iframeEl: DebugElement;
    let dotIframeService: DotIframeService;
    let dotUiColorsService: DotUiColorsService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [IframeComponent, DotLoadingIndicatorComponent, SafePipe],
            imports: [RouterTestingModule],
            providers: [
                DotLoadingIndicatorService,
                IframeOverlayService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
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

        spyOn(dotUiColorsService, 'setColors');

        comp.isLoading = false;
        comp.src = 'etc/etc?hello=world';
        fixture.detectChanges();
        iframeEl = de.query(By.css('iframe'));
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
        const fakeHtmlEl = {
            hello: 'html'
        };

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
            fakeHtmlEl = {
                hello: 'html'
            };

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
            iframeEl.triggerEventHandler('load', {});

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
            iframeEl.triggerEventHandler('load', {});

            expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
        });
    });

    xit('should trigger keydown', () => {});
});
