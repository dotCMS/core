import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DotContentletService } from '../../../../../api/services/dot-contentlet.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { IFrameModule } from '../index';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { SocketFactory, SiteService, LoginService } from 'dotcms-js/dotcms-js';
import { IframeComponent } from '../iframe-component';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';

describe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let dotIframe: IframeComponent;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotMenuService: DotMenuService;
    let dotRouterService: DotRouterService;
    let route: ActivatedRoute;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [],
                imports: [IFrameModule, RouterTestingModule],
                providers: [
                    DotContentletService,
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
            dotIframe = de.query(By.css('dot-iframe')).componentInstance;
            dotLoadingIndicatorService = de.injector.get(DotLoadingIndicatorService);
            dotMenuService = de.injector.get(DotMenuService);
            dotRouterService = de.injector.get(DotRouterService);
            route = de.injector.get(ActivatedRoute);
        })
    );

    it('should have dot-iframe component', () => {
        expect(fixture.debugElement.query(By.css('dot-iframe'))).toBeDefined();
    });

    it('should set query param url to the dot-iframe src', () => {
        route.params = Observable.of({ id: 'portlet-id' });
        route.queryParams = Observable.of({ url: 'hello/world' });

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

    it('should show loading indicator and go to edit page when event is emited by iframe', fakeAsync(() => {
        route.queryParams = Observable.of({ url: 'hello/world' });
        fixture.detectChanges();
        tick(); // There is a timeout to show the iframe in the dot-iframe component
        fixture.detectChanges();

        spyOn(dotLoadingIndicatorService, 'show');
        spyOn(dotRouterService, 'goToEditPage');

        const iframe = de.query(By.css('dot-iframe')).query(By.css('iframe')).nativeElement;

        dotIframe.load.emit({
            target: iframe
        });

        const customEvent = document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false,  {
            name: 'edit-page',
            data: {
                url: 'some/url'
            }
        });
        iframe.contentDocument.dispatchEvent(customEvent);

        expect(dotLoadingIndicatorService.show).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith('some/url');
    }));
});
