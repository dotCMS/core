import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DotContentletService } from '../../../../../api/services/dot-contentlet.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { IFrameModule } from '../index';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { SocketFactory, SiteService, LoginService } from 'dotcms-js/dotcms-js';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { DotIframeEventsHandler } from './services/iframe-events-handler.service';

describe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let dotIframe: DebugElement;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotMenuService: DotMenuService;
    let dotRouterService: DotRouterService;
    let dotIframeEventsHandler: DotIframeEventsHandler;
    let route: ActivatedRoute;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [],
            imports: [IFrameModule, RouterTestingModule],
            providers: [
                DotContentletService,
                DotIframeEventsHandler,
                DotMenuService,
                LoginService,
                SiteService,
                SocketFactory,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        parent: {
                            url: Observable.of([
                                {
                                    path: 'an-url'
                                }
                            ])
                        }
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(IframePortletLegacyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
        dotIframe = de.query(By.css('dot-iframe'));
        dotLoadingIndicatorService = de.injector.get(DotLoadingIndicatorService);
        dotMenuService = de.injector.get(DotMenuService);
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeEventsHandler = de.injector.get(DotIframeEventsHandler);
        route = de.injector.get(ActivatedRoute);
    }));

    it('should have dot-iframe component', () => {
        expect(fixture.debugElement.query(By.css('dot-iframe'))).toBeDefined();
    });

    it('should set query param url to the dot-iframe src', () => {
        route.queryParams = Observable.of({ url: 'hello/world' });
        route.params = Observable.of({ id: 'portlet-id' });

        let src: string;
        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(src).toEqual('hello/world');
    });

    it('should set router param id to the dot-iframe src', () => {
        route.queryParams = Observable.of({});
        route.params = Observable.of({ id: 'portlet-id' });

        spyOn(dotMenuService, 'getUrlById').and.returnValue(Observable.of('fake-url'));

        let src: string;

        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(dotMenuService.getUrlById).toHaveBeenCalledWith('portlet-id');
        expect(src).toEqual('fake-url');
    });

    it('should handle custom events', () => {
        spyOn(dotIframeEventsHandler, 'handle');

        dotIframe.triggerEventHandler('custom', {
            this: {
                is: 'a custom event'
            }
        });

        expect(dotIframeEventsHandler.handle).toHaveBeenCalledWith({
            this: {
                is: 'a custom event'
            }
        });
    });
});
