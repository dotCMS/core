import { of as observableOf, Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DotContentletService } from '@services/dot-contentlet/dot-contentlet.service';
import { DotMenuService } from '@services/dot-menu.service';
import { IFrameModule } from '../index';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
import { SocketFactory, SiteService, LoginService } from 'dotcms-js/dotcms-js';
import { DotIframeEventsHandler } from './services/iframe-events-handler.service';

describe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let dotIframe: DebugElement;
    let dotMenuService: DotMenuService;
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
                            url: observableOf([
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
        dotMenuService = de.injector.get(DotMenuService);
        dotIframeEventsHandler = de.injector.get(DotIframeEventsHandler);
        route = de.injector.get(ActivatedRoute);
    }));

    it('should have dot-iframe component', () => {
        expect(dotIframe).toBeDefined();
    });

    it('should set query param url to the dot-iframe src', () => {
        route.queryParams = observableOf({ url: 'hello/world' });
        route.params = observableOf({ id: 'portlet-id' });

        let src: string;
        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(src).toEqual('hello/world');
    });

    it('should set router param id to the dot-iframe src', () => {
        route.queryParams = observableOf({});
        route.params = observableOf({ id: 'portlet-id' });

        spyOn(dotMenuService, 'getUrlById').and.returnValue(observableOf('fake-url'));

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
