import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DotMenuService } from '@services/dot-menu.service';
import { IFrameModule } from '../index';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
import { SiteService, LoginService, DotPushPublishDialogService } from 'dotcms-js';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { LoginServiceMock } from '@tests/login-service.mock';

const routeDatamock = {
    canAccessPortlet: true
};

class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }

    get parent() {
        return {
            url: of([
                {
                    path: 'an-url'
                }
            ])
        };
    }
}

describe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let dotIframe: DebugElement;
    let dotMenuService: DotMenuService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let route: ActivatedRoute;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [],
            imports: [IFrameModule, RouterTestingModule],
            providers: [
                DotContentTypeService,
                DotCustomEventHandlerService,
                DotPushPublishDialogService,
                DotMenuService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                SiteService,
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(IframePortletLegacyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotMenuService = de.injector.get(DotMenuService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        route = de.injector.get(ActivatedRoute);
    }));

    it('should set query param url to the dot-iframe src', () => {
        route.queryParams = of({ url: 'hello/world' });
        route.params = of({ id: 'portlet-id' });

        let src: string;
        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(src).toEqual('hello/world');
    });

    it('should set router param id to the dot-iframe src', () => {
        route.queryParams = of({});
        route.params = of({ id: 'portlet-id' });

        spyOn(dotMenuService, 'getUrlById').and.returnValue(of('fake-url'));

        let src: string;

        comp.url.subscribe((url) => {
            src = url;
        });

        fixture.detectChanges();

        expect(dotMenuService.getUrlById).toHaveBeenCalledWith('portlet-id');
        expect(src).toEqual('fake-url');
    });

    it('should handle custom events', () => {
        route.queryParams = of({ url: 'hello/world' });
        route.params = of({ id: 'portlet-id' });
        spyOn(dotCustomEventHandlerService, 'handle');
        fixture.detectChanges();

        dotIframe = de.query(By.css('dot-iframe'));

        dotIframe.triggerEventHandler('custom', {
            this: {
                is: 'a custom event'
            }
        });

        expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
            this: {
                is: 'a custom event'
            }
        });
    });

    it('should load Not Licensed component when no license and enterprise portlet ', () => {
        routeDatamock.canAccessPortlet = false;
        fixture.detectChanges();
        expect(de.query(By.css('dot-not-licensed-component'))).toBeTruthy();
    });
});
